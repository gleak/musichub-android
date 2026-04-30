package com.mediaplayer.android.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Long,
    val email: String? = null,
    val name: String? = null,
    val anonymous: Boolean = false,
)
