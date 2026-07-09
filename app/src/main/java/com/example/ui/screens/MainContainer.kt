package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val prefs by viewModel.preferencesState.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()

    var currentRoute by remember { mutableStateOf("home") }
    var isPlayerExpanded by remember { mutableStateOf(false) }

    val cornerShape = RoundedCornerShape(prefs.cardRadiusDp.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (prefs.backgroundImageUri.isNotEmpty()) {
            AsyncImage(
                model = Uri.parse(prefs.backgroundImageUri),
                contentDescription = "Background",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.15f
            )
        }

        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 1. Floating Mini Player overlay if a song is loaded and Player is not expanded
                    AnimatedVisibility(
                        visible = currentSong != null && !isPlayerExpanded,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        Card(
                            shape = cornerShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable { isPlayerExpanded = true }
                                .testTag("mini_player_card")
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = currentSong?.albumArtUri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(cornerShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            currentSong?.title ?: "",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            currentSong?.artist ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.togglePlayPause() },
                                        modifier = Modifier.testTag("mini_play_pause")
                                    ) {
                                        Icon(
                                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause"
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.skipToNext() },
                                        modifier = Modifier.testTag("mini_next")
                                    ) {
                                        Icon(Icons.Default.SkipNext, contentDescription = "Next")
                                    }
                                }
                                
                                // Soft slim progress line at the bottom of the Mini Player
                                val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = Color.Transparent
                                )
                            }
                        }
                    }

                    // 2. Navigation Bar
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        tonalElevation = 8.dp,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        NavigationBarItem(
                            selected = currentRoute == "home",
                            onClick = {
                                currentRoute = "home"
                                navController.navigate("home") {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") },
                            modifier = Modifier.testTag("nav_home")
                        )
                        NavigationBarItem(
                            selected = currentRoute == "library",
                            onClick = {
                                currentRoute = "library"
                                navController.navigate("library") {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                            label = { Text("Library") },
                            modifier = Modifier.testTag("nav_library")
                        )
                        NavigationBarItem(
                            selected = currentRoute == "search_online",
                            onClick = {
                                currentRoute = "search_online"
                                navController.navigate("search_online") {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.Public, contentDescription = "Search Online") },
                            label = { Text("Search Online") },
                            modifier = Modifier.testTag("nav_search_online")
                        )
                        NavigationBarItem(
                            selected = currentRoute == "equalizer",
                            onClick = {
                                currentRoute = "equalizer"
                                navController.navigate("equalizer") {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.Equalizer, contentDescription = "Equalizer") },
                            label = { Text("Engine") },
                            modifier = Modifier.testTag("nav_equalizer")
                        )
                        NavigationBarItem(
                            selected = currentRoute == "copilot",
                            onClick = {
                                currentRoute = "copilot"
                                navController.navigate("copilot") {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Copilot") },
                            label = { Text("Copilot") },
                            modifier = Modifier.testTag("nav_copilot")
                        )
                        NavigationBarItem(
                            selected = currentRoute == "settings",
                            onClick = {
                                currentRoute = "settings"
                                navController.navigate("settings") {
                                    popUpTo("home") { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(Icons.Default.Palette, contentDescription = "Themes Settings") },
                            label = { Text("Themes") },
                            modifier = Modifier.testTag("nav_settings")
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(innerPadding)
            ) {
                composable("home") {
                    HomeScreen(
                        viewModel = viewModel,
                        onNavigateToPlayer = { isPlayerExpanded = true }
                    )
                }
                composable("library") {
                    LibraryScreen(
                        viewModel = viewModel,
                        onNavigateToPlayer = { isPlayerExpanded = true }
                    )
                }
                composable("search_online") {
                    SearchOnlineScreen(
                        viewModel = viewModel,
                        onNavigateToPlayer = { isPlayerExpanded = true }
                    )
                }
                composable("equalizer") {
                    EqualizerScreen(viewModel = viewModel)
                }
                composable("copilot") {
                    CopilotScreen(viewModel = viewModel)
                }
                composable("settings") {
                    ThemeSettingsScreen(viewModel = viewModel)
                }
            }
        }

        // Full Screen sliding Player View Overlay
        AnimatedVisibility(
            visible = isPlayerExpanded,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(400)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(400)
            ) + fadeOut()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("expanded_player_overlay"),
                color = MaterialTheme.colorScheme.background
            ) {
                // Background gradient matching album art if loaded
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )
                    
                    // Display back button and full player
                    Box {
                        PlayerScreen(viewModel = viewModel)
                        
                        // Overlay Collapse Arrow clickable trigger
                        IconButton(
                            onClick = { isPlayerExpanded = false },
                            modifier = Modifier
                                .padding(top = 12.dp, start = 24.dp)
                                .align(Alignment.TopStart)
                                .testTag("player_back_button")
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Back")
                        }
                    }
                }
            }
        }
    }
}
