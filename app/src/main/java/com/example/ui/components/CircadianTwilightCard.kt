package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.solarcircadian.CircadianPhase
import com.example.ui.util.FlowHapticManager
import com.example.viewmodel.FlowViewModel

@Composable
fun CircadianTwilightCard(
    viewModel: FlowViewModel,
    modifier: Modifier = Modifier
) {
    val circadianInfo by viewModel.circadianSolarInfo.collectAsState()
    val autoSyncEnabled by viewModel.circadianAutoSyncEnabled.collectAsState()
    val hapticEnabled by viewModel.hapticFeedbackEnabled.collectAsState()
    val context = LocalContext.current

    var showLocationDialog by remember { mutableStateOf(false) }

    val ZenAmber = MaterialTheme.colorScheme.primary
    val ZenStoneDark = MaterialTheme.colorScheme.surface
    val ZenStoneGrey = MaterialTheme.colorScheme.surfaceVariant
    val ZenTextPrimary = MaterialTheme.colorScheme.onSurface
    val ZenTextSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    if (showLocationDialog) {
        LocationSelectionDialog(
            viewModel = viewModel,
            currentCity = circadianInfo.locationName,
            onDismiss = { showLocationDialog = false },
            onSelectLocation = { lat, lng, name ->
                viewModel.updateLocationAndSyncSolarInfo(lat, lng, name)
                showLocationDialog = false
            }
        )
    }

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
            // Header: Title, Solar Phase Icon, Location button
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
                        Text(circadianInfo.phase.iconEmoji, fontSize = 16.sp)
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.circadian_twilight_sync),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ZenAmber,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        Text(
                            text = stringResource(circadianInfo.phase.displayNameResId),
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ZenTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Location selection
                    TextButton(
                        onClick = {
                            FlowHapticManager.performClickHaptic(context, hapticEnabled)
                            showLocationDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("circadian_location_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = ZenAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = circadianInfo.locationName,
                            style = MaterialTheme.typography.labelSmall.copy(color = ZenAmber)
                        )
                    }

                    // Refresh API button
                    IconButton(
                        onClick = {
                            FlowHapticManager.performClickHaptic(context, hapticEnabled)
                            viewModel.refreshSolarCircadianInfo()
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .background(ZenStoneGrey, CircleShape)
                            .testTag("refresh_solar_info")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh_solar_data),
                            tint = ZenTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Twilight & Solar Times Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ZenStoneGrey.copy(alpha = 0.5f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.sunrise_label),
                        style = MaterialTheme.typography.labelSmall.copy(color = ZenTextSecondary)
                    )
                    Text(
                        text = circadianInfo.formattedSunrise,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ZenTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.sunset_label),
                        style = MaterialTheme.typography.labelSmall.copy(color = ZenTextSecondary)
                    )
                    Text(
                        text = circadianInfo.formattedSunset,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ZenAmber,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.twilight_end_label),
                        style = MaterialTheme.typography.labelSmall.copy(color = ZenTextSecondary)
                    )
                    Text(
                        text = circadianInfo.formattedCivilTwilightEnd,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ZenTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Phase Description
            Text(
                text = stringResource(circadianInfo.phase.descriptionResId),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = ZenTextSecondary,
                    fontSize = 11.5.sp
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Auto Sync & Dimming Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Brightness4,
                        contentDescription = null,
                        tint = if (autoSyncEnabled) ZenAmber else ZenTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.circadian_auto_sync_title),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = ZenTextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = if (autoSyncEnabled)
                                stringResource(R.string.circadian_auto_sync_active, (circadianInfo.dimRatio * 100).toInt())
                            else
                                stringResource(R.string.circadian_auto_sync_off),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ZenTextSecondary,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                Switch(
                    checked = autoSyncEnabled,
                    onCheckedChange = { enabled ->
                        FlowHapticManager.performClickHaptic(context, hapticEnabled)
                        viewModel.setCircadianAutoSync(enabled)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ZenAmber,
                        checkedTrackColor = ZenAmber.copy(alpha = 0.3f),
                        uncheckedThumbColor = ZenTextSecondary,
                        uncheckedTrackColor = ZenStoneGrey
                    ),
                    modifier = Modifier.testTag("circadian_sync_switch")
                )
            }
        }
    }
}

@Composable
private fun LocationSelectionDialog(
    viewModel: FlowViewModel,
    currentCity: String,
    onDismiss: () -> Unit,
    onSelectLocation: (Double, Double, String) -> Unit
) {
    val isGeocodingLoading by viewModel.isGeocodingLoading.collectAsState()
    var customSearchInput by remember { mutableStateOf("") }

    val presetCities = listOf(
        Triple(10.7769, 106.7009, "Ho Chi Minh City"),
        Triple(21.0285, 105.8542, "Hanoi"),
        Triple(35.6762, 139.6503, "Tokyo"),
        Triple(51.5074, -0.1278, "London"),
        Triple(40.7128, -74.0060, "New York"),
        Triple(37.7749, -122.4194, "San Francisco"),
        Triple(-33.8688, 151.2093, "Sydney")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.select_location_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.select_location_subtitle),
                    style = MaterialTheme.typography.bodySmall
                )

                // OpenStreetMap Custom City Search
                OutlinedTextField(
                    value = customSearchInput,
                    onValueChange = { customSearchInput = it },
                    placeholder = { Text("Search any city in the world…", fontSize = 12.sp) },
                    trailingIcon = {
                        if (isGeocodingLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(
                                onClick = {
                                    if (customSearchInput.isNotBlank()) {
                                        viewModel.searchAndSetCityLocation(customSearchInput)
                                        onDismiss()
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Search, contentDescription = "Search City", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                presetCities.forEach { (lat, lng, name) ->
                    val isSelected = currentCity.equals(name, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectLocation(lat, lng, name) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close_button))
            }
        }
    )
}
