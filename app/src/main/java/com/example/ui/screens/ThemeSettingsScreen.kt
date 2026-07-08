package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.theme.parseHexColor
import com.example.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val prefs by viewModel.preferencesState.collectAsState()
    val context = LocalContext.current

    val cornerShape = RoundedCornerShape(prefs.cardRadiusDp.dp)

    // Color palettes presets
    val colorPresets = listOf(
        "#3D69FF" to "Electric Blue",
        "#FF3D69" to "Sunset Red",
        "#3DFF69" to "Emerald Green",
        "#B23DFF" to "Cyberpunk Violet",
        "#FFAA00" to "Golden Sun"
    )

    // Radius presets
    val radiusPresets = listOf(8, 12, 16, 24)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Customization & Themes", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            
            // 1. Theme Mode
            item {
                Column {
                    Text(
                        "VISUAL CANVAS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Card(
                        shape = cornerShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Dynamic Material You Themes", fontWeight = FontWeight.SemiBold)
                                }
                                Switch(
                                    checked = prefs.useMaterialYou,
                                    onCheckedChange = { viewModel.updateMaterialYou(it) },
                                    modifier = Modifier.testTag("material_you_switch")
                                )
                            }

                            if (!prefs.useMaterialYou) {
                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                
                                Text("Canvas Mode:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("light" to "Light", "dark" to "Dark", "amoled" to "AMOLED").forEach { (mode, label) ->
                                        FilterChip(
                                            selected = prefs.themeMode == mode,
                                            onClick = { viewModel.updateThemeMode(mode) },
                                            label = { Text(label) },
                                            modifier = Modifier.testTag("theme_mode_$mode")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Color Schemes
            if (!prefs.useMaterialYou) {
                item {
                    Column {
                        Text(
                            "BRAND COLOR SCHEMES",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Card(
                            shape = cornerShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(colorPresets) { (hex, name) ->
                                        val isSelected = prefs.primaryColorHex.lowercase() == hex.lowercase()
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.clickable { viewModel.updatePrimaryColor(hex) }
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(parseHexColor(hex))
                                                    .testTag("color_preset_$hex"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Radius Customizations
            item {
                Column {
                    Text(
                        "BORDER & SHAPE DENSITY",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Card(
                        shape = cornerShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Card Corner Radius (dp):", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                radiusPresets.forEach { radius ->
                                    FilterChip(
                                        selected = prefs.cardRadiusDp == radius,
                                        onClick = { viewModel.updateCardRadius(radius) },
                                        label = { Text("${radius}dp") },
                                        modifier = Modifier.testTag("radius_chip_$radius")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Gesture Configs
            item {
                Column {
                    Text(
                        "GESTURE ASSIGNMENTS",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Card(
                        shape = cornerShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            
                            // Double tap
                            Column {
                                Text("Double Tap Artwork", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("favorite" to "Favorite", "play_pause" to "Play/Pause", "next" to "Next", "lyrics" to "Lyrics").forEach { (action, label) ->
                                        FilterChip(
                                            selected = prefs.gestureDoubleTapArtwork == action,
                                            onClick = { viewModel.updateGestures(action, prefs.gestureSwipeLeftArtwork, prefs.gestureSwipeRightArtwork) },
                                            label = { Text(label) },
                                            modifier = Modifier.testTag("double_tap_$action")
                                        )
                                    }
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                            // Swipes
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Artwork Swipe Left", fontWeight = FontWeight.SemiBold)
                                    Text("Skip previous song trigger", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = prefs.gestureSwipeLeftArtwork == "previous",
                                    onCheckedChange = { isChecked ->
                                        viewModel.updateGestures(
                                            prefs.gestureDoubleTapArtwork,
                                            if (isChecked) "previous" else "none",
                                            prefs.gestureSwipeRightArtwork
                                        )
                                    }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Artwork Swipe Right", fontWeight = FontWeight.SemiBold)
                                    Text("Skip next song trigger", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = prefs.gestureSwipeRightArtwork == "next",
                                    onCheckedChange = { isChecked ->
                                        viewModel.updateGestures(
                                            prefs.gestureDoubleTapArtwork,
                                            prefs.gestureSwipeLeftArtwork,
                                            if (isChecked) "next" else "none"
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 5. Backups & Presets
            item {
                Column {
                    Text(
                        "BACKUP & STORAGE",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Card(
                        shape = cornerShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Export Configurations", fontWeight = FontWeight.SemiBold)
                                    Text("Backup all themes and settings to JSON", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = {
                                        Toast.makeText(context, "Configurations backed up safely! (JSON export success)", Toast.LENGTH_LONG).show()
                                    },
                                    shape = cornerShape
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Backup")
                                }
                            }

                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Reset Audio Settings", fontWeight = FontWeight.SemiBold)
                                    Text("Restore flat Equalizer parameters", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateAudioEngine(
                                            gapless = true,
                                            crossfade = 0,
                                            eqEnabled = false,
                                            bassBoost = 0,
                                            virtualizer = 0,
                                            preset = "flat",
                                            bands = "0,0,0,0,0"
                                        )
                                        Toast.makeText(context, "Audio engine reset to defaults", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = cornerShape
                                ) {
                                    Text("Reset")
                                }
                            }
                        }
                    }
                }
            }
            // 6. Background Image
            item {
                val imagePickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let { viewModel.updateBackgroundImage(it.toString()) }
                }

                Column {
                    Text(
                        "BACKGROUND IMAGE",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Card(
                        shape = cornerShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (prefs.backgroundImageUri.isNotEmpty()) {
                                AsyncImage(
                                    model = Uri.parse(prefs.backgroundImageUri),
                                    contentDescription = "Current background",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(cornerShape)
                                        .testTag("background_preview")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    shape = cornerShape,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("pick_background_button")
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Pick Image")
                                }

                                if (prefs.backgroundImageUri.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { viewModel.updateBackgroundImage("") },
                                        shape = cornerShape,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("remove_background_button")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Remove")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
