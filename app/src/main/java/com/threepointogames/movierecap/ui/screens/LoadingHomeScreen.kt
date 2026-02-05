package com.threepointogames.movierecap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.threepointogames.movierecap.ui.components.shimmerEffect

@Composable
fun LoadingHomeScreen() {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
             Box(
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(16.dp)
                     .height(60.dp)
                     .clip(RoundedCornerShape(8.dp))
                     .shimmerEffect()
             )
        }
    ) { paddingValues ->
        LazyColumn(
             modifier = Modifier
                 .fillMaxSize()
                 .padding(paddingValues)
                 .padding(horizontal = 16.dp)
        ) {
            // Banner Ad Placeholder
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .shimmerEffect()
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Fake Categories
            items(3) {
                 Column {
                     // Category Title
                     Box(
                         modifier = Modifier
                             .width(150.dp)
                             .height(24.dp)
                             .clip(RoundedCornerShape(4.dp))
                             .shimmerEffect()
                     )
                     Spacer(modifier = Modifier.height(16.dp))
                     
                     // Horizontal List of Cards
                     LazyRow(
                         horizontalArrangement = Arrangement.spacedBy(16.dp)
                     ) {
                         items(5) {
                             Box(
                                 modifier = Modifier
                                     .width(280.dp)
                                     .aspectRatio(16f / 9f)
                                     .clip(RoundedCornerShape(8.dp))
                                     .shimmerEffect()
                             )
                         }
                     }
                     Spacer(modifier = Modifier.height(32.dp))
                 }
            }
        }
    }
}
