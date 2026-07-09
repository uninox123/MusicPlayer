package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.api.JamendoArtist
import com.example.data.api.JamendoTrack
import com.example.data.api.JamendoService
import com.example.ui.viewmodel.MusicViewModel
import org.json.JSONObject
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchOnlineScreen(
    viewModel: MusicViewModel,
    onNavigateToPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val prefs by viewModel.preferencesState.collectAsState()
    val onlineTracks by viewModel.onlineTracks.collectAsState()
    val onlineArtists by viewModel.onlineArtists.collectAsState()
    val isOnlineLoading by viewModel.isOnlineLoading.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val downloadedTrackIds by viewModel.downloadedTrackIds.collectAsState()

    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Tracks, 1 = Artists

    var selectedTrackForDetails by remember { mutableStateOf<JamendoTrack?>(null) }
    var selectedArtistForDetails by remember { mutableStateOf<JamendoArtist?>(null) }

    val cornerShape = RoundedCornerShape(prefs.cardRadiusDp.dp)

    // Run initial empty query search to populate popular/trending tracks/artists
    LaunchedEffect(Unit) {
        if (onlineTracks.isEmpty() && onlineArtists.isEmpty()) {
            viewModel.searchOnline("")
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.Transparent)) {
                LargeTopAppBar(
                    title = {
                        Text(
                            "Search Online",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineLarge
                        )
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(containerColor = Color.Transparent)
                )
                
                // Search Input Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search songs, artists, genres...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                    trailingIcon = {
                        if (searchQuery.text.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = TextFieldValue("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = cornerShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("online_search_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )

                // Search trigger button or auto-trigger
                Button(
                    onClick = { viewModel.searchOnline(searchQuery.text) },
                    shape = cornerShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("online_search_submit_button")
                ) {
                    Text("Search Jamendo Music")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tab Selector
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Tracks (${onlineTracks.size})", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Artists (${onlineArtists.size})", fontWeight = FontWeight.Bold) }
                    )
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
            if (isOnlineLoading) {
                // Loading spinner
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Fetching fresh music from Jamendo...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                when (selectedTab) {
                    0 -> {
                        if (onlineTracks.isEmpty()) {
                            // Empty results state
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "No online tracks found.\nTry a different search term.",
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp, start = 16.dp, end = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(onlineTracks) { track ->
                                    TrackItemRow(
                                        track = track,
                                        isDownloaded = downloadedTrackIds.contains(track.id),
                                        downloadProgress = downloadProgress[track.id],
                                        onPlay = { viewModel.playOnlineTrack(track) },
                                        onDownload = { viewModel.downloadOnlineTrack(track) },
                                        onShowDetails = { selectedTrackForDetails = track },
                                        cornerShape = cornerShape
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        if (onlineArtists.isEmpty()) {
                            // Empty results state
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "No artists found.\nTry a different search term.",
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp, start = 16.dp, end = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(onlineArtists) { artist ->
                                    ArtistItemRow(
                                        artist = artist,
                                        onClick = { selectedArtistForDetails = artist },
                                        cornerShape = cornerShape
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Track details bottom sheet / dialog
            selectedTrackForDetails?.let { track ->
                TrackDetailsDialog(
                    track = track,
                    isDownloaded = downloadedTrackIds.contains(track.id),
                    onDismiss = { selectedTrackForDetails = null },
                    cornerShape = cornerShape
                )
            }

            // Artist details dialog
            selectedArtistForDetails?.let { artist ->
                ArtistDetailsDialog(
                    artist = artist,
                    viewModel = viewModel,
                    downloadedTrackIds = downloadedTrackIds,
                    downloadProgress = downloadProgress,
                    onDismiss = { selectedArtistForDetails = null },
                    cornerShape = cornerShape
                )
            }
        }
    }
}

@Composable
fun TrackItemRow(
    track: JamendoTrack,
    isDownloaded: Boolean,
    downloadProgress: Float?,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onShowDetails: () -> Unit,
    cornerShape: RoundedCornerShape
) {
    Card(
        shape = cornerShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onShowDetails() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art
            AsyncImage(
                model = if (track.image.isNotBlank()) track.image else track.albumImage,
                contentDescription = track.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(cornerShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Track details (Title, Artist, Album)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artistName} • ${track.albumName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Track tags preview
                if (track.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(track.tags.take(3)) { tag ->
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Play stream button
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                    .size(36.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Stream Track",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Download action
            Box(contentAlignment = Alignment.Center) {
                if (downloadProgress != null) {
                    CircularProgressIndicator(
                        progress = { downloadProgress },
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "${(downloadProgress * 100).toInt()}%",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    IconButton(
                        onClick = onDownload,
                        enabled = !isDownloaded,
                        modifier = Modifier
                            .background(
                                if (isDownloaded) MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                CircleShape
                            )
                            .size(36.dp)
                    ) {
                        Icon(
                            if (isDownloaded) Icons.Default.Check else Icons.Default.Download,
                            contentDescription = if (isDownloaded) "Downloaded" else "Download Track",
                            tint = if (isDownloaded) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistItemRow(
    artist: JamendoArtist,
    onClick: () -> Unit,
    cornerShape: RoundedCornerShape
) {
    Card(
        shape = cornerShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artist image
            AsyncImage(
                model = artist.image,
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artist.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (artist.joinDate.isNotBlank()) {
                    Text(
                        text = "Joined: ${artist.joinDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                if (artist.website.isNotBlank()) {
                    Text(
                        text = artist.website,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View Details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun TrackDetailsDialog(
    track: JamendoTrack,
    isDownloaded: Boolean,
    onDismiss: () -> Unit,
    cornerShape: RoundedCornerShape
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Track Information",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = if (track.image.isNotBlank()) track.image else track.albumImage,
                    contentDescription = track.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(cornerShape)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = track.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "by ${track.artistName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider()

                Spacer(modifier = Modifier.height(12.dp))

                // Detail Items
                DetailItemRow(icon = Icons.Default.Album, label = "Album", value = track.albumName)
                DetailItemRow(icon = Icons.Default.CalendarToday, label = "Release Date", value = track.releaseDate)
                DetailItemRow(icon = Icons.Default.Timer, label = "Duration", value = formatDuration(track.duration))
                
                if (track.playCount > 0) {
                    DetailItemRow(icon = Icons.Default.Headset, label = "Jamendo Streams", value = track.playCount.toString())
                }
                if (track.downloadCount > 0) {
                    DetailItemRow(icon = Icons.Default.CloudDownload, label = "Jamendo Downloads", value = track.downloadCount.toString())
                }
                
                DetailItemRow(
                    icon = Icons.Default.MusicNote,
                    label = "Type",
                    value = "${track.vocalInstrumental.replaceFirstChar { it.uppercase() }} • ${track.acousticElectric.replaceFirstChar { it.uppercase() }}"
                )

                if (track.speed.isNotBlank()) {
                    DetailItemRow(icon = Icons.Default.Speed, label = "Tempo", value = track.speed.replaceFirstChar { it.uppercase() })
                }
                if (track.language.isNotBlank()) {
                    DetailItemRow(icon = Icons.Default.Language, label = "Language", value = track.language.uppercase())
                }

                DetailItemRow(
                    icon = Icons.Default.Info,
                    label = "Library Status",
                    value = if (isDownloaded) "Downloaded (Offline)" else "Online Stream Only"
                )

                // Creative Commons License click
                if (track.licenseCcUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(track.licenseCcUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // ignore
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Copyright,
                                contentDescription = "License",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Creative Commons License",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    track.licenseCcUrl,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                Icons.Default.Launch,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Raw JSON metadata display for total details (Complete API Fetch)
                Text(
                    text = "Raw Jamendo API Payload",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = JSONObject(track.rawJson).toString(2),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DetailItemRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ArtistDetailsDialog(
    artist: JamendoArtist,
    viewModel: MusicViewModel,
    downloadedTrackIds: Set<String>,
    downloadProgress: Map<String, Float>,
    onDismiss: () -> Unit,
    cornerShape: RoundedCornerShape
) {
    val context = LocalContext.current
    var artistTracks by remember { mutableStateOf<List<JamendoTrack>>(emptyList()) }
    var isTracksLoading by remember { mutableStateOf(true) }

    LaunchedEffect(artist.id) {
        isTracksLoading = true
        try {
            val service = JamendoService()
            artistTracks = service.getArtistTracks(artist.id)
        } catch (e: Exception) {
            // ignore
        } finally {
            isTracksLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Artist Details",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header details
                AsyncImage(
                    model = artist.image,
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = artist.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                
                if (artist.joinDate.isNotBlank()) {
                    Text(
                        text = "Member since ${artist.joinDate}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                if (artist.website.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(artist.website))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // ignore
                            }
                        },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = "Website", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(artist.website, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Artist Tracks",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (isTracksLoading) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (artistTracks.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("No tracks found for this artist.", fontSize = 12.sp)
                    }
                } else {
                    // Tracks scrollable sublist
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(artistTracks) { track ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            track.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            track.albumName,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.playOnlineTrack(track) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    val isTrackDl = downloadedTrackIds.contains(track.id)
                                    val dlProgress = downloadProgress[track.id]

                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(28.dp)) {
                                        if (dlProgress != null) {
                                            CircularProgressIndicator(
                                                progress = { dlProgress },
                                                modifier = Modifier.size(20.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                strokeWidth = 1.5.dp
                                            )
                                        } else {
                                            IconButton(
                                                onClick = { viewModel.downloadOnlineTrack(track) },
                                                enabled = !isTrackDl,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    if (isTrackDl) Icons.Default.Check else Icons.Default.Download,
                                                    contentDescription = "Download",
                                                    tint = if (isTrackDl) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Artist JSON metadata payload
                Text(
                    text = "Raw Artist Metadata",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                        .padding(4.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = JSONObject(artist.rawJson).toString(2),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun formatDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val hours = (durationMs / (1000 * 60 * 60))
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}
