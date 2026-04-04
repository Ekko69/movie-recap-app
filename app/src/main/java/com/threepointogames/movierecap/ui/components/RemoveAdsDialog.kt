package com.threepointogames.movierecap.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.threepointogames.movierecap.util.PurchaseManager

@Composable
fun RemoveAdsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val isAdFree = PurchaseManager.isAdFree
    val productPrice = PurchaseManager.productPrice
    val priceLabel = if (productPrice.isNotEmpty()) "$productPrice — Remove Ads" else "Remove Ads"

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isAdFree) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "You're Ad-Free!",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Thank you for your support! You're getting the full Movie Recap experience with zero interruptions.",
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Awesome!", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            } else {
                // Header
                Text(
                    text = "Go Ad-Free",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Unlock the full Movie Recap experience with a single one-time purchase.",
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(20.dp))

                // Perks list
                PerkRow(emoji = "🚫", text = "No banner ads — ever again")
                Spacer(modifier = Modifier.height(12.dp))
                PerkRow(emoji = "⚡", text = "Skip interstitial ads instantly")
                Spacer(modifier = Modifier.height(12.dp))
                PerkRow(emoji = "🎬", text = "Watch movies without interruptions")
                Spacer(modifier = Modifier.height(12.dp))
                PerkRow(emoji = "♾️", text = "Lifetime access on this device")

                Spacer(modifier = Modifier.height(24.dp))

                // Purchase button
                Button(
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
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = priceLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Secure payment via Google Play",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))
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
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = Color.LightGray,
            fontSize = 14.sp
        )
    }
}
