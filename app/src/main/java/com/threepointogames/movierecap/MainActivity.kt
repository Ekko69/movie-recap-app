package com.threepointogames.movierecap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.threepointogames.movierecap.model.Movie
import com.threepointogames.movierecap.ui.screens.AllMoviesScreen
import com.threepointogames.movierecap.ui.screens.DetailScreen
import com.threepointogames.movierecap.ui.screens.HomeScreen
import com.threepointogames.movierecap.ui.theme.MovieRecapTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        
        setContent {
            com.threepointogames.movierecap.util.AnalyticsManager.initialize(androidx.compose.ui.platform.LocalContext.current)
            // Initialize AdManager
            com.threepointogames.movierecap.util.AdManager.loadInterstitial(this)
            
            MovieRecapTheme {
                val navController = rememberNavController()
                
                // Using a simple state to pass data or we could pass arguments in route.
                // For simplicity and to avoid serialization issues in route arguments for complex objects
                // we can just stick to ID passing or simple storage. 
                // But since our Movie list is loaded in Home, passing it to Detail via route arg is standard pattern 
                // but requires serialization.
                // Let's implement a simple memory storage for this MVP or pass ID and reload (but we don't have reloading by ID easily setup).
                // I will pass the Movie object by saving it temporarily in a shared state or just serialize it to URL param.
                
                // Simplest for MVP: Single Activity State
                // Hoist State for Stability
                val context = androidx.compose.ui.platform.LocalContext.current
                val repository = remember { com.threepointogames.movierecap.data.MovieRepository(context) }
                // Load and shuffle ONCE
                var allMovies by remember { mutableStateOf<List<Movie>>(emptyList()) }
                
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    try {
                        val loaded = repository.getMovies().shuffled()
                        allMovies = loaded
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                var selectedMovie by remember { mutableStateOf<Movie?>(null) }

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            movies = allMovies, // Pass the stable list
                            onMovieClick = { movie ->
                                selectedMovie = movie
                                com.threepointogames.movierecap.util.AdManager.showInterstitial(this@MainActivity) {
                                    navController.navigate("details")
                                }
                            },
                            onSeeAllClick = {
                                navController.navigate("all_movies")
                            },
                            onCategorySeeAllClick = { category ->
                                navController.navigate("category/${category}")
                            }
                        )
                    }
                    composable("details") {
                         DetailScreen(
                            movie = selectedMovie,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("all_movies") {
                        AllMoviesScreen(
                            movies = allMovies, // Pass stable list here too
                            onMovieClick = { movie ->
                                selectedMovie = movie
                                com.threepointogames.movierecap.util.AdManager.showInterstitial(this@MainActivity) {
                                    navController.navigate("details")
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(
                        "category/{categoryName}",
                        arguments = listOf(androidx.navigation.navArgument("categoryName") { type = androidx.navigation.NavType.StringType })
                    ) { backStackEntry ->
                        val categoryName = backStackEntry.arguments?.getString("categoryName") ?: "Movies"
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val categoryMovies = remember(allMovies, categoryName) {
                            if (categoryName == "Recently Watched") {
                                val historyIds = com.threepointogames.movierecap.util.WatchHistoryManager.getWatchedMovieIds(context)
                                allMovies.filter { it.id in historyIds }
                                    .sortedBy { historyIds.indexOf(it.id) }
                            } else if (categoryName == "Recently Added") {
                                allMovies.sortedByDescending { it.updatedAt ?: 0L }
                            } else {
                                allMovies.filter { it.categories.contains(categoryName) }
                            }
                        }
                        
                        AllMoviesScreen(
                            movies = categoryMovies,
                            title = categoryName,
                            onMovieClick = { movie ->
                                selectedMovie = movie
                                com.threepointogames.movierecap.util.AdManager.showInterstitial(this@MainActivity) {
                                    navController.navigate("details")
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
