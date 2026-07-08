package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.example.data.database.PlaylistEntity
import com.example.data.database.PlaylistSongCrossRef
import com.example.data.database.SongDao
import com.example.data.database.SongEntity
import com.example.data.database.PlaybackHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

class MusicRepository(
    private val context: Context,
    private val songDao: SongDao
) {
    val allSongs: Flow<List<SongEntity>> = songDao.getAllSongs()
    val favoriteSongs: Flow<List<SongEntity>> = songDao.getFavoriteSongs()
    val recentlyAddedSongs: Flow<List<SongEntity>> = songDao.getRecentlyAddedSongs()
    val mostPlayedSongs: Flow<List<SongEntity>> = songDao.getMostPlayedSongs()
    val recentlyPlayedSongs: Flow<List<SongEntity>> = songDao.getRecentlyPlayedSongs()
    val playlists: Flow<List<PlaylistEntity>> = songDao.getAllPlaylists()
    val playbackHistory: Flow<List<SongEntity>> = songDao.getPlaybackHistory()

    suspend fun getSongsForPlaylist(playlistId: Long): Flow<List<SongEntity>> {
        return songDao.getSongsForPlaylist(playlistId)
    }

    suspend fun toggleFavorite(path: String, isFavorite: Boolean) {
        songDao.updateFavoriteStatus(path, isFavorite)
    }

    suspend fun incrementPlayCount(path: String) {
        songDao.incrementPlayCount(path, System.currentTimeMillis())
        songDao.insertHistoryEntry(PlaybackHistoryEntity(songPath = path))
    }

    suspend fun createPlaylist(name: String, description: String = ""): Long {
        return songDao.insertPlaylist(PlaylistEntity(name = name, description = description))
    }

    suspend fun addSongToPlaylist(playlistId: Long, songPath: String) {
        songDao.insertPlaylistSongCrossRef(PlaylistSongCrossRef(playlistId, songPath))
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songPath: String) {
        songDao.removeSongFromPlaylist(playlistId, songPath)
    }

    suspend fun deletePlaylist(playlistId: Long) {
        songDao.deletePlaylist(playlistId)
    }

    suspend fun clearHistory() {
        songDao.clearHistory()
    }

    // Pre-populate with beautiful royalty-free tracks if empty
    suspend fun prepopulateIfEmpty() = withContext(Dispatchers.IO) {
        val currentSongs = allSongs.firstOrNull() ?: emptyList()
        if (currentSongs.isEmpty()) {
            val defaultTracks = listOf(
                SongEntity(
                    path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                    title = "Starlight Voyage",
                    artist = "Aether Horizon",
                    album = "Cosmic Odyssey",
                    duration = 372000, // ~6:12
                    albumArtUri = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?q=80&w=600&auto=format&fit=crop", // beautiful space photography
                    isFavorite = false,
                    bitrate = 320,
                    sampleRate = 44100,
                    fileType = "mp3"
                ),
                SongEntity(
                    path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                    title = "Cyberpunk Dreams",
                    artist = "Tokyo Glitch",
                    album = "Neon Grid",
                    duration = 423000, // ~7:03
                    albumArtUri = "https://images.unsplash.com/photo-1578894381163-e72c17f2d45f?q=80&w=600&auto=format&fit=crop", // neon cyberpunk style
                    isFavorite = true, // start with one favorite
                    bitrate = 320,
                    sampleRate = 44100,
                    fileType = "mp3"
                ),
                SongEntity(
                    path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                    title = "Chillwave Solitude",
                    artist = "Lofi Echoes",
                    album = "Bedroom Sunset",
                    duration = 344000, // ~5:44
                    albumArtUri = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?q=80&w=600&auto=format&fit=crop", // sunset lofi chill
                    isFavorite = false,
                    bitrate = 320,
                    sampleRate = 44100,
                    fileType = "mp3"
                ),
                SongEntity(
                    path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                    title = "Retro Resonance",
                    artist = "Pixel Arcade",
                    album = "8-Bit Nostalgia",
                    duration = 302000, // ~5:02
                    albumArtUri = "https://images.unsplash.com/photo-1550745165-9bc0b252726f?q=80&w=600&auto=format&fit=crop", // retro tech/gaming
                    isFavorite = false,
                    bitrate = 320,
                    sampleRate = 44100,
                    fileType = "mp3"
                )
            )
            songDao.insertSongs(defaultTracks)
            Log.d("MusicRepository", "Pre-populated database with 4 default tracks")
            
            // Pre-populate an initial playlist
            val playlistId = createPlaylist("Interstellar Flight", "A deep cosmic playlist for coding and relaxation.")
            addSongToPlaylist(playlistId, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3")
            addSongToPlaylist(playlistId, "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3")
        }
    }

    // Actual Android MediaStore scanner to look for local device MP3s
    suspend fun scanLocalMedia() = withContext(Dispatchers.IO) {
        val songsList = mutableListOf<SongEntity>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (c.moveToNext()) {
                    val id = c.getLong(idColumn)
                    val title = c.getString(titleColumn) ?: "Unknown Song"
                    val artist = c.getString(artistColumn) ?: "Unknown Artist"
                    val album = c.getString(albumColumn) ?: "Unknown Album"
                    val duration = c.getLong(durationColumn)
                    val data = c.getString(dataColumn)

                    // Construct content URI for album art from album ID
                    // Here we can use content://media/external/audio/media/[id]/albumart or a generic placeholder
                    val albumArtUri = "content://media/external/audio/albumart/$id"

                    songsList.add(
                        SongEntity(
                            path = data,
                            title = title,
                            artist = artist,
                            album = album,
                            duration = duration,
                            albumArtUri = albumArtUri,
                            isFavorite = false,
                            addedDate = System.currentTimeMillis()
                        )
                    )
                }
            }

            if (songsList.isNotEmpty()) {
                songDao.insertSongs(songsList)
                Log.d("MusicRepository", "Scanned and added ${songsList.size} local songs")
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error scanning local media: ${e.message}")
        }
    }
}
