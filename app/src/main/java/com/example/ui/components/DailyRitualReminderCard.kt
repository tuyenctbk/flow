package com.example.ui.components

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.service.DailyReminderScheduler
import com.example.viewmodel.FlowViewModel
import java.util.Locale

@Composable
fun DailyRitualReminderCard(
    viewModel: FlowViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val isFocusEnabled by viewModel.isFocusReminderEnabled.collectAsState()
    val focusHour by viewModel.focusReminderHour.collectAsState()
    val focusMinute by viewModel.focusReminderMinute.collectAsState()

    val isReflectionEnabled by viewModel.isReflectionReminderEnabled.collectAsState()
    val reflectionHour by viewModel.reflectionReminderHour.collectAsState()
    val reflectionMinute by viewModel.reflectionReminderMinute.collectAsState()

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    val textVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val secondaryColor = MaterialTheme.colorScheme.secondary

    fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour >= 12) "PM" else "AM"
        val h = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.getDefault(), "%02d:%02d %s", h, minute, amPm)
    }

    fun showTimePicker(initialHour: Int, initialMinute: Int, onTimeSelected: (Int, Int) -> Unit) {
        val dialog = TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                onTimeSelected(hourOfDay, minute)
                Toast.makeText(
                    context,
                    "Reminder set for ${formatTime(hourOfDay, minute)}",
                    Toast.LENGTH_SHORT
                ).show()
            },
            initialHour,
            initialMinute,
            false // 12-hour format
        )
        dialog.show()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_ritual_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = BorderStroke(1.dp, surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(primaryColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = "Rituals",
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Daily Flow Consistency",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            ),
                            color = textColor
                        )
                        Text(
                            text = "Mindful prompts to anchor daily practice",
                            style = MaterialTheme.typography.bodySmall,
                            color = textVariant
                        )
                    }
                }

                // Active badge
                val anyEnabled = isFocusEnabled || isReflectionEnabled
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (anyEnabled) secondaryColor.copy(alpha = 0.2f)
                            else surfaceVariant
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (anyEnabled) "Active" else "Off",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (anyEnabled) secondaryColor else textVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. FOCUS SESSION REMINDER ROW
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(1.dp, surfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
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
                            imageVector = Icons.Outlined.Spa,
                            contentDescription = "Focus",
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Flow Session Ritual",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = textColor
                            )
                            Text(
                                text = "Prompt to enter focused flow",
                                style = MaterialTheme.typography.labelSmall,
                                color = textVariant
                            )
                        }
                    }

                    Switch(
                        checked = isFocusEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateFocusReminder(enabled, focusHour, focusMinute)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = primaryColor,
                            checkedTrackColor = primaryColor.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("focus_reminder_toggle")
                    )
                }

                AnimatedVisibility(visible = isFocusEnabled) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Scheduled Time:",
                                style = MaterialTheme.typography.bodySmall,
                                color = textVariant
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = primaryColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .clickable {
                                        showTimePicker(focusHour, focusMinute) { h, m ->
                                            viewModel.updateFocusReminder(true, h, m)
                                        }
                                    }
                                    .testTag("focus_time_picker_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = "Clock",
                                        tint = primaryColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = formatTime(focusHour, focusMinute),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = primaryColor
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "08:30 AM" to Pair(8, 30),
                                "09:00 AM" to Pair(9, 0),
                                "01:30 PM" to Pair(13, 30),
                                "04:00 PM" to Pair(16, 0)
                            ).forEach { (label, time) ->
                                val isSelected = focusHour == time.first && focusMinute == time.second
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateFocusReminder(true, time.first, time.second)
                                        Toast.makeText(context, "Set to $label", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) primaryColor.copy(alpha = 0.15f) else Color.Transparent
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) primaryColor else surfaceVariant
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = label.split(" ")[0],
                                        fontSize = 11.sp,
                                        color = if (isSelected) primaryColor else textVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. EVENING REFLECTION REMINDER ROW
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(1.dp, surfaceVariant.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
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
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = "Reflection",
                            tint = secondaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Evening Reflection & Journal",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = textColor
                            )
                            Text(
                                text = "Daily prompt to capture insights",
                                style = MaterialTheme.typography.labelSmall,
                                color = textVariant
                            )
                        }
                    }

                    Switch(
                        checked = isReflectionEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateReflectionReminder(enabled, reflectionHour, reflectionMinute)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = secondaryColor,
                            checkedTrackColor = secondaryColor.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("reflection_reminder_toggle")
                    )
                }

                AnimatedVisibility(visible = isReflectionEnabled) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Scheduled Time:",
                                style = MaterialTheme.typography.bodySmall,
                                color = textVariant
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = secondaryColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, secondaryColor.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .clickable {
                                        showTimePicker(reflectionHour, reflectionMinute) { h, m ->
                                            viewModel.updateReflectionReminder(true, h, m)
                                        }
                                    }
                                    .testTag("reflection_time_picker_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = "Clock",
                                        tint = secondaryColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = formatTime(reflectionHour, reflectionMinute),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = secondaryColor
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Reflection Quick Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "07:00 PM" to Pair(19, 0),
                                "08:30 PM" to Pair(20, 30),
                                "09:30 PM" to Pair(21, 30),
                                "10:00 PM" to Pair(22, 0)
                            ).forEach { (label, time) ->
                                val isSelected = reflectionHour == time.first && reflectionMinute == time.second
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateReflectionReminder(true, time.first, time.second)
                                        Toast.makeText(context, "Set to $label", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) secondaryColor.copy(alpha = 0.15f) else Color.Transparent
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) secondaryColor else surfaceVariant
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = label.split(" ")[0],
                                        fontSize = 11.sp,
                                        color = if (isSelected) secondaryColor else textVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. TEST NOTIFICATION TRIGGER ACTION BUTTONS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.triggerTestNotification(DailyReminderScheduler.REMINDER_TYPE_FOCUS)
                        Toast.makeText(context, "⚡ Sent test focus notification!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("test_focus_reminder_button"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Test Focus",
                        modifier = Modifier.size(14.dp),
                        tint = primaryColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Focus Alert", fontSize = 11.sp, color = textColor)
                }

                OutlinedButton(
                    onClick = {
                        viewModel.triggerTestNotification(DailyReminderScheduler.REMINDER_TYPE_REFLECTION)
                        Toast.makeText(context, "🪞 Sent test reflection notification!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("test_reflection_reminder_button"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EditNote,
                        contentDescription = "Test Reflection",
                        modifier = Modifier.size(14.dp),
                        tint = secondaryColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Test Journal Alert", fontSize = 11.sp, color = textColor)
                }
            }
        }
    }
}
