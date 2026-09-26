/*
 * Copyright 2026 Duck Apps Contributor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eltavine.duckdetector.core.cli

import android.content.Context
import android.util.AtomicFile
import com.eltavine.duckdetector.BuildConfig
import com.eltavine.duckdetector.core.evidence.DetectionSeverity
import com.eltavine.duckdetector.core.evidence.DetectorStatus
import com.eltavine.duckdetector.core.evidence.InfoKind
import com.eltavine.duckdetector.core.localization.DisplayTextLocalizer
import com.eltavine.duckdetector.core.scan.DetectorSummary
import com.eltavine.duckdetector.core.ui.detector.DetectorSession
import com.eltavine.duckdetector.core.ui.detector.DeviceProfileSession
import com.eltavine.duckdetector.core.ui.presentation.formatBuildTimeUtc
import com.eltavine.duckdetector.features.dashboard.presentation.export.DashboardExport
import com.eltavine.duckdetector.features.dashboard.presentation.export.DashboardReportRenderer
import com.eltavine.duckdetector.features.dashboard.presentation.export.ExportHeader
import com.eltavine.duckdetector.features.dashboard.presentation.model.DashboardUiState
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CliSnapshotStore {
    private const val SchemaVersion = 1
    private const val DirectoryName = "adb_cli"
    private const val SnapshotFileName = "snapshot.json"
    private const val ReportFileName = "report.txt"

    fun persist(
        context: Context,
        state: DashboardUiState,
        summaries: List<DetectorSummary>,
        detectors: List<DetectorSession>,
        deviceProfile: DeviceProfileSession,
    ) {
        val appContext = context.applicationContext
        val localize: (String) -> String = { text ->
            DisplayTextLocalizer.translate(appContext, text)
        }
        val generatedAt = System.currentTimeMillis()
        val detectorItems = JSONArray()
        val anomalies = JSONArray()

        summaries.forEach { summary ->
            val item = JSONObject()
                .put("id", summary.id.value)
                .put("title", localize(summary.title))
                .put("severity", severityName(summary.status))
                .put("ready", summary.ready)
                .put("headline", localize(summary.headline))
                .put("summary", localize(summary.findingDetail ?: summary.summary))
            detectorItems.put(item)
            if (isAnomaly(summary.status)) {
                anomalies.put(JSONObject(item.toString()))
            }
        }

        val readyCount = summaries.count { it.ready }
        val dangerCount = summaries.count { it.status.severity == DetectionSeverity.DANGER }
        val warningCount = summaries.count { it.status.severity == DetectionSeverity.WARNING }
        val errorCount = summaries.count {
            it.status.severity == DetectionSeverity.INFO && it.status.infoKind == InfoKind.ERROR
        }
        val snapshot = JSONObject()
            .put("schema", SchemaVersion)
            .put("available", true)
            .put("generated_at_epoch_ms", generatedAt)
            .put("version_name", BuildConfig.VERSION_NAME)
            .put("version_code", BuildConfig.VERSION_CODE)
            .put("scanning", state.isLoading)
            .put(
                "overview",
                JSONObject()
                    .put("severity", severityName(state.overview.status))
                    .put("headline", localize(state.overview.headline))
                    .put("summary", localize(state.overview.summary)),
            )
            .put(
                "counts",
                JSONObject()
                    .put("detectors", summaries.size)
                    .put("ready", readyCount)
                    .put("pending", summaries.size - readyCount)
                    .put("danger", dangerCount)
                    .put("warning", warningCount)
                    .put("error", errorCount)
                    .put("anomalies", anomalies.length()),
            )
            .put("anomalies", anomalies)
            .put("detectors", detectorItems)

        synchronized(this) {
            writeAtomic(snapshotFile(appContext), snapshot.toString(2))
            if (!state.isLoading) {
                val detectorsById = detectors.associateBy { it.id }
                val reports = state.cardOrder.mapNotNull(detectorsById::get).map { it.report() }
                val export = DashboardExport(
                    header = ExportHeader(
                        versionName = BuildConfig.VERSION_NAME,
                        versionCode = BuildConfig.VERSION_CODE,
                        buildHash = BuildConfig.BUILD_HASH,
                        buildTime = formatBuildTimeUtc(BuildConfig.BUILD_TIME_UTC),
                        reportTime = SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss (z)",
                            Locale.US,
                        ).format(Date(generatedAt)),
                    ),
                    overview = state.overview,
                    topFindings = state.topFindings,
                    detectors = reports,
                    device = deviceProfile.report(),
                )
                writeAtomic(
                    reportFile(appContext),
                    DashboardReportRenderer.render(export, localize),
                )
            }
        }
    }

    fun markScanRequested(context: Context) {
        val appContext = context.applicationContext
        synchronized(this) {
            val snapshot = readSnapshot(appContext)
            snapshot
                .put("schema", SchemaVersion)
                .put("scanning", true)
                .put("scan_requested_at_epoch_ms", System.currentTimeMillis())
            writeAtomic(snapshotFile(appContext), snapshot.toString(2))
        }
    }

    fun readStatus(context: Context): String {
        val snapshot = readSnapshot(context.applicationContext)
        val counts = snapshot.optJSONObject("counts") ?: JSONObject()
        return JSONObject()
            .put("available", snapshot.optBoolean("available", false))
            .put("generated_at_epoch_ms", snapshot.optLong("generated_at_epoch_ms", 0L))
            .put("scan_requested_at_epoch_ms", snapshot.optLong("scan_requested_at_epoch_ms", 0L))
            .put("version_name", snapshot.optString("version_name", BuildConfig.VERSION_NAME))
            .put("version_code", snapshot.optInt("version_code", BuildConfig.VERSION_CODE))
            .put("scanning", snapshot.optBoolean("scanning", false))
            .put("overview", snapshot.optJSONObject("overview") ?: JSONObject.NULL)
            .put("counts", counts)
            .toString(2)
    }

    fun readAnomalies(context: Context): String {
        val snapshot = readSnapshot(context.applicationContext)
        val anomalies = snapshot.optJSONArray("anomalies") ?: JSONArray()
        return JSONObject()
            .put("available", snapshot.optBoolean("available", false))
            .put("generated_at_epoch_ms", snapshot.optLong("generated_at_epoch_ms", 0L))
            .put("scanning", snapshot.optBoolean("scanning", false))
            .put("count", anomalies.length())
            .put("items", anomalies)
            .toString(2)
    }

    fun reportFile(context: Context): File = File(cliDirectory(context), ReportFileName)

    internal fun isAnomaly(status: DetectorStatus): Boolean {
        return when (status.severity) {
            DetectionSeverity.DANGER,
            DetectionSeverity.WARNING -> true

            DetectionSeverity.INFO -> status.infoKind == InfoKind.ERROR
            DetectionSeverity.ALL_CLEAR -> false
        }
    }

    private fun severityName(status: DetectorStatus): String {
        return when (status.severity) {
            DetectionSeverity.DANGER -> "DANGER"
            DetectionSeverity.WARNING -> "WARNING"
            DetectionSeverity.ALL_CLEAR -> "CLEAR"
            DetectionSeverity.INFO -> if (status.infoKind == InfoKind.ERROR) "ERROR" else "INFO"
        }
    }

    private fun readSnapshot(context: Context): JSONObject {
        val file = snapshotFile(context)
        if (!file.isFile) {
            return JSONObject()
                .put("schema", SchemaVersion)
                .put("available", false)
                .put("scanning", false)
        }
        return runCatching { JSONObject(file.readText(Charsets.UTF_8)) }
            .getOrElse {
                JSONObject()
                    .put("schema", SchemaVersion)
                    .put("available", false)
                    .put("scanning", false)
                    .put("error", "snapshot_unreadable")
            }
    }

    private fun snapshotFile(context: Context): File = File(cliDirectory(context), SnapshotFileName)

    private fun cliDirectory(context: Context): File {
        return File(context.filesDir, DirectoryName).apply { mkdirs() }
    }

    private fun writeAtomic(file: File, value: String) {
        file.parentFile?.mkdirs()
        val atomicFile = AtomicFile(file)
        var output: FileOutputStream? = null
        try {
            output = atomicFile.startWrite()
            output.write(value.toByteArray(Charsets.UTF_8))
            atomicFile.finishWrite(output)
        } catch (error: Throwable) {
            output?.let(atomicFile::failWrite)
            throw error
        }
    }
}
