package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.CopilotResponse
import com.example.data.api.GeminiCopilotService
import com.example.data.api.JamendoService
import com.example.data.api.JamendoTrack
import com.example.data.api.JamendoArtist
import org.json.JSONObject
import com.example.data.database.AppDatabase
import com.example.data.database.SongEntity
import com.example.data.preferences.UserPreferences
import com.example.data.repository.MusicRepository
import com.example.player.PlaybackManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    // Database & Repository
    private val database = AppDatabase.getDatabase(application)
    private val songDao = database.songDao()
    private val repository = MusicRepository(application, songDao)

    // Datastore Preferences
    private val userPreferences = UserPreferences(application)
    val preferencesState = userPreferences.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences.PreferencesState()
    )

    // Player Engine
    val playbackManager = PlaybackManager(application)
    
    // UI reactive bindings to Player Engine
    val currentSong: StateFlow<SongEntity?> = playbackManager.currentSong
    val isPlaying: StateFlow<Boolean> = playbackManager.isPlaying
    val currentPosition: StateFlow<Long> = playbackManager.currentPosition
    val duration: StateFlow<Long> = playbackManager.duration
    val queue: StateFlow<List<SongEntity>> = playbackManager.queue
    val currentIndex: StateFlow<Int> = playbackManager.currentIndex
    val isShuffleEnabled: StateFlow<Boolean> = playbackManager.isShuffleEnabled
    val isRepeatEnabled: StateFlow<Boolean> = playbackManager.isRepeatEnabled
    val sleepTimeRemaining: StateFlow<Int?> = playbackManager.sleepTimeRemaining

    // Database reactive bindings
    val allSongs = repository.allSongs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favoriteSongs = repository.favoriteSongs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recentlyAddedSongs = repository.recentlyAddedSongs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val mostPlayedSongs = repository.mostPlayedSongs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val recentlyPlayedSongs = repository.recentlyPlayedSongs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val playlists = repository.playlists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val playbackHistory = repository.playbackHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search and Filtering UI State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val filteredSongs = combine(allSongs, _searchQuery) { songs, query ->
        if (query.isBlank()) {
            songs
        } else {
            songs.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Jamendo Search Online UI State
    private val jamendoService = JamendoService()

    private val _onlineTracks = MutableStateFlow<List<JamendoTrack>>(emptyList())
    val onlineTracks = _onlineTracks.asStateFlow()

    private val _onlineArtists = MutableStateFlow<List<JamendoArtist>>(emptyList())
    val onlineArtists = _onlineArtists.asStateFlow()

    private val _isOnlineLoading = MutableStateFlow(false)
    val isOnlineLoading = _isOnlineLoading.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress = _downloadProgress.asStateFlow()

    val downloadedTrackIds = allSongs.map { songs ->
        songs.filter { it.isDownloaded && it.onlineMetadataJson != null }.mapNotNull { song ->
            try {
                val json = JSONObject(song.onlineMetadataJson!!)
                val track = json.optJSONObject("track")
                track?.optString("id")
            } catch (e: Exception) {
                null
            }
        }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Gemini Copilot UI State
    private val copilotService = GeminiCopilotService()
    
    private val _copilotMessages = MutableStateFlow<List<Pair<String, Boolean>>>(
        listOf("Hello! I am your Nova Music AI Copilot. Ask me to make a playlist, recommend songs, or explain some lyrics!" to false)
    )
    val copilotMessages = _copilotMessages.asStateFlow()

    private val _isCopilotLoading = MutableStateFlow(false)
    val isCopilotLoading = _isCopilotLoading.asStateFlow()

    init {
        viewModelScope.launch {
            // Populate database with mock tracks if empty so player immediately works
            repository.prepopulateIfEmpty()
            
            // Incrementally scan local system files in background
            repository.scanLocalMedia()

            // Observe preferences state to apply equalizer settings and playback params to the player
            preferencesState.collect { prefs ->
                playbackManager.updatePlaybackParams(prefs.playbackSpeed, prefs.playbackPitch)
                
                // Convert string bands back to list
                val bands = prefs.eqBands.split(",").mapNotNull { it.toIntOrNull() }
                playbackManager.configureAudioEffects(
                    enabled = prefs.equalizerEnabled,
                    bass = prefs.bassBoostLevel,
                    virt = prefs.virtualizerLevel,
                    gains = if (bands.isNotEmpty()) bands else listOf(0, 0, 0, 0, 0)
                )
            }
        }
    }

    // Player Actions
    fun setQueueAndPlay(songs: List<SongEntity>, startIndex: Int = 0) {
        playbackManager.setQueue(songs, startIndex)
        viewModelScope.launch {
            if (songs.isNotEmpty() && startIndex in songs.indices) {
                repository.incrementPlayCount(songs[startIndex].path)
            }
        }
    }

    fun togglePlayPause() {
        playbackManager.togglePlayPause()
    }

    fun skipToNext() {
        playbackManager.skipToNext()
        recordCurrentSongHistory()
    }

    fun skipToPrevious() {
        playbackManager.skipToPrevious()
        recordCurrentSongHistory()
    }

    private fun recordCurrentSongHistory() {
        val current = currentSong.value
        if (current != null) {
            viewModelScope.launch {
                repository.incrementPlayCount(current.path)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        playbackManager.seekTo(positionMs)
    }

    fun toggleShuffle() {
        playbackManager.toggleShuffle()
    }

    fun toggleRepeat() {
        playbackManager.toggleRepeat()
    }

    fun toggleFavorite(song: SongEntity) {
        val targetFav = !song.isFavorite
        viewModelScope.launch {
            repository.toggleFavorite(song.path, targetFav)
            playbackManager.updateSongFavoriteStatus(song.path, targetFav)
        }
    }

    fun createPlaylist(name: String, description: String = "", onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.createPlaylist(name, description)
            onComplete?.invoke(id)
        }
    }

    fun addSongToPlaylist(playlistId: Long, songPath: String) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songPath)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songPath: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songPath)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }

    fun getSongsForPlaylistFlow(playlistId: Long): Flow<List<SongEntity>> {
        val flow = MutableStateFlow<List<SongEntity>>(emptyList())
        viewModelScope.launch {
            repository.getSongsForPlaylist(playlistId).collect {
                flow.value = it
            }
        }
        return flow.asStateFlow()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Theme Customizations Actions
    fun updateThemeMode(mode: String) {
        viewModelScope.launch { userPreferences.updateThemeMode(mode) }
    }

    fun updateMaterialYou(use: Boolean) {
        viewModelScope.launch { userPreferences.updateMaterialYou(use) }
    }

    fun updatePrimaryColor(hex: String) {
        viewModelScope.launch { userPreferences.updatePrimaryColor(hex) }
    }

    fun updateCardRadius(radius: Int) {
        viewModelScope.launch { userPreferences.updateCardRadius(radius) }
    }

    fun updateBackgroundImage(uri: String) {
        viewModelScope.launch { userPreferences.updateBackgroundImage(uri) }
    }

    fun updateHomeSectionsOrder(order: String) {
        viewModelScope.launch { userPreferences.updateHomeSectionsOrder(order) }
    }

    fun updateGestures(doubleTap: String, swipeLeft: String, swipeRight: String) {
        viewModelScope.launch { userPreferences.updateGestures(doubleTap, swipeLeft, swipeRight) }
    }

    // Equalizer Adjustments Actions
    fun updateAudioEngine(
        gapless: Boolean,
        crossfade: Int,
        eqEnabled: Boolean,
        bassBoost: Int,
        virtualizer: Int,
        preset: String,
        bands: String
    ) {
        viewModelScope.launch {
            userPreferences.updateAudioEngine(
                gapless, crossfade, eqEnabled, bassBoost, virtualizer, preset, bands
            )
        }
    }

    fun updatePlaybackParams(speed: Float, pitch: Float) {
        viewModelScope.launch {
            userPreferences.updatePlaybackParams(speed, pitch)
        }
    }

    // Sleep Timer Actions
    fun startSleepTimer(minutes: Int) {
        playbackManager.startSleepTimer(minutes)
    }

    fun cancelSleepTimer() {
        playbackManager.cancelSleepTimer()
    }

    // AI Music Assistant Copilot
    fun sendCopilotMessage(prompt: String) {
        if (prompt.isBlank()) return
        
        // Append user message
        val currentHistory = _copilotMessages.value.toMutableList()
        currentHistory.add(prompt to true)
        _copilotMessages.value = currentHistory
        _isCopilotLoading.value = true

        viewModelScope.launch {
            val response = copilotService.queryCopilot(
                prompt = prompt,
                availableSongs = allSongs.value,
                chatHistory = currentHistory.dropLast(1) // exclude last prompt
            )
            
            // Execute command if recommended by AI
            executeCopilotCommand(response)

            // Append assistant response
            val updatedHistory = _copilotMessages.value.toMutableList()
            updatedHistory.add(response.explanation to false)
            _copilotMessages.value = updatedHistory
            _isCopilotLoading.value = false
        }
    }

    private fun executeCopilotCommand(response: CopilotResponse) {
        when (response.command) {
            "CREATE_PLAYLIST" -> {
                if (response.playlistName.isNotBlank() && response.songPaths.isNotEmpty()) {
                    viewModelScope.launch {
                        val playlistId = repository.createPlaylist(response.playlistName, "AI Curated Playlist")
                        response.songPaths.forEach { path ->
                            repository.addSongToPlaylist(playlistId, path)
                        }
                    }
                }
            }
            "PLAY_SONG" -> {
                if (response.songPath.isNotBlank()) {
                    val matchingSong = allSongs.value.find { it.path == response.songPath }
                    if (matchingSong != null) {
                        setQueueAndPlay(listOf(matchingSong), 0)
                    }
                }
            }
            "FILTER_LIBRARY" -> {
                if (response.filterQuery.isNotBlank()) {
                    _searchQuery.value = response.filterQuery
                }
            }
        }
    }

    fun scanLocalSongs() {
        viewModelScope.launch {
            repository.scanLocalMedia()
        }
    }

    fun clearCopilotChat() {
        _copilotMessages.value = listOf(
            "Hello! I am your Nova Music AI Copilot. Ask me to make a playlist, recommend songs, or explain some lyrics!" to false
        )
    }

    // Jamendo actions
    fun searchOnline(query: String) {
        viewModelScope.launch {
            _isOnlineLoading.value = true
            try {
                val tracks = jamendoService.searchTracks(query)
                val artists = jamendoService.searchArtists(query)
                _onlineTracks.value = tracks
                _onlineArtists.value = artists
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Error fetching from Jamendo", e)
            } finally {
                _isOnlineLoading.value = false
            }
        }
    }

    fun playOnlineTrack(track: JamendoTrack) {
        val song = mapJamendoTrackToSongEntity(track)
        
        // Add other track suggestions from search results into the player queue
        val currentTracks = _onlineTracks.value
        val queueSongs = currentTracks.map { mapJamendoTrackToSongEntity(it) }
        val startIndex = currentTracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        
        setQueueAndPlay(queueSongs, startIndex)
    }

    fun downloadOnlineTrack(track: JamendoTrack) {
        if (_downloadProgress.value.containsKey(track.id)) return // already downloading
        
        viewModelScope.launch {
            // Fetch full artist details in background if artist id is available
            var artistDetails: JamendoArtist? = null
            if (track.artistId.isNotBlank()) {
                try {
                    artistDetails = jamendoService.getArtistDetails(track.artistId)
                } catch (e: Exception) {
                    Log.e("MusicViewModel", "Failed to fetch artist details for download", e)
                }
            }
            
            _downloadProgress.value = _downloadProgress.value + (track.id to 0.0f)
            repository.downloadTrack(track, artistDetails) { progress ->
                _downloadProgress.value = _downloadProgress.value + (track.id to progress)
            }
            _downloadProgress.value = _downloadProgress.value - track.id
        }
    }

    private fun mapJamendoTrackToSongEntity(track: JamendoTrack): SongEntity {
        // If already downloaded in library, use the local path!
        val downloaded = allSongs.value.find { song ->
            song.isDownloaded && song.onlineMetadataJson?.contains("\"id\":\"${track.id}\"") == true
        }
        if (downloaded != null) {
            return downloaded
        }
        
        return SongEntity(
            path = track.audio,
            title = track.name,
            artist = track.artistName,
            album = track.albumName,
            duration = track.duration,
            albumArtUri = if (track.image.isNotBlank()) track.image else track.albumImage,
            isFavorite = false,
            bitrate = 192,
            sampleRate = 44100,
            fileType = "mp3",
            isDownloaded = false,
            onlineMetadataJson = JSONObject().apply { put("track", JSONObject(track.rawJson)) }.toString()
        )
    }

    override fun onCleared() {
        super.onCleared()
        playbackManager.release()
    }
}
