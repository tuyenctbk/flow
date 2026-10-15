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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.archiveaudio.ArchiveAudioItem
import com.example.ui.util.FlowHapticManager
import com.example.viewmodel.FlowViewModel

@Composable
fun SoundSlumberArchiveCard(
    viewModel: FlowViewModel,
    modifier: Modifier = Modifier
) {
    val archiveItems by viewModel.archiveAudioItems.collectAsState()
    val searchQuery by viewModel.archiveSearchQuery.collectAsState()
    val isSearching by viewModel.isArchiveSearching.collectAsState()
    val activeItem by viewModel.activeArchiveItem.collectAsState()
    val isPlaying by viewModel.isArchivePlaying.collectAsState()
    val isBuffering by viewModel.isArchiveBuffering.collectAsState()
    val archiveVolume by viewModel.archiveVolume.collectAsState()
    val hapticEnabled by viewModel.hapticFeedbackEnabled.collectAsState()

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val ZenAmber = MaterialTheme.colorScheme.primary
    val ZenStoneDark = MaterialTheme.colorScheme.surface
    val ZenStoneGrey = MaterialTheme.colorScheme.surfaceVariant
    val ZenTextPrimary = MaterialTheme.colorScheme.onSurface
    val ZenTextSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    val categoryChips = listOf(
        "All", "Rainforest", "Thunderstorm", "NASA Space", "Ocean", "Campfire", "Night Slumber"
    )
    var selectedCategory by remember { mutableStateOf("All") }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ZenStoneDark),
        border = BorderStroke(1.dp, ZenAmber.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
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
                        Icon(
                            imageVector = Icons.Outlined.LibraryMusic,
                            contentDescription = null,
                            tint = ZenAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.soundslumber_header_title),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ZenAmber,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = stringResource(R.string.soundslumber_header_subtitle),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ZenTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar for Internet Archive
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchArchiveAudio(it) },
                placeholder = {
                    Text(
                        text = stringResource(R.string.soundslumber_search_placeholder),
                        style = MaterialTheme.typography.bodySmall.copy(color = ZenTextSecondary)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = ZenAmber,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = ZenAmber,
                            strokeWidth = 2.dp
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboardController?.hide()
                    viewModel.searchArchiveAudio(searchQuery)
                }),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ZenAmber,
                    unfocusedBorderColor = ZenStoneGrey,
                    focusedContainerColor = ZenStoneGrey.copy(alpha = 0.3f),
                    unfocusedContainerColor = ZenStoneGrey.copy(alpha = 0.3f),
                    focusedTextColor = ZenTextPrimary,
                    unfocusedTextColor = ZenTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("archive_search_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                categoryChips.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) ZenAmber.copy(alpha = 0.2f) else ZenStoneGrey.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, if (isSelected) ZenAmber else ZenStoneGrey),
                        modifier = Modifier
                            .clickable {
                                FlowHapticManager.performClickHaptic(context, hapticEnabled)
                                selectedCategory = cat
                                val query = if (cat == "All") "" else cat
                                viewModel.searchArchiveAudio(query)
                            }
                            .testTag("archive_cat_${cat.lowercase().replace(" ", "_")}")
                    ) {
                        Text(
                            text = cat,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isSelected) ZenAmber else ZenTextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Currently Playing Stream Deck Banner
            AnimatedVisibility(
                visible = activeItem != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                activeItem?.let { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = ZenAmber.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, ZenAmber.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(item.iconEmoji, fontSize = 20.sp)
                                    Column {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = ZenAmber,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = if (isBuffering) stringResource(R.string.buffering_archive_stream)
                                            else if (isPlaying) stringResource(R.string.streaming_from_archive_org)
                                            else stringResource(R.string.stream_paused),
                                            style = MaterialTheme.typography.labelSmall.copy(color = ZenTextSecondary, fontSize = 10.sp)
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            FlowHapticManager.performClickHaptic(context, hapticEnabled)
                                            viewModel.toggleArchivePlayback()
                                        },
                                        modifier = Modifier.testTag("toggle_archive_playback")
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = ZenAmber
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            FlowHapticManager.performClickHaptic(context, hapticEnabled)
                                            viewModel.stopArchiveAudio()
                                        },
                                        modifier = Modifier.testTag("stop_archive_playback")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Stop,
                                            contentDescription = null,
                                            tint = ZenTextSecondary
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Volume Slider
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = ZenAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Slider(
                                    value = archiveVolume,
                                    onValueChange = { viewModel.setArchiveVolume(it) },
                                    valueRange = 0f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = ZenAmber,
                                        activeTrackColor = ZenAmber,
                                        inactiveTrackColor = ZenStoneGrey
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${(archiveVolume * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall.copy(color = ZenAmber, fontSize = 10.sp)
                                )
                            }
                        }
                    }
                }
            }

            // Audio Items List
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                archiveItems.take(6).forEach { item ->
                    val isThisActive = activeItem?.id == item.id
                    val isThisPlaying = isThisActive && isPlaying

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isThisActive) ZenAmber.copy(alpha = 0.1f) else ZenStoneGrey.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, if (isThisActive) ZenAmber else ZenStoneGrey),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                FlowHapticManager.performClickHaptic(context, hapticEnabled)
                                viewModel.playArchiveItem(item)
                            }
                            .testTag("play_archive_item_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isThisActive) ZenAmber.copy(alpha = 0.2f) else ZenStoneGrey),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(item.iconEmoji, fontSize = 18.sp)
                                }

                                Column {
                                    Text(
                                        text = item.title,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isThisActive) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isThisActive) ZenAmber else ZenTextPrimary
                                        ),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${item.category} • ${item.durationText}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = ZenTextSecondary,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isThisPlaying) ZenAmber else ZenStoneGrey),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isThisActive && isBuffering) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = ZenAmber,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = if (isThisPlaying) ZenStoneDark else ZenAmber,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
