package com.threepointogames.movierecap.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {
    private const val TAG = "AdManager"
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-2958975586761098/7877855823"
    
    // Rate Limiting Config
    private const val RATE_LIMIT_DURATION_MS = 60_000L // 60 seconds
    private const val MAX_ADS_PER_DURATION = 2
    
    private var interstitialAd: InterstitialAd? = null
    private var isAdLoading = false
    
    // Timestamp history of shown ads
    private val adShownTimestamps = mutableListOf<Long>()

    fun loadInterstitial(context: Context) {
        // Prevent multiple simultaneous loads, but allow retry if ad is null
        if (isAdLoading) {
            Log.d(TAG, "Ad is already loading. Skipping request.")
            return
        }
        
        if (interstitialAd != null) {
            Log.d(TAG, "Ad already loaded. Skipping request.")
            return
        }
        
        Log.d(TAG, "Starting ad load request...")
        isAdLoading = true
        val adRequest = AdRequest.Builder().build()
        
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, "Ad failed to load: ${adError.message}")
                    interstitialAd = null
                    isAdLoading = false
                }

                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Ad loaded successfully.")
                    interstitialAd = ad
                    isAdLoading = false
                }
            }
        )
    }

    private fun canShowAd(): Boolean {
        val currentTime = System.currentTimeMillis()
        // Remove timestamps older than the duration window
        val initialSize = adShownTimestamps.size
        adShownTimestamps.removeAll { it < currentTime - RATE_LIMIT_DURATION_MS }
        
        if (adShownTimestamps.size != initialSize) {
            Log.d(TAG, "Cleared ${initialSize - adShownTimestamps.size} expired timestamps.")
        }

        // Check if we are under the limit
        val isUnderLimit = adShownTimestamps.size < MAX_ADS_PER_DURATION
        if (!isUnderLimit) {
            Log.d(TAG, "Rate limit hit: ${adShownTimestamps.size}/$MAX_ADS_PER_DURATION ads shown in last minute.")
        }
        return isUnderLimit
    }
    
    // reset control if needed manually (for debug)
    fun resetAdsControl() {
        adShownTimestamps.clear()
        interstitialAd = null
        isAdLoading = false
        Log.d(TAG, "Ads control reset.")
    }

    /**
     * Checks if an ad is ready to be shown immediately.
     */
    fun isAdReady(): Boolean {
        return interstitialAd != null && canShowAd()
    }

    fun showInterstitial(activity: Activity, onAdDismissed: () -> Unit) {
        // 1. Check if Ad is Loaded
        if (interstitialAd == null) {
            Log.d(TAG, "showInterstitial: Ad is NOT ready. Triggering load.")
            loadInterstitial(activity)
            onAdDismissed()
            return
        }

        // 2. Check Rate Limit
        if (!canShowAd()) {
            Log.d(TAG, "showInterstitial: Rate limit active. Skipping ad.")
            onAdDismissed()
            return
        }

        // 3. Show Ad
        Log.d(TAG, "showInterstitial: Showing ad.")
        interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Ad dismissed.")
                interstitialAd = null 
                // Preload the next one immediately
                loadInterstitial(activity) 
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.d(TAG, "Ad failed to show: ${adError.message}")
                interstitialAd = null
                onAdDismissed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Ad showed fullscreen content.")
                interstitialAd = null 
                
                // Record timestamp for rate limiting
                adShownTimestamps.add(System.currentTimeMillis())
            }
        }
        interstitialAd?.show(activity)
    }
}
