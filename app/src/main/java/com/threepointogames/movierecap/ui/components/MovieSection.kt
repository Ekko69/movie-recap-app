package com.threepointogames.movierecap.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.threepointogames.movierecap.model.Movie

@Composable
fun MovieSection(
    title: String,
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit,
    isInfinite: Boolean = false,
    maxItems: Int? = null,
    onSeeAllClick: (() -> Unit)? = null,
    cardSize: MovieCardSize = MovieCardSize.LANDSCAPE
) {
    if (movies.isEmpty()) return

    Column(
        modifier = Modifier.padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), // Reduced end padding
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            if (onSeeAllClick != null) {
                IconButton(onClick = onSeeAllClick) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Rounded.KeyboardArrowRight,
                        contentDescription = "See All",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp) // Slightly larger to match the bold look
                    )
                }
            }
        }
        
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            if (isInfinite) {
                 items(
                     count = Int.MAX_VALUE,
                     itemContent = { index ->
                         val movie = movies[index % movies.size]
                         MovieCard(
                             movie = movie, 
                             onClick = { onMovieClick(movie) },
                             size = cardSize
                         )
                     }
                 )
            } else {
                val displayMovies = if (maxItems != null) movies.take(maxItems) else movies
                items(displayMovies) { movie ->
                    MovieCard(
                        movie = movie, 
                        onClick = { onMovieClick(movie) },
                        size = cardSize
                    )
                }
            }
        }
    }
}
