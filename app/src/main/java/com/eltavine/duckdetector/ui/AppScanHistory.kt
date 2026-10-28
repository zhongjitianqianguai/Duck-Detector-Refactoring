package com.eltavine.duckdetector.ui

import android.content.Context
import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eltavine.duckdetector.R
import com.eltavine.duckdetector.core.localization.DisplayTextLocalizer
import com.eltavine.duckdetector.ui.scan.history.ScanHistoryJournal
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.IOException

@Composable
internal fun rememberAppScanHistory(context: Context): ScanHistoryJournal {
    val journal = remember(context) {
        AppScanHistory.get(File(context.noBackupFilesDir, "scan-history/records.bin"))
    }
    LaunchedEffect(journal) {
        try {
            journal.load()
        } catch (error: IOException) {
            Log.e("ScanHistory", "Failed to load history at startup", error)
        }
    }
    return journal
}

@Composable
internal fun AppScanHistoryIssue(journal: ScanHistoryJournal) {
    val context = LocalContext.current
    val issue by journal.issue.collectAsStateWithLifecycle()
    var dismissed by rememberSaveable { mutableStateOf<String?>(null) }
    val current = issue ?: return
    val id = current.toString()
    if (id == dismissed) return
    AlertDialog(
        onDismissRequest = { dismissed = id },
        title = { Text(stringResource(R.string.scan_history_issue_title)) },
        text = {
            Text(when (current) {
                is ScanHistoryJournal.Issue.Recovered -> stringResource(
                    R.string.scan_history_recovered, current.discardedRecords, current.backupName,
                )
                is ScanHistoryJournal.Issue.Failed -> stringResource(
                    R.string.scan_history_failed, DisplayTextLocalizer.translate(context, current.reason),
                )
            })
        },
        confirmButton = {
            TextButton(onClick = { dismissed = id }) { Text(stringResource(android.R.string.ok)) }
        },
    )
}

private object AppScanHistory {
    private var instance: ScanHistoryJournal? = null

    @Synchronized
    fun get(file: File): ScanHistoryJournal = instance ?: ScanHistoryJournal(file).also { instance = it }
}
