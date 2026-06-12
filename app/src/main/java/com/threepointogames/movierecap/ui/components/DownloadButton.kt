package com.threepointogames.movierecap.ui.components

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.threepointogames.movierecap.model.Movie
import com.threepointogames.movierecap.util.DownloadManager
import com.threepointogames.movierecap.util.PurchaseManager

@Composable
fun DownloadButton(
    movie: Movie,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val isDownloaded = DownloadManager.isDownloaded(movie.id)
    val progress = DownloadManager.downloadProgress[movie.id]
    val isDownloading = progress != null
    var showLimitDialog by remember { mutableStateOf(false) }

    if (showLimitDialog) {
        DownloadLimitDialog(
            onDismiss = { showLimitDialog = false },
            onUnlock = {
                showLimitDialog = false
                activity?.let { act ->
                    PurchaseManager.launchDownloadsPurchaseFlow(act) { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    Column(modifier = modifier) {
        OutlinedButton(
            onClick = {
                if (!isDownloaded && !isDownloading) {
                    DownloadManager.downloadMovie(
                        movie = movie,
                        onLimitReached = { showLimitDialog = true },
                        onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (isDownloaded) Color(0xFF1E3A2E) else Color.Transparent,
                contentColor = Color(0xFF4ADE80)
            ),
            border = BorderStroke(
                1.dp,
                if (isDownloaded) Color(0xFF4ADE80) else Color(0xFF4ADE80).copy(alpha = 0.6f)
            ),
            shape = RoundedCornerShape(25.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        isDownloaded -> "✓  Downloaded"
                        isDownloading -> "↻  ${progress}%"
                        else -> "⬇  Download"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isDownloading && progress != null) {
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF4ADE80),
                trackColor = Color(0xFF333333)
            )
        }
    }
}
