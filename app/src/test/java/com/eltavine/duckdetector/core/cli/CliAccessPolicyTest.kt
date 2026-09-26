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

import com.eltavine.duckdetector.core.evidence.DetectorStatus
import com.eltavine.duckdetector.core.evidence.InfoKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CliAccessPolicyTest {
    @Test
    fun allowsOnlyRootShellAndTheAppItself() {
        assertTrue(CliAccessPolicy.isAllowed(CliAccessPolicy.RootUid, ownUid = 10_321))
        assertTrue(CliAccessPolicy.isAllowed(CliAccessPolicy.ShellUid, ownUid = 10_321))
        assertTrue(CliAccessPolicy.isAllowed(callingUid = 10_321, ownUid = 10_321))
        assertFalse(CliAccessPolicy.isAllowed(callingUid = 10_555, ownUid = 10_321))
    }

    @Test
    fun anomalyFilterIncludesOnlyActionableResults() {
        assertTrue(CliSnapshotStore.isAnomaly(DetectorStatus.danger()))
        assertTrue(CliSnapshotStore.isAnomaly(DetectorStatus.warning()))
        assertTrue(CliSnapshotStore.isAnomaly(DetectorStatus.info(InfoKind.ERROR)))
        assertFalse(CliSnapshotStore.isAnomaly(DetectorStatus.info(InfoKind.SUPPORT)))
        assertFalse(CliSnapshotStore.isAnomaly(DetectorStatus.allClear()))
    }

    @Test
    fun parsesPublicCommandsAndKeepsRescanAlias() {
        assertEquals(CliCommand.HELP, CliCommand.parse("help"))
        assertEquals(CliCommand.STATUS, CliCommand.parse("status"))
        assertEquals(CliCommand.ANOMALIES, CliCommand.parse("anomalies"))
        assertEquals(CliCommand.REPORT, CliCommand.parse("report"))
        assertEquals(CliCommand.SCAN, CliCommand.parse("scan"))
        assertEquals(CliCommand.SCAN, CliCommand.parse("rescan"))
        assertEquals(CliCommand.SCAN, CliCommand.parse("SCAN"))
        assertNull(CliCommand.parse("unknown"))
    }

    @Test
    fun parsesReadEndpointsAndTheirMimeTypes() {
        assertEquals(CliEndpoint.HELP, CliEndpoint.parse("HELP"))
        assertEquals(CliEndpoint.STATUS, CliEndpoint.parse("status"))
        assertEquals(CliEndpoint.ANOMALIES, CliEndpoint.parse("anomalies"))
        assertEquals(CliEndpoint.REPORT, CliEndpoint.parse("report"))
        assertNull(CliEndpoint.parse("scan"))
        assertNull(CliEndpoint.parse(null))

        assertEquals(CliMimeType.JSON, CliEndpoint.parse("status")?.mimeType)
        assertEquals(CliMimeType.JSON, CliEndpoint.parse("anomalies")?.mimeType)
        assertEquals(CliMimeType.TEXT, CliEndpoint.parse("report")?.mimeType)
    }

    @Test
    fun acceptsOnlyTheReadOnlyContentProviderMode() {
        assertEquals(CliOpenMode.READ_ONLY, CliOpenMode.parse("r"))
        assertNull(CliOpenMode.parse("R"))
        assertNull(CliOpenMode.parse("w"))
        assertNull(CliOpenMode.parse("rw"))
    }
}
