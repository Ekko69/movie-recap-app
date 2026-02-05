package com.threepointogames.movierecap.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

object AnalyticsManager {

    private var firebaseAnalytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        if (firebaseAnalytics == null) {
            firebaseAnalytics = Firebase.analytics
        }
    }

    fun logScreenView(screenName: String, screenClass: String = "MainActivity") {
        firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        })
    }

    fun logMovieClick(movieId: String, movieTitle: String) {
        firebaseAnalytics?.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, Bundle().apply {
            putString(FirebaseAnalytics.Param.CONTENT_TYPE, "movie")
            putString(FirebaseAnalytics.Param.ITEM_ID, movieId)
            putString(FirebaseAnalytics.Param.ITEM_NAME, movieTitle)
        })
    }

    fun logCategoryView(categoryName: String) {
        firebaseAnalytics?.logEvent("view_category", Bundle().apply {
            putString("category_name", categoryName)
        })
    }

    fun logVideoStart(movieId: String, movieTitle: String) {
        firebaseAnalytics?.logEvent("video_start", Bundle().apply {
            putString("movie_id", movieId)
            putString("movie_title", movieTitle)
        })
    }

    fun logVideoComplete(movieId: String, movieTitle: String, duration: String) {
        firebaseAnalytics?.logEvent("video_complete", Bundle().apply {
            putString("movie_id", movieId)
            putString("movie_title", movieTitle)
            putString("duration", duration)
        })
    }

    fun logAdRewardEarned(movieTitle: String) {
        firebaseAnalytics?.logEvent("ad_reward_earned", Bundle().apply {
            putString("movie_title", movieTitle)
        })
    }
}
