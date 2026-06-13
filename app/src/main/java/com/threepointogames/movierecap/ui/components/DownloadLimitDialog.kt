package com.threepointogames.movierecap.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.threepointogames.movierecap.R
import com.threepointogames.movierecap.ui.theme.GomoPrimaryPink
import com.threepointogames.movierecap.ui.theme.GomoPurpleMid
import com.threepointogames.movierecap.util.PurchaseManager
import kotlinx.coroutines.launch

@Composable
fun DownloadLimitDialog(
    onDismiss: () -> Unit,
    onUnlock: () -> Unit
) {
    val price = PurchaseManager.productPriceDownloads.ifEmpty { "₱200" }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        // Entry animations
        val scale = remember { Animatable(0.8f) }
        val alpha = remember { Animatable(0f) }

        LaunchedEffect(Unit) {
            launch { scale.animateTo(1f, animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f)) }
            launch { alpha.animateTo(1f, animationSpec = tween(300)) }
        }

        // Pulse animation for icon
        val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "PulseScale"
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f * alpha.value))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(320.dp)
                    .scale(scale.value)
                    .background(Color(0xFF1E1E1E), RoundedCornerShape(24.dp))
                    .clickable(enabled = false) {}
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pulsing icon
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(100.dp * pulseScale)
                            .background(
                                GomoPrimaryPink.copy(alpha = 0.2f),
                                CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                Brush.radialGradient(listOf(GomoPurpleMid, GomoPrimaryPink)),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.download_white),
                            contentDescription = "Download",
                            modifier = Modifier.size(38.dp),
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    "Download Limit Reached",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    "You've used all 3 free downloads. Upgrade to unlock unlimited offline viewing.",
                    fontSize = 14.sp,
                    color = Color(0xFFAAAAAA),
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp
                )

                Spacer(Modifier.height(24.dp))
                Divider(color = Color.White.copy(alpha = 0.08f))
                Spacer(Modifier.height(20.dp))

                // Perks
                PerkRow(emoji = "⬇️", text = "Unlimited movie downloads")
                Spacer(Modifier.height(12.dp))
                PerkRow(emoji = "📱", text = "Watch offline anytime, anywhere")
                Spacer(Modifier.height(12.dp))
                PerkRow(emoji = "🎬", text = "All movies included")
                Spacer(Modifier.height(12.dp))
                PerkRow(emoji = "♾️", text = "Lifetime access on this device")

                Spacer(Modifier.height(28.dp))

                // Unlock button
                Button(
                    onClick = onUnlock,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GomoPrimaryPink,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(28.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.download_white),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        colorFilter = ColorFilter.tint(Color.White)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Unlock Unlimited — $price",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    "Secure payment via Google Play",
                    color = Color(0xFF666666),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                TextButton(onClick = onDismiss) {
                    Text("Maybe Later", color = Color.Gray, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun PerkRow(emoji: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = emoji, fontSize = 18.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            color = Color.LightGray,
            fontSize = 14.sp
        )
    }
}
