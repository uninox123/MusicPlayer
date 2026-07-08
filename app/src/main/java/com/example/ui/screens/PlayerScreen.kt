package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.database.SongEntity
import com.example.ui.components.LiveVisualizer
import com.example.ui.components.WaveformProgressBar
import com.example.ui.viewmodel.MusicViewModel
import kotlinx.coroutines.launch

// High-fidelity pre-defined sync lyrics for default tracks
val SYNCED_LYRICS = mapOf(
    "Starlight Voyage" to listOf(
        0L to "🌌 Ambient intro hums...",
        8000L to "Deep in the cosmic expanse,",
        16000L to "Stars ignite and fade away.",
        24000L to "Floating through a river of light,",
        32000L to "Searching for a distant home.",
        40000L to "Instruments swell into deep space...",
        55000L to "Echoes of past horizons,",
        65000L to "Lost in the infinite blue.",
        75000L to "We are but dust in the starlight,",
        85000L to "Sailing to worlds yet untold."
    ),
    "Cyberpunk Dreams" to listOf(
        0L to "⚡ Cybernetic initialization...",
        8000L to "In the neon rain of Tokyo,",
        16000L to "Wires hum and circuits glow.",
        24000L to "Virtual minds in plastic shells,",
        32000L to "Chasing shadows in the grid.",
        40000L to "Synthesizers surge into high voltage...",
        55000L to "Hackers coding in the dark,",
        65000L to "Digital spirits set free.",
        75000L to "Rebel souls against the machine,",
        85000L to "Living the cyberpunk dream."
    ),
    "Chillwave Solitude" to listOf(
        0L to "🌊 Soft ocean waves rolling...",
        8000L to "Quiet sunset on the beach,",
        16000L to "Waves receding out of reach.",
        24000L to "Lofi echoes in the breeze,",
        32000L to "Mind is wandering at ease.",
        40000L to "Vinyl crackle and calm keys enter...",
        55000L to "Sipping tea in fading light,",
        65000L to "Welcoming the gentle night.",
        75000L to "No more worries, no more rush,",
        85000L to "Surrendered to the evening hush."
    ),
    "Retro Resonance" to listOf(
        0L to "🕹️ Coin inserted. Ready Player One...",
        8000L to "Press start to begin the quest,",
        16000L to "8-bit worlds we used to win.",
        24000L to "Glitchy sounds and plastic keys,",
        32000L to "Relive those childhood memories.",
        40000L to "Arpeggiator rises in velocity...",
        55000L to "High score flashing on the screen,",
        65000L to "Best adventure ever seen.",
        75000L to "Side-scrolling through the neon sky,",
        85000L to "Pixel dreams that never die."
    )
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PlayerScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val isShuffle by viewModel.isShuffleEnabled.collectAsState()
    val isRepeat by viewModel.isRepeatEnabled.collectAsState()
    val sleepTimerRemaining by viewModel.sleepTimeRemaining.collectAsState()
    val prefs by viewModel.preferencesState.collectAsState()

    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var selectedSleepMinutes by remember { mutableStateOf("15") }
    var showLyricsOverlay by remember { mutableStateOf(false) }

    // Floating rotating cover disk rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "disc_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )
    val activeRotation = if (isPlaying) rotationAngle else 0f

    val cornerShape = RoundedCornerShape(prefs.cardRadiusDp.dp)

    if (currentSong == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No song loaded in queue", style = MaterialTheme.typography.titleMedium)
        }
        return
    }

    val song = currentSong!!

    Scaffold(
        containerColor = Color.Transparent,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            
            // Header: Close drawer indicator / Active view
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* Slide down is handled by sheet collapse */ }) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Collapse Player")
                }
                
                Text(
                    "NOW PLAYING",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(onClick = { showSleepTimerDialog = true }) {
                    Icon(
                        Icons.Outlined.Timer,
                        contentDescription = "Sleep Timer",
                        tint = if (sleepTimerRemaining != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Central Cover Art with support for customizable Gestures!
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(cornerShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                when (prefs.gestureDoubleTapArtwork) {
                                    "favorite" -> {
                                        viewModel.toggleFavorite(song)
                                        Toast.makeText(context, if (song.isFavorite) "Removed from Favorites" else "Added to Favorites", Toast.LENGTH_SHORT).show()
                                    }
                                    "play_pause" -> viewModel.togglePlayPause()
                                    "next" -> viewModel.skipToNext()
                                    "lyrics" -> showLyricsOverlay = !showLyricsOverlay
                                    else -> {}
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = { /* Swipe left/right detection if desired */ },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (dragAmount.x < -15) { // Left Swipe
                                    if (prefs.gestureSwipeRightArtwork == "next") { // flipped or regular
                                        viewModel.skipToNext()
                                    }
                                } else if (dragAmount.x > 15) { // Right Swipe
                                    if (prefs.gestureSwipeLeftArtwork == "previous") {
                                        viewModel.skipToPrevious()
                                    }
                                }
                            }
                        )
                    }
                    .testTag("artwork_gestures_area"),
                contentAlignment = Alignment.Center
            ) {
                if (showLyricsOverlay) {
                    // Synchronized Live Lyrics Panel Overlay
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.85f))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val activeLyricsList = SYNCED_LYRICS[song.title]
                        if (activeLyricsList != null) {
                            val listState = rememberLazyListState()
                            
                            // Determine which index is active
                            val activeIndex = activeLyricsList.indexOfLast { currentPosition >= it.first }.coerceAtLeast(0)
                            
                            LaunchedEffect(activeIndex) {
                                listState.animateScrollToItem(activeIndex)
                            }

                            LazyColumn(
                                state = listState,
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(activeLyricsList) { idx, item ->
                                    val lyricLine = item.second
                                    val isActive = idx == activeIndex
                                    Text(
                                        lyricLine,
                                        style = if (isActive) {
                                            MaterialTheme.typography.headlineSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else {
                                            MaterialTheme.typography.titleMedium.copy(
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                        },
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp)
                                    )
                                }
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No local lyrics embedded.\nAsk Copilot to research some lyrics!", color = Color.White, textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    // Regular Rotating Vinyl Cover
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        AsyncImage(
                            model = song.albumArtUri,
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(activeRotation)
                                .clip(if (prefs.themeMode == "amoled") CircleShape else cornerShape)
                        )
                    }
                }
            }

            // Song Info (Title, Artist, and Favorite/Lyrics toggles)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showLyricsOverlay = !showLyricsOverlay },
                        modifier = Modifier.testTag("lyrics_toggle_button")
                    ) {
                        Icon(
                            Icons.Default.Notes, 
                            contentDescription = "Toggle Synced Lyrics",
                            tint = if (showLyricsOverlay) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            song.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            song.artist,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleFavorite(song) },
                        modifier = Modifier.testTag("favorite_toggle_button")
                    ) {
                        Icon(
                            if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (song.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Bitrate & File info badge
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(
                            "${song.bitrate} kbps", 
                            style = MaterialTheme.typography.bodySmall, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(
                            "${song.sampleRate / 1000f} kHz", 
                            style = MaterialTheme.typography.bodySmall, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Badge(containerColor = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(
                            song.fileType.uppercase(), 
                            style = MaterialTheme.typography.bodySmall, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Custom Waveform Progress Bar & Position Timers
            Column(modifier = Modifier.fillMaxWidth()) {
                WaveformProgressBar(
                    positionMs = currentPosition,
                    durationMs = duration,
                    onSeek = { viewModel.seekTo(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatTime(currentPosition),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatTime(duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Real-time Visualizer Bar matching active progress!
            LiveVisualizer(
                isPlaying = isPlaying,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .padding(vertical = 4.dp)
            )

            // Playback Controls Row (Shuffle, Prev, Play/Pause, Next, Repeat)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.toggleShuffle() },
                    modifier = Modifier.testTag("shuffle_button")
                ) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { viewModel.skipToPrevious() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("prev_button")
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Main circular Play/Pause button with soft interactive ripple depth
                FilledIconButton(
                    onClick = { viewModel.togglePlayPause() },
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("play_pause_button")
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        modifier = Modifier.size(42.dp),
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = { viewModel.skipToNext() },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("next_button")
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(36.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.toggleRepeat() },
                    modifier = Modifier.testTag("repeat_button")
                ) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (isRepeat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Sleep Timer Config Dialog
        if (showSleepTimerDialog) {
            AlertDialog(
                onDismissRequest = { showSleepTimerDialog = false },
                title = { Text("Sleep Timer") },
                text = {
                    Column {
                        Text("Pause playback automatically after:", modifier = Modifier.padding(bottom = 12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("5", "15", "30", "60").forEach { minutes ->
                                FilterChip(
                                    selected = selectedSleepMinutes == minutes,
                                    onClick = { selectedSleepMinutes = minutes },
                                    label = { Text("${minutes}m") }
                                )
                            }
                        }
                        
                        if (sleepTimerRemaining != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Active Timer: $sleepTimerRemaining minutes left.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.startSleepTimer(selectedSleepMinutes.toInt())
                            showSleepTimerDialog = false
                        }
                    ) {
                        Text("Start Timer")
                    }
                },
                dismissButton = {
                    if (sleepTimerRemaining != null) {
                        TextButton(
                            onClick = {
                                viewModel.cancelSleepTimer()
                                showSleepTimerDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Cancel Timer")
                        }
                    } else {
                        TextButton(onClick = { showSleepTimerDialog = false }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
    }
}

// Convert Milliseconds into formatted "M:SS" string
fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%d:%02d", mins, secs)
}

