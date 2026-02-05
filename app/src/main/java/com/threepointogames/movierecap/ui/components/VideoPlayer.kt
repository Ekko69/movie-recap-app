package com.threepointogames.movierecap.ui.components

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    exoPlayer: ExoPlayer,
    onFullscreenClick: () -> Unit,
    onControllerVisibilityChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AndroidView(
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                // Enable controls
                useController = true
                
                // Show buffering indicator
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                
                // Enable seeking - critical settings
                controllerShowTimeoutMs = 5000 // Hide controls after 5 seconds
                setControllerHideOnTouch(true) // Allow hiding on touch
                
                // Show rewind and fast forward buttons
                setShowRewindButton(true)
                setShowFastForwardButton(true)
                setShowSubtitleButton(true)
                
                // Show next/previous buttons (optional, can be disabled)
                setShowNextButton(false)
                setShowPreviousButton(false)
                
                // Enable fullscreen button
                setFullscreenButtonClickListener { isFullscreen ->
                    onFullscreenClick()
                }

                setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                    onControllerVisibilityChanged(visibility == View.VISIBLE)
                })
            }
        },
        modifier = modifier,
        update = { playerView ->
            // Ensure player is set and controls are enabled
            playerView.player = exoPlayer
            playerView.useController = true
            
            playerView.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                onControllerVisibilityChanged(visibility == View.VISIBLE)
            })

            // Configure Subtitles
            playerView.subtitleView?.apply {
                setApplyEmbeddedStyles(false) // Ignore styles from the file
                setApplyEmbeddedFontSizes(false)
                setBottomPaddingFraction(0.08f) // Raise slightly
                // Ensure text wraps
                setFractionalTextSize(androidx.media3.ui.SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * 1.1f) 
            }
        }
    )
}
