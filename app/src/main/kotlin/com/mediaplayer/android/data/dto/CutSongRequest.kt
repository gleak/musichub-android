package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

/**
 * Body for POST /api/songs/{id}/cut — slice the source MP3 into a new master row.
 *
 * [fadeEnabled] flips the backend pipeline from frame-aligned `-c copy`
 * (lossless) to a re-encoding path that applies a 0.5s fade-in/out. Costs
 * ~2-4s extra server CPU per cut; defaults off so the lossless fast path
 * stays the norm.
 */
@Serializable
data class CutSongRequest(
    val inMs: Long,
    val outMs: Long,
    val fadeEnabled: Boolean = false,
)
