package com.mediaplayer.android.ui.common

import androidx.compose.runtime.compositionLocalOf
import com.mediaplayer.android.data.dto.UserDto

/**
 * Carries the current user's [UserDto] and a sign-out callback. Screens read
 * this to render account-aware bits (avatar initial, profile section) without
 * threading the user through every nav callback.
 */
data class CurrentUser(
    val user: UserDto,
    val onSignOut: () -> Unit,
)

val LocalCurrentUser = compositionLocalOf<CurrentUser?> { null }

fun UserDto.displayInitial(): String {
    val source = name?.takeIf { it.isNotBlank() } ?: email ?: return "?"
    return source.trim().firstOrNull()?.uppercase() ?: "?"
}
