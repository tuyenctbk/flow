package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AmbientSoundProfile
import com.example.viewmodel.FlowViewModel

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.util.FlowHapticManager

@Composable
fun SoundscapeMixerCard(
    viewModel: FlowViewModel,
    modifier: Modifier = Modifier
) {
    val activeProfile by viewModel.selectedAmbientProfile.collectAsState()
    val binauralVol by viewModel.binauralLayerVol.collectAsState()
    val noiseVol by viewModel.noiseLayerVol.collectAsState()
    val bowlVol by viewModel.singingBowlVol.collectAsState()
    val masterVol by viewModel.masterVolume.collectAsState()
    val binauralMode by viewModel.binauralMode.collectAsState()
    val isTransitioning by viewModel.isSoundscapeTransitioning.collectAsState()
    val transitioningTo by viewModel.soundscapeTransitioningTo.collectAsState()
    val hapticEnabled by viewModel.hapticFeedbackEnabled.collectAsState()
    val context = LocalContext.current

    var showMixerPanel by remember { mutableStateOf(false) }

    val ZenAmber = MaterialTheme.colorScheme.primary
    val ZenStoneDark = MaterialTheme.colorScheme.surface
    val ZenStoneGrey = MaterialTheme.colorScheme.surfaceVariant
    val ZenTextPrimary = MaterialTheme.colorScheme.onSurface
    val ZenTextSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ZenStoneDark),
        border = BorderStroke(1.dp, if (showMixerPanel) ZenAmber.copy(alpha = 0.5f) else ZenStoneGrey),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Title, Active Status, Bell Strike & Mixer Expand Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ZenAmber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(activeProfile.emoji, fontSize = 16.sp)
                    }
                    Column {
                        Text(
                            text = "SOUNDSCAPE MATRIX",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ZenAmber,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = activeProfile.displayName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ZenTextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Strike Singing Bowl Button
                    IconButton(
                        onClick = { viewModel.strikeBowl() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(ZenStoneGrey, CircleShape)
                            .testTag("soundscape_bell_strike")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = "Strike Singing Bowl",
                            tint = ZenAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Open / Close Mixer Layers
                    OutlinedButton(
                        onClick = { showMixerPanel = !showMixerPanel },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (showMixerPanel) ZenAmber else ZenStoneGrey),
                        modifier = Modifier.testTag("toggle_soundscape_mixer")
                    ) {
                        Icon(
                            imageVector = if (showMixerPanel) Icons.Default.Tune else Icons.Outlined.Tune,
                            contentDescription = "Layers",
                            tint = if (showMixerPanel) ZenAmber else ZenTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showMixerPanel) "Hide Mixer" else "Mix Layers",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (showMixerPanel) ZenAmber else ZenTextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Crossfading notification pill
            AnimatedVisibility(
                visible = isTransitioning && transitioningTo != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ZenAmber.copy(alpha = 0.12f))
                        .border(1.dp, ZenAmber.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GraphicEq,
                        contentDescription = null,
                        tint = ZenAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = stringResource(R.string.soundscape_crossfading_into, transitioningTo?.displayName ?: ""),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ZenAmber,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // Presets Horizontal Slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AmbientSoundProfile.entries.forEach { profile ->
                    val isSelected = activeProfile == profile
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) ZenAmber.copy(alpha = 0.15f) else MaterialTheme.colorScheme.background)
                            .border(1.dp, if (isSelected) ZenAmber else ZenStoneGrey, RoundedCornerShape(12.dp))
                            .clickable {
                                FlowHapticManager.performClickHaptic(context, hapticEnabled)
                                viewModel.setAmbientProfile(profile)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("sound_profile_${profile.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(profile.emoji, fontSize = 14.sp)
                            Text(
                                text = profile.displayName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = if (isSelected) ZenAmber else ZenTextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            // Expanded Multi-Layer Acoustic Mixing Console
            AnimatedVisibility(
                visible = showMixerPanel,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Divider(color = ZenStoneGrey.copy(alpha = 0.6f))

                    Text(
                        text = "ACOUSTIC MULTI-LAYER BLEND",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ZenAmber,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )

                    // 1. Binaural Beat Entrainment Frequency Selector
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🧠 Binaural Brainwave Entrainment",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = ZenTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = "${(binauralVol * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(color = ZenAmber)
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                40.0 to "40Hz Gamma (Deep Focus)",
                                10.0 to "10Hz Alpha (Fluid Calm)",
                                6.0 to "6Hz Theta (Deep Insight)"
                            ).forEach { (freq, label) ->
                                val isSelected = binauralMode == freq
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) ZenAmber.copy(alpha = 0.2f) else MaterialTheme.colorScheme.background,
                                    border = BorderStroke(1.dp, if (isSelected) ZenAmber else ZenStoneGrey),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.setBinauralFrequency(freq) }
                                ) {
                                    Text(
                                        text = label.split(" ")[0],
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) ZenAmber else ZenTextSecondary,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Slider(
                            value = binauralVol,
                            onValueChange = { viewModel.setBinauralLayerVol(it) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = ZenAmber,
                                activeTrackColor = ZenAmber,
                                inactiveTrackColor = ZenStoneGrey
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 2. Continuous Organic Noise & Rainfall Texture
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🌊 Organic Noise & Rain Filter",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = ZenTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = "${(noiseVol * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(color = ZenAmber)
                            )
                        }
                        Slider(
                            value = noiseVol,
                            onValueChange = { viewModel.setNoiseLayerVol(it) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = ZenAmber,
                                activeTrackColor = ZenAmber,
                                inactiveTrackColor = ZenStoneGrey
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 3. Singing Bowl Acoustic Resonance
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🔔 Tibetan Singing Bowl Resonance",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = ZenTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = "${(bowlVol * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(color = ZenAmber)
                            )
                        }
                        Slider(
                            value = bowlVol,
                            onValueChange = { viewModel.setSingingBowlVol(it) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = ZenAmber,
                                activeTrackColor = ZenAmber,
                                inactiveTrackColor = ZenStoneGrey
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 4. Master Gain
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🎚️ Master Acoustic Volume",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = ZenTextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = "${(masterVol * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall.copy(color = ZenAmber)
                            )
                        }
                        Slider(
                            value = masterVol,
                            onValueChange = { viewModel.setMasterVolume(it) },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = ZenAmber,
                                activeTrackColor = ZenAmber,
                                inactiveTrackColor = ZenStoneGrey
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
