package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FlowSession
import com.example.ui.theme.*
import com.example.viewmodel.FlowViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MirrorScreen(
    viewModel: FlowViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.allSessions.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }

    val totalTimeMin = remember(sessions) {
        sessions.sumOf { it.durationSeconds } / 60
    }

    val avgFlowScore = remember(sessions) {
        if (sessions.isEmpty()) 0f else sessions.map { it.flowScore }.average().toFloat()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Title & Export Actions
        item {
            val context = androidx.compose.ui.platform.LocalContext.current
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "THE MIRROR",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Serif,
                                letterSpacing = 4.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Quantified Awareness & Historical Focus",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.exportSessionsShareIntent(context, "CSV") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("export_csv_button")
                        ) {
                            Text("📥 CSV", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.exportSessionsShareIntent(context, "JSON") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("export_json_button")
                        ) {
                            Text("📥 JSON", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Theme Selector
        item {
            val currentThemeMode by viewModel.themeMode.collectAsState()
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "FLOW AMBIENT MOODS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Align your physical environment with the digital canvas to prevent eye strain and cultivate presence.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        lineHeight = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(18.dp))
                    
                    // Options row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf(
                            Triple(com.example.viewmodel.FlowThemeMode.CIRCADIAN, "Circadian", "Auto-shift"),
                            Triple(com.example.viewmodel.FlowThemeMode.DAWN, "Dawn", "Warm light"),
                            Triple(com.example.viewmodel.FlowThemeMode.DUSK, "Dusk", "Slate gray"),
                            Triple(com.example.viewmodel.FlowThemeMode.MIDNIGHT, "Midnight", "Amoled dark")
                        )
                        
                        modes.forEach { (mode, title, desc) ->
                            val isSelected = currentThemeMode == mode
                            val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            val cardBg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent
                            
                            Box(
                                modifier = Modifier
                                    .widthIn(min = 85.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBg)
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = borderColor,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { viewModel.setThemeMode(mode) }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val emoji = when (mode) {
                                        com.example.viewmodel.FlowThemeMode.CIRCADIAN -> "🌿"
                                        com.example.viewmodel.FlowThemeMode.DAWN -> "🌅"
                                        com.example.viewmodel.FlowThemeMode.DUSK -> "🌇"
                                        com.example.viewmodel.FlowThemeMode.MIDNIGHT -> "🌌"
                                    }
                                    Text(text = emoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Firebase Cloud Hub Card
        item {
            FirebaseCloudHubCard(viewModel = viewModel)
        }

        // Stats Summary Cards - Refactored to prioritize single high-impact 'Total Time in Flow' metric
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("total_time_in_flow_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = "Total Time in Flow",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "TOTAL TIME IN FLOW",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (totalTimeMin >= 60) "${totalTimeMin / 60}h ${totalTimeMin % 60}m" else "${totalTimeMin}m",
                        style = MaterialTheme.typography.displayMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Light,
                            fontFamily = FontFamily.Serif
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Accumulated stillness in action over ${sessions.size} sessions",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Daily Focus Trend Canvas Chart
        item {
            DailyFocusTrendChart(sessions = sessions)
        }

        // The Custom Flow Compass Scatter Map (Mind state matrix)
        item {
            val axisColor = MaterialTheme.colorScheme.outline
            val gridColor = MaterialTheme.colorScheme.surfaceVariant
            val primaryAccent = MaterialTheme.colorScheme.primary
            val secondaryAccent = MaterialTheme.colorScheme.secondary

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Mind State Mapping (Challenge vs Skill)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Serif
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Dots represent logged sessions. Ideally clusters fall in the diagonal Flow channel.",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 5x5 Scatter Map Grid
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Draw axes
                            drawLine(axisColor, Offset(0f, h), Offset(w, h), strokeWidth = 2f)
                            drawLine(axisColor, Offset(0f, 0f), Offset(0f, h), strokeWidth = 2f)

                            // Draw grid lines and labels (5 spaces)
                            val stepX = w / 5f
                            val stepY = h / 5f

                            for (i in 1..4) {
                                val xPos = i * stepX
                                val yPos = i * stepY
                                // Vertical Grid Line
                                drawLine(gridColor.copy(alpha = 0.5f), Offset(xPos, 0f), Offset(xPos, h), strokeWidth = 1f)
                                // Horizontal Grid Line
                                drawLine(gridColor.copy(alpha = 0.5f), Offset(0f, yPos), Offset(w, yPos), strokeWidth = 1f)
                            }

                            // Group sessions by (skill, challenge) to draw weighted nodes
                            val grouped = sessions.groupBy { it.skillLevel to it.challengeLevel }

                            grouped.forEach { (coords, list) ->
                                val (s, c) = coords
                                // Convert 1..5 values to grid coordinates (with 1 in bottom-left and 5 in top-right)
                                val normX = ((s - 1) / 4f) * w
                                val normY = h - (((c - 1) / 4f) * h)

                                // Prevent boundary clip
                                val drawX = normX.coerceIn(20f, w - 20f)
                                val drawY = normY.coerceIn(20f, h - 20f)

                                // Node weight size
                                val nodeWeight = list.size
                                val radius = (12f + nodeWeight * 5f).coerceAtMost(35f)

                                // Draw circular aura glow
                                drawCircle(
                                    color = if (s == c) secondaryAccent.copy(alpha = 0.4f) else primaryAccent.copy(alpha = 0.4f),
                                    radius = radius + 8f,
                                    center = Offset(drawX, drawY)
                                )

                                // Draw solid node center
                                drawCircle(
                                    color = if (s == c) secondaryAccent else primaryAccent,
                                    radius = radius,
                                    center = Offset(drawX, drawY)
                                )
                            }
                        }

                        // Grid Labels overlay
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text("Skill Lvl 1", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, modifier = Modifier.padding(bottom = 2.dp))
                            Text("Flow Channel (Diagonal) ➔", color = MaterialTheme.colorScheme.secondary, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 2.dp))
                            Text("Skill Lvl 5", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, modifier = Modifier.padding(bottom = 2.dp))
                        }
                        
                        Box(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            Text("Anxiety\n(Challenge Lvl 5)", color = MaterialTheme.colorScheme.primary, fontSize = 8.sp)
                        }

                        Box(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Text("Boredom\n(Skill Lvl 5)", color = MaterialTheme.colorScheme.primary, fontSize = 8.sp, textAlign = TextAlign.End, modifier = Modifier.padding(bottom = 14.dp))
                        }
                    }
                }
            }
        }

        // Sessions Log Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "FOCUS CHRONICLES",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                if (sessions.isNotEmpty()) {
                    Text(
                        text = "Clear All",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .clickable { showClearConfirm = true }
                            .padding(4.dp)
                    )
                }
            }
        }

        // Sessions List empty state
        if (sessions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.History,
                            contentDescription = "No sessions yet",
                            tint = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No focus chronicles recorded yet.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Complete your first entry ritual to see mirrors of your focus.",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(sessions, key = { it.id }) { session ->
                SessionItemRow(session = session, onDelete = { viewModel.deleteSessionItem(session) })
            }
        }
    }

    // Clear History Dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Erase Focus Chronicles", color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Serif) },
            text = { Text("Are you absolutely sure you want to clear your local history? This cannot be undone.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearConfirm = false
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Keep", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }
}

@Composable
fun SessionItemRow(
    session: FlowSession,
    onDelete: () -> Unit
) {
    val dateString = remember(session.timestamp) {
        val formatter = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault())
        formatter.format(Date(session.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val displayedIntent = if (session.intent.isBlank() || session.intent == "Center" || session.intent == "Vô tâm / Center") {
                        stringResource(R.string.intent_center)
                    } else {
                        session.intent
                    }
                    Text(
                        text = displayedIntent,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                // Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("delete_session_${session.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete Session",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Focus time tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${session.durationSeconds / 60}m ${session.durationSeconds % 60}s",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Metacognitive Flow score
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Flow Index: ",
                        style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Text(
                        text = "${session.flowScore}/5",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            if (session.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = session.notes,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontFamily = FontFamily.Serif
                    )
                )
            }
        }
    }
}

private data class DailyTrendData(
    val dayLabel: String,
    val dateKey: String,
    val focusMinutes: Int,
    val isToday: Boolean
)

@Composable
fun DailyFocusTrendChart(
    sessions: List<FlowSession>,
    modifier: Modifier = Modifier
) {
    val trendData = remember(sessions) {
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val days = mutableListOf<DailyTrendData>()
        val todayKey = keyFormat.format(Calendar.getInstance().time)

        val sessionsByDay = sessions.groupBy {
            keyFormat.format(Date(it.timestamp))
        }

        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val key = keyFormat.format(cal.time)
            val dayLabel = dayFormat.format(cal.time)
            val daySessions = sessionsByDay[key] ?: emptyList()
            val totalMinutes = daySessions.sumOf { it.durationSeconds } / 60

            days.add(
                DailyTrendData(
                    dayLabel = dayLabel,
                    dateKey = key,
                    focusMinutes = totalMinutes,
                    isToday = (key == todayKey)
                )
            )
        }
        days
    }

    val maxMinutes = remember(trendData) {
        (trendData.maxOfOrNull { it.focusMinutes } ?: 60).coerceAtLeast(30)
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryDim = MaterialTheme.colorScheme.tertiary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DAILY FOCUS TRENDS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = primaryColor,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "7-Day Focus Velocity from Room DB",
                        style = MaterialTheme.typography.bodySmall.copy(color = onSurfaceVariant)
                    )
                }

                val weeklyTotal = remember(trendData) { trendData.sumOf { it.focusMinutes } }
                Text(
                    text = "${weeklyTotal / 60}h ${weeklyTotal % 60}m this week",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = primaryColor,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Canvas Bar Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .testTag("daily_focus_trend_canvas_chart")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val bottomPadding = 32.dp.toPx()
                    val chartHeight = h - bottomPadding

                    // Reference grid lines
                    val gridLevels = listOf(0.25f, 0.5f, 0.75f, 1.0f)
                    gridLevels.forEach { level ->
                        val y = chartHeight * (1f - level)
                        drawLine(
                            color = surfaceVariant.copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }

                    val barCount = trendData.size
                    val spacing = w / barCount
                    val barWidth = (spacing * 0.45f).coerceAtMost(36.dp.toPx())

                    trendData.forEachIndexed { index, data ->
                        val xCenter = (index * spacing) + (spacing / 2f)
                        val fraction = (data.focusMinutes.toFloat() / maxMinutes.toFloat()).coerceAtMost(1f)
                        val barHeight = (chartHeight * fraction).coerceAtLeast(8.dp.toPx())
                        val topY = chartHeight - barHeight

                        // Bar background track
                        drawRoundRect(
                            color = surfaceVariant.copy(alpha = 0.3f),
                            topLeft = Offset(xCenter - (barWidth / 2f), 0f),
                            size = androidx.compose.ui.geometry.Size(barWidth, chartHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
                        )

                        // Active progress bar with gradient
                        if (data.focusMinutes > 0) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = if (data.isToday) listOf(primaryColor, primaryDim)
                                             else listOf(primaryDim, primaryColor.copy(alpha = 0.6f))
                                ),
                                topLeft = Offset(xCenter - (barWidth / 2f), topY),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
                            )
                        }
                    }
                }

                // X-Axis Day Labels and minute badges overlay
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.Bottom
                ) {
                    trendData.forEach { data ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (data.focusMinutes > 0) {
                                Text(
                                    text = "${data.focusMinutes}m",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (data.isToday) primaryColor else onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = "-",
                                    fontSize = 9.sp,
                                    color = surfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = data.dayLabel,
                                fontSize = 10.sp,
                                fontWeight = if (data.isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (data.isToday) primaryColor else onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FirebaseCloudHubCard(viewModel: FlowViewModel) {
    val communityStats by viewModel.communityStats.collectAsState()
    val userAuth by viewModel.userAuth.collectAsState()
    val remoteConfig by viewModel.remoteConfig.collectAsState()
    val isAppCheckVerified by viewModel.isAppCheckVerified.collectAsState()
    var showEmailDialog by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🔥 FIREBASE CLOUD HUB",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Spark Plan Free Services (Auth, Firestore, Remote Config, AI, App Check)",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isAppCheckVerified) "🛡️ Verified" else "🛡️ Unverified",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Firebase Auth Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Authentication: ${userAuth.displayName}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (userAuth.isAnonymous) "Mode: Anonymous Guest (${userAuth.uid})" else "Email: ${userAuth.email}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (userAuth.isAnonymous) {
                            OutlinedButton(
                                onClick = { showEmailDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("link_email_button")
                            ) {
                                Text("Link Email", fontSize = 10.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.signOut() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("sign_out_button")
                            ) {
                                Text("Sign Out", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Firestore Realtime Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Cloud Focus Min", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${communityStats.totalCloudFocusMinutes}m",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Global Practitioners", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${communityStats.activeMindfulUsers}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Sessions Today", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${communityStats.globalSessionsToday}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Remote Config Banner & Refresh
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚡ Remote Config Daily Motto",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        )
                        TextButton(
                            onClick = { viewModel.fetchRemoteConfig() },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier
                                .height(24.dp)
                                .testTag("refresh_remote_config_button")
                        ) {
                            Text("Refresh", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"${remoteConfig.dailyMotto}\"",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Smart Engagement & Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.triggerSharePrompt() },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("hub_share_button"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("🔗 Share App", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = { viewModel.triggerRatePrompt() },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("hub_rate_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("⭐ Rate 5 Stars", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }

    if (showEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = { Text("Sign In with Email", fontFamily = FontFamily.Serif) },
            text = {
                Column {
                    Text("Enter email to sync your focus sessions to Firebase Cloud Firestore:", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email address") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_auth_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (emailInput.isNotBlank()) {
                            viewModel.signInWithEmail(emailInput.trim())
                            showEmailDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_email_auth_button")
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmailDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
