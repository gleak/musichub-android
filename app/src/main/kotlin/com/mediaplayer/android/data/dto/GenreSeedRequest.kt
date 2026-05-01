package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

/** Onboarding tag picks. Backend records each as a UserTaste(GENRE, key, weight=5). */
@Serializable
data class GenreSeedRequest(val genres: List<String>)
