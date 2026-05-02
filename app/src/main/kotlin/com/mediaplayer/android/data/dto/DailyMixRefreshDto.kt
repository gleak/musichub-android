package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class DailyMixRefreshDto(
    val userId: Long,
    val refreshed: Int,
)
