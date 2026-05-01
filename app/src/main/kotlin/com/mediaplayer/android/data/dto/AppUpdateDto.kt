package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

/** Mirror of `com.mediaplayer.backend.update.AppUpdateDto`. */
@Serializable
data class AppUpdateDto(
    val version: String,
    val versionCode: Int,
    val url: String,
    val sha256: String = "",
    val releaseNotes: String = "",
    val required: Boolean = false,
)
