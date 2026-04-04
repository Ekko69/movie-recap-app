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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.app.Activity
import android.widget.Toast
import com.threepointogames.movierecap.util.PurchaseManager

@Composable
fun UnlockDialog(
    onDismiss: () -> Unit,
    onUnlock: () -> Unit,
    onPurchased: () -> Unit = {}
) {
    val context = LocalContext.current
    val productPrice = PurchaseManager.productPrice
    val removeAdsLabel = if (productPrice.isNotEmpty()) "Remove Ads Forever — $productPrice" else "Remove Ads Forever"
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
                
                // Watch Ad button
                Button(
                    onClick = onUnlock,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.threepointogames.movierecap.ui.theme.GomoPrimaryPink,
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

                Spacer(modifier = Modifier.height(12.dp))

                // Divider with label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Divider(modifier = Modifier.weight(1f), color = Color(0xFF444444))
                    Text(
                        text = "  or  ",
                        color = Color(0xFF888888),
                        fontSize = 12.sp
                    )
                    Divider(modifier = Modifier.weight(1f), color = Color(0xFF444444))
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Remove Ads purchase button
                OutlinedButton(
                    onClick = {
                        val activity = context as? Activity
                        if (activity != null) {
                            PurchaseManager.launchPurchaseFlow(activity) { errorMsg ->
                                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                            }
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(28.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        com.threepointogames.movierecap.ui.theme.GomoPrimaryPink
                    )
                ) {
                    Text(
                        text = removeAdsLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = com.threepointogames.movierecap.ui.theme.GomoPrimaryPink
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "One-time purchase · No banners · No interruptions · Forever",
                    fontSize = 11.sp,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
