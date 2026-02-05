package com.threepointogames.movierecap.model

import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: String = "",
    val movieId: String,
    val userName: String,
    val text: String,
    val timestamp: Long,
    val replies: Map<String, Reply> = emptyMap()
)
