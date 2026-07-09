package com.example.data.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val path: String, // file path or unique identifier (e.g., asset path or URL)
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long, // in milliseconds
    val albumArtUri: String?,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayed: Long = 0,
    val addedDate: Long = System.currentTimeMillis(),
    val bitrate: Int = 320, // kbps
    val sampleRate: Int = 44100, // Hz
    val fileType: String = "mp3",
    val isDownloaded: Boolean = false,
    val onlineMetadataJson: String? = null
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdTime: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_song_cross_ref",
    primaryKeys = ["playlistId", "songPath"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["path"],
            childColumns = ["songPath"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["songPath"])]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songPath: String,
    val orderIndex: Int = 0
)

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songPath: String,
    val timestamp: Long = System.currentTimeMillis()
)
