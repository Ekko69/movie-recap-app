package com.threepointogames.movierecap.util

import android.content.Context
import android.content.SharedPreferences

object UserManager {
    private const val PREF_NAME = "user_prefs"
    private const val KEY_USERNAME = "username"

    private val adjectives = listOf(
        "Happy", "Brave", "Calm", "Eager", "Fancy", "Jollie", 
        "Kind", "Lively", "Nice", "Proud", "Silly", "Witty", 
        "Zesty", "Neon", "Super", "Cyber", "Mega", "Ultra"
    )
    private val animals = listOf(
        "Badger", "Bear", "Cat", "Dog", "Eagle", "Fox", 
        "Goose", "Hawk", "Lion", "Owl", "Panda", "Tiger", 
        "Wolf", "Shark", "Whale", "Dino", "Raptor", "Cobra"
    )

    fun getUsername(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var username = prefs.getString(KEY_USERNAME, null)
        
        if (username == null) {
            username = generateRandomUsername()
            prefs.edit().putString(KEY_USERNAME, username).apply()
        }
        
        return username ?: "Anonymous User"
    }

    private fun generateRandomUsername(): String {
        val adj = adjectives.random()
        val animal = animals.random()
        return "$adj $animal"
    }
}
