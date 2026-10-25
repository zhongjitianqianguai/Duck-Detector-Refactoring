package com.eltavine.duckdetector.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.eltavine.duckdetector.core.cli.CliSnapshotStore
import com.eltavine.duckdetector.core.scan.DetectorSummary
import com.eltavine.duckdetector.core.ui.detector.DetectorSession
import com.eltavine.duckdetector.core.ui.detector.DeviceProfileSession
import com.eltavine.duckdetector.features.dashboard.presentation.model.DashboardUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun AppCliSnapshotEffects(
    context: Context,
    requestId: Long,
    sessions: List<DetectorSession>,
    state: DashboardUiState,
    summaries: List<DetectorSummary>,
    detectors: List<DetectorSession>,
    deviceProfile: DeviceProfileSession,
) {
    LaunchedEffect(requestId) {
        if (requestId <= 0L) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            CliSnapshotStore.markScanRequested(context)
        }
        sessions.forEach { detector -> detector.rescan() }
    }
    LaunchedEffect(state, summaries) {
        withContext(Dispatchers.IO) {
            CliSnapshotStore.persist(
                context = context,
                state = state,
                summaries = summaries,
                detectors = detectors,
                deviceProfile = deviceProfile,
            )
        }
    }
}
