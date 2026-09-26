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

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.Process
import com.eltavine.duckdetector.MainActivity
import java.io.FileNotFoundException

class CliContentProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        enforceAdbCaller()
        val appContext = requireNotNull(context).applicationContext
        return when (CliCommand.parse(method)) {
            CliCommand.HELP -> response(CliCommand.HELP.wireName, CliContract.HelpText)
            CliCommand.STATUS -> response(
                CliCommand.STATUS.wireName,
                CliSnapshotStore.readStatus(appContext),
            )
            CliCommand.ANOMALIES -> response(
                CliCommand.ANOMALIES.wireName,
                CliSnapshotStore.readAnomalies(appContext),
            )
            CliCommand.REPORT -> response(
                CliCommand.REPORT.wireName,
                "Use: adb shell content read --uri ${CliContract.BaseUri}/report",
            )
            CliCommand.SCAN -> {
                val requestId = System.currentTimeMillis()
                CliSnapshotStore.markScanRequested(appContext)
                val intent = Intent(appContext, MainActivity::class.java)
                    .setAction(CliContract.ActionScan)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                appContext.startActivity(intent)
                Bundle().apply {
                    putBoolean("ok", true)
                    putString("command", CliCommand.SCAN.wireName)
                    putLong("request_id", requestId)
                    putString("output", "扫描已启动；请轮询 ${CliContract.BaseUri}/status")
                }
            }
            null -> Bundle().apply {
                putBoolean("ok", false)
                putString("command", method)
                putString("error", "unknown_command")
                putString("output", CliContract.HelpText)
            }
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        enforceAdbCaller()
        if (CliOpenMode.parse(mode) != CliOpenMode.READ_ONLY) {
            throw FileNotFoundException("CLI endpoints are read-only")
        }
        val appContext = requireNotNull(context).applicationContext
        return when (CliEndpoint.parse(uri.pathSegments.singleOrNull())) {
            CliEndpoint.HELP -> pipeText(CliContract.HelpText)
            CliEndpoint.STATUS -> pipeText(CliSnapshotStore.readStatus(appContext))
            CliEndpoint.ANOMALIES -> pipeText(CliSnapshotStore.readAnomalies(appContext))
            CliEndpoint.REPORT -> {
                val report = CliSnapshotStore.reportFile(appContext)
                if (!report.isFile) {
                    pipeText("尚无扫描报告。请先执行 scan，并等待 status 中 scanning=false。\n")
                } else {
                    ParcelFileDescriptor.open(report, ParcelFileDescriptor.MODE_READ_ONLY)
                }
            }
            null -> throw FileNotFoundException("Unknown CLI path: $uri")
        }
    }

    override fun getType(uri: Uri): String =
        CliEndpoint.parse(uri.lastPathSegment)?.mimeType?.wireName ?: CliMimeType.TEXT.wireName

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        enforceAdbCaller()
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        enforceAdbCaller()
        throw UnsupportedOperationException("Duck Detector CLI is read-only")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        enforceAdbCaller()
        throw UnsupportedOperationException("Duck Detector CLI is read-only")
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int {
        enforceAdbCaller()
        throw UnsupportedOperationException("Duck Detector CLI is read-only")
    }

    private fun enforceAdbCaller() {
        val callingUid = Binder.getCallingUid()
        if (!CliAccessPolicy.isAllowed(callingUid, Process.myUid())) {
            throw SecurityException("Duck Detector CLI only accepts ADB shell or Root callers")
        }
    }

    private fun response(command: String, output: String): Bundle = Bundle().apply {
        putBoolean("ok", true)
        putString("command", command)
        putString("output", output)
    }

    private fun pipeText(value: String): ParcelFileDescriptor {
        val pipe = ParcelFileDescriptor.createPipe()
        Thread({
            ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { output ->
                output.write(value.toByteArray(Charsets.UTF_8))
                if (!value.endsWith('\n')) output.write('\n'.code)
            }
        }, "duck-cli-output").start()
        return pipe[0]
    }
}
