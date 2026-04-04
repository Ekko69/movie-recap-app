package com.threepointogames.movierecap.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isAdFree) {
                // Already purchased
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Ads Removed",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You're enjoying Movie Recap ad-free. Thank you for your support!",
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                TextButton(onClick = onDismiss) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            } else {
                // Purchase offer
                Text(
                    text = "Remove Ads",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Enjoy Movie Recap completely ad-free — no banners, no interruptions, forever.",
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "One-time purchase • Lifetime access • Per device",
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
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
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "₱300.00 — Remove Ads",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Maybe Later", color = Color.Gray)
                }
            }
        }
    }
}
