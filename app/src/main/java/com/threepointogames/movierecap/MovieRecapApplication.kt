package com.threepointogames.movierecap

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.threepointogames.movierecap.util.AdManager
import com.threepointogames.movierecap.util.PurchaseManager
import com.google.android.gms.ads.MobileAds

class MovieRecapApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Initialize In-App Purchases (must run before AdManager so isAdFree is ready)
        PurchaseManager.initialize(this)

        // Initialize AdMob
        MobileAds.initialize(this) {}

        // Preload Interstitial Ad (skipped automatically if user is ad-free)
        AdManager.loadInterstitial(this)
    }
}
