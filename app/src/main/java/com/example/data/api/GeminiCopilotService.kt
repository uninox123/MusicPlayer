package com.example.data.api

import android.util.Log
import com.example.data.database.SongEntity
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.BuildConfig

class GeminiCopilotService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(90, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .build()

    private fun extractJson(text: String): String {
        val trimmed = text.trim()
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1)
        }
        return trimmed
    }

    suspend fun queryCopilot(
        prompt: String,
        availableSongs: List<SongEntity>,
        chatHistory: List<Pair<String, Boolean>> = emptyList() // Pair of message to isUser
    ): CopilotResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val token = if (apiKey.startsWith("nvapi-")) {
            apiKey
        } else {
            "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"
        }

        // Contextual prompt showing available library
        val songsListBuilder = StringBuilder()
        availableSongs.forEachIndexed { index, song ->
            songsListBuilder.append("${index + 1}. Path: \"${song.path}\", Title: \"${song.title}\", Artist: \"${song.artist}\", Album: \"${song.album}\", Favorite: ${song.isFavorite}\n")
        }

        val systemInstruction = """
            You are Nova Music AI Copilot, a brilliant music concierge for the local-first Nova Music player.
            You have access to the user's local music library. Here is their current scanned library:
            ${songsListBuilder.toString()}
            
            Your job is to respond to user questions about their music, suggest custom playlists, help filter songs, or discuss lyrics.
            You MUST return a JSON object with two fields:
            1. "explanation": A friendly, helpful markdown description explaining your response or recommendation. Show deep, premium thinking about the music styles, vibe, or genres.
            2. "command": A structured instruction to the music player. Supported commands:
               - "CREATE_PLAYLIST": Set this when the user asks to generate/make a playlist of songs. Include the "playlistName" (string) and "songPaths" (JSON array of strings representing song paths from the library above) fields.
               - "PLAY_SONG": Set this if the user asks to play a specific song. Include a "songPath" field.
               - "FILTER_LIBRARY": Set this to filter their view. Include a "filterQuery" field (string).
               - "NONE": For general questions where no action is needed.
               
            Example playlist command response:
            {
               "explanation": "I've carefully curated a space-ambient mix featuring 'Starlight Voyage' and 'Cyberpunk Dreams' for your interstellar journey.",
               "command": "CREATE_PLAYLIST",
               "playlistName": "Interstellar Flight",
               "songPaths": ["https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3", "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"]
            }
            
            Return ONLY the raw JSON object. Do not wrap it in markdown code blocks like ```json.
        """.trimIndent()

        try {
            val url = "https://integrate.api.nvidia.com/v1/chat/completions"

            val messagesArray = JSONArray()

            // System instructions
            val systemMsg = JSONObject()
            systemMsg.put("role", "system")
            systemMsg.put("content", systemInstruction)
            messagesArray.put(systemMsg)

            // Add history
            chatHistory.forEach { (msg, isUser) ->
                val contentObj = JSONObject()
                contentObj.put("role", if (isUser) "user" else "assistant")
                contentObj.put("content", msg)
                messagesArray.put(contentObj)
            }

            // Add current prompt
            val currentContentObj = JSONObject()
            currentContentObj.put("role", "user")
            currentContentObj.put("content", prompt)
            messagesArray.put(currentContentObj)

            val requestBodyJson = JSONObject()
            requestBodyJson.put("model", "stepfun-ai/step-3.7-flash")
            requestBodyJson.put("messages", messagesArray)
            requestBodyJson.put("max_tokens", 16384)
            requestBodyJson.put("temperature", 1.00)
            requestBodyJson.put("top_p", 0.95)
            requestBodyJson.put("stream", false)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Log.e("GeminiCopilotService", "API call failed: $errorBody")
                    return@withContext CopilotResponse(
                        explanation = "Error communicating with Nvidia NIM: ${response.message}. Details: $errorBody",
                        command = "NONE"
                    )
                }

                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val choices = jsonResponse.optJSONArray("choices")
                    val firstChoice = choices?.optJSONObject(0)
                    val message = firstChoice?.optJSONObject("message")
                    val responseText = message?.optString("content")

                    if (responseText != null) {
                        try {
                            // Extract JSON from response text
                            val cleanText = extractJson(responseText)
                            val parsedResponse = JSONObject(cleanText)
                            
                            val explanation = parsedResponse.optString("explanation", "Here is your response.")
                            val command = parsedResponse.optString("command", "NONE")
                            
                            val playlistName = parsedResponse.optString("playlistName", "")
                            val songPaths = mutableListOf<String>()
                            val pathsArray = parsedResponse.optJSONArray("songPaths")
                            if (pathsArray != null) {
                                for (i in 0 until pathsArray.length()) {
                                    songPaths.add(pathsArray.getString(i))
                                }
                            }
                            
                            val songPath = parsedResponse.optString("songPath", "")
                            val filterQuery = parsedResponse.optString("filterQuery", "")

                            return@withContext CopilotResponse(
                                explanation = explanation,
                                command = command,
                                playlistName = playlistName,
                                songPaths = songPaths,
                                songPath = songPath,
                                filterQuery = filterQuery
                            )
                        } catch (e: Exception) {
                            Log.e("GeminiCopilotService", "Failed to parse JSON response: $responseText", e)
                            return@withContext CopilotResponse(
                                explanation = responseText, // Fallback to raw text if parsing fails
                                command = "NONE"
                            )
                        }
                    }
                }
                return@withContext CopilotResponse(
                    explanation = "Received empty response from Nvidia NIM.",
                    command = "NONE"
                )
            }
        } catch (e: Exception) {
            Log.e("GeminiCopilotService", "Network request exception", e)
            return@withContext CopilotResponse(
                explanation = "Failed to reach Nvidia NIM server. Connection error: ${e.localizedMessage}",
                command = "NONE"
            )
        }
    }
}

data class CopilotResponse(
    val explanation: String,
    val command: String,
    val playlistName: String = "",
    val songPaths: List<String> = emptyList(),
    val songPath: String = "",
    val filterQuery: String = ""
)
