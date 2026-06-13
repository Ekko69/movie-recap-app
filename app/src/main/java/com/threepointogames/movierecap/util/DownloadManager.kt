package com.threepointogames.movierecap.util

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf
import com.threepointogames.movierecap.model.Movie
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object DownloadManager {
    private const val PREFS_NAME = "download_prefs"
    private const val KEY_IDS = "downloaded_movie_ids"
    private const val KEY_PATHS = "downloaded_file_paths"
    private const val KEY_METADATA = "downloaded_movie_metadata"
    private const val KEY_THUMB_PATHS = "downloaded_thumb_paths"
    const val FREE_LIMIT = 3

    private var appContext: Context? = null

    // Compose-observable state — UI recomposes automatically when these change
    val downloadedMovieIds: SnapshotStateList<String> = mutableStateListOf()
    val downloadProgress = mutableStateMapOf<String, Int>()  // movieId → 0..100
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activeJobs = mutableMapOf<String, Job>()

    // ConcurrentHashMap: read on main thread (recomposition), written on IO thread (download coroutine)
    private val thumbPathsCache = ConcurrentHashMap<String, String>()

    fun initialize(context: Context) {
        appContext = context.applicationContext
        val ids = loadIds(context)
        val paths = loadPaths(context)
        // Drop entries whose local file no longer exists (e.g. uninstall/reinstall)
        val validIds = ids.filter { id -> paths[id]?.let { File(it).exists() } == true }
        val thumbPaths = loadThumbPaths(context)
        if (validIds.size != ids.size) {
            saveIds(context, validIds)
            savePaths(context, paths.filter { (id, _) -> id in validIds })
            prefs(context).edit()
                .putString(KEY_METADATA, Json.encodeToString(loadMetadata(context).filter { (id, _) -> id in validIds }))
                .putString(KEY_THUMB_PATHS, Json.encodeToString(thumbPaths.filter { (id, _) -> id in validIds }))
                .apply()
            thumbPathsCache.clear()
            thumbPathsCache.putAll(thumbPaths.filter { (id, _) -> id in validIds })
        } else {
            thumbPathsCache.clear()
            thumbPathsCache.putAll(thumbPaths)
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

    fun getLocalThumbnailUri(movieId: String): Uri? {
        val path = thumbPathsCache[movieId] ?: return null
        val file = File(path)
        return if (file.exists()) Uri.fromFile(file) else null
    }

    fun getSavedMovie(movieId: String): Movie? {
        val ctx = appContext ?: return null
        return loadMetadata(ctx)[movieId]
    }

    fun getDownloadedMovies(): List<Movie> {
        val ctx = appContext ?: return emptyList()
        val metadata = loadMetadata(ctx)
        return downloadedMovieIds.mapNotNull { metadata[it] }
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

        activeJobs[movie.id] = scope.launch {
            val dir = ctx.getExternalFilesDir("downloads") ?: run {
                withContext(Dispatchers.Main) { onError("Storage not available") }
                return@launch
            }
            val file = File(dir, "${movie.id}.mp4")

            withContext(Dispatchers.Main) { downloadProgress[movie.id] = 0 }

            try {
                val conn = URL(movie.videoURL).openConnection() as HttpURLConnection
                conn.connectTimeout = 15_000
                conn.readTimeout = 30_000
                conn.connect()
                val total = conn.contentLengthLong

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

                // Download thumbnail image for offline display
                var thumbPath: String? = null
                val thumbUrl = movie.thumbnail ?: movie.featuredThumbnail
                if (!thumbUrl.isNullOrEmpty()) {
                    try {
                        val thumbDir = ctx.getExternalFilesDir("thumbnails")
                        if (thumbDir != null) {
                            val thumbFile = File(thumbDir, "${movie.id}.jpg")
                            val thumbConn = URL(thumbUrl).openConnection() as HttpURLConnection
                            thumbConn.connectTimeout = 10_000
                            thumbConn.readTimeout = 15_000
                            thumbConn.connect()
                            thumbConn.inputStream.use { it.copyTo(thumbFile.outputStream()) }
                            thumbPath = thumbFile.absolutePath
                        }
                    } catch (e: Exception) {
                        // Thumbnail is optional — video download still succeeds
                    }
                }

                // Persist file path, IDs, metadata, and thumbnail path
                val updatedIds = loadIds(ctx).toMutableList().also { if (movie.id !in it) it.add(0, movie.id) }
                val updatedPaths = loadPaths(ctx).toMutableMap().also { it[movie.id] = file.absolutePath }
                val updatedMeta = loadMetadata(ctx).toMutableMap().also { it[movie.id] = movie }
                val updatedThumbPaths = loadThumbPaths(ctx).toMutableMap()
                if (thumbPath != null) updatedThumbPaths[movie.id] = thumbPath
                saveIds(ctx, updatedIds)
                savePaths(ctx, updatedPaths)
                prefs(ctx).edit()
                    .putString(KEY_METADATA, Json.encodeToString(updatedMeta))
                    .putString(KEY_THUMB_PATHS, Json.encodeToString(updatedThumbPaths))
                    .apply()
                if (thumbPath != null) thumbPathsCache[movie.id] = thumbPath

                withContext(Dispatchers.Main) {
                    activeJobs.remove(movie.id)
                    downloadProgress.remove(movie.id)
                    if (movie.id !in downloadedMovieIds) downloadedMovieIds.add(0, movie.id)
                }
            } catch (e: Exception) {
                file.delete()
                withContext(Dispatchers.Main) {
                    activeJobs.remove(movie.id)
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
        activeJobs.remove(movieId)?.cancel()
        downloadProgress.remove(movieId)
        val paths = loadPaths(ctx)
        paths[movieId]?.let { File(it).delete() }
        thumbPathsCache.remove(movieId)?.let { File(it).delete() }
        saveIds(ctx, loadIds(ctx).toMutableList().also { it.remove(movieId) })
        savePaths(ctx, paths.toMutableMap().also { it.remove(movieId) })
        prefs(ctx).edit()
            .putString(KEY_METADATA, Json.encodeToString(loadMetadata(ctx).toMutableMap().also { it.remove(movieId) }))
            .putString(KEY_THUMB_PATHS, Json.encodeToString(loadThumbPaths(ctx).toMutableMap().also { it.remove(movieId) }))
            .apply()
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

    private fun loadMetadata(context: Context): Map<String, Movie> {
        val json = prefs(context).getString(KEY_METADATA, null) ?: return emptyMap()
        return try { Json.decodeFromString(json) } catch (e: Exception) { emptyMap() }
    }

    private fun loadThumbPaths(context: Context): Map<String, String> {
        val json = prefs(context).getString(KEY_THUMB_PATHS, null) ?: return emptyMap()
        return try { Json.decodeFromString(json) } catch (e: Exception) { emptyMap() }
    }

    private fun saveThumbPaths(context: Context, paths: Map<String, String>) {
        prefs(context).edit().putString(KEY_THUMB_PATHS, Json.encodeToString(paths)).apply()
    }
}
