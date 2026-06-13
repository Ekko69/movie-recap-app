package com.threepointogames.movierecap.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.threepointogames.movierecap.R
import com.threepointogames.movierecap.util.PurchaseManager

private val Purple = Color(0xFF6D28D9)
private val PurpleLight = Color(0xFFEDE9FE)
private val PurpleGradientStart = Color(0xFF8B5CF6)
private val PurpleGradientEnd = Color(0xFF5B21B6)
private val TextDark = Color(0xFF111827)
private val TextMuted = Color(0xFF6B7280)

@Composable
fun DownloadLimitDialog(
    onDismiss: () -> Unit,
    onUnlock: () -> Unit
) {
    val price = PurchaseManager.productPriceDownloads.ifEmpty { "\$4" }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(340.dp)
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .clickable(enabled = false) {}
            ) {
                // Close button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 32.dp, bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Download icon in light purple circle
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(PurpleLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_download),
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            colorFilter = ColorFilter.tint(Purple)
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    // Title
                    Text("Unlock", fontSize = 20.sp, fontWeight = FontWeight.Normal, color = TextDark)
                    Text(
                        "UNLIMITED",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Purple,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "MOVIE DOWNLOADS!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        letterSpacing = 0.5.sp
                    )

                    Spacer(Modifier.height(14.dp))

                    // Subtitle with "No limits!" in purple
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(color = TextMuted)) {
                                append("Download as many movies as you want.\nWatch anytime, anywhere. ")
                            }
                            withStyle(SpanStyle(color = Purple, fontWeight = FontWeight.Bold)) {
                                append("No limits!")
                            }
                        },
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 21.sp
                    )

                    Spacer(Modifier.height(24.dp))

                    // 3 feature items — Box dividers to avoid Divider's fillMaxWidth side effect
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                        FeatureItem(R.drawable.ic_infinity, "Unlimited\nDownloads")
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(72.dp)
                                .background(Color(0xFFE5E7EB))
                        )
                        FeatureItem(R.drawable.ic_download, "High Quality\nDownloads")
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(72.dp)
                                .background(Color(0xFFE5E7EB))
                        )
                        FeatureItem(R.drawable.ic_phone_outline, "Watch Offline\nAnytime")
                    }

                    Spacer(Modifier.height(28.dp))

                    // CTA button with purple gradient
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(PurpleGradientStart, PurpleGradientEnd)
                                ),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { onUnlock() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "UNLOCK NOW FOR $price",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // Footer with lock icon from our own XML drawable
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_lock),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            colorFilter = ColorFilter.tint(Purple)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Secure payment  •  Instant access",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(iconRes: Int, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(88.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .border(1.5.dp, Purple, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(Purple)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            fontSize = 12.sp,
            color = TextDark,
            textAlign = TextAlign.Center,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
