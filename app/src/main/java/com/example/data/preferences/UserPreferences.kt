package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nova_preferences")

class UserPreferences(private val context: Context) {

    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode") // "light", "dark", "system", "amoled"
        val MATERIAL_YOU = booleanPreferencesKey("material_you")
        val PRIMARY_COLOR = stringPreferencesKey("primary_color") // hex color string
        val CARD_RADIUS = intPreferencesKey("card_radius") // 8, 12, 16, 24
        val HOME_SECTIONS_ORDER = stringPreferencesKey("home_sections_order") // JSON or comma separated
        val GESTURE_DOUBLE_TAP_ARTWORK = stringPreferencesKey("gesture_double_tap_artwork") // "play_pause", "favorite", "next", "lyrics", "none"
        val GESTURE_SWIPE_LEFT_ARTWORK = stringPreferencesKey("gesture_swipe_left_artwork") // "previous", "none"
        val GESTURE_SWIPE_RIGHT_ARTWORK = stringPreferencesKey("gesture_swipe_right_artwork") // "next", "none"
        
        // Audio Engine
        val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val CROSSFADE_DURATION = intPreferencesKey("crossfade_duration") // seconds
        val EQUALIZER_ENABLED = booleanPreferencesKey("equalizer_enabled")
        val BASS_BOOST_LEVEL = intPreferencesKey("bass_boost_level") // 0 to 1000
        val VIRTUALIZER_LEVEL = intPreferencesKey("virtualizer_level") // 0 to 1000
        val EQ_PRESET = stringPreferencesKey("eq_preset") // "flat", "bass_boost", "rock", "pop", "classical", "custom"
        val EQ_BANDS = stringPreferencesKey("eq_bands") // comma separated gains
        
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val PLAYBACK_PITCH = floatPreferencesKey("playback_pitch")
    }

    data class PreferencesState(
        val themeMode: String = "dark",
        val useMaterialYou: Boolean = false,
        val primaryColorHex: String = "#3D69FF", // Beautiful electric blue default
        val cardRadiusDp: Int = 16,
        val homeSectionsOrder: String = "recently_played,most_played,favorites,recently_added,playlists",
        val gestureDoubleTapArtwork: String = "favorite",
        val gestureSwipeLeftArtwork: String = "previous",
        val gestureSwipeRightArtwork: String = "next",
        val gaplessPlayback: Boolean = true,
        val crossfadeDuration: Int = 0,
        val equalizerEnabled: Boolean = false,
        val bassBoostLevel: Int = 0,
        val virtualizerLevel: Int = 0,
        val eqPreset: String = "flat",
        val eqBands: String = "0,0,0,0,0", // 5 bands by default
        val playbackSpeed: Float = 1.0f,
        val playbackPitch: Float = 1.0f
    )

    val preferencesFlow: Flow<PreferencesState> = context.dataStore.data.map { preferences ->
        PreferencesState(
            themeMode = preferences[THEME_MODE] ?: "dark",
            useMaterialYou = preferences[MATERIAL_YOU] ?: false,
            primaryColorHex = preferences[PRIMARY_COLOR] ?: "#3D69FF",
            cardRadiusDp = preferences[CARD_RADIUS] ?: 16,
            homeSectionsOrder = preferences[HOME_SECTIONS_ORDER] ?: "recently_played,most_played,favorites,recently_added,playlists",
            gestureDoubleTapArtwork = preferences[GESTURE_DOUBLE_TAP_ARTWORK] ?: "favorite",
            gestureSwipeLeftArtwork = preferences[GESTURE_SWIPE_LEFT_ARTWORK] ?: "previous",
            gestureSwipeRightArtwork = preferences[GESTURE_SWIPE_RIGHT_ARTWORK] ?: "next",
            gaplessPlayback = preferences[GAPLESS_PLAYBACK] ?: true,
            crossfadeDuration = preferences[CROSSFADE_DURATION] ?: 0,
            equalizerEnabled = preferences[EQUALIZER_ENABLED] ?: false,
            bassBoostLevel = preferences[BASS_BOOST_LEVEL] ?: 0,
            virtualizerLevel = preferences[VIRTUALIZER_LEVEL] ?: 0,
            eqPreset = preferences[EQ_PRESET] ?: "flat",
            eqBands = preferences[EQ_BANDS] ?: "0,0,0,0,0",
            playbackSpeed = preferences[PLAYBACK_SPEED] ?: 1.0f,
            playbackPitch = preferences[PLAYBACK_PITCH] ?: 1.0f
        )
    }

    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun updateMaterialYou(use: Boolean) {
        context.dataStore.edit { it[MATERIAL_YOU] = use }
    }

    suspend fun updatePrimaryColor(hex: String) {
        context.dataStore.edit { it[PRIMARY_COLOR] = hex }
    }

    suspend fun updateCardRadius(radius: Int) {
        context.dataStore.edit { it[CARD_RADIUS] = radius }
    }

    suspend fun updateHomeSectionsOrder(order: String) {
        context.dataStore.edit { it[HOME_SECTIONS_ORDER] = order }
    }

    suspend fun updateGestures(doubleTap: String, swipeLeft: String, swipeRight: String) {
        context.dataStore.edit {
            it[GESTURE_DOUBLE_TAP_ARTWORK] = doubleTap
            it[GESTURE_SWIPE_LEFT_ARTWORK] = swipeLeft
            it[GESTURE_SWIPE_RIGHT_ARTWORK] = swipeRight
        }
    }

    suspend fun updateAudioEngine(
        gapless: Boolean,
        crossfade: Int,
        eqEnabled: Boolean,
        bassBoost: Int,
        virtualizer: Int,
        preset: String,
        bands: String
    ) {
        context.dataStore.edit {
            it[GAPLESS_PLAYBACK] = gapless
            it[CROSSFADE_DURATION] = crossfade
            it[EQUALIZER_ENABLED] = eqEnabled
            it[BASS_BOOST_LEVEL] = bassBoost
            it[VIRTUALIZER_LEVEL] = virtualizer
            it[EQ_PRESET] = preset
            it[EQ_BANDS] = bands
        }
    }

    suspend fun updatePlaybackParams(speed: Float, pitch: Float) {
        context.dataStore.edit {
            it[PLAYBACK_SPEED] = speed
            it[PLAYBACK_PITCH] = pitch
        }
    }
}
