package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import com.example.data.database.PlaylistEntity
import com.example.data.database.SongEntity
import com.example.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MusicViewModel,
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.preferencesState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filteredSongs by viewModel.filteredSongs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val favoriteSongs by viewModel.favoriteSongs.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Songs", "Albums", "Artists", "Playlists")

    var showAddPlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var newPlaylistDesc by remember { mutableStateOf("") }

    val cornerShape = RoundedCornerShape(prefs.cardRadiusDp.dp)

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Search Bar & Scan Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search songs, artists, albums...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear Search")
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_field"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = cornerShape
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { viewModel.scanLocalSongs() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Scan Storage",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 3) {
                FloatingActionButton(
                    onClick = { showAddPlaylistDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_playlist_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Playlist")
                }
            }
        },
        containerColor = Color.Transparent,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> { // Songs Tab
                    if (filteredSongs.isEmpty()) {
                        EmptyStatePlaceholder(text = "No songs found in library")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp, start = 16.dp, end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(filteredSongs) { index, song ->
                                SongListItem(
                                    song = song,
                                    index = index,
                                    cornerShape = cornerShape,
                                    onSongClick = {
                                        viewModel.setQueueAndPlay(filteredSongs, index)
                                        onNavigateToPlayer()
                                    },
                                    onFavoriteToggle = {
                                        viewModel.toggleFavorite(song)
                                    },
                                    playlists = playlists,
                                    onAddToPlaylist = { playlistId ->
                                        viewModel.addSongToPlaylist(playlistId, song.path)
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> { // Albums Tab
                    val albums = filteredSongs.groupBy { it.album }
                    var expandedAlbum by remember { mutableStateOf<String?>(null) }
                    if (albums.isEmpty()) {
                        EmptyStatePlaceholder(text = "No albums found")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp, start = 16.dp, end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(albums.keys.toList()) { albumName ->
                                val albumSongs = albums[albumName] ?: emptyList()
                                val leadSong = albumSongs.firstOrNull()
                                val isExpanded = expandedAlbum == albumName
                                Column {
                                    AlbumListItem(
                                        albumName = albumName,
                                        artistName = leadSong?.artist ?: "Various Artists",
                                        songCount = albumSongs.size,
                                        albumArtUri = leadSong?.albumArtUri,
                                        cornerShape = cornerShape,
                                        isExpanded = isExpanded,
                                        onAlbumClick = {
                                            expandedAlbum = if (isExpanded) null else albumName
                                        }
                                    )
                                    AnimatedVisibility(visible = isExpanded) {
                                        Column(
                                            modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // Play all button
                                            TextButton(
                                                onClick = {
                                                    viewModel.setQueueAndPlay(albumSongs, 0)
                                                    onNavigateToPlayer()
                                                }
                                            ) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Play All")
                                            }
                                            albumSongs.forEachIndexed { index, song ->
                                                SongListItem(
                                                    song = song,
                                                    index = index,
                                                    cornerShape = cornerShape,
                                                    onSongClick = {
                                                        viewModel.setQueueAndPlay(albumSongs, index)
                                                        onNavigateToPlayer()
                                                    },
                                                    onFavoriteToggle = {
                                                        viewModel.toggleFavorite(song)
                                                    },
                                                    playlists = playlists,
                                                    onAddToPlaylist = { playlistId ->
                                                        viewModel.addSongToPlaylist(playlistId, song.path)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> { // Artists Tab
                    val artists = filteredSongs.groupBy { it.artist }
                    var expandedArtist by remember { mutableStateOf<String?>(null) }
                    if (artists.isEmpty()) {
                        EmptyStatePlaceholder(text = "No artists found")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp, start = 16.dp, end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(artists.keys.toList()) { artistName ->
                                val artistSongs = artists[artistName] ?: emptyList()
                                val leadSong = artistSongs.firstOrNull()
                                val isExpanded = expandedArtist == artistName
                                Column {
                                    ArtistListItem(
                                        artistName = artistName,
                                        songCount = artistSongs.size,
                                        albumArtUri = leadSong?.albumArtUri,
                                        isExpanded = isExpanded,
                                        onArtistClick = {
                                            expandedArtist = if (isExpanded) null else artistName
                                        }
                                    )
                                    AnimatedVisibility(visible = isExpanded) {
                                        Column(
                                            modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            // Play all button
                                            TextButton(
                                                onClick = {
                                                    viewModel.setQueueAndPlay(artistSongs, 0)
                                                    onNavigateToPlayer()
                                                }
                                            ) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Play All")
                                            }
                                            artistSongs.forEachIndexed { index, song ->
                                                SongListItem(
                                                    song = song,
                                                    index = index,
                                                    cornerShape = cornerShape,
                                                    onSongClick = {
                                                        viewModel.setQueueAndPlay(artistSongs, index)
                                                        onNavigateToPlayer()
                                                    },
                                                    onFavoriteToggle = {
                                                        viewModel.toggleFavorite(song)
                                                    },
                                                    playlists = playlists,
                                                    onAddToPlaylist = { playlistId ->
                                                        viewModel.addSongToPlaylist(playlistId, song.path)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> { // Playlists Tab
                    var expandedPlaylistId by remember { mutableStateOf<Long?>(null) }
                    var expandedFavorites by remember { mutableStateOf(false) }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Favorites Section
                        item {
                            Column {
                                Card(
                                    shape = cornerShape,
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { expandedFavorites = !expandedFavorites }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(cornerShape)
                                                .background(Color.Red.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Favorite,
                                                contentDescription = null,
                                                tint = Color.Red,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "❤️ Favorites",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                "${favoriteSongs.size} songs",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            if (expandedFavorites) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (expandedFavorites) "Collapse" else "Expand",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                AnimatedVisibility(visible = expandedFavorites) {
                                    Column(
                                        modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (favoriteSongs.isEmpty()) {
                                            Text(
                                                "No favorite songs yet",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(16.dp)
                                            )
                                        } else {
                                            // Play all favorites button
                                            TextButton(
                                                onClick = {
                                                    viewModel.setQueueAndPlay(favoriteSongs, 0)
                                                    onNavigateToPlayer()
                                                }
                                            ) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Play All")
                                            }
                                            favoriteSongs.forEachIndexed { index, song ->
                                                SongListItem(
                                                    song = song,
                                                    index = index,
                                                    cornerShape = cornerShape,
                                                    onSongClick = {
                                                        viewModel.setQueueAndPlay(favoriteSongs, index)
                                                        onNavigateToPlayer()
                                                    },
                                                    onFavoriteToggle = {
                                                        viewModel.toggleFavorite(song)
                                                    },
                                                    playlists = playlists,
                                                    onAddToPlaylist = { playlistId ->
                                                        viewModel.addSongToPlaylist(playlistId, song.path)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Playlists
                        if (playlists.isEmpty() && !expandedFavorites && favoriteSongs.isEmpty()) {
                            item {
                                EmptyStatePlaceholder(text = "No playlists created yet. Create one!")
                            }
                        }

                        items(playlists) { playlist ->
                            val isExpanded = expandedPlaylistId == playlist.id
                            Column {
                                PlaylistListItem(
                                    playlist = playlist,
                                    cornerShape = cornerShape,
                                    isExpanded = isExpanded,
                                    onPlaylistClick = {
                                        expandedPlaylistId = if (isExpanded) null else playlist.id
                                    },
                                    onDeletePlaylist = {
                                        viewModel.deletePlaylist(playlist.id)
                                    }
                                )
                                AnimatedVisibility(visible = isExpanded) {
                                    val playlistSongs by viewModel.getSongsForPlaylistFlow(playlist.id)
                                        .collectAsState(initial = emptyList())

                                    Column(
                                        modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (playlistSongs.isEmpty()) {
                                            Text(
                                                "No songs in this playlist",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(16.dp)
                                            )
                                        } else {
                                            // Play all button
                                            TextButton(
                                                onClick = {
                                                    viewModel.setQueueAndPlay(playlistSongs, 0)
                                                    onNavigateToPlayer()
                                                }
                                            ) {
                                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Play All")
                                            }
                                            playlistSongs.forEachIndexed { index, song ->
                                                PlaylistSongItem(
                                                    song = song,
                                                    index = index,
                                                    cornerShape = cornerShape,
                                                    onSongClick = {
                                                        viewModel.setQueueAndPlay(playlistSongs, index)
                                                        onNavigateToPlayer()
                                                    },
                                                    onRemoveFromPlaylist = {
                                                        viewModel.removeSongFromPlaylist(playlist.id, song.path)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Create Playlist Dialog
            if (showAddPlaylistDialog) {
                AlertDialog(
                    onDismissRequest = { showAddPlaylistDialog = false },
                    title = { Text("New Playlist") },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = newPlaylistName,
                                onValueChange = { newPlaylistName = it },
                                label = { Text("Playlist Name") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("playlist_name_input"),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = newPlaylistDesc,
                                onValueChange = { newPlaylistDesc = it },
                                label = { Text("Description (Optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 3
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newPlaylistName.isNotBlank()) {
                                    viewModel.createPlaylist(newPlaylistName, newPlaylistDesc)
                                    newPlaylistName = ""
                                    newPlaylistDesc = ""
                                    showAddPlaylistDialog = false
                                }
                            }
                        ) {
                            Text("Create")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddPlaylistDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SongListItem(
    song: SongEntity,
    index: Int,
    cornerShape: RoundedCornerShape,
    onSongClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    playlists: List<PlaylistEntity> = emptyList(),
    onAddToPlaylist: (Long) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = cornerShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSongClick() }
            .testTag("song_item_$index")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(52.dp)
                    .clip(cornerShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier.testTag("fav_button_$index")
            ) {
                Icon(
                    if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (song.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 3-dot menu for "Add to Playlist"
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (playlists.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No playlists available") },
                            onClick = { showMenu = false },
                            enabled = false
                        )
                    } else {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Add to Playlist",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {},
                            enabled = false
                        )
                        playlists.forEach { playlist ->
                            DropdownMenuItem(
                                text = { Text(playlist.name) },
                                onClick = {
                                    onAddToPlaylist(playlist.id)
                                    showMenu = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.QueueMusic,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistSongItem(
    song: SongEntity,
    index: Int,
    cornerShape: RoundedCornerShape,
    onSongClick: () -> Unit,
    onRemoveFromPlaylist: () -> Unit
) {
    Card(
        shape = cornerShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSongClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(cornerShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onRemoveFromPlaylist) {
                Icon(
                    Icons.Default.RemoveCircleOutline,
                    contentDescription = "Remove from playlist",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AlbumListItem(
    albumName: String,
    artistName: String,
    songCount: Int,
    albumArtUri: String?,
    cornerShape: RoundedCornerShape,
    isExpanded: Boolean = false,
    onAlbumClick: () -> Unit
) {
    Card(
        shape = cornerShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAlbumClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(cornerShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    albumName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    artistName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Badge(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("$songCount songs", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ArtistListItem(
    artistName: String,
    songCount: Int,
    albumArtUri: String?,
    isExpanded: Boolean = false,
    onArtistClick: () -> Unit
) {
    Card(
        shape = androidx.compose.foundation.shape.CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onArtistClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    artistName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "$songCount tracks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun PlaylistListItem(
    playlist: PlaylistEntity,
    cornerShape: RoundedCornerShape,
    isExpanded: Boolean = false,
    onPlaylistClick: () -> Unit,
    onDeletePlaylist: () -> Unit
) {
    Card(
        shape = cornerShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlaylistClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(cornerShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.QueueMusic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    playlist.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    playlist.description.ifBlank { "Custom Collection" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onDeletePlaylist) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Playlist", tint = MaterialTheme.colorScheme.error)
            }
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyStatePlaceholder(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
