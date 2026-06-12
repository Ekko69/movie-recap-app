package com.threepointogames.movierecap.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.threepointogames.movierecap.model.Movie
import com.threepointogames.movierecap.ui.components.DownloadLimitDialog
import com.threepointogames.movierecap.ui.components.shimmerEffect
import com.threepointogames.movierecap.util.DownloadManager
import com.threepointogames.movierecap.util.PurchaseManager

@Composable
fun DownloadsScreen(
    allMovies: List<Movie>,
    onMovieClick: (Movie) -> Unit,
    onBack: () -> Unit
) {
    val isPremium = PurchaseManager.isUnlimitedDownloads
    val downloadedIds = DownloadManager.downloadedMovieIds.toList()
    val downloadedMovies = remember(downloadedIds, allMovies) {
        downloadedIds.mapNotNull { id -> allMovies.find { it.id == id } }
    }
    var showUpgradeDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as? Activity

    if (showUpgradeDialog) {
        DownloadLimitDialog(
            onDismiss = { showUpgradeDialog = false },
            onUnlock = {
                showUpgradeDialog = false
                activity?.let { act ->
                    PurchaseManager.launchDownloadsPurchaseFlow(act) { msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    "My Downloads",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            if (!isPremium) {
                item {
                    val count = DownloadManager.downloadCount
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "$count of ${DownloadManager.FREE_LIMIT} free slots used",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        Button(
                            onClick = { showUpgradeDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A2A1A)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 12.dp, vertical = 6.dp
                            )
                        ) {
                            val price = PurchaseManager.productPriceDownloads.ifEmpty { "₱200" }
                            Text(
                                "⬆ Unlock Unlimited — $price",
                                fontSize = 11.sp,
                                color = Color(0xFF4ADE80)
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = count.toFloat() / DownloadManager.FREE_LIMIT.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color(0xFFE50914),
                        trackColor = Color(0xFF333333)
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            if (downloadedMovies.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⬇", fontSize = 48.sp)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No downloads yet",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Find a movie and tap ⬇ to save it for offline viewing.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(downloadedMovies, key = { it.id }) { movie ->
                    DownloadedMovieRow(
                        movie = movie,
                        onPlay = { onMovieClick(movie) },
                        onDelete = { DownloadManager.deleteDownload(movie.id) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun DownloadedMovieRow(
    movie: Movie,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val fileSize = remember(movie.id) { DownloadManager.getFileSizeFormatted(movie.id) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A1A), RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context).data(movie.thumbnail).crossfade(true).build(),
            contentDescription = movie.title,
            modifier = Modifier
                .size(width = 60.dp, height = 80.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop,
            loading = {
                Box(Modifier.fillMaxSize().shimmerEffect())
            }
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                movie.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2
            )
            Spacer(Modifier.height(4.dp))
            Text(
                buildString {
                    append("${movie.year} · ${movie.duration}")
                    if (fileSize.isNotEmpty()) append(" · $fileSize")
                },
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onPlay,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 10.dp, vertical = 6.dp
            )
        ) {
            Text("▶ Play", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(6.dp))
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .background(Color(0xFF2A1A1A), RoundedCornerShape(8.dp))
                .size(36.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color(0xFFE05252),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
