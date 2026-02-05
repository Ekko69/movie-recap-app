package com.threepointogames.movierecap.data

import android.content.Context
import com.threepointogames.movierecap.model.Movie
import com.threepointogames.movierecap.model.MovieResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MovieRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getMovies(): List<Movie> = withContext(Dispatchers.IO) {
        try {
            // Fetch from Firebase Realtime Database
            val url = "https://movie-recap-1306e-default-rtdb.firebaseio.com/movies.json"
            val jsonString = java.net.URL(url).readText()
            
            // Firebase might return null for empty nodes
            if (jsonString == "null") return@withContext emptyList()

            // Parse the map-based structure from Firebase
            try {
                // Try parsing as MovieResponse with map structure
                val response = json.decodeFromString<MovieResponse>(jsonString)
                return@withContext response.movies.values.toList()
            } catch (e: Exception) {
                // Fallback: Try parsing as direct map
                try {
                    val moviesMap = json.decodeFromString<Map<String, Movie>>(jsonString)
                    return@withContext moviesMap.values.toList()
                } catch (e2: Exception) {
                    throw e2 // Re-throw to see the actual error in UI
                }
            }
        } catch (e: Exception) {
            throw e // Re-throw to see the actual error in UI
        }
    }


    suspend fun getComments(movieId: String): List<com.threepointogames.movierecap.model.Comment> = withContext(Dispatchers.IO) {
        try {
            val url = "https://movie-recap-1306e-default-rtdb.firebaseio.com/comments/$movieId.json"
            val jsonString = java.net.URL(url).readText()
            if (jsonString == "null") return@withContext emptyList()
            
            // Firebase returns Map<ID, Comment>
            try {
                val commentsMap = json.decodeFromString<Map<String, com.threepointogames.movierecap.model.Comment>>(jsonString)
                return@withContext commentsMap.map { (key, value) -> 
                    value.copy(id = key) 
                }.sortedByDescending { it.timestamp }
            } catch (e: Exception) {
                // If parsing fails (e.g. structure change), return empty
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addComment(movieId: String, text: String, userName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "https://movie-recap-1306e-default-rtdb.firebaseio.com/comments/$movieId.json"
            val comment = com.threepointogames.movierecap.model.Comment(
                id = "", // Firebase generate ID
                movieId = movieId,
                userName = userName,
                text = text,
                timestamp = System.currentTimeMillis()
            )
            
            val jsonBody = json.encodeToString(comment)
            
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            
            connection.outputStream.use { os ->
                val input = jsonBody.toByteArray(java.nio.charset.StandardCharsets.UTF_8)
                os.write(input, 0, input.size)
            }
            
            val responseCode = connection.responseCode
            return@withContext responseCode in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun addReply(movieId: String, commentId: String, text: String, userName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "https://movie-recap-1306e-default-rtdb.firebaseio.com/comments/$movieId/$commentId/replies.json"
            val reply = com.threepointogames.movierecap.model.Reply(
                id = "", 
                userName = userName,
                text = text,
                timestamp = System.currentTimeMillis()
            )
            
            val jsonBody = json.encodeToString(reply)
            
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            
            connection.outputStream.use { os ->
                val input = jsonBody.toByteArray(java.nio.charset.StandardCharsets.UTF_8)
                os.write(input, 0, input.size)
            }
            
            val responseCode = connection.responseCode
            return@withContext responseCode in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
