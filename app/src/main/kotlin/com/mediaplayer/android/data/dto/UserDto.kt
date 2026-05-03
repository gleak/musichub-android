package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Long,
    val email: String? = null,
    val name: String? = null,
    /**
     * True once the user has any GENRE row in `user_taste` — either seeded
     * via the M14e onboarding tag picker or accumulated otherwise. The
     * Android client uses this to decide whether to route a fresh sign-in
     * through OnboardingScreen before AppScaffold.
     */
    val onboardingComplete: Boolean = false,
)
