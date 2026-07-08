package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import coil.compose.AsyncImage
import com.example.data.database.SongEntity
import com.example.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MusicViewModel,
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.preferencesState.collectAsState()
    val allSongs by viewModel.allSongs.collectAsState()
    val favorites by viewModel.favoriteSongs.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayedSongs.collectAsState()
    val mostPlayed by viewModel.mostPlayedSongs.collectAsState()
    val recentlyAdded by viewModel.recentlyAddedSongs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    var showSectionManager by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val cornerShape = RoundedCornerShape(prefs.cardRadiusDp.dp)
    val sectionOrder = prefs.homeSectionsOrder.split(",")

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Nova Music",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
                actions = {
                    IconButton(
                        onClick = { showSectionManager = true },
                        modifier = Modifier.testTag("manage_sections_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Manage Sections")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier
    ) { innerPadding ->
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (allSongs.isEmpty()) {
                // Empty state
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Scanning local media...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(sectionOrder) { section ->
                        when (section) {
                            "recently_played" -> {
                                if (recentlyPlayed.isNotEmpty()) {
                                    MusicSectionRow(
                                        title = "Recently Played",
                                        songs = recentlyPlayed,
                                        cornerShape = cornerShape,
                                        onSongClick = { song ->
                                            viewModel.setQueueAndPlay(recentlyPlayed, recentlyPlayed.indexOf(song))
                                            onNavigateToPlayer()
                                        }
                                    )
                                }
                            }
                            "most_played" -> {
                                if (mostPlayed.isNotEmpty()) {
                                    MusicSectionRow(
                                        title = "Most Played",
                                        songs = mostPlayed,
                                        cornerShape = cornerShape,
                                        onSongClick = { song ->
                                            viewModel.setQueueAndPlay(mostPlayed, mostPlayed.indexOf(song))
                                            onNavigateToPlayer()
                                        }
                                    )
                                }
                            }
                            "favorites" -> {
                                if (favorites.isNotEmpty()) {
                                    MusicSectionRow(
                                        title = "My Favorites",
                                        songs = favorites,
                                        cornerShape = cornerShape,
                                        onSongClick = { song ->
                                            viewModel.setQueueAndPlay(favorites, favorites.indexOf(song))
                                            onNavigateToPlayer()
                                        }
                                    )
                                }
                            }
                            "recently_added" -> {
                                if (recentlyAdded.isNotEmpty()) {
                                    MusicSectionRow(
                                        title = "Newly Added",
                                        songs = recentlyAdded,
                                        cornerShape = cornerShape,
                                        onSongClick = { song ->
                                            viewModel.setQueueAndPlay(recentlyAdded, recentlyAdded.indexOf(song))
                                            onNavigateToPlayer()
                                        }
                                    )
                                }
                            }
                            "playlists" -> {
                                if (playlists.isNotEmpty()) {
                                    PlaylistSectionRow(
                                        title = "Custom Playlists",
                                        playlists = playlists,
                                        cornerShape = cornerShape,
                                        onPlaylistClick = { playlist ->
                                            scope.launch {
                                                viewModel.getSongsForPlaylistFlow(playlist.id).take(1).collect { playlistSongs ->
                                                    if (playlistSongs.isNotEmpty()) {
                                                        viewModel.setQueueAndPlay(playlistSongs, 0)
                                                        onNavigateToPlayer()
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Interactive Dialog to configure which home sections are visible & reordered
            if (showSectionManager) {
                AlertDialog(
                    onDismissRequest = { showSectionManager = false },
                    title = { Text("Customize Home Layout") },
                    text = {
                        Column {
                            Text("Reorder and toggle your home segments:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 12.dp))
                            sectionOrder.forEachIndexed { idx, section ->
                                val label = when (section) {
                                    "recently_played" -> "Recently Played"
                                    "most_played" -> "Most Played"
                                    "favorites" -> "My Favorites"
                                    "recently_added" -> "Newly Added"
                                    "playlists" -> "Playlists"
                                    else -> section
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(label, fontWeight = FontWeight.SemiBold)
                                    Row {
                                        IconButton(
                                            onClick = {
                                                if (idx > 0) {
                                                    val newList = sectionOrder.toMutableList()
                                                    val temp = newList[idx]
                                                    newList[idx] = newList[idx - 1]
                                                    newList[idx - 1] = temp
                                                    viewModel.updateHomeSectionsOrder(newList.joinToString(","))
                                                }
                                            },
                                            enabled = idx > 0
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up")
                                        }
                                        IconButton(
                                            onClick = {
                                                if (idx < sectionOrder.size - 1) {
                                                    val newList = sectionOrder.toMutableList()
                                                    val temp = newList[idx]
                                                    newList[idx] = newList[idx + 1]
                                                    newList[idx + 1] = temp
                                                    viewModel.updateHomeSectionsOrder(newList.joinToString(","))
                                                }
                                            },
                                            enabled = idx < sectionOrder.size - 1
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down")
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSectionManager = false }) {
                            Text("Close")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun MusicSectionRow(
    title: String,
    songs: List<SongEntity>,
    cornerShape: RoundedCornerShape,
    onSongClick: (SongEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(songs) { song ->
                SongCardItem(
                    song = song,
                    cornerShape = cornerShape,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
fun PlaylistSectionRow(
    title: String,
    playlists: List<com.example.data.database.PlaylistEntity>,
    cornerShape: RoundedCornerShape,
    onPlaylistClick: (com.example.data.database.PlaylistEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(playlists) { playlist ->
                Card(
                    shape = cornerShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .width(140.dp)
                        .clickable { onPlaylistClick(playlist) }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clip(cornerShape)
                                .clickable { onPlaylistClick(playlist) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.QueueMusic,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            playlist.name,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            playlist.description.ifBlank { "Smart Mix" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SongCardItem(
    song: SongEntity,
    cornerShape: RoundedCornerShape,
    onClick: () -> Unit
) {
    Card(
        shape = cornerShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = song.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(cornerShape)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                song.title,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
