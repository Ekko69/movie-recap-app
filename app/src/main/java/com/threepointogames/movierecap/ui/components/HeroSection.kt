package com.threepointogames.movierecap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.threepointogames.movierecap.model.Movie

@Composable
fun HeroSection(
    movie: Movie,
    onMovieClick: (Movie) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp)
    ) {
        // 1. Background Image
        AsyncImage(
            model = movie.featuredThumbnail ?: movie.thumbnail,
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f),
                            Color(0xFF20162C) // Fade out to background color (GomoSurfacePurple)
                        )
                    )
                )
        )

        // 2b. Top Gradient Overlay (Seamless Fade)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp) // Facade height
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF20162C), // GomoSurfacePurple
                            Color.Transparent
                        )
                    )
                )
        )

        // 3. Content
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Title
            Text(
                text = movie.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Metadata Chips
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionChip(
                    onClick = { },
                    label = { Text(movie.year.toString(), color = Color.White) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color(0xFF333333),
                        labelColor = Color.White
                    ),
                    border = null,
                    shape = RoundedCornerShape(50)
                )

                movie.categories.take(2).forEach { category ->
                    SuggestionChip(
                        onClick = { },
                        label = { Text(category, color = Color.White) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xFF333333),
                            labelColor = Color.White
                        ),
                        border = null,
                        shape = RoundedCornerShape(50)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Gradient "Watch Now" / Details Button
            Button(
                onClick = { onMovieClick(movie) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                contentPadding = PaddingValues(), // Necessary for gradient background
                shape = RoundedCornerShape(28.dp) // Capsule shape
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFF0060), // GomoPrimaryPink
                                    Color(0xFFb448b5)  // GomoPurpleMid
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Get More Details", // Keeping "Get More Details" text but styled like "Watch Now"
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
