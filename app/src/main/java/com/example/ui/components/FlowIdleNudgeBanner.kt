package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.util.FlowHapticManager
import com.example.viewmodel.FlowViewModel

/**
 * Gentle Visual Cue displayed when the user has been idle for 10+ minutes without starting a flow session.
 * Features a soft breathing halo and non-intrusive action buttons to transition into focus or breathing.
 */
@Composable
fun FlowIdleNudgeBanner(
    viewModel: FlowViewModel,
    modifier: Modifier = Modifier
) {
    val isIdleNudgeActive by viewModel.isIdleNudgeActive.collectAsState()
    val idleSeconds by viewModel.idleSeconds.collectAsState()
    val hapticEnabled by viewModel.hapticFeedbackEnabled.collectAsState()
    val context = LocalContext.current

    val ZenAmber = MaterialTheme.colorScheme.primary
    val ZenStoneDark = MaterialTheme.colorScheme.surface
    val ZenStoneGrey = MaterialTheme.colorScheme.surfaceVariant
    val ZenTextPrimary = MaterialTheme.colorScheme.onSurface
    val ZenTextSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    // Breathing pulse animation for the visual cue halo
    val infiniteTransition = rememberInfiniteTransition(label = "IdleNudgePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HaloAlpha"
    )

    AnimatedVisibility(
        visible = isIdleNudgeActive,
        enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn(tween(600)),
        exit = slideOutVertically(targetOffsetY = { -it / 2 }) + fadeOut(tween(400)),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("idle_state_nudge_banner"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = ZenStoneDark),
            border = BorderStroke(1.5.dp, ZenAmber.copy(alpha = haloAlpha))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                ZenAmber.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(18.dp)
            ) {
                // Header: Presence Badge & Dismiss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Pulsing Zen Lotus Core
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(ZenAmber.copy(alpha = 0.2f))
                                .border(1.dp, ZenAmber.copy(alpha = haloAlpha), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Spa,
                                contentDescription = null,
                                tint = ZenAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "MINDFUL PRESENCE DETECTED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.4.sp,
                                    color = ZenAmber
                                )
                            )
                            val idleMinutes = (idleSeconds / 60).coerceAtLeast(10)
                            Text(
                                text = "$idleMinutes minutes of stillness observed",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = ZenTextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            FlowHapticManager.performClickHaptic(context, hapticEnabled)
                            viewModel.dismissIdleNudge()
                        },
                        modifier = Modifier.size(32.dp).testTag("dismiss_idle_nudge_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = ZenTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "You’ve maintained stillness in the app. Channel this settled awareness into your next deep focus session or brief grounding ritual.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ZenTextPrimary,
                        lineHeight = 18.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            FlowHapticManager.performSessionStartHaptic(context, hapticEnabled)
                            viewModel.startFlowFromIdleNudge()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ZenAmber,
                            contentColor = MaterialTheme.colorScheme.background
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("idle_nudge_start_flow_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Begin Focus",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            FlowHapticManager.performClickHaptic(context, hapticEnabled)
                            viewModel.dismissIdleNudge()
                            viewModel.proceedToRitual()
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ZenStoneGrey),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ZenTextPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("idle_nudge_breath_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Air,
                            contentDescription = null,
                            tint = ZenAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("3 Breaths")
                    }
                }
            }
        }
    }
}
