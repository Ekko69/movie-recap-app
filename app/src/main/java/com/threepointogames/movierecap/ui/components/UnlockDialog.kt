package com.threepointogames.movierecap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun UnlockDialog(
    onDismiss: () -> Unit,
    onUnlock: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false // Allow full width control if needed, but we'll limit box width
        )
    ) {
        // Animation States
        val scale = remember { androidx.compose.animation.core.Animatable(0.8f) }
        val alpha = remember { androidx.compose.animation.core.Animatable(0f) }
        
        LaunchedEffect(Unit) {
            // Parallel animation
            launch {
                scale.animateTo(1f, animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 600f))
            }
            launch {
                alpha.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(300))
            }
        }

        // Pulse Animation for Icon
        val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "Pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "PulseScale"
        )
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 0f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Restart
            ),
            label = "PulseAlpha"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f * alpha.value)) // Dim background
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }, // Dismiss on scrim click
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(320.dp) // Fixed width for nice modal look
                    .scale(scale.value)
                    .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(24.dp))
                    .clickable(enabled = false) {} // Prevent click through
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Gift Icon with Glow/Pulse
                Box(contentAlignment = Alignment.Center) {
                    // Ripple/Pulse Effect
                    Box(
                        modifier = Modifier
                            .size(100.dp * pulseScale) // Slightly larger pulse for logo
                            .background(com.threepointogames.movierecap.ui.theme.GomoPrimaryPink.copy(alpha = 0.3f), CircleShape)
                    )
                    
                    // Main Circle (Platform for Logo)
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color.White, CircleShape), // White background for logo clarity
                        contentAlignment = Alignment.Center
                    ) {
                         androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.threepointogames.movierecap.R.drawable.header_icon),
                            contentDescription = "Unlock",
                            modifier = Modifier.size(70.dp).clip(CircleShape),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Title
                Text(
                    text = "Unlock Movie",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Description
                Text(
                    text = "Watch a short ad to unlock this content and enjoy uninterrupted viewing experience.",
                    fontSize = 15.sp, // Slightly smaller for better fit
                    color = Color(0xFFAAAAAA), // Lighter gray for readability
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Button
                Button(
                    onClick = onUnlock,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.threepointogames.movierecap.ui.theme.GomoPrimaryPink, // Pink Button
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(28.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Watch Ad & Unlock",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
