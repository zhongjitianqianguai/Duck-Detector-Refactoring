/*
 * Copyright 2026 Duck Apps Contributor
 * If you have any questions, suggestions, or other inquiries, please email Eltavine <me@eltavine.com>.
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

package com.eltavine.duckdetector.features.dashboard.presentation.export

import com.eltavine.duckdetector.core.evidence.DetectionSeverity
import com.eltavine.duckdetector.core.report.DetectorReport
import com.eltavine.duckdetector.core.report.DeviceReport
import com.eltavine.duckdetector.core.report.ReportBlock
import com.eltavine.duckdetector.core.report.ReportFact
import com.eltavine.duckdetector.core.report.ReportRow
import com.eltavine.duckdetector.features.dashboard.presentation.model.DashboardFindingModel
import com.eltavine.duckdetector.features.dashboard.presentation.model.DashboardOverviewMetric
import com.eltavine.duckdetector.features.dashboard.presentation.model.DashboardOverviewModel

/** Values the report banner shows, already formatted for display by the caller. */
data class ExportHeader(
    val versionName: String,
    val versionCode: Int,
    val buildHash: String,
    val buildTime: String,
    val reportTime: String,
)

data class DashboardExport(
    val header: ExportHeader,
    val overview: DashboardOverviewModel,
    val topFindings: List<DashboardFindingModel>,
    val detectors: List<DetectorReport>,
    val device: DeviceReport,
)

/**
 * Renders the plain-text scan report.
 *
 * The renderer owns the report envelope and the generic block layout; each detector owns the
 * content of its own [DetectorReport], so no detector-specific knowledge lives here.
 */
object DashboardReportRenderer {

    fun render(
        export: DashboardExport,
        localize: (String) -> String = { it },
    ): String = buildString {
        // Localize leaf values before labels, indentation and inline details are joined.
        // The runtime catalog cannot reliably match those formatted composite lines.
        appendBanner(export, localize)
        appendExecutiveSummary(export.overview, localize)
        appendTopFindings(export.topFindings, localize)
        export.detectors.forEach { appendDetectorReport(it, localize) }
        appendLine()
        appendDeviceReport(export.device, localize)
        appendFooter(localize)
    }

    private fun StringBuilder.appendBanner(export: DashboardExport, localize: (String) -> String) {
        val identity = export.device.identity
        val header = export.header
        appendLine("================================================================================")
        appendLine("                      ${localize("DUCK DETECTOR — SECURITY SCAN REPORT")}                      ")
        appendLine("================================================================================")
        if (!identity.brand.isNullOrBlank() && !identity.model.isNullOrBlank()) {
            val osStr = if (!identity.androidRelease.isNullOrBlank()) {
                " (Android ${identity.androidRelease}, API ${identity.sdk ?: "unknown"})"
            } else {
                ""
            }
            appendLine("  ${localize("Target Device")}  : ${identity.brand} ${identity.model}$osStr")
        }
        appendLine("  ${localize("App Version")}    : ${header.versionName} (Build ${header.versionCode})")
        appendLine("  ${localize("Build Commit")}   : ${header.buildHash}")
        appendLine("  ${localize("Build Time")}     : ${header.buildTime} (UTC)")
        appendLine("  ${localize("Report Time")}    : ${header.reportTime}")
        appendLine()
    }

    private fun StringBuilder.appendExecutiveSummary(overview: DashboardOverviewModel, localize: (String) -> String) {
        appendLine("--------------------------------------------------------------------------------")
        appendLine("  ${localize("EXECUTIVE SUMMARY")}")
        appendLine("--------------------------------------------------------------------------------")
        val badge = localize(severityBadge(overview.status.severity))
        appendLine("  ${localize("Overall Status")} : $badge ${localize(overview.headline)}")
        appendLine("  ${localize("Summary")}        : ${localize(overview.summary)}")
        appendLine()
        appendLine("  ${localize("Scan Statistics:")}")
        overview.metrics.forEach { metric ->
            val icon = when (metric.metric) {
                DashboardOverviewMetric.DANGER -> "[✖]"
                DashboardOverviewMetric.WARNING -> "[▲]"
                DashboardOverviewMetric.READY -> "[✔]"
                DashboardOverviewMetric.PENDING -> "[·]"
            }
            appendLine("    $icon ${localize(metric.label).padEnd(9)}: ${metric.value}")
        }
        appendLine()
    }

    private fun StringBuilder.appendTopFindings(findings: List<DashboardFindingModel>, localize: (String) -> String) {
        appendLine("--------------------------------------------------------------------------------")
        appendLine("  ${localize("TOP FINDINGS")}")
        appendLine("--------------------------------------------------------------------------------")
        if (findings.isEmpty()) {
            appendLine("  ${localize("(No security threats or warnings detected)")}")
        } else {
            findings.forEach { finding ->
                val badge = localize(severityBadge(finding.status.severity))
                appendLine("  $badge ${localize(finding.detectorTitle)}")
                appendLine("    ${localize("Headline")} : ${localize(finding.headline)}")
                if (finding.detail.isNotBlank() && finding.detail != finding.headline) {
                    appendLine("    ${localize("Details")}  :")
                    finding.detail.trim().lines().map { it.trimEnd() }.filter { it.isNotBlank() }.forEach { line ->
                        appendLine("      ${localize(line)}")
                    }
                }
                appendLine()
            }
        }
    }

