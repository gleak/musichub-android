package com.mediaplayer.android.data.local

import kotlinx.serialization.Serializable

/**
 * On-device-only playlist. Lives entirely in DataStore — never round-trips
 * the backend, never gets a positive song id assigned, never shares a token.
 * Tracks reference MediaStore `_ID`s as positive Longs (the same shape
 * `LocalTrack.id` carries); playback flips them through [LocalMediaResolver]
 * to negative synthetic ids before handing off to Media3.
 *
 * Snapshot semantics: when built from a folder we freeze the track list at
 * creation time. New files dropped into the folder later do NOT auto-join,
 * and files removed from disk are filtered at play time (the resolver returns
 * null and we drop them silently).
 */
@Serializable
data class LocalPlaylist(
    val id: String,
    val name: String,
    val trackIds: List<Long>,
    val createdAt: Long,
    val updatedAt: Long,
)
