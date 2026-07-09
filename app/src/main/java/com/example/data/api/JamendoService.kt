package com.example.data.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class JamendoTrack(
    val id: String,
    val name: String,
    val duration: Long, // duration in milliseconds
    val artistId: String,
    val artistName: String,
    val albumId: String,
    val albumName: String,
    val albumImage: String,
    val audio: String, // stream URL
    val audiodownload: String, // download URL
    val image: String,
    val licenseCcUrl: String,
    val releaseDate: String,
    val playCount: Int,
    val downloadCount: Int,
    val language: String,
    val gender: String,
    val speed: String,
    val vocalInstrumental: String,
    val acousticElectric: String,
    val tags: List<String>,
    val rawJson: String
)

data class JamendoArtist(
    val id: String,
    val name: String,
    val website: String,
    val joinDate: String,
    val image: String,
    val shareUrl: String,
    val rawJson: String
)

class JamendoService {
    private val clientId = "b06b364b"
    private val baseUrl = "https://api.jamendo.com/v3.0"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun safeEncode(value: String): String {
        return try {
            URLEncoder.encode(value, "UTF-8")
        } catch (e: Exception) {
            value
        }
    }

    suspend fun searchTracks(query: String): List<JamendoTrack> = withContext(Dispatchers.IO) {
        val url = if (query.isBlank()) {
            "$baseUrl/tracks/?client_id=$clientId&format=json&limit=50&order=popularity_week&include=musicinfo+licenses+stats"
        } else {
            "$baseUrl/tracks/?client_id=$clientId&format=json&limit=50&namesearch=${safeEncode(query)}&include=musicinfo+licenses+stats"
        }
        
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                return@withContext parseTracks(body)
            }
        } catch (e: Exception) {
            Log.e("JamendoService", "Error searching tracks", e)
            emptyList()
        }
    }

    suspend fun searchArtists(query: String): List<JamendoArtist> = withContext(Dispatchers.IO) {
        val url = if (query.isBlank()) {
            "$baseUrl/artists/?client_id=$clientId&format=json&limit=30&order=popularity"
        } else {
            "$baseUrl/artists/?client_id=$clientId&format=json&limit=30&namesearch=${safeEncode(query)}"
        }
        
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                return@withContext parseArtists(body)
            }
        } catch (e: Exception) {
            Log.e("JamendoService", "Error searching artists", e)
            emptyList()
        }
    }

    suspend fun getArtistTracks(artistId: String): List<JamendoTrack> = withContext(Dispatchers.IO) {
        val url = "$baseUrl/tracks/?client_id=$clientId&format=json&limit=50&artist_id=$artistId&include=musicinfo+licenses+stats"
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                return@withContext parseTracks(body)
            }
        } catch (e: Exception) {
            Log.e("JamendoService", "Error getting artist tracks", e)
            emptyList()
        }
    }

    suspend fun getArtistDetails(artistId: String): JamendoArtist? = withContext(Dispatchers.IO) {
        val url = "$baseUrl/artists/?client_id=$clientId&format=json&id=$artistId"
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                return@withContext parseArtists(body).firstOrNull()
            }
        } catch (e: Exception) {
            Log.e("JamendoService", "Error getting artist details", e)
            null
        }
    }

    private fun parseTracks(jsonString: String): List<JamendoTrack> {
        val list = mutableListOf<JamendoTrack>()
        try {
            val obj = JSONObject(jsonString)
            val results = obj.getJSONArray("results")
            for (i in 0 until results.length()) {
                val trackObj = results.getJSONObject(i)
                val id = trackObj.getString("id")
                val name = trackObj.getString("name")
                val duration = trackObj.optLong("duration", 0L) * 1000L // convert to ms
                val artistId = trackObj.optString("artist_id", "")
                val artistName = trackObj.optString("artist_name", "Unknown Artist")
                val albumId = trackObj.optString("album_id", "")
                val albumName = trackObj.optString("album_name", "Unknown Album")
                val albumImage = trackObj.optString("album_image", "")
                val audio = trackObj.optString("audio", "")
                val audiodownload = trackObj.optString("audiodownload", "")
                val image = trackObj.optString("image", "")
                val licenseCcUrl = trackObj.optString("license_ccurl", "")
                val releaseDate = trackObj.optString("releasedate", "")
                
                // parse stats
                val statsObj = trackObj.optJSONObject("stats")
                val playCount = statsObj?.optInt("playcount_total", 0) ?: 0
                val downloadCount = statsObj?.optInt("downloadcount_total", 0) ?: 0
                
                // parse musicinfo
                val musicInfoObj = trackObj.optJSONObject("musicinfo")
                val language = musicInfoObj?.optString("lang", "") ?: ""
                val gender = musicInfoObj?.optString("gender", "") ?: ""
                val speed = musicInfoObj?.optString("speed", "") ?: ""
                val vocalInstrumental = musicInfoObj?.optString("vocalinstrumental", "") ?: ""
                val acousticElectric = musicInfoObj?.optString("acousticelectric", "") ?: ""
                
                val tagsList = mutableListOf<String>()
                val tagsObj = musicInfoObj?.optJSONObject("tags")
                if (tagsObj != null) {
                    val genresArr = tagsObj.optJSONArray("genres")
                    if (genresArr != null) {
                        for (j in 0 until genresArr.length()) {
                            tagsList.add(genresArr.getString(j))
                        }
                    }
                    val instrumentsArr = tagsObj.optJSONArray("instruments")
                    if (instrumentsArr != null) {
                        for (j in 0 until instrumentsArr.length()) {
                            tagsList.add(instrumentsArr.getString(j))
                        }
                    }
                }

                list.add(
                    JamendoTrack(
                        id = id,
                        name = name,
                        duration = duration,
                        artistId = artistId,
                        artistName = artistName,
                        albumId = albumId,
                        albumName = albumName,
                        albumImage = albumImage,
                        audio = audio,
                        audiodownload = audiodownload,
                        image = image,
                        licenseCcUrl = licenseCcUrl,
                        releaseDate = releaseDate,
                        playCount = playCount,
                        downloadCount = downloadCount,
                        language = language,
                        gender = gender,
                        speed = speed,
                        vocalInstrumental = vocalInstrumental,
                        acousticElectric = acousticElectric,
                        tags = tagsList,
                        rawJson = trackObj.toString()
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("JamendoService", "Error parsing tracks", e)
        }
        return list
    }

    private fun parseArtists(jsonString: String): List<JamendoArtist> {
        val list = mutableListOf<JamendoArtist>()
        try {
            val obj = JSONObject(jsonString)
            val results = obj.getJSONArray("results")
            for (i in 0 until results.length()) {
                val artistObj = results.getJSONObject(i)
                val id = artistObj.getString("id")
                val name = artistObj.getString("name")
                val website = artistObj.optString("website", "")
                val joinDate = artistObj.optString("joindate", "")
                val image = artistObj.optString("image", "")
                val shareUrl = artistObj.optString("shareurl", "")
                
                list.add(
                    JamendoArtist(
                        id = id,
                        name = name,
                        website = website,
                        joinDate = joinDate,
                        image = image,
                        shareUrl = shareUrl,
                        rawJson = artistObj.toString()
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("JamendoService", "Error parsing artists", e)
        }
        return list
    }
}
