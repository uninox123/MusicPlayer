package com.example.player

import android.content.Context
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.data.database.SongEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.IOException

class PlaybackManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    
    // Audio Effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    // Coroutine Scope for background operations (e.g. sleep timer)
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var sleepTimerJob: Job? = null
    private var progressUpdateJob: Job? = null

    // Playback State flows
    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong: StateFlow<SongEntity?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _queue = MutableStateFlow<List<SongEntity>>(emptyList())
    val queue: StateFlow<List<SongEntity>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _isRepeatEnabled = MutableStateFlow(false) // false = repeat off, true = repeat all (can add repeat-one later if needed)
    val isRepeatEnabled: StateFlow<Boolean> = _isRepeatEnabled.asStateFlow()

    private val _sleepTimeRemaining = MutableStateFlow<Int?>(null) // in minutes, null if disabled
    val sleepTimeRemaining: StateFlow<Int?> = _sleepTimeRemaining.asStateFlow()

    // Config parameters
    private var playbackSpeed = 1.0f
    private var playbackPitch = 1.0f
    private var equalizerEnabled = false
    private var bassBoostLevel = 0
    private var virtualizerLevel = 0
    private var eqBandsGains = listOf(0, 0, 0, 0, 0) // default 5 bands flat

    init {
        initMediaPlayer()
    }

    private fun initMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setOnCompletionListener {
                handlePlaybackCompletion()
            }
            setOnErrorListener { mp, what, extra ->
                Log.e("PlaybackManager", "MediaPlayer error: what=$what extra=$extra")
                // Gracefully try to play next on error
                skipToNext()
                true
            }
        }
    }

    private fun handlePlaybackCompletion() {
        if (_isRepeatEnabled.value) {
            // Repeat all: if we are at the end, wrap around
            if (_currentIndex.value == _queue.value.size - 1) {
                playAtIndex(0)
            } else {
                skipToNext()
            }
        } else {
            // No repeat: if we are at the end, stop; otherwise play next
            if (_currentIndex.value < _queue.value.size - 1) {
                skipToNext()
            } else {
                _isPlaying.value = false
                stopProgressUpdate()
            }
        }
    }

    fun setQueue(songs: List<SongEntity>, startIdx: Int = 0) {
        _queue.value = songs
        if (songs.isNotEmpty() && startIdx in songs.indices) {
            playAtIndex(startIdx)
        }
    }

    fun addToQueue(song: SongEntity) {
        val currentList = _queue.value.toMutableList()
        currentList.add(song)
        _queue.value = currentList
        if (_currentIndex.value == -1) {
            playAtIndex(0)
        }
    }

    fun playAtIndex(index: Int) {
        if (index !in _queue.value.indices) return
        _currentIndex.value = index
        val song = _queue.value[index]
        _currentSong.value = song
        playSong(song)
    }

    private fun playSong(song: SongEntity) {
        try {
            initMediaPlayer()
            mediaPlayer?.let { player ->
                player.setDataSource(song.path)
                player.prepareAsync()
                player.setOnPreparedListener {
                    applyPlaybackParams()
                    applyAudioEffects()
                    it.start()
                    _isPlaying.value = true
                    _duration.value = it.duration.toLong()
                    startProgressUpdate()
                }
            }
        } catch (e: Exception) {
            Log.e("PlaybackManager", "Error playing song: ${e.message}", e)
            _isPlaying.value = false
            // Retry next song
            skipToNext()
        }
    }

    fun togglePlayPause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
                stopProgressUpdate()
            } else {
                // Ensure we have something to play, otherwise play first from queue
                if (_currentSong.value != null) {
                    player.start()
                    _isPlaying.value = true
                    startProgressUpdate()
                } else if (_queue.value.isNotEmpty()) {
                    playAtIndex(0)
                }
            }
        }
    }

    fun pause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
                stopProgressUpdate()
            }
        }
    }

    fun stop() {
        mediaPlayer?.let { player ->
            player.stop()
            _isPlaying.value = false
            stopProgressUpdate()
            _currentPosition.value = 0L
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        _currentPosition.value = positionMs
    }

    fun skipToNext() {
        if (_queue.value.isEmpty()) return
        var nextIdx = _currentIndex.value + 1
        if (_isShuffleEnabled.value) {
            nextIdx = _queue.value.indices.random()
        } else if (nextIdx >= _queue.value.size) {
            nextIdx = 0 // loop to start
        }
        playAtIndex(nextIdx)
    }

    fun skipToPrevious() {
        if (_queue.value.isEmpty()) return
        var prevIdx = _currentIndex.value - 1
        if (prevIdx < 0) {
            prevIdx = _queue.value.size - 1 // wrap to end
        }
        playAtIndex(prevIdx)
    }

    fun toggleShuffle() {
        _isShuffleEnabled.value = !_isShuffleEnabled.value
    }

    fun toggleRepeat() {
        _isRepeatEnabled.value = !_isRepeatEnabled.value
    }

    fun updateSongFavoriteStatus(path: String, isFavorite: Boolean) {
        val current = _currentSong.value
        if (current != null && current.path == path) {
            _currentSong.value = current.copy(isFavorite = isFavorite)
        }
        val updatedQueue = _queue.value.map { song ->
            if (song.path == path) {
                song.copy(isFavorite = isFavorite)
            } else {
                song
            }
        }
        _queue.value = updatedQueue
    }

    // Parameters Config
    fun updatePlaybackParams(speed: Float, pitch: Float) {
        playbackSpeed = speed
        playbackPitch = pitch
        applyPlaybackParams()
    }

    private fun applyPlaybackParams() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            try {
                mediaPlayer?.let { player ->
                    if (player.isPlaying || _isPlaying.value) {
                        val params = player.playbackParams
                        params.speed = playbackSpeed
                        params.pitch = playbackPitch
                        player.playbackParams = params
                    }
                }
            } catch (e: Exception) {
                Log.e("PlaybackManager", "Failed to apply playback parameters: ${e.message}")
            }
        }
    }

    // Audio Effects Configuration
    fun configureAudioEffects(
        enabled: Boolean,
        bass: Int,
        virt: Int,
        gains: List<Int>
    ) {
        equalizerEnabled = enabled
        bassBoostLevel = bass
        virtualizerLevel = virt
        eqBandsGains = gains
        applyAudioEffects()
    }

    private fun applyAudioEffects() {
        mediaPlayer?.let { player ->
            try {
                val audioSessionId = player.audioSessionId
                
                // Release old ones if session changed
                equalizer?.release()
                bassBoost?.release()
                virtualizer?.release()

                if (equalizerEnabled && audioSessionId != 0) {
                    equalizer = Equalizer(0, audioSessionId).apply {
                        enabled = true
                        val count = numberOfBands
                        for (i in 0 until count.toInt()) {
                            if (i < eqBandsGains.size) {
                                val gain = eqBandsGains[i] * 100 // convert to millibels
                                setBandLevel(i.toShort(), gain.toShort())
                            }
                        }
                    }

                    if (bassBoostLevel > 0) {
                        bassBoost = BassBoost(0, audioSessionId).apply {
                            enabled = true
                            setStrength(bassBoostLevel.toShort())
                        }
                    }

                    if (virtualizerLevel > 0) {
                        virtualizer = Virtualizer(0, audioSessionId).apply {
                            enabled = true
                            setStrength(virtualizerLevel.toShort())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PlaybackManager", "Error applying audio effects: ${e.message}")
            }
        }
    }

    // Sleep Timer
    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _sleepTimeRemaining.value = minutes
        
        sleepTimerJob = scope.launch {
            var timeLeft = minutes
            while (timeLeft > 0) {
                delay(60000) // wait 1 minute
                timeLeft--
                _sleepTimeRemaining.value = timeLeft
            }
            // Timer expired, pause playback!
            pause()
            _sleepTimeRemaining.value = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimeRemaining.value = null
    }

    // Progress updates loop
    private fun startProgressUpdate() {
        progressUpdateJob?.cancel()
        progressUpdateJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _currentPosition.value = player.currentPosition.toLong()
                    }
                }
                delay(250) // update progress every 250ms
            }
        }
    }

    private fun stopProgressUpdate() {
        progressUpdateJob?.cancel()
    }

    fun release() {
        stopProgressUpdate()
        sleepTimerJob?.cancel()
        scope.cancel()
        mediaPlayer?.release()
        equalizer?.release()
        bassBoost?.release()
        virtualizer?.release()
    }
}
