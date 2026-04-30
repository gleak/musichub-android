package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReinitStatusDto(
    val status: String,
    val error: String = "",
)
