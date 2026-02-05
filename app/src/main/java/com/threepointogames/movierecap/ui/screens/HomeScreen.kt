package com.threepointogames.movierecap.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import com.threepointogames.movierecap.ui.components.MovieCard

import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.threepointogames.movierecap.data.MovieRepository
import com.threepointogames.movierecap.model.Movie
import com.threepointogames.movierecap.ui.components.MovieCardSize
import com.threepointogames.movierecap.ui.components.MovieSection

@Composable
fun HomeScreen(
    movies: List<Movie>,
    onMovieClick: (Movie) -> Unit,
    onSeeAllClick: () -> Unit,
    onCategorySeeAllClick: (String) -> Unit
) {
    // Extract unique categories from all movies and sort with "Top Picks" first
    // Deduplicate movies into categories (Smart Distribution)
    val moviesByCategory = remember(movies) {
        val distribution = mutableMapOf<String, MutableList<Movie>>()
        val assignedMovieIds = mutableSetOf<String>()

        // 1. Top Picks (Priority)
        val topPicks = movies.filter { it.categories.contains("Top Picks") }
        if (topPicks.isNotEmpty()) {
            distribution["Top Picks"] = topPicks.toMutableList()
            assignedMovieIds.addAll(topPicks.map { it.id })
        }

        // 2. Distribute remaining movies to their first valid genre
        movies.forEach { movie ->
            if (movie.id !in assignedMovieIds) {
                // Find the first valid unique category
                val targetCategory = movie.categories.firstOrNull { it != "New" && it != "Top Picks" }
                if (targetCategory != null) {
                    distribution.getOrPut(targetCategory) { mutableListOf() }.add(movie)
                    assignedMovieIds.add(movie.id)
                }
            }
        }
        distribution
    }

    // Extract unique categories from keys to maintain order
    val allCategories = remember(moviesByCategory) {
        moviesByCategory.keys.toList().sortedWith(compareBy { 
            when (it) {
                "Top Picks" -> 0
                else -> 1
            }
        })
    }
    
    // Search State
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Analytics: Log Screen View
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        com.threepointogames.movierecap.util.AnalyticsManager.logScreenView("Home Screen")
    }

    // State for IDs (Refreshed on lifecycle events)
    var historyIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var favoriteIds by remember { mutableStateOf<List<String>>(emptyList()) }

    fun refreshIds() {
         historyIds = com.threepointogames.movierecap.util.WatchHistoryManager.getWatchedMovieIds(context)
         favoriteIds = com.threepointogames.movierecap.util.FavoritesManager.getFavoriteMovieIds(context)
    }

    // Initial load
    LaunchedEffect(Unit) {
        refreshIds()
    }
    
    // Compute lists derived from movies and IDs
    val continueWatchingMovies = remember(movies, historyIds) {
        movies.filter { it.id in historyIds }.sortedBy { historyIds.indexOf(it.id) }
    }
    
    val favoriteMovies = remember(movies, favoriteIds) {
        movies.filter { it.id in favoriteIds }
    }

    // 10-second rotation for Hero Movie
    var heroMovie by remember { mutableStateOf<Movie?>(null) }
    LaunchedEffect(movies) {
        if (movies.isNotEmpty()) {
            val candidates = movies
            while (true) {
                heroMovie = candidates.random()
                kotlinx.coroutines.delay(10000) // 10 seconds
            }
        }
    }

    // Banner Ad: Hoisted State to persist across LazyColumn recycling
    val adView = remember {
        com.google.android.gms.ads.AdView(context).apply {
            setAdSize(com.google.android.gms.ads.AdSize.BANNER)
            adUnitId = "ca-app-pub-2958975586761098/5355724210"
            setAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(adError: com.google.android.gms.ads.LoadAdError) {
                    android.util.Log.e("HomeScreen", "Ad failed to load: ${adError.message}")
                }
            })
            loadAd(com.google.android.gms.ads.AdRequest.Builder().build())
        }
    }

    // Lifecycle Owner
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    // Lifecycle for AdView
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> adView.resume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> adView.pause()
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> adView.destroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    // Recently Added Movies (for filtering)
    // Take top 20 newest to ensure relevance, then shuffle and take 10 for variety
    val recentlyAdded = remember(movies) {
        movies.sortedByDescending { it.updatedAt ?: 0L }
            .take(20)
            .shuffled()
            .take(10)
    }

    // Refresh history on resume to capture recent watches
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                 refreshIds()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }

    }

    if (movies.isEmpty()) {
        LoadingHomeScreen()
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (isSearchActive) {
                 // Search Bar
                 Row(
                     modifier = Modifier
                         .fillMaxWidth()
                         .background(MaterialTheme.colorScheme.background)
                         .padding(horizontal = 16.dp, vertical = 8.dp),
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     IconButton(onClick = { 
                         isSearchActive = false 
                         searchQuery = ""
                     }) {
                         Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                     }
                     TextField(
                         value = searchQuery,
                         onValueChange = { searchQuery = it },
                         placeholder = { Text("Search any movies...", color = Color.Gray) },
                         modifier = Modifier.weight(1f),
                         colors = TextFieldDefaults.colors(
                             focusedContainerColor = Color.Transparent,
                             unfocusedContainerColor = Color.Transparent,
                             focusedIndicatorColor = Color.Transparent,
                             unfocusedIndicatorColor = Color.Transparent,
                             cursorColor = Color.White,
                             focusedTextColor = Color.White,
                             unfocusedTextColor = Color.White
                         ),
                         singleLine = true
                     )
                     if (searchQuery.isNotEmpty()) {
                         IconButton(onClick = { searchQuery = "" }) {
                             Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White)
                         }
                     }
                 }
            } else {
                // Custom App Bar
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Logo Image
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.threepointogames.movierecap.R.drawable.app_logo),
                            contentDescription = "Movie Recap Logo",
                            modifier = Modifier
                                .height(60.dp) 
                                .width(200.dp),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )

                        Row {
                            IconButton(onClick = { isSearchActive = true }) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            IconButton(onClick = { /* TODO */ }) {
                                androidx.compose.foundation.Image(
                                    painter = androidx.compose.ui.res.painterResource(id = com.threepointogames.movierecap.R.drawable.header_icon),
                                    contentDescription = "Profile",
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                )
                            }
                        }
                    }
                    
                    // Category Tabs removed as per request
                }
            }
        }
    ) { paddingValues ->
        if (!isSearchActive) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
            // Banner Ad Section
            item(key = "banner_ad") {
                com.threepointogames.movierecap.ui.components.ReusableAdMobBanner(
                    adView = adView,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // 0. Hero Section (Random)
            heroMovie?.let { movie ->
                item(key = "hero_section") {
                   com.threepointogames.movierecap.ui.components.HeroSection(
                       movie = movie,
                       onMovieClick = { selectedMovie ->
                           com.threepointogames.movierecap.util.AnalyticsManager.logMovieClick(selectedMovie.id, selectedMovie.title)
                           onMovieClick(selectedMovie)
                       }
                   )
                   Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // Dynamically create sections for each category
            // 0. Continue Watching Section (Added dynamically)

            
            allCategories.forEach { category ->
                item {
                    val categoryMovies = moviesByCategory[category] ?: emptyList()
                    // ... rest of code
                    val cardSize = if (category == "Top Picks") {
                        MovieCardSize.PORTRAIT
                    } else {
                        MovieCardSize.LANDSCAPE
                    }
                    MovieSection(
                        title = category,
                        movies = categoryMovies,
                        onMovieClick = { movie -> 
                            com.threepointogames.movierecap.util.AnalyticsManager.logMovieClick(movie.id, movie.title)
                            onMovieClick(movie) 
                        },
                        isInfinite = true,
                        cardSize = cardSize,
                        onSeeAllClick = {
                             onCategorySeeAllClick(category)
                        }
                    )
                }

                if (category == "Top Picks") {
                     if (recentlyAdded.isNotEmpty()) {
                         item {
                             MovieSection(
                                 title = "Recently Added",
                                 movies = recentlyAdded,
                                 onMovieClick = { movie ->
                                     com.threepointogames.movierecap.util.AnalyticsManager.logMovieClick(movie.id, movie.title)
                                     onMovieClick(movie)
                                 },
                                 isInfinite = true,
                                 cardSize = MovieCardSize.LANDSCAPE,
                                 onSeeAllClick = {
                                      onCategorySeeAllClick("Recently Added")
                                 }
                             )
                         }
                     }
                }


            }

            // My Favorites Section
            if (favoriteMovies.isNotEmpty()) {
                item {
                    MovieSection(
                        title = "My Favorites",
                        movies = favoriteMovies,
                        onMovieClick = { movie ->
                            com.threepointogames.movierecap.util.AnalyticsManager.logMovieClick(movie.id, movie.title)
                            onMovieClick(movie)
                        },
                        isInfinite = false,
                        onSeeAllClick = {
                            onCategorySeeAllClick("Favorites")
                        },
                        cardSize = MovieCardSize.LANDSCAPE
                    )
                }
            }



            // Recently Watched Section
            if (continueWatchingMovies.isNotEmpty()) {
                item {
                    MovieSection(
                        title = "Recently Watched",
                        movies = continueWatchingMovies,
                        onMovieClick = { movie ->
                            com.threepointogames.movierecap.util.AnalyticsManager.logMovieClick(movie.id, movie.title)
                            onMovieClick(movie)
                        },
                        isInfinite = false,
                        onSeeAllClick = {
                            onCategorySeeAllClick("Recently Watched")
                        },
                        cardSize = MovieCardSize.LANDSCAPE
                    )
                }
            }
            
            // Add "All Movies" section at the end
            item {
                MovieSection(
                    title = "All Movies",
                    movies = movies,
                    onMovieClick = { movie ->
                        com.threepointogames.movierecap.util.AnalyticsManager.logMovieClick(movie.id, movie.title)
                        onMovieClick(movie)
                    },
                    isInfinite = true,
                    onSeeAllClick = {
                        com.threepointogames.movierecap.util.AnalyticsManager.logCategoryView("All Movies Bottom")
                        onSeeAllClick()
                    }
                )
            }
            
            // Extra padding at bottom
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        } else {
            // Search Results Grid
            val searchResults = movies.filter { it.title.contains(searchQuery, ignoreCase = true) }
            
            if (searchResults.isEmpty() && searchQuery.isNotEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Text("No movies found for \"$searchQuery\"", color = Color.White)
                }
            } else {
                LazyVerticalGrid(
                     columns = GridCells.Fixed(2),
                     contentPadding = androidx.compose.foundation.layout.PaddingValues(
                         start = 16.dp, end = 16.dp, top = paddingValues.calculateTopPadding() + 16.dp, bottom = 16.dp
                     ),
                     horizontalArrangement = Arrangement.spacedBy(16.dp),
                     verticalArrangement = Arrangement.spacedBy(16.dp),
                     modifier = Modifier.fillMaxSize()
                ) {
                     items(searchResults) { movie ->
                         MovieCard(
                             movie = movie,
                             onClick = { 
                                 com.threepointogames.movierecap.util.AnalyticsManager.logMovieClick(movie.id, movie.title)
                                 onMovieClick(movie) 
                             },
                             modifier = Modifier.fillMaxWidth()
                         )
                     }
                }
            }
        }
        }
    }
    }

