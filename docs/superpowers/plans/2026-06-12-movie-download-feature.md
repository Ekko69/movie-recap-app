# Movie Download Feature Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add per-movie offline downloads with a 3-download free tier and a ₱200 lifetime IAP (`unlimited_downloads_lifetime`) for unlimited downloads.

**Architecture:** `DownloadManager` singleton follows the existing `FavoritesManager`/`PurchaseManager` object pattern — SharedPreferences for persistence, Compose SnapshotState for reactive UI. File downloads run via `HttpURLConnection` coroutines into `context.getExternalFilesDir("downloads")`. `PurchaseManager` gains a second IAP product. No new Android permissions needed (scoped storage).

**Tech Stack:** Kotlin, Jetpack Compose, ExoPlayer (Media3 1.1.1), Google Play Billing 6.1.0, SharedPreferences, `HttpURLConnection`, `kotlinx.serialization`, Material3 1.1.1

---

## File Structure

### New Files
| Path | Purpose |
|---|---|
| `app/src/main/java/com/threepointogames/movierecap/util/DownloadManager.kt` | Download state, file I/O, free-tier limit logic |
| `app/src/main/java/com/threepointogames/movierecap/ui/components/DownloadButton.kt` | Reusable download button (Idle / Downloading / Downloaded states + limit dialog) |
| `app/src/main/java/com/threepointogames/movierecap/ui/components/DownloadLimitDialog.kt` | Bottom-sheet paywall dialog shown at 3-download limit |
| `app/src/main/java/com/threepointogames/movierecap/ui/screens/DownloadsScreen.kt` | Full downloads list screen with Play + Delete per item |

### Modified Files
| Path | Change |
|---|---|
| `app/src/main/java/com/threepointogames/movierecap/util/PurchaseManager.kt` | Add `unlimited_downloads_lifetime` product, `isUnlimitedDownloads` state, `launchDownloadsPurchaseFlow()` |
| `app/src/main/java/com/threepointogames/movierecap/ui/components/MovieCard.kt` | Add optional download icon overlay (bottom-left corner) |
| `app/src/main/java/com/threepointogames/movierecap/ui/screens/DetailScreen.kt` | Add `DownloadButton` below action row; use local URI when movie is downloaded |
| `app/src/main/java/com/threepointogames/movierecap/ui/screens/HomeScreen.kt` | Add "MY DOWNLOADS" section row; add `onDownloadsSeeAllClick` param |
| `app/src/main/java/com/threepointogames/movierecap/MainActivity.kt` | Initialize `DownloadManager`; add `downloads` nav route; thread `onDownloadsSeeAllClick` |

---

## Task 1: DownloadManager

**Files:**
- Create: `app/src/main/java/com/threepointogames/movierecap/util/DownloadManager.kt`

- [ ] **Step 1: Create the file with full implementation**

