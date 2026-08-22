package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SyncStatus
import com.example.ui.util.FlowHapticManager
import com.example.viewmodel.FlowViewModel

@Composable
fun FirestoreSyncCard(
    viewModel: FlowViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val syncState by viewModel.syncState.collectAsState()
    val hapticEnabled by viewModel.hapticFeedbackEnabled.collectAsState()
    val context = LocalContext.current

    val ZenAmber = MaterialTheme.colorScheme.primary
    val ZenStoneDark = MaterialTheme.colorScheme.surface
    val ZenStoneGrey = MaterialTheme.colorScheme.surfaceVariant
    val ZenTextPrimary = MaterialTheme.colorScheme.onSurface
    val ZenTextSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val ZenMossGreen = MaterialTheme.colorScheme.secondary

    var showQuickSignIn by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("firestore_sync_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ZenStoneDark),
        border = BorderStroke(1.dp, ZenStoneGrey)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Title & Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ZenAmber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (syncState.status) {
                                SyncStatus.SYNCING -> Icons.Default.CloudSync
                                SyncStatus.SUCCESS -> Icons.Default.CloudDone
                                SyncStatus.ERROR -> Icons.Default.ErrorOutline
                                SyncStatus.OFFLINE -> Icons.Default.CloudOff
                                else -> Icons.Default.Cloud
                            },
                            contentDescription = "Cloud Sync",
                            tint = when (syncState.status) {
                                SyncStatus.SUCCESS -> ZenMossGreen
                                SyncStatus.ERROR -> MaterialTheme.colorScheme.error
                                else -> ZenAmber
                            },
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "FIREBASE FIRESTORE SYNC",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                color = ZenAmber
                            )
                        )
                        Text(
                            text = "Cross-Device Room DB Sync",
                            style = MaterialTheme.typography.bodySmall.copy(color = ZenTextSecondary)
                        )
                    }
                }

                // Sync Status Chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when (syncState.status) {
                        SyncStatus.SYNCING -> ZenAmber.copy(alpha = 0.2f)
                        SyncStatus.SUCCESS -> ZenMossGreen.copy(alpha = 0.2f)
                        SyncStatus.ERROR -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        else -> ZenStoneGrey
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (syncState.status == SyncStatus.SYNCING) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = ZenAmber,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = when (syncState.status) {
                                SyncStatus.SYNCING -> "Syncing..."
                                SyncStatus.SUCCESS -> "Synced"
                                SyncStatus.ERROR -> "Notice"
                                SyncStatus.OFFLINE -> "Offline"
                                SyncStatus.IDLE -> if (currentUser != null) "Ready" else "Disconnected"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = when (syncState.status) {
                                    SyncStatus.SUCCESS -> ZenMossGreen
                                    SyncStatus.ERROR -> MaterialTheme.colorScheme.error
                                    else -> ZenTextPrimary
                                }
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // User Info or Connect Banner
            if (currentUser != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, ZenStoneGrey),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ZenAmber.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = ZenAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = currentUser?.displayName ?: "Mindful User",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = ZenTextPrimary
                                    )
                                )
                                Text(
                                    text = currentUser?.email ?: "Synced via Firestore",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = ZenTextSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                FlowHapticManager.performClickHaptic(context, hapticEnabled)
                                viewModel.signOutFromCloud()
                            },
                            modifier = Modifier.size(32.dp).testTag("firestore_signout_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Sign Out",
                                tint = ZenTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, ZenStoneGrey),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = ZenAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "End-to-End Secure Cloud Backup",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = ZenTextPrimary
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Sign in to securely sync your Room reflections, daily focus history, and insights across all your phones, tablets, and devices.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = ZenTextSecondary,
                                lineHeight = 16.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    FlowHapticManager.performClickHaptic(context, hapticEnabled)
                                    viewModel.signInWithGoogle(context)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ZenAmber,
                                    contentColor = MaterialTheme.colorScheme.background
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).testTag("google_sign_in_button")
                            ) {
                                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign in with Google", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }

                            OutlinedButton(
                                onClick = {
                                    FlowHapticManager.performClickHaptic(context, hapticEnabled)
                                    viewModel.signInQuick("Zen Flow User", "user@mindfulflow.app")
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ZenStoneGrey),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ZenTextSecondary),
                                modifier = Modifier.testTag("quick_connect_sync_button")
                            ) {
                                Text("Quick Connect")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sync Metrics & Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Cloud Sessions
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, ZenStoneGrey),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Cloud Sessions", style = MaterialTheme.typography.labelSmall, color = ZenTextSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${syncState.cloudSessionsCount}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ZenTextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }

                // Cloud Reflections
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, ZenStoneGrey),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Cloud Reflections", style = MaterialTheme.typography.labelSmall, color = ZenTextSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${syncState.cloudReflectionsCount}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = ZenTextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }

                // Last Sync Timestamp
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, ZenStoneGrey),
                    modifier = Modifier.weight(1.2f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Last Synced", style = MaterialTheme.typography.labelSmall, color = ZenTextSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = syncState.lastSyncTime,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = ZenAmber,
                                fontSize = 12.sp
                            ),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sync Status Message & Manual Trigger Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = syncState.message,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ZenTextSecondary,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    maxLines = 2
                )

                OutlinedButton(
                    onClick = {
                        FlowHapticManager.performClickHaptic(context, hapticEnabled)
                        if (currentUser == null) {
                            viewModel.signInQuick("Mindful User", "user@mindfulflow.app")
                        } else {
                            viewModel.syncWithFirestore()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ZenAmber.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ZenAmber),
                    enabled = syncState.status != SyncStatus.SYNCING,
                    modifier = Modifier.testTag("sync_now_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sync Now", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}