    private fun StringBuilder.appendDetectorReport(report: DetectorReport, localize: (String) -> String) {
        appendLine()
        appendLine("--------------------------------------------------------------------------------")
        appendLine("  ${localize(severityBadge(report.severity))} ${localize(report.title)}")
        appendLine("  ${localize("Verdict")}      : ${localize(report.verdict)}")
        appendQuickFacts(report.quickFacts, localize)
        report.blocks.forEach { block ->
            when (block) {
                is ReportBlock.Rows -> appendRows(block.title, block.rows, localize)
                is ReportBlock.Bullets -> appendBullets(block.title, block.items, localize)
                is ReportBlock.Verbatim -> appendVerbatim(block, localize)
            }
        }
    }

    private fun StringBuilder.appendQuickFacts(facts: List<ReportFact>, localize: (String) -> String) {
        if (facts.isEmpty()) return
        val formatted = facts.joinToString("  |  ") { "${localize(it.label)}: ${localize(it.value)}" }
        appendLine("  ${localize("Quick Facts")}  : $formatted")
    }

    private fun StringBuilder.appendRows(title: String, rows: List<ReportRow>, localize: (String) -> String) {
        if (rows.isEmpty()) return
        appendLine()
        appendLine("  [${localize(title)}]")
        rows.forEach { appendRow(it, localize) }
    }

    private fun StringBuilder.appendRow(row: ReportRow, localize: (String) -> String) {
        val label = localize(row.label)
        val trimmedValue = row.value.trim()
        val displayValue = localize(trimmedValue)
        val hasValue = trimmedValue.isNotBlank()
        val trimmedDetail = row.detail?.trim()
        val hasDetail = !trimmedDetail.isNullOrBlank() && trimmedDetail != trimmedValue

        if (!hasValue && !hasDetail) {
            appendLine("    • $label")
            return
        }

        if (!hasValue && hasDetail) {
            appendLine("    • $label")
            appendDetailLines(trimmedDetail, localize)
            return
        }

        if (!hasDetail) {
            appendLine("    • $label: $displayValue")
            return
        }

        val detailLines = trimmedDetail.lines().map { it.trimEnd() }.filter { it.isNotBlank() }
        if (detailLines.size == 1 && detailLines[0].length <= 60 && !detailLines[0].contains(" = ") && !detailLines[0].contains(" | ")) {
            appendLine("    • $label: $displayValue (${localize(detailLines[0])})")
        } else {
            appendLine("    • $label: $displayValue")
            detailLines.forEach { line ->
                appendLine("        ${localize(line)}")
            }
        }
    }

    private fun StringBuilder.appendDetailLines(detail: String, localize: (String) -> String) {
        detail.lines().map { it.trimEnd() }.filter { it.isNotBlank() }.forEach { line ->
            appendLine("        ${localize(line)}")
        }
    }

    private fun StringBuilder.appendBullets(title: String, items: List<String>, localize: (String) -> String) {
        if (items.isEmpty()) return
        appendLine()
        appendLine("  [${localize(title)}]")
        items.forEach { text ->
            val lines = text.trim().lines().map { it.trimEnd() }.filter { it.isNotBlank() }
            if (lines.isNotEmpty()) {
                appendLine("    • ${localize(lines.first())}")
                lines.drop(1).forEach { line ->
                    appendLine("      ${localize(line)}")
                }
            }
        }
    }

    private fun StringBuilder.appendVerbatim(block: ReportBlock.Verbatim, localize: (String) -> String) {
        appendLine()
        appendLine("  [${localize(block.title)}]")
        block.lines.forEach { appendLine(localize(it)) }
    }

    private fun StringBuilder.appendDeviceReport(device: DeviceReport, localize: (String) -> String) {
        appendLine()
        appendLine("================================================================================")
        appendLine("  ${localize("DEVICE & SYSTEM SPECIFICATIONS")}")
        appendLine("================================================================================")
        appendQuickFacts(device.quickFacts, localize)
        device.sections.forEach { appendVerbatim(it, localize) }
    }

    private fun StringBuilder.appendFooter(localize: (String) -> String) {
        appendLine()
        appendLine("================================================================================")
        appendLine("                                 ${localize("END OF REPORT")}                                  ")
        appendLine("================================================================================")
    }

    private fun severityBadge(severity: DetectionSeverity): String = when (severity) {
        DetectionSeverity.DANGER -> "[✖ DANGER]"
        DetectionSeverity.WARNING -> "[▲ WARNING]"
        DetectionSeverity.INFO -> "[ℹ INFO]"
        DetectionSeverity.ALL_CLEAR -> "[✔ CLEAR]"
    }
}