```kotlin
package com.threepointogames.movierecap.util

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import com.threepointogames.movierecap.model.Movie
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object DownloadManager {
    private const val PREFS_NAME = "download_prefs"
    private const val KEY_IDS = "downloaded_movie_ids"
    private const val KEY_PATHS = "downloaded_file_paths"
    const val FREE_LIMIT = 3

    private var appContext: Context? = null

    // Compose-observable state — UI recomposes automatically when these change
    val downloadedMovieIds: SnapshotStateList<String> = mutableStateListOf()
    val downloadProgress = mutableStateMapOf<String, Int>()  // movieId → 0..100

    fun initialize(context: Context) {
        appContext = context.applicationContext
        val ids = loadIds(context)
        val paths = loadPaths(context)
        // Drop entries whose local file no longer exists (e.g. uninstall/reinstall)
        val validIds = ids.filter { id -> paths[id]?.let { File(it).exists() } == true }
        if (validIds.size != ids.size) {
            saveIds(context, validIds)
            savePaths(context, paths.filter { (id, _) -> id in validIds })
        }
        downloadedMovieIds.clear()
        downloadedMovieIds.addAll(validIds)
    }

    val downloadCount: Int get() = downloadedMovieIds.size

    fun isDownloaded(movieId: String): Boolean = movieId in downloadedMovieIds

    fun getLocalUri(movieId: String): Uri? {
        val ctx = appContext ?: return null
        val path = loadPaths(ctx)[movieId] ?: return null
        val file = File(path)
        return if (file.exists()) Uri.fromFile(file) else null
    }

    fun getFileSizeFormatted(movieId: String): String {
        val ctx = appContext ?: return ""
        val path = loadPaths(ctx)[movieId] ?: return ""
        val bytes = File(path).length()
        return when {
            bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            bytes >= 1024 -> "${bytes / 1024} KB"
            else -> "$bytes B"
        }
    }

    fun downloadMovie(
        movie: Movie,
        onLimitReached: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (isDownloaded(movie.id)) return
        if (downloadProgress.containsKey(movie.id)) return  // already in progress

        val ctx = appContext ?: return

        if (!PurchaseManager.isUnlimitedDownloads && downloadCount >= FREE_LIMIT) {
            onLimitReached()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val dir = ctx.getExternalFilesDir("downloads") ?: run {
                withContext(Dispatchers.Main) { onError("Storage not available") }
                return@launch
            }
            val file = File(dir, "${movie.id}.mp4")

            withContext(Dispatchers.Main) { downloadProgress[movie.id] = 0 }

            try {
                val conn = URL(movie.videoURL).openConnection() as HttpURLConnection
                conn.connect()
                val total = conn.contentLength.toLong()

                conn.inputStream.use { input ->
                    FileOutputStream(file).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var downloaded = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            if (total > 0) {
                                val progress = ((downloaded * 100) / total).toInt()
                                withContext(Dispatchers.Main) { downloadProgress[movie.id] = progress }
                            }
                        }
                    }
                }

                // Persist
                val updatedIds = loadIds(ctx).toMutableList().also { if (movie.id !in it) it.add(0, movie.id) }
                val updatedPaths = loadPaths(ctx).toMutableMap().also { it[movie.id] = file.absolutePath }
                saveIds(ctx, updatedIds)
                savePaths(ctx, updatedPaths)

                withContext(Dispatchers.Main) {
                    downloadProgress.remove(movie.id)
                    if (movie.id !in downloadedMovieIds) downloadedMovieIds.add(0, movie.id)
                }
            } catch (e: Exception) {
                file.delete()
                withContext(Dispatchers.Main) {
                    downloadProgress.remove(movie.id)
                    val msg = when {
                        e.message?.contains("space", ignoreCase = true) == true -> "Not enough storage space"
                        e is java.net.SocketTimeoutException -> "Download timed out. Check your connection."
                        else -> "Download failed. Please try again."
                    }
                    onError(msg)
                }
            }
        }
    }

    fun deleteDownload(movieId: String) {
        val ctx = appContext ?: return
        val paths = loadPaths(ctx)
        paths[movieId]?.let { File(it).delete() }
        saveIds(ctx, loadIds(ctx).toMutableList().also { it.remove(movieId) })
        savePaths(ctx, paths.toMutableMap().also { it.remove(movieId) })
        downloadedMovieIds.remove(movieId)
    }

    // SharedPreferences helpers
    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun loadIds(context: Context): List<String> {
        val json = prefs(context).getString(KEY_IDS, null) ?: return emptyList()
        return try { Json.decodeFromString(json) } catch (e: Exception) { emptyList() }
    }

    private fun saveIds(context: Context, ids: List<String>) {
        prefs(context).edit().putString(KEY_IDS, Json.encodeToString(ids)).apply()
    }

    private fun loadPaths(context: Context): Map<String, String> {
        val json = prefs(context).getString(KEY_PATHS, null) ?: return emptyMap()
        return try { Json.decodeFromString(json) } catch (e: Exception) { emptyMap() }
    }

    private fun savePaths(context: Context, paths: Map<String, String>) {
        prefs(context).edit().putString(KEY_PATHS, Json.encodeToString(paths)).apply()
    }
}
```

- [ ] **Step 2: Build to verify compilation**

