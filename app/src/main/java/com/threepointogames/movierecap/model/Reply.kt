package com.threepointogames.movierecap.model

import kotlinx.serialization.Serializable

@Serializable
data class Reply(
    val id: String = "",
    val userName: String,
    val text: String,
    val timestamp: Long
)
