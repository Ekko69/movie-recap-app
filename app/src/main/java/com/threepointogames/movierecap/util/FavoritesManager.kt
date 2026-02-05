package com.threepointogames.movierecap.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object FavoritesManager {
    private const val PREF_NAME = "favorites_prefs"
    private const val KEY_FAVORITES = "favorite_movie_ids"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun addFavorite(context: Context, movieId: String) {
        val prefs = getPrefs(context)
        val currentList = getFavoriteMovieIds(context).toMutableList()
        
        if (!currentList.contains(movieId)) {
            currentList.add(0, movieId) // Add to top
            val jsonString = Json.encodeToString(currentList)
            prefs.edit().putString(KEY_FAVORITES, jsonString).apply()
        }
    }

    fun removeFavorite(context: Context, movieId: String) {
        val prefs = getPrefs(context)
        val currentList = getFavoriteMovieIds(context).toMutableList()
        
        if (currentList.contains(movieId)) {
            currentList.remove(movieId)
            val jsonString = Json.encodeToString(currentList)
            prefs.edit().putString(KEY_FAVORITES, jsonString).apply()
        }
    }

    fun isFavorite(context: Context, movieId: String): Boolean {
        return getFavoriteMovieIds(context).contains(movieId)
    }

    fun getFavoriteMovieIds(context: Context): List<String> {
        val prefs = getPrefs(context)
        val jsonString = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
        return try {
            Json.decodeFromString(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