```bash
cd "/Users/punx/Desktop/Projects/Personal Projects/movie-recap-app"
./gradlew :app:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL` with no errors. If `PurchaseManager.isUnlimitedDownloads` is unresolved, that's expected — it's added in Task 2. Comment out that line temporarily to verify the rest compiles.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/threepointogames/movierecap/util/DownloadManager.kt
git commit -m "feat: add DownloadManager with per-movie download logic and free-tier limit"
```

---

## Task 2: PurchaseManager — Add Unlimited Downloads IAP

**Files:**
- Modify: `app/src/main/java/com/threepointogames/movierecap/util/PurchaseManager.kt`

- [ ] **Step 1: Add the new product constant, state, and prefs key**

After line 27 (`private const val PREF_KEY_AD_FREE = "ad_free_purchased"`), add:

```kotlin
    const val PRODUCT_ID_DOWNLOADS = "unlimited_downloads_lifetime"
    private const val PREF_KEY_UNLIMITED_DOWNLOADS = "unlimited_downloads_purchased"
```

After line 37 (`var productPrice by mutableStateOf("")`), add:

```kotlin
    var isUnlimitedDownloads by mutableStateOf(false)
        private set

    var productPriceDownloads by mutableStateOf("")
        private set
```

- [ ] **Step 2: Restore `isUnlimitedDownloads` from prefs in `initialize()`**

In `initialize()`, after the line that sets `isAdFree`, add:

```kotlin
        isUnlimitedDownloads = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(PREF_KEY_UNLIMITED_DOWNLOADS, false)
```

- [ ] **Step 3: Fetch both product prices in `fetchProductPrice()`**

Replace the entire `fetchProductPrice()` method with:

```kotlin
    private fun fetchProductPrice() {
        CoroutineScope(Dispatchers.IO).launch {
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID_DOWNLOADS)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                ))
                .build()
            val (result, detailsList) = billingClient?.queryProductDetails(params) ?: return@launch
            if (result.responseCode == BillingClient.BillingResponseCode.OK && !detailsList.isNullOrEmpty()) {
                detailsList.forEach { details ->
                    val price = details.oneTimePurchaseOfferDetails?.formattedPrice ?: ""
                    when (details.productId) {
                        PRODUCT_ID -> productPrice = price
                        PRODUCT_ID_DOWNLOADS -> productPriceDownloads = price
                    }
                    Log.d(TAG, "Price fetched for ${details.productId}: $price")
                }
            }
        }
    }
```

- [ ] **Step 4: Handle both products in `handlePurchase()`**

Replace the entire `handlePurchase()` method with:

```kotlin
    private fun handlePurchase(context: Context, purchase: Purchase) {
        val isRemoveAds = purchase.products.contains(PRODUCT_ID)
        val isUnlimitedDL = purchase.products.contains(PRODUCT_ID_DOWNLOADS)
        if (!isRemoveAds && !isUnlimitedDL) return
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient?.acknowledgePurchase(params) { result ->
                Log.d(TAG, "Acknowledge result: ${result.responseCode}")
            }
        }
        if (isRemoveAds) setAdFree(context, true)
        if (isUnlimitedDL) setUnlimitedDownloads(context, true)
    }
```

- [ ] **Step 5: Add `setUnlimitedDownloads()` helper and `launchDownloadsPurchaseFlow()`**

After the closing brace of `setAdFree()`, add:

```kotlin
    private fun setUnlimitedDownloads(context: Context, value: Boolean) {
        isUnlimitedDownloads = value
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(PREF_KEY_UNLIMITED_DOWNLOADS, value)
            .apply()
        Log.d(TAG, "Unlimited downloads set to: $value")
    }

    /**
     * Launch the Google Play purchase sheet for unlimited downloads.
     * [onError] is called on the main thread if the flow cannot start.
     */
    fun launchDownloadsPurchaseFlow(activity: Activity, onError: (String) -> Unit) {
        if (isUnlimitedDownloads) {
            onError("You already have unlimited downloads!")
            return
        }
        if (billingClient?.isReady != true) {
            connect()
            onError("Connecting to Google Play… Please try again in a moment.")
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID_DOWNLOADS)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                ))
                .build()
            val (billingResult, productDetailsList) = billingClient!!.queryProductDetails(params)
            if (billingResult.responseCode != BillingClient.BillingResponseCode.OK || productDetailsList.isNullOrEmpty()) {
                Log.e(TAG, "Downloads product query failed: ${billingResult.debugMessage}")
                activity.runOnUiThread { onError("Product not available. Check your connection and try again.") }
                return@launch
            }
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetailsList[0])
                        .build()
                ))
                .build()
            activity.runOnUiThread { billingClient?.launchBillingFlow(activity, flowParams) }
        }
    }
