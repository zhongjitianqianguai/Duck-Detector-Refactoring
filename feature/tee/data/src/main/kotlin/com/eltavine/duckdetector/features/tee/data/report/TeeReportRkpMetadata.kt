package com.eltavine.duckdetector.features.tee.data.report

import com.eltavine.duckdetector.features.tee.domain.TeeRkpState

internal fun rkpBuildMetadataValue(rkp: TeeRkpState): String = buildString {
    when {
        !rkp.serverSigned -> append("Unavailable: provisioning info was not authenticated by the local chain.")
        rkp.buildMetadata != null -> {
            append("Provisioning key 5 (CBOR): ")
            append(rkp.buildMetadata)
        }
        rkp.provisioningInfoIssue != null -> append("Unavailable: provisioning info was not fully decoded.")
        else -> append("Provisioning key 5 was not included in this certificate.")
    }
    rkp.provisioningInfoIssue?.let {
        // Separate human-readable issues from raw CBOR so localization preserves its notation.
        append("\n")
        append(it.replace(". ", ".\n"))
    }
    append("\nInformational only: approval fields have undocumented semantics. ")
    append("CTS status cannot be determined from these fields, including on RKP V3.")
}
