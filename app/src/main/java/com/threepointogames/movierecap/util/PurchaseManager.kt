package com.threepointogames.movierecap.util

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.enablePendingPurchases
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PurchaseManager {
    private const val TAG = "PurchaseManager"
    const val PRODUCT_ID = "remove_ads_lifetime"
    private const val PREFS_NAME = "purchase_prefs"
    private const val PREF_KEY_AD_FREE = "ad_free_purchased"

    private var billingClient: BillingClient? = null
    private var appContext: Context? = null

    // Observed by Compose — recompose automatically when this changes
    var isAdFree by mutableStateOf(false)
        private set

    fun initialize(context: Context) {
        appContext = context.applicationContext

        // Restore from local cache immediately so the UI is correct on launch
        isAdFree = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(PREF_KEY_AD_FREE, false)

        billingClient = BillingClient.newBuilder(context.applicationContext)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases?.forEach { handlePurchase(context.applicationContext, it) }
                }
            }
            .enablePendingPurchases()
            .build()

        connect()
    }

    private fun connect() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connected — verifying purchases...")
                    queryExistingPurchases()
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing disconnected.")
            }
        })
    }

    private fun queryExistingPurchases() {
        val ctx = appContext ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
            val (billingResult, purchases) = billingClient!!.queryPurchasesAsync(params)
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Found ${purchases.size} existing purchase(s).")
                purchases.forEach { handlePurchase(ctx, it) }
            }
        }
    }

    private fun handlePurchase(context: Context, purchase: Purchase) {
        if (!purchase.products.contains(PRODUCT_ID)) return
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient?.acknowledgePurchase(params) { result ->
                Log.d(TAG, "Acknowledge result: ${result.responseCode}")
            }
        }
        setAdFree(context, true)
    }

    private fun setAdFree(context: Context, value: Boolean) {
        isAdFree = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_KEY_AD_FREE, value)
            .apply()
        Log.d(TAG, "Ad-free set to: $value")
    }

    /**
     * Launch the Google Play purchase sheet for removing ads.
     * [onError] is called on the main thread if the flow cannot start.
     */
    fun launchPurchaseFlow(activity: Activity, onError: (String) -> Unit) {
        if (isAdFree) {
            onError("You already have ads removed!")
            return
        }

        if (billingClient?.isReady != true) {
            connect()
            onError("Connecting to Google Play… Please try again in a moment.")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            )
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            val (billingResult, productDetailsList) = billingClient!!.queryProductDetails(params)

            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK || productDetailsList.isNullOrEmpty()) {
                Log.e(TAG, "Product query failed: ${billingResult.debugMessage}")
                activity.runOnUiThread { onError("Product not available. Check your connection and try again.") }
                return@launch
            }

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetailsList[0])
                            .build()
                    )
                )
                .build()

            activity.runOnUiThread {
                billingClient?.launchBillingFlow(activity, flowParams)
            }
        }
    }
}
