package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

/** Body for POST /api/auth/version — phone reports its installed versionName on cold launch. */
@Serializable
data class AppVersionRequest(val version: String)
