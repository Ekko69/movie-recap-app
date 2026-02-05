package com.threepointogames.movierecap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.threepointogames.movierecap.model.Movie

enum class MovieCardSize {
    LANDSCAPE,  // 16:9 landscape (default)
    PORTRAIT    // Portrait size for Top Picks
}

@Composable
fun MovieCard(
    movie: Movie,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: MovieCardSize = MovieCardSize.LANDSCAPE
) {
    val cardWidth = when (size) {
        MovieCardSize.LANDSCAPE -> 280.dp
        MovieCardSize.PORTRAIT -> 140.dp  // Half the width
    }
    
    val aspectRatio = when (size) {
        MovieCardSize.LANDSCAPE -> 16f / 9f
        MovieCardSize.PORTRAIT -> 9f / 16f  // Portrait aspect ratio 9:16
    }
    
    Card(
        modifier = modifier
            .width(cardWidth)
            .padding(end = 12.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent // Transparent to let image shine
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            // Thumbnail Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                com.threepointogames.movierecap.ui.theme.GomoPrimaryPink,
                                com.threepointogames.movierecap.ui.theme.Purple40
                            )
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Determine which image to show
                val imageModel = if (size == MovieCardSize.PORTRAIT && !movie.featuredThumbnail.isNullOrEmpty()) {
                    movie.featuredThumbnail
                } else {
                    movie.thumbnail
                }

                if (!imageModel.isNullOrEmpty()) {

                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageModel)
                            .crossfade(true)
                            .build(),
                        contentDescription = movie.title,
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
                    // Placeholder when no thumbnail
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                         Text(
                             text = movie.title.take(1).uppercase(),
                             style = MaterialTheme.typography.titleLarge,
                             color = MaterialTheme.colorScheme.onPrimaryContainer
                         )
                    }
                }
                
                // Duration Badge inside image
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = movie.duration,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            // Metadata below image
            Column(
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = movie.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = if (size == MovieCardSize.PORTRAIT) 14.sp else 16.sp
                    ),
                    color = Color.White,
                    maxLines = if (size == MovieCardSize.PORTRAIT) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = movie.year.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
