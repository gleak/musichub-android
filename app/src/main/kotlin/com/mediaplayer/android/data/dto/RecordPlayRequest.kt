package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecordPlayRequest(
    val songId: Long,
    val durationListenedMs: Long,
    val completionRatio: Double? = null,
    val wasSkipped: Boolean? = null,
)