```

- [ ] **Step 6: Build to verify**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`. The `DownloadManager.kt` reference to `PurchaseManager.isUnlimitedDownloads` (if you commented it out in Task 1) can now be uncommented.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/threepointogames/movierecap/util/PurchaseManager.kt
git commit -m "feat: add unlimited_downloads_lifetime IAP to PurchaseManager"
```

---

## Task 3: DownloadLimitDialog

**Files:**
- Create: `app/src/main/java/com/threepointogames/movierecap/ui/components/DownloadLimitDialog.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.threepointogames.movierecap.ui.components

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment.Companion.BottomCenter
import com.threepointogames.movierecap.util.PurchaseManager

@Composable
fun DownloadLimitDialog(
    onDismiss: () -> Unit,
    onUnlock: () -> Unit
) {
    val price = PurchaseManager.productPriceDownloads.ifEmpty { "₱200" }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFF1A1A1A),
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🔒", fontSize = 40.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Download Limit Reached",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "You've used all 3 free downloads. Unlock unlimited downloads to keep going.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onUnlock,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Text(
                        "Unlock Unlimited Downloads — $price",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }
    }
}
```

- [ ] **Step 2: Build to verify**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/threepointogames/movierecap/ui/components/DownloadLimitDialog.kt
git commit -m "feat: add DownloadLimitDialog paywall bottom sheet"
```

---

## Task 4: DownloadButton

**Files:**
- Create: `app/src/main/java/com/threepointogames/movierecap/ui/components/DownloadButton.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.threepointogames.movierecap.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.threepointogames.movierecap.model.Movie
import com.threepointogames.movierecap.util.DownloadManager
import com.threepointogames.movierecap.util.PurchaseManager

@Composable
fun DownloadButton(
    movie: Movie,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val isDownloaded = DownloadManager.isDownloaded(movie.id)
    val progress = DownloadManager.downloadProgress[movie.id]
    val isDownloading = progress != null
    var showLimitDialog by remember { mutableStateOf(false) }

    if (showLimitDialog) {
        DownloadLimitDialog(
            onDismiss = { showLimitDialog = false },
            onUnlock = {
                showLimitDialog = false
                activity?.let { act ->
                    PurchaseManager.launchDownloadsPurchaseFlow(act) { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    Column(modifier = modifier) {
        OutlinedButton(
            onClick = {
                if (!isDownloaded && !isDownloading) {
                    DownloadManager.downloadMovie(
                        movie = movie,
                        onLimitReached = { showLimitDialog = true },
                        onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isDownloaded) Color(0xFF1E3A2E) else Color.Transparent,
                contentColor = Color(0xFF4ADE80)
            ),
            border = BorderStroke(
                1.dp,
                if (isDownloaded) Color(0xFF4ADE80) else Color(0xFF4ADE80).copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(25.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        isDownloaded -> "✓  Downloaded"
                        isDownloading -> "↻  ${progress}%"
                        else -> "⬇  Download"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isDownloading && progress != null) {
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF4ADE80),
                trackColor = Color(0xFF333333)
            )
        }
    }
}
```

- [ ] **Step 2: Build to verify**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/threepointogames/movierecap/ui/components/DownloadButton.kt
git commit -m "feat: add DownloadButton composable with Idle/Downloading/Downloaded states"
```

---

## Task 5: DownloadsScreen

**Files:**
- Create: `app/src/main/java/com/threepointogames/movierecap/ui/screens/DownloadsScreen.kt`

- [ ] **Step 1: Create the file**

```kotlin
package com.threepointogames.movierecap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.threepointogames.movierecap.model.Movie
import com.threepointogames.movierecap.ui.components.shimmerEffect
import com.threepointogames.movierecap.util.DownloadManager
import com.threepointogames.movierecap.util.PurchaseManager

