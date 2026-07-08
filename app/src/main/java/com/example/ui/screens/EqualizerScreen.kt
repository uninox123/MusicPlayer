package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.preferencesState.collectAsState()
    val cornerShape = RoundedCornerShape(prefs.cardRadiusDp.dp)

    // Parse current band values from preferences
    val currentBains = remember(prefs.eqBands) {
        prefs.eqBands.split(",").map { it.toIntOrNull() ?: 0 }
    }

    // 5 standard Equalizer Band center frequencies
    val frequencies = listOf("60 Hz", "230 Hz", "910 Hz", "4 kHz", "14 kHz")
    val presets = listOf("Flat", "Bass Boost", "Rock", "Pop", "Classical")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Audio Engine", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // Equalizer Master Switch
            Card(
                shape = cornerShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Equalizer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Equalizer Engine", fontWeight = FontWeight.Bold)
                            Text("10-Band Virtual Signal Processor", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = prefs.equalizerEnabled,
                        onCheckedChange = { isChecked ->
                            viewModel.updateAudioEngine(
                                gapless = prefs.gaplessPlayback,
                                crossfade = prefs.crossfadeDuration,
                                eqEnabled = isChecked,
                                bassBoost = prefs.bassBoostLevel,
                                virtualizer = prefs.virtualizerLevel,
                                preset = prefs.eqPreset,
                                bands = prefs.eqBands
                            )
                        },
                        modifier = Modifier.testTag("eq_switch")
                    )
                }
            }

            // Presets row
            if (prefs.equalizerEnabled) {
                Column {
                    Text(
                        "PRESETS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(presets) { idx, name ->
                            val isSelected = prefs.eqPreset.lowercase() == name.lowercase()
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newBands = when (name.lowercase()) {
                                        "bass boost" -> "8,6,3,1,0"
                                        "rock" -> "5,3,-1,2,4"
                                        "pop" -> "-2,1,4,2,-1"
                                        "classical" -> "4,2,0,2,4"
                                        else -> "0,0,0,0,0" // flat
                                    }
                                    viewModel.updateAudioEngine(
                                        gapless = prefs.gaplessPlayback,
                                        crossfade = prefs.crossfadeDuration,
                                        eqEnabled = prefs.equalizerEnabled,
                                        bassBoost = if (name.lowercase() == "bass boost") 500 else prefs.bassBoostLevel,
                                        virtualizer = prefs.virtualizerLevel,
                                        preset = name.lowercase(),
                                        bands = newBands
                                    )
                                },
                                label = { Text(name) },
                                modifier = Modifier.testTag("preset_chip_$name")
                            )
                        }
                    }
                }

                // 5-Band Vertical sliders
                Column {
                    Text(
                        "BAND ADJUSTMENTS (dB)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        frequencies.forEachIndexed { bandIdx, freq ->
                            val gain = currentBains.getOrNull(bandIdx) ?: 0
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                            ) {
                                Text("$gain dB", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Slider(
                                    value = gain.toFloat(),
                                    onValueChange = { newVal ->
                                        val mutableBands = currentBains.toMutableList()
                                        mutableBands[bandIdx] = newVal.toInt()
                                        val bandsStr = mutableBands.joinToString(",")
                                        viewModel.updateAudioEngine(
                                            gapless = prefs.gaplessPlayback,
                                            crossfade = prefs.crossfadeDuration,
                                            eqEnabled = prefs.equalizerEnabled,
                                            bassBoost = prefs.bassBoostLevel,
                                            virtualizer = prefs.virtualizerLevel,
                                            preset = "custom",
                                            bands = bandsStr
                                        )
                                    },
                                    valueRange = -12f..12f,
                                    steps = 24,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("eq_slider_$bandIdx"),
                                )
                                Text(freq, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // Bass Boost & Virtualizer sliders
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "SPATIALIZERS & EFFECTS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Bass Boost
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Deep Bass Extender", fontWeight = FontWeight.SemiBold)
                            Text("${prefs.bassBoostLevel / 10}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = prefs.bassBoostLevel.toFloat(),
                            onValueChange = { newVal ->
                                viewModel.updateAudioEngine(
                                    gapless = prefs.gaplessPlayback,
                                    crossfade = prefs.crossfadeDuration,
                                    eqEnabled = prefs.equalizerEnabled,
                                    bassBoost = newVal.toInt(),
                                    virtualizer = prefs.virtualizerLevel,
                                    preset = prefs.eqPreset,
                                    bands = prefs.eqBands
                                )
                            },
                            valueRange = 0f..1000f,
                            modifier = Modifier.testTag("bass_slider")
                        )
                    }

                    // Virtualizer
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("3D Virtualizer / Hall Depth", fontWeight = FontWeight.SemiBold)
                            Text("${prefs.virtualizerLevel / 10}%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = prefs.virtualizerLevel.toFloat(),
                            onValueChange = { newVal ->
                                viewModel.updateAudioEngine(
                                    gapless = prefs.gaplessPlayback,
                                    crossfade = prefs.crossfadeDuration,
                                    eqEnabled = prefs.equalizerEnabled,
                                    bassBoost = prefs.bassBoostLevel,
                                    virtualizer = newVal.toInt(),
                                    preset = prefs.eqPreset,
                                    bands = prefs.eqBands
                                )
                            },
                            valueRange = 0f..1000f,
                            modifier = Modifier.testTag("virtualizer_slider")
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Equalizer,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Turn on the Equalizer Engine above\nto adjust frequency channels, bass boost, and 3D acoustics.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
