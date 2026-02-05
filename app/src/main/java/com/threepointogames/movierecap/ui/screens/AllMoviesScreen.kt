package com.threepointogames.movierecap.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.threepointogames.movierecap.data.MovieRepository
import com.threepointogames.movierecap.model.Movie
import com.threepointogames.movierecap.ui.components.MovieCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllMoviesScreen(
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit,
    onBack: () -> Unit,
    title: String = "All Movies"
) {
    var sortOption by remember { mutableStateOf(SortOption.NONE) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }

    val sortedMovies = remember(movies, sortOption) {
        when (sortOption) {
            SortOption.YEAR_DESC -> movies.sortedByDescending { it.year }
            SortOption.YEAR_ASC -> movies.sortedBy { it.year }
            SortOption.TITLE_ASC -> movies.sortedBy { it.title }
            SortOption.TITLE_DESC -> movies.sortedByDescending { it.title }
            SortOption.NONE -> movies
        }
    }
    LaunchedEffect(Unit) {
        com.threepointogames.movierecap.util.AnalyticsManager.logScreenView("All Movies Screen")
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { isSortMenuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Sort",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = isSortMenuExpanded,
                            onDismissRequest = { isSortMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Year (Newest)") },
                                onClick = { sortOption = SortOption.YEAR_DESC; isSortMenuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Year (Oldest)") },
                                onClick = { sortOption = SortOption.YEAR_ASC; isSortMenuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Title (A-Z)") },
                                onClick = { sortOption = SortOption.TITLE_ASC; isSortMenuExpanded = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Title (Z-A)") },
                                onClick = { sortOption = SortOption.TITLE_DESC; isSortMenuExpanded = false }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = paddingValues.calculateTopPadding() + 8.dp,
                bottom = 16.dp
            ),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(sortedMovies) { movie ->
                // MovieCard is designed for horizontal rows (fixed width).
                // In grid, we might want it to fill width.
                // We'll trust Modifier.fillMaxWidth() passed by Grid if MovieCard supports it,
                // or we wrap it in a Box.
                // Looking at MovieCard.kt: .width(280.dp) is hardcoded.
                // We should make MovieCard width flexible or create a helper for grid.
                // For now, let's use the existing one but we might need to override width modifier.
                // Since MovieCard accepts modifier, we can try overriding it.
                
                Box(modifier = Modifier.fillMaxWidth()) {
                     MovieCard(
                        movie = movie,
                        onClick = { 
                            com.threepointogames.movierecap.util.AnalyticsManager.logMovieClick(movie.id, movie.title)
                            onMovieClick(movie)
                        },
                        modifier = Modifier.fillMaxWidth() // Attempt to override width
                    )
                }
            }
        }
    }
}

enum class SortOption {
    NONE, YEAR_DESC, YEAR_ASC, TITLE_ASC, TITLE_DESC
}
