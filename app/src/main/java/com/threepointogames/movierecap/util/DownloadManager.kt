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

        // TODO Task 2: replace with PurchaseManager.isUnlimitedDownloads when available
        if (downloadCount >= FREE_LIMIT) {
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
