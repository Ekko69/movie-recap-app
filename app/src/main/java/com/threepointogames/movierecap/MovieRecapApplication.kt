package com.threepointogames.movierecap

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.threepointogames.movierecap.util.AdManager
import com.google.android.gms.ads.MobileAds

class MovieRecapApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize AdMob
        MobileAds.initialize(this) {}
        
        // Load Ads
        // Preload Interstitial Ad
        AdManager.loadInterstitial(this)
    }
}
