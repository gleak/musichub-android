package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class LyricLineDto(
    val positionMs: Long,
    val text: String,
)
