package com.threepointogames.movierecap.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object WatchHistoryManager {
    private const val PREF_NAME = "watch_history_prefs"
    private const val KEY_HISTORY = "watch_history_ids"
    private const val MAX_HISTORY_SIZE = 20

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveMovieId(context: Context, movieId: String) {
        val prefs = getPrefs(context)
        val currentList = getWatchedMovieIds(context).toMutableList()
        
        // Remove existing to move it to top
        currentList.remove(movieId)
        // Add to front (0 index is most recent)
        currentList.add(0, movieId)
        
        // Limit size
        if (currentList.size > MAX_HISTORY_SIZE) {
            currentList.removeAt(currentList.lastIndex)
        }

        val jsonString = Json.encodeToString(currentList)
        prefs.edit().putString(KEY_HISTORY, jsonString).apply()
    }

    fun getWatchedMovieIds(context: Context): List<String> {
        val prefs = getPrefs(context)
        val jsonString = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            Json.decodeFromString(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