@Composable
fun DownloadsScreen(
    allMovies: List<Movie>,
    onMovieClick: (Movie) -> Unit,
    onBack: () -> Unit
) {
    val isPremium = PurchaseManager.isUnlimitedDownloads
    val downloadedIds = DownloadManager.downloadedMovieIds.toList()
    val downloadedMovies = remember(downloadedIds, allMovies) {
        downloadedIds.mapNotNull { id -> allMovies.find { it.id == id } }
    }
    var showUpgradeDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as? Activity

    if (showUpgradeDialog) {
        com.threepointogames.movierecap.ui.components.DownloadLimitDialog(
            onDismiss = { showUpgradeDialog = false },
            onUnlock = {
                showUpgradeDialog = false
                activity?.let { act ->
                    PurchaseManager.launchDownloadsPurchaseFlow(act) { msg ->
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    "My Downloads",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Slot counter + progress bar (hidden for premium)
            if (!isPremium) {
                item {
                    val count = DownloadManager.downloadCount
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "$count of ${DownloadManager.FREE_LIMIT} free slots used",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        Button(
                            onClick = { showUpgradeDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2A1A)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            val price = PurchaseManager.productPriceDownloads.ifEmpty { "₱200" }
                            Text("⬆ Unlock Unlimited — $price", fontSize = 11.sp, color = Color(0xFF4ADE80))
                        }
                    }
                    LinearProgressIndicator(
                        progress = count.toFloat() / DownloadManager.FREE_LIMIT.toFloat(),
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = Color(0xFFE50914),
                        trackColor = Color(0xFF333333)
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            if (downloadedMovies.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⬇", fontSize = 48.sp)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No downloads yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Find a movie and tap ⬇ to save it for offline viewing.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(downloadedMovies, key = { it.id }) { movie ->
                    DownloadedMovieRow(
                        movie = movie,
                        onPlay = { onMovieClick(movie) },
                        onDelete = { DownloadManager.deleteDownload(movie.id) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun DownloadedMovieRow(
    movie: Movie,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val fileSize = remember(movie.id) { DownloadManager.getFileSizeFormatted(movie.id) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context).data(movie.thumbnail).crossfade(true).build(),
            contentDescription = movie.title,
            modifier = Modifier
                .size(width = 60.dp, height = 80.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop,
            loading = {
                Box(Modifier.fillMaxSize().shimmerEffect())
            }
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                movie.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2
            )
            Spacer(Modifier.height(4.dp))
            Text(
                buildString {
                    append("${movie.year} · ${movie.duration}")
                    if (fileSize.isNotEmpty()) append(" · $fileSize")
                },
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onPlay,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text("▶ Play", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(6.dp))
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .background(Color(0xFF2A1A1A), RoundedCornerShape(8.dp))
                .size(36.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE05252), modifier = Modifier.size(18.dp))
        }
    }
}
```

- [ ] **Step 2: Build to verify**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/threepointogames/movierecap/ui/screens/DownloadsScreen.kt
git commit -m "feat: add DownloadsScreen with play/delete per item and free-tier progress bar"
```

---

## Task 6: MovieCard — Download Icon Overlay

**Files:**
- Modify: `app/src/main/java/com/threepointogames/movierecap/ui/components/MovieCard.kt`

- [ ] **Step 1: Add `onDownloadClick` parameter to `MovieCard`**

Change the function signature from:
```kotlin
fun MovieCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: MovieCardSize = MovieCardSize.LANDSCAPE
)
```
to:
```kotlin
fun MovieCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: MovieCardSize = MovieCardSize.LANDSCAPE,
    onDownloadClick: ((Movie) -> Unit)? = null
)
```

- [ ] **Step 2: Add required imports**

Add these imports to the top of `MovieCard.kt` (after the existing imports):
```kotlin
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.dp
import com.threepointogames.movierecap.util.DownloadManager
```

Note: `androidx.compose.foundation.clickable` and `androidx.compose.ui.unit.dp` are already imported — only add what is missing.

- [ ] **Step 3: Add the download icon overlay inside the thumbnail `Box`**

In the thumbnail `Box` (the one containing `SubcomposeAsyncImage` and the Duration badge), find the Duration badge block at the end of the Box:
```kotlin
                // Duration Badge inside image
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        ...
                ) {
                    Text(text = movie.duration, ...)
                }
```

After the closing brace of the Duration badge block and before the closing brace of the thumbnail `Box`, add:
```kotlin
                // Download icon overlay — only shown when caller provides onDownloadClick
                if (onDownloadClick != null) {
                    val isDownloaded = DownloadManager.isDownloaded(movie.id)
                    val isDownloading = DownloadManager.downloadProgress.containsKey(movie.id)
                    val limitReached = !com.threepointogames.movierecap.util.PurchaseManager.isUnlimitedDownloads
                            && DownloadManager.downloadCount >= DownloadManager.FREE_LIMIT
                            && !isDownloaded

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .background(
                                Color.Black.copy(alpha = 0.75f),
                                androidx.compose.foundation.shape.CircleShape
                            )
                            .size(28.dp)
                            .clickable { if (!isDownloaded && !isDownloading) onDownloadClick(movie) },
                        contentAlignment = Alignment.Center
                    ) {
                        val iconText = when {
                            isDownloaded -> "✓"
                            limitReached -> "🔒"
                            isDownloading -> "↻"
                            else -> "⬇"
                        }
                        val iconColor = when {
                            isDownloaded -> Color(0xFF4ADE80)
                            limitReached -> Color.Gray
                            isDownloading -> Color.Gray
                            else -> Color.White
                        }
                        Text(iconText, fontSize = 13.sp, color = iconColor, fontWeight = FontWeight.Bold)
                    }
                }
```

- [ ] **Step 4: Build to verify**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`. All existing callers of `MovieCard` are unaffected since `onDownloadClick` defaults to `null`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/threepointogames/movierecap/ui/components/MovieCard.kt
git commit -m "feat: add optional download icon overlay to MovieCard"
```

---

## Task 7: DetailScreen — DownloadButton + Local URI Playback

**Files:**
- Modify: `app/src/main/java/com/threepointogames/movierecap/ui/screens/DetailScreen.kt`

- [ ] **Step 1: Add import for DownloadManager and DownloadButton**

Add to the imports section of `DetailScreen.kt`:
```kotlin
import com.threepointogames.movierecap.ui.components.DownloadButton
import com.threepointogames.movierecap.util.DownloadManager
```

- [ ] **Step 2: Derive `videoUri` from download state**

Inside `DetailScreen`, after the line `val context = LocalContext.current` (around line 175), add:
```kotlin
    // Use local file when downloaded, otherwise stream remote URL
    val isDownloaded = DownloadManager.isDownloaded(movie.id)
    val videoUri = if (isDownloaded) {
        DownloadManager.getLocalUri(movie.id)?.toString() ?: movie.videoURL
    } else {
        movie.videoURL
    }
```

- [ ] **Step 3: Replace `movie.videoURL` with `videoUri` in the player LaunchedEffect**

Find the `LaunchedEffect` that sets up the media item (around line 272):
```kotlin
    LaunchedEffect(movie.videoURL, rawSubtitleContent, subtitleOffset) {
```
Change it to:
```kotlin
    LaunchedEffect(videoUri, rawSubtitleContent, subtitleOffset) {
```

Then inside that LaunchedEffect, find:
```kotlin
        val mediaItemBuilder = MediaItem.Builder()
            .setUri(movie.videoURL)
```
Change to:
```kotlin
        val mediaItemBuilder = MediaItem.Builder()
            .setUri(videoUri)
```

- [ ] **Step 4: Add DownloadButton below the action buttons row**

Find the section after the action buttons `Row` closes (the Row containing Watch Now, Fullscreen, Favorite buttons). It ends around line 700 followed by:
```kotlin
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = movie.description,
```

Replace `Spacer(modifier = Modifier.height(24.dp))` before `movie.description` with:
```kotlin
                    Spacer(modifier = Modifier.height(12.dp))

                    DownloadButton(
                        movie = movie,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))
```

- [ ] **Step 5: Build to verify**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/threepointogames/movierecap/ui/screens/DetailScreen.kt
git commit -m "feat: add DownloadButton to DetailScreen and local URI playback for downloaded movies"
```

---

## Task 8: HomeScreen — My Downloads Section

**Files:**
- Modify: `app/src/main/java/com/threepointogames/movierecap/ui/screens/HomeScreen.kt`

- [ ] **Step 1: Add `onDownloadsSeeAllClick` parameter and required imports**

Change the `HomeScreen` signature from:
```kotlin
fun HomeScreen(
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit,
    onSeeAllClick: () -> Unit,
    onCategorySeeAllClick: (String) -> Unit
)
```
to:
```kotlin
fun HomeScreen(
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit,
    onSeeAllClick: () -> Unit,
    onCategorySeeAllClick: (String) -> Unit,
    onDownloadsSeeAllClick: () -> Unit
)
```

Add to imports:
```kotlin
import android.widget.Toast
import com.threepointogames.movierecap.ui.components.DownloadLimitDialog
import com.threepointogames.movierecap.util.DownloadManager
import com.threepointogames.movierecap.util.PurchaseManager
```

- [ ] **Step 2: Add download limit dialog state inside `HomeScreen`**

After the existing `var showRemoveAdsDialog by remember { mutableStateOf(false) }` line, add:
```kotlin
    var showDownloadLimitDialogFor by remember { mutableStateOf<Movie?>(null) }
```

- [ ] **Step 3: Add the limit dialog invocation**

In the Scaffold's `content` lambda, after the existing `RemoveAdsDialog` block, add:
```kotlin
        // Download limit dialog — triggered from movie card download taps
        showDownloadLimitDialogFor?.let { movie ->
            DownloadLimitDialog(
                onDismiss = { showDownloadLimitDialogFor = null },
                onUnlock = {
                    showDownloadLimitDialogFor = null
                    val activity = context as? android.app.Activity
                    activity?.let { act ->
                        PurchaseManager.launchDownloadsPurchaseFlow(act) { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
```

- [ ] **Step 4: Hoist `downloadedMovies` above the LazyColumn**

`LazyListScope` is not a `@Composable` scope — `remember` cannot be called inside the `LazyColumn { }` DSL block. Add `downloadedMovies` alongside the other hoisted computed lists near the top of `HomeScreen`, after the `favoriteMovies` declaration:

```kotlin
    val downloadedMovies = remember(DownloadManager.downloadedMovieIds.toList(), movies) {
        DownloadManager.downloadedMovieIds.mapNotNull { id -> movies.find { it.id == id } }
    }
```

- [ ] **Step 5: Add the "MY DOWNLOADS" section inside the LazyColumn**

Find the "Recently Watched Section" block:
```kotlin
            // Recently Watched Section
            if (continueWatchingMovies.isNotEmpty()) {
```

After the closing `}` of the Recently Watched section item, add:
```kotlin
            // My Downloads Section
            if (downloadedMovies.isNotEmpty()) {
                item {
                    MovieSection(
                        title = "My Downloads",
                        movies = downloadedMovies,
                        onMovieClick = { movie ->
                            com.threepointogames.movierecap.util.AnalyticsManager.logMovieClick(movie.id, movie.title)
                            onMovieClick(movie)
                        },
                        isInfinite = false,
                        cardSize = MovieCardSize.LANDSCAPE,
                        onSeeAllClick = { onDownloadsSeeAllClick() },
                        onDownloadClick = { movie ->
                            DownloadManager.downloadMovie(
                                movie = movie,
                                onLimitReached = { showDownloadLimitDialogFor = movie },
                                onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                            )
                        }
                    )
                }
            }
```

- [ ] **Step 5: Update MovieSection to accept and pass `onDownloadClick`**

Open `app/src/main/java/com/threepointogames/movierecap/ui/components/MovieSection.kt` and find the `MovieSection` composable signature. Add the optional parameter:
```kotlin
fun MovieSection(
    title: String,
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit,
    isInfinite: Boolean = false,
    cardSize: MovieCardSize = MovieCardSize.LANDSCAPE,
    onSeeAllClick: (() -> Unit)? = null,
    onDownloadClick: ((Movie) -> Unit)? = null   // ADD THIS
)
```

Then inside `MovieSection`, find where `MovieCard` is called and pass the callback:
```kotlin
MovieCard(
    movie = movie,
    onClick = { onMovieClick(movie) },
    size = cardSize,
    onDownloadClick = if (onDownloadClick != null) { m -> onDownloadClick(m) } else null
)
```

- [ ] **Step 6: Add `onDownloadClick` to category sections in HomeScreen that show movie cards** (Step numbers continue from Step 5 above)

Find the `allCategories.forEach { category ->` loop. Inside the `MovieSection(...)` call for each category, add:
```kotlin
                        onDownloadClick = { movie ->
                            DownloadManager.downloadMovie(
                                movie = movie,
                                onLimitReached = { showDownloadLimitDialogFor = movie },
                                onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                            )
                        }
```

Do the same for the "Recently Added", "My Favorites", and "All Movies" `MovieSection` calls.

- [ ] **Step 7: Build to verify**

```bash
./gradlew :app:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/threepointogames/movierecap/ui/screens/HomeScreen.kt
git add app/src/main/java/com/threepointogames/movierecap/ui/components/MovieSection.kt
git commit -m "feat: add My Downloads section to HomeScreen with download icon on movie cards"
```

---

## Task 9: MainActivity — Wire DownloadManager + Downloads Route

**Files:**
- Modify: `app/src/main/java/com/threepointogames/movierecap/MainActivity.kt`

- [ ] **Step 1: Add `DownloadManager.initialize` call**

Inside `setContent { ... }`, after `com.threepointogames.movierecap.util.AdManager.loadRewarded(this)`, add:
```kotlin
            com.threepointogames.movierecap.util.DownloadManager.initialize(this)
```

- [ ] **Step 2: Add PurchaseManager.initialize call (if not already present)**

Check if `PurchaseManager.initialize(this)` is already called in `MainActivity`. If not, add it after the `DownloadManager.initialize(this)` line:
```kotlin
            com.threepointogames.movierecap.util.PurchaseManager.initialize(this)
```

- [ ] **Step 3: Update `HomeScreen` call to pass `onDownloadsSeeAllClick`**

Find the `composable("home")` block and the `HomeScreen(...)` call inside it. Add the new parameter:
```kotlin
                        HomeScreen(
                            movies = allMovies,
                            onMovieClick = { movie ->
                                selectedMovie = movie
                                com.threepointogames.movierecap.util.AdManager.showInterstitial(this@MainActivity) {
                                    navController.navigate("details")
                                }
                            },
                            onSeeAllClick = {
                                navController.navigate("all_movies")
                            },
                            onCategorySeeAllClick = { category ->
                                navController.navigate("category/${category}")
                            },
                            onDownloadsSeeAllClick = {
                                navController.navigate("downloads")
                            }
                        )
```

- [ ] **Step 4: Add the `downloads` route**

After the `composable("all_movies") { ... }` block, add:
```kotlin
                    composable("downloads") {
                        com.threepointogames.movierecap.ui.screens.DownloadsScreen(
                            allMovies = allMovies,
                            onMovieClick = { movie ->
                                selectedMovie = movie
                                navController.navigate("details")
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
```

Note: Downloads screen navigation to detail does NOT show an interstitial ad (user already paid or is about to play their saved content).

- [ ] **Step 5: Build the full app**

```bash
./gradlew :app:assembleDebug
```
Expected: `BUILD SUCCESSFUL`. Fix any remaining compilation errors before continuing.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/threepointogames/movierecap/MainActivity.kt
git commit -m "feat: wire DownloadManager initialization and downloads nav route in MainActivity"
```

---

## Verification Checklist

Run the app on a device or emulator (`./gradlew :app:installDebug`) and verify each scenario:

- [ ] **HomeScreen section hidden initially** — Fresh install shows no "MY DOWNLOADS" row. After downloading one movie, the row appears.
- [ ] **Download button on Detail screen** — Open any movie detail. The "⬇  Download" button appears below the Watch Now row. Tap it; progress bar appears and percentage increments. Completes to "✓  Downloaded".
- [ ] **Download icon on movie cards** — Cards show "⬇" overlay in bottom-left. After downloading, they show "✓" in green.
- [ ] **Free tier limit** — Download 3 movies. Attempt a 4th. `DownloadLimitDialog` appears. Tap Cancel → dialog dismisses, no download starts.
- [ ] **Hard paywall** — `DownloadLimitDialog` has no delete option; only "Unlock" CTA and Cancel.
- [ ] **Local URI playback** — Enable airplane mode. Open a downloaded movie. Tap Watch Now. Video plays without buffering.
- [ ] **Delete** — On Downloads screen, tap the delete icon. Movie disappears from the list. Re-check HomeScreen: movie no longer has checkmark on its card.
- [ ] **Downloads screen empty state** — After deleting all movies, Downloads screen shows the empty-state message.
- [ ] **Premium bypass** — After completing a test purchase for `unlimited_downloads_lifetime`, `isUnlimitedDownloads` becomes `true`. The limit dialog no longer appears. Progress bar and slot counter are hidden on Downloads screen.
- [ ] **Purchase restore** — On a device where `unlimited_downloads_lifetime` was previously purchased, `PurchaseManager.queryExistingPurchases()` restores `isUnlimitedDownloads = true` on app launch.
