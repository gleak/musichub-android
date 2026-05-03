package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

/**
 * Body for `PATCH /api/playlists/{id}/auto-sync`. Single boolean — when
 * true the client downloads any missing songs on cold launch.
 */
@Serializable
data class SetAutoSyncRequest(val enabled: Boolean)
