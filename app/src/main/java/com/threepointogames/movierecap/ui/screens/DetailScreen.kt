package com.threepointogames.movierecap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Base64
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.threepointogames.movierecap.ui.components.shimmerEffect
import com.threepointogames.movierecap.model.Movie
import com.threepointogames.movierecap.ui.components.VideoPlayer
import com.threepointogames.movierecap.model.Comment
import com.threepointogames.movierecap.util.UserManager
import com.threepointogames.movierecap.data.MovieRepository

@Composable
fun DetailScreen(
    movie: Movie?,
    onBack: () -> Unit
) {
    if (movie == null) {
        Text("Movie not found", color = Color.White)
        return
    }

    // Analytics: Screen View
    LaunchedEffect(movie.id) {
        com.threepointogames.movierecap.util.AnalyticsManager.logScreenView("Detail Screen: ${movie.title}")
    }

    // Custom Pause Icon so we don't need extended library
    val PauseIcon = remember {
        ImageVector.Builder(
            name = "Pause",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = androidx.compose.ui.graphics.SolidColor(Color.White)) {
                moveTo(6f, 19f)
                horizontalLineTo(10f)
                verticalLineTo(5f)
                horizontalLineTo(6f)
                verticalLineTo(19f)
                close()
                moveTo(14f, 5f)
                verticalLineTo(19f)
                horizontalLineTo(18f)
                verticalLineTo(5f)
                horizontalLineTo(14f)
                close()
            }
        }.build()
    }

    // Custom Fullscreen Icon
    val FullscreenIcon = remember {
        ImageVector.Builder(
            name = "Fullscreen",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = androidx.compose.ui.graphics.SolidColor(Color.White)) {
                // Top Left
                moveTo(7f, 14f) // Wait, this is corners
                // Let's do simple lines or filled polygons
                // Top-Left
                moveTo(5f, 5f)
                lineTo(9f, 5f)
                lineTo(9f, 7f)
                lineTo(7f, 7f)
                lineTo(7f, 9f)
                lineTo(5f, 9f)
                close()
                // Top-Right
                moveTo(15f, 5f)
                lineTo(19f, 5f)
                lineTo(19f, 9f)
                lineTo(17f, 9f)
                lineTo(17f, 7f)
                lineTo(15f, 7f)
                close()
                // Bottom-Right
                moveTo(19f, 15f)
                lineTo(19f, 19f)
                lineTo(15f, 19f)
                lineTo(15f, 17f)
                lineTo(17f, 17f)
                lineTo(17f, 15f)
                close()
                // Bottom-Left
                moveTo(5f, 15f)
                lineTo(7f, 15f)
                lineTo(7f, 17f)
                lineTo(9f, 17f)
                lineTo(9f, 19f)
                lineTo(5f, 19f)
                close()
            }
        }.build()
    }

    val context = LocalContext.current
    var isFullscreen by remember { mutableStateOf(false) }
    var isVideoVisible by remember { mutableStateOf(false) }
    var isPlayerPlaying by remember { mutableStateOf(false) }
    var showUnlockDialog by remember { mutableStateOf(false) }
    var areControlsVisible by remember { mutableStateOf(true) }

    // Subtitle Sync State
    var rawSubtitleContent by remember { mutableStateOf<String?>(null) }
    var subtitleDefMimeType by remember { mutableStateOf(MimeTypes.TEXT_VTT) }
    var subtitleOffset by remember { mutableStateOf(0L) }

    // Favorites State
    var isFavorite by remember { mutableStateOf(false) }
    LaunchedEffect(movie.id) {
        isFavorite =
            com.threepointogames.movierecap.util.FavoritesManager.isFavorite(context, movie.id)
    }

    // Comments State
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var isLoadingComments by remember { mutableStateOf(true) }
    var commentText by remember { mutableStateOf("") }
    var isPosting by remember { mutableStateOf(false) }
    var replyingTo by remember { mutableStateOf<Comment?>(null) } // State for reply context
    val userName = remember { UserManager.getUsername(context) }
    val repository = remember { MovieRepository(context) }

    // Fetch Comments
    LaunchedEffect(movie.id) {
        isLoadingComments = true
        comments = repository.getComments(movie.id)
        isLoadingComments = false
    }

    // Initialize ExoPlayer with AudioAttributes
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .setLoadControl(
                androidx.media3.exoplayer.DefaultLoadControl.Builder()
                    .setBufferDurationsMs(
                        15000, // Min buffer
                        50000, // Max buffer
                        2500,  // Buffer for playback
                        5000   // Buffer for playback after rebuffer
                    )
                    .build()
            )
            .setAudioAttributes(
                androidx.media3.common.AudioAttributes.Builder()
                    .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                    .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            .build()
            .apply {
                playWhenReady = false // Do not auto-play. Wait for "Watch Now".
                addListener(object : androidx.media3.common.Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == androidx.media3.common.Player.STATE_ENDED) {
                            com.threepointogames.movierecap.util.AnalyticsManager.logVideoComplete(
                                movie.id,
                                movie.title,
                                movie.duration
                            )
                        }
                    }
                })
            }
    }

    // 1. Fetch Subtitle Content (Once)
    LaunchedEffect(movie.subtitleURL) {
        if (!movie.subtitleURL.isNullOrEmpty()) {
            withContext(Dispatchers.IO) {
                try {
                    val content = URL(movie.subtitleURL).readText()
                    val isVtt = content.contains("WEBVTT", ignoreCase = true)
                    // Update state on Main thread
                    withContext(Dispatchers.Main) {
                        rawSubtitleContent = content
                        subtitleDefMimeType =
                            if (isVtt) MimeTypes.TEXT_VTT else MimeTypes.APPLICATION_SUBRIP
                        subtitleOffset = movie.subtitleDelay // Initialize with default delay
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // 2. Configure Player with Subtitles (Re-run when offset changes)
    LaunchedEffect(movie.videoURL, rawSubtitleContent, subtitleOffset) {
        val currentPos = exoPlayer.currentPosition
        val wasPlaying = exoPlayer.isPlaying

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(movie.videoURL)

        if (rawSubtitleContent != null) {
            try {
                val processedContent = withContext(Dispatchers.Default) {
                    shiftSubtitleTimestamps(rawSubtitleContent!!, subtitleOffset)
                }

                val dataMime =
                    if (subtitleDefMimeType == MimeTypes.TEXT_VTT) "text/vtt" else "application/x-subrip"
                val encoded = Base64.encodeToString(
                    processedContent.toByteArray(StandardCharsets.UTF_8),
                    Base64.NO_WRAP
                )
                val subtitleUri = android.net.Uri.parse("data:$dataMime;base64,$encoded")

                val subtitle = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
                    .setMimeType(subtitleDefMimeType)
                    .setLanguage("en")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()

                mediaItemBuilder.setSubtitleConfigurations(listOf(subtitle))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        exoPlayer.setMediaItem(mediaItemBuilder.build())
        exoPlayer.prepare()
        // Restore state
        if (currentPos > 0) {
            exoPlayer.seekTo(currentPos)
        }
        if (wasPlaying) {
            exoPlayer.play()
        }
    }


    // Control Playback via State
    LaunchedEffect(isPlayerPlaying) {
        if (isPlayerPlaying) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    // Lifecycle Management (Auto-Pause on Minimize)
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                isPlayerPlaying = false
                exoPlayer.pause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    // ... [Fullscreen Dialog Code Omitted - Unchanged] ...
    if (isFullscreen) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { isFullscreen = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                VideoPlayer(
                    exoPlayer = exoPlayer,
                    onFullscreenClick = { isFullscreen = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showUnlockDialog) {
        com.threepointogames.movierecap.ui.components.UnlockDialog(
            onDismiss = { showUnlockDialog = false },
            onUnlock = {
                showUnlockDialog = false
                val activity = context as? android.app.Activity
                if (activity != null) {
                    com.threepointogames.movierecap.util.AdManager.showInterstitial(activity) {
                        // After Ad (or if failed), Start Video Logic
                        com.threepointogames.movierecap.util.AnalyticsManager.logVideoStart(
                            movie.id,
                            movie.title
                        )
                        com.threepointogames.movierecap.util.WatchHistoryManager.saveMovieId(
                           activity,
                            movie.id
                        )
                        isVideoVisible = true
                        isPlayerPlaying = true
                    }
                }
            }
        )
    }



    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Section (Video or Thumbnail)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                if (isVideoVisible && !isFullscreen) {
                    VideoPlayer(
                        exoPlayer = exoPlayer,
                        onFullscreenClick = { isFullscreen = true },
                        onControllerVisibilityChanged = { areControlsVisible = it },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Show Thumbnail if not playing
                    if (!movie.thumbnail.isNullOrEmpty()) {
                        // Show Thumbnail if not playing
                        if (!movie.thumbnail.isNullOrEmpty()) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(movie.thumbnail)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .shimmerEffect()
                                    )
                                }
                            )
                        } else {
                            // Placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.DarkGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }

                        // Gradient Overlay for smooth transition to UI
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.background
                                        ),
                                        startY = 300f
                                    )
                                )
                        )
                    }

                    // Back Button Overlay
                    val showBackButton = if (isVideoVisible) areControlsVisible else true
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !isFullscreen && showBackButton,
                        enter = androidx.compose.animation.fadeIn(),
                        exit = androidx.compose.animation.fadeOut(),
                        modifier = Modifier.zIndex(10f) // Ensure it stays on top of video surface
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .padding(16.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Metadata & Actions
            Column(modifier = Modifier.padding(16.dp)) {

                    // ... [Metadata Omitted - Unchanged] ...
                    Text(
                        text = movie.title,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = movie.year.toString(),
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .background(Color.DarkGray, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = movie.duration,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    androidx.compose.material3.Divider(
                        color = Color.DarkGray.copy(alpha = 0.5f),
                        thickness = 1.dp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Watch Now & Fullscreen Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Watch Now Button
                        Button(
                            onClick = {
                                if (!isVideoVisible) {
                                    // Check Ad Readiness
                                    if (com.threepointogames.movierecap.util.AdManager.isAdReady()) {
                                        showUnlockDialog = true
                                    } else {
                                        // No Ad ready (or rate limit), play directly
                                        com.threepointogames.movierecap.util.AnalyticsManager.logVideoStart(
                                            movie.id,
                                            movie.title
                                        )
                                        // Add to Watch History
                                        com.threepointogames.movierecap.util.WatchHistoryManager.saveMovieId(
                                            context,
                                            movie.id
                                        )
                                        isVideoVisible = true
                                        isPlayerPlaying = true
                                    }
                                } else {
                                    isPlayerPlaying = !isPlayerPlaying
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent, // Transparent for gradient
                                contentColor = Color.White
                            ),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp), // Remove padding for full gradient
                            shape = RoundedCornerShape(25.dp) // Rounded edges like image
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                com.threepointogames.movierecap.ui.theme.GomoPrimaryPink,
                                                com.threepointogames.movierecap.ui.theme.GomoPurpleMid
                                            )
                                        ),
                                        shape = RoundedCornerShape(25.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isPlayerPlaying) PauseIcon else Icons.Default.PlayArrow,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isPlayerPlaying) "Watching" else "Watch Now",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Fullscreen Button
                        Button(
                            onClick = {
                                // Fullscreen Button Click
                                // Direct action, no ad (Interstitial handled on entry)
                                isVideoVisible = true
                                isPlayerPlaying = true
                                isFullscreen = true
                            },
                            modifier = Modifier
                                .size(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF333333),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Icon(FullscreenIcon, contentDescription = "Fullscreen")
                        }

                        // Favorite Button
                        val scale by animateFloatAsState(
                            targetValue = if (isFavorite) 1.2f else 1.0f,
                            animationSpec = tween(durationMillis = 300),
                            label = "Heart Animation"
                        )

                        Button(
                            onClick = {
                                isFavorite = !isFavorite
                                if (isFavorite) {
                                    com.threepointogames.movierecap.util.FavoritesManager.addFavorite(
                                        context,
                                        movie.id
                                    )
                                } else {
                                    com.threepointogames.movierecap.util.FavoritesManager.removeFavorite(
                                        context,
                                        movie.id
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .scale(scale),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF333333),
                                contentColor = if (isFavorite) Color.Red else Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color.Red else Color.White
                            )
                        }


                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = movie.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 24.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Comments Section
                    Text(
                        text = "Comments",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Comment Input (Main - Root Comments)
                    if (replyingTo == null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = commentText,
                                onValueChange = { if (it.length <= 280) commentText = it },
                                placeholder = { Text("Add a comment...", color = Color.Gray, style = MaterialTheme.typography.bodyMedium) },
                                modifier = Modifier.weight(1f),
                                supportingText = {
                                    if (commentText.length > 200) {
                                        Text(
                                            text = "${commentText.length}/280",
                                            color = if (commentText.length == 280) Color.Red else Color.LightGray,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                                        )
                                    }
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF2A2A2A),
                                    unfocusedContainerColor = Color(0xFF2A2A2A),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(24.dp),
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            IconButton(
                                onClick = {
                                    if (commentText.isNotBlank() && !isPosting) {
                                        isPosting = true
                                    }
                                },
                                enabled = commentText.isNotBlank() && !isPosting,
                                modifier = Modifier.size(48.dp)
                            ) {
                               if (isPosting) {
                                   CircularProgressIndicator(
                                       modifier = Modifier.size(24.dp),
                                       color = com.threepointogames.movierecap.ui.theme.GomoPrimaryPink,
                                       strokeWidth = 2.dp
                                   )
                               } else {
                                   Icon(
                                       Icons.Default.Send, 
                                       contentDescription = "Send", 
                                       tint = if (commentText.isNotBlank()) com.threepointogames.movierecap.ui.theme.GomoPrimaryPink else Color.Gray
                                   )
                               }
                            }
                        }
                    }
                    
                    // Hack to launch effect when posting
                    LaunchedEffect(isPosting) {
                        if (isPosting) {
                             val success = if (replyingTo != null) {
                                 repository.addReply(movie.id, replyingTo!!.id, commentText, userName)
                             } else {
                                 repository.addComment(movie.id, commentText, userName)
                             }
                             
                             if (success) {
                                 commentText = ""
                                 replyingTo = null
                                 comments = repository.getComments(movie.id)
                             }
                             isPosting = false
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isLoadingComments) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = com.threepointogames.movierecap.ui.theme.GomoPrimaryPink)
                        }
                    } else {
                        if (comments.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No comments yet. Start the discussion!",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            comments.forEach { comment ->
                                Column(
                                    modifier = Modifier
                                        .padding(vertical = 1.dp)
                                        .fillMaxWidth()
                                        .background(Color(0xFF20162C), RoundedCornerShape(12.dp))
                                        .padding(8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Avatar Circle
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .background(
                                                    com.threepointogames.movierecap.ui.theme.GomoPurpleMid.copy(alpha=0.3f), 
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = comment.userName.take(1).uppercase(),
                                                color = com.threepointogames.movierecap.ui.theme.GomoPrimaryPink,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = comment.userName,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        // Optional: Time ago
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = comment.text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                    
                                    // Reply/Cancel Buttons
                                    if (replyingTo?.id == comment.id) {
                                        // Inline Input
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextField(
                                                value = commentText,
                                                onValueChange = { if (it.length <= 280) commentText = it },
                                                placeholder = { Text("Reply to ${comment.userName}...", color = Color.Gray, style = MaterialTheme.typography.bodySmall) },
                                                modifier = Modifier.weight(1f),
                                                supportingText = {
                                                    if (commentText.length > 200) {
                                                        Text(
                                                            text = "${commentText.length}/280",
                                                            color = if (commentText.length == 280) Color.Red else Color.LightGray,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            modifier = Modifier.fillMaxWidth(),
                                                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                                                        )
                                                    }
                                                },
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = Color.Black.copy(alpha=0.3f),
                                                    unfocusedContainerColor = Color.Black.copy(alpha=0.3f),
                                                    focusedTextColor = Color.White,
                                                    unfocusedTextColor = Color.White,
                                                    focusedIndicatorColor = Color.Transparent,
                                                    unfocusedIndicatorColor = Color.Transparent,
                                                    disabledIndicatorColor = Color.Transparent
                                                ),
                                                shape = RoundedCornerShape(16.dp),
                                                textStyle = MaterialTheme.typography.bodySmall
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            IconButton(
                                                onClick = {
                                                    if (commentText.isNotBlank() && !isPosting) {
                                                        isPosting = true
                                                    }
                                                },
                                                enabled = commentText.isNotBlank() && !isPosting,
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                               if (isPosting) {
                                                   CircularProgressIndicator(
                                                       modifier = Modifier.size(20.dp),
                                                       color = com.threepointogames.movierecap.ui.theme.GomoSecondaryMint,
                                                       strokeWidth = 2.dp
                                                   )
                                               } else {
                                                   Icon(
                                                       Icons.Default.Send, 
                                                       contentDescription = "Send Reply", 
                                                       tint = if (commentText.isNotBlank()) com.threepointogames.movierecap.ui.theme.GomoSecondaryMint else Color.Gray
                                                   )
                                               }
                                            }
                                        }
                                        // Cancel Button
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            TextButton(onClick = { 
                                                replyingTo = null
                                                commentText = "" // Clear draft on cancel?
                                            }) {
                                                Text("Cancel", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }

                                    } else {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            TextButton(onClick = { 
                                                replyingTo = comment 
                                                commentText = "" // Start fresh
                                            }) {
                                                Text("Reply", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }

                                    // Render Replies
                                    if (comment.replies.isNotEmpty()) {
                                        comment.replies.values.sortedBy { it.timestamp }.forEach { reply ->
                                            Row(modifier = Modifier.padding(start = 16.dp, top = 8.dp)) {
                                                // Small connector line (visual only, simplified)
                                                Box(
                                                    modifier = Modifier
                                                        .width(2.dp)
                                                        .height(30.dp)
                                                        .background(Color.Gray.copy(alpha=0.3f))
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = reply.userName,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = com.threepointogames.movierecap.ui.theme.GomoSecondaryMint.copy(alpha=0.8f)
                                                        )
                                                    }
                                                    Text(
                                                        text = reply.text,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color.White.copy(alpha=0.8f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
        }
    }

}
}

// Helper function to shift Subtitle timestamps (VTT uses . and SRT uses ,)
fun shiftSubtitleTimestamps(content: String, delayMs: Long): String {
    // Regex matches "MM:SS.mmm" or "HH:MM:SS.mmm" or with comma for SRT
    val pattern = Pattern.compile("(\\d{2,}:)?(\\d{2}):(\\d{2})([.,])(\\d{3})")
    val matcher = pattern.matcher(content)
    val sb = StringBuffer()

    while (matcher.find()) {
        try {
            val hours = matcher.group(1)?.removeSuffix(":")?.toLong() ?: 0L
            val minutes = matcher.group(2)?.toLong() ?: 0L
            val seconds = matcher.group(3)?.toLong() ?: 0L
            val separator = matcher.group(4) ?: "."
            val millis = matcher.group(5)?.toLong() ?: 0L

            val originalTimeMs =
                (hours * 3600000) + (minutes * 60000) + (seconds * 1000) + millis
            val newTimeMs = (originalTimeMs + delayMs).coerceAtLeast(0)

            val newHours = newTimeMs / 3600000
            val newMinutes = (newTimeMs % 3600000) / 60000
            val newSeconds = (newTimeMs % 60000) / 1000
            val newMillis = newTimeMs % 1000

            val newTimeStr = if (newHours > 0 || matcher.group(1) != null) {
                String.format(
                    "%02d:%02d:%02d%s%03d",
                    newHours,
                    newMinutes,
                    newSeconds,
                    separator,
                    newMillis
                )
            } else {
                String.format("%02d:%02d%s%03d", newMinutes, newSeconds, separator, newMillis)
            }

            matcher.appendReplacement(sb, newTimeStr)
        } catch (e: Exception) {
            // If parsing fails, preserve original
        }
    }
    matcher.appendTail(sb)
    return sb.toString()
}
