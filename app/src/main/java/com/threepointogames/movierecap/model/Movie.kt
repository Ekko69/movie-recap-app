package com.threepointogames.movierecap.model

import kotlinx.serialization.Serializable

@Serializable
data class MovieResponse(
    val movies: Map<String, Movie>
)

@Serializable
data class Movie(
    val id: String,
    val videoURL: String,
    val thumbnail: String? = null,
    val featuredThumbnail: String? = null,
    val title: String,
    @Serializable(with = IntOrStringSerializer::class)
    val year: Int,
    val duration: String,
    val categories: List<String>,
    val description: String,
    val subtitleURL: String? = null,
    val subtitleDelay: Long = 0, // Delay in milliseconds (positive = delay, negative = advance)
    val updatedAt: Long? = null
)

object IntOrStringSerializer : kotlinx.serialization.KSerializer<Int> {
    override val descriptor = kotlinx.serialization.descriptors.PrimitiveSerialDescriptor("IntOrString", kotlinx.serialization.descriptors.PrimitiveKind.INT)

    override fun serialize(encoder: kotlinx.serialization.encoding.Encoder, value: Int) {
        encoder.encodeInt(value)
    }

    override fun deserialize(decoder: kotlinx.serialization.encoding.Decoder): Int {
        val jsonInput = decoder as? kotlinx.serialization.json.JsonDecoder 
            ?: throw kotlinx.serialization.SerializationException("Expected JsonDecoder")
        val element = jsonInput.decodeJsonElement()
        return if (element is kotlinx.serialization.json.JsonPrimitive) {
            if (element.isString) {
                element.content.toIntOrNull() ?: 0
            } else {
                try {
                    element.content.toInt()
                } catch (e: NumberFormatException) {
                    0
                }
            }
        } else {
             0 
        }
    }
}
