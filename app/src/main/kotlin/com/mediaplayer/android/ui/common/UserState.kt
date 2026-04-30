package com.mediaplayer.android.ui.common

import androidx.compose.runtime.compositionLocalOf
import com.mediaplayer.android.data.dto.UserDto

/**
 * Carries the current user's [UserDto] (including the {@code anonymous} flag) and a
 * callback to upgrade an anonymous session to Google sign-in. Screens read this to
 * branch UX between "guest" and "signed in" states without threading the user
 * through every nav callback.
 */
data class CurrentUser(
    val user: UserDto,
    val onSignIn: () -> Unit,
    val onSignOut: () -> Unit,
)

val LocalCurrentUser = compositionLocalOf<CurrentUser?> { null }

fun UserDto.displayInitial(): String {
    if (anonymous) return ""
    val source = name?.takeIf { it.isNotBlank() } ?: email ?: return "?"
    return source.trim().firstOrNull()?.uppercase() ?: "?"
}
