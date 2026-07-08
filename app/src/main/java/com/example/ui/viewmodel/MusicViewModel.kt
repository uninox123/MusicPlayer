package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.CopilotResponse
import com.example.data.api.GeminiCopilotService
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
        viewModelScope.launch {
            repository.toggleFavorite(song.path, !song.isFavorite)
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

    override fun onCleared() {
        super.onCleared()
        playbackManager.release()
    }
}
