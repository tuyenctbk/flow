package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.UserReflection
import com.example.viewmodel.FlowViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class MirrorTab {
    CHRONICLES,
    JOURNAL
}

@Composable
fun MirrorScreen(
    viewModel: FlowViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.allSessions.collectAsState()
    val reflections by viewModel.filteredReflections.collectAsState()
    val allReflectionsList by viewModel.allReflections.collectAsState()
    val reflectionQuery by viewModel.reflectionQuery.collectAsState()

    var selectedTab by remember { mutableStateOf(MirrorTab.CHRONICLES) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showNewReflectionDialog by remember { mutableStateOf(false) }

    val totalTimeMin = remember(sessions) {
        sessions.sumOf { it.durationSeconds } / 60
    }

    val avgFlowScore = remember(sessions) {
        if (sessions.isEmpty()) 0f else sessions.map { it.flowScore }.average().toFloat()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (selectedTab == MirrorTab.JOURNAL) {
                FloatingActionButton(
                    onClick = { showNewReflectionDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_reflection_fab")
                ) {
                    Icon(
                        imageVector = Icons.Filled.EditNote,
                        contentDescription = "New Reflection"
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header with Title & Export Actions
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
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Quantified Awareness & Metacognitive Logs",
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

            // Top Segmented Switcher (Chronicles vs Journal)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Chronicles Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selectedTab == MirrorTab.CHRONICLES) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { selectedTab = MirrorTab.CHRONICLES }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Timer,
                                    contentDescription = null,
                                    tint = if (selectedTab == MirrorTab.CHRONICLES) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Focus Chronicles (${sessions.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (selectedTab == MirrorTab.CHRONICLES) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        // Journal Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selectedTab == MirrorTab.JOURNAL) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { selectedTab = MirrorTab.JOURNAL }
                                .padding(vertical = 10.dp)
                                .testTag("mirror_journal_tab"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SelfImprovement,
                                    contentDescription = null,
                                    tint = if (selectedTab == MirrorTab.JOURNAL) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Reflections (${allReflectionsList.size})",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (selectedTab == MirrorTab.JOURNAL) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Tab Content
            if (selectedTab == MirrorTab.CHRONICLES) {
                // High-Impact Total Time Card
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
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = "Total Time in Flow",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "TOTAL TIME IN FLOW",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (totalTimeMin >= 60) "${totalTimeMin / 60}h ${totalTimeMin % 60}m" else "${totalTimeMin}m",
                                style = MaterialTheme.typography.displaySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Light,
                                    fontFamily = FontFamily.Serif
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Logged in Room DB across ${sessions.size} focused sessions",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // 7-Day Bar Chart
                item {
                    DailyFocusTrendChart(sessions = sessions)
                }

                // Mind State Scatter Map
                item {
                    MindStateScatterMap(sessions = sessions)
                }

                // Firebase Cloud Sync Hub
                item {
                    FirebaseCloudHubCard(viewModel = viewModel)
                }

                // Sessions Log Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LOCAL FOCUS SESSIONS",
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

                // Sessions List
                if (sessions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.History,
                                    contentDescription = "No sessions yet",
                                    tint = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No focus chronicles recorded yet.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Begin a focus ritual on the Flow tab to calibrate your mind.",
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
            } else {
                // JOURNAL TAB
                // Search Bar & Filter
                item {
                    OutlinedTextField(
                        value = reflectionQuery,
                        onValueChange = { viewModel.setReflectionQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reflection_search_input"),
                        placeholder = { Text("Search reflections by keyword, friction, tags...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (reflectionQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.setReflectionQuery("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                // Quick Reflection Actions Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Metacognitive Journal",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Record insights, frictions, and breakthroughs to cultivate self-regulation.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }

                            Button(
                                onClick = { showNewReflectionDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.testTag("compose_reflection_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Log Entry")
                            }
                        }
                    }
                }

                // Reflections List
                if (reflections.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.AutoStories,
                                    contentDescription = "No reflections",
                                    tint = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (reflectionQuery.isNotBlank()) "No reflections match \"$reflectionQuery\""
                                           else "No reflection journal entries yet.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap 'Log Entry' or complete a focus session to record deep reflections.",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(reflections, key = { it.id }) { reflection ->
                        ReflectionItemRow(
                            reflection = reflection,
                            onDelete = { viewModel.deleteReflection(reflection) }
                        )
                    }
                }
            }
        }
    }

    // New Reflection Dialog
    if (showNewReflectionDialog) {
        NewReflectionDialog(
            viewModel = viewModel,
            onDismiss = { showNewReflectionDialog = false }
        )
    }

    // Clear History Dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Erase Focus Chronicles", color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Serif) },
            text = { Text("Are you sure you want to clear your local session history from Room database? This cannot be undone.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearConfirm = false
                    }
                ) {
                    Text("Clear All", color = MaterialTheme.colorScheme.primary)
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
fun NewReflectionDialog(
    viewModel: FlowViewModel,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var moodRating by remember { mutableStateOf(4) }
    var frictionNotes by remember { mutableStateOf("") }
    var breakthroughNotes by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("Focus, Mindfulness") }
    var aiInsight by remember { mutableStateOf("") }
    var isGeneratingAi by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.SelfImprovement, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = "New Metacognitive Log",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Mood Selector (1 to 5)
                Column {
                    Text(
                        text = "AWARENESS & MIND STATE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val moods = listOf(
                            1 to "😫 Drained",
                            2 to "🌊 Restless",
                            3 to "🧘 Centered",
                            4 to "🎯 Focused",
                            5 to "✨ Flow"
                        )
                        moods.forEach { (rating, label) ->
                            val isSelected = moodRating == rating
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { moodRating = rating }
                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label.split(" ")[0],
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Core Theme") },
                    placeholder = { Text("e.g. Deep Coding Session, Morning Clarity") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Reflection Content
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Reflection & Observations") },
                    placeholder = { Text("What did you notice about your attention span and flow state?") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )

                // Friction & Breakthrough Accordion Fields
                OutlinedTextField(
                    value = frictionNotes,
                    onValueChange = { frictionNotes = it },
                    label = { Text("Friction / Internal Distractions") },
                    placeholder = { Text("e.g. Checking notifications, fatigue, task ambiguity") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = breakthroughNotes,
                    onValueChange = { breakthroughNotes = it },
                    label = { Text("Breakthroughs / What Worked") },
                    placeholder = { Text("e.g. Gamma binaural beats, clear sub-task breakdown") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // AI Insight Assistant Button
                if (aiInsight.isBlank()) {
                    OutlinedButton(
                        onClick = {
                            isGeneratingAi = true
                            coroutineScope.launch {
                                val generated = viewModel.generateAiJournalInsight(
                                    title = title,
                                    content = content,
                                    friction = frictionNotes,
                                    breakthrough = breakthroughNotes
                                )
                                aiInsight = generated
                                isGeneratingAi = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isGeneratingAi
                    ) {
                        if (isGeneratingAi) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gemini is analyzing reflection...")
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Generate AI Metacognitive Insight", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "AI Metacognitive Coach",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = aiInsight,
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.isNotBlank() || title.isNotBlank()) {
                        viewModel.createReflection(
                            title = title,
                            content = content,
                            moodRating = moodRating,
                            frictionNotes = frictionNotes,
                            breakthroughNotes = breakthroughNotes,
                            tags = tags,
                            aiInsight = aiInsight
                        )
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save to Room DB")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ReflectionItemRow(
    reflection: UserReflection,
    onDelete: () -> Unit
) {
    val dateString = remember(reflection.timestamp) {
        val formatter = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault())
        formatter.format(Date(reflection.timestamp))
    }

    val moodEmoji = when (reflection.moodRating) {
        1 -> "😫 Drained"
        2 -> "🌊 Restless"
        3 -> "🧘 Centered"
        4 -> "🎯 Focused"
        5 -> "✨ Flow"
        else -> "🧘 Mindful"
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
                    Text(
                        text = reflection.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                }

                // Mood Indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = moodEmoji,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete Reflection",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (reflection.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = reflection.content,
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
                )
            }

            // Friction & Breakthrough Badges
            if (reflection.frictionNotes.isNotBlank() || reflection.breakthroughNotes.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (reflection.frictionNotes.isNotBlank()) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("⚠️ Friction", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                Text(reflection.frictionNotes, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }

                    if (reflection.breakthroughNotes.isNotBlank()) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("💡 Breakthrough", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                Text(reflection.breakthroughNotes, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }

            // AI Insight Card if present
            if (reflection.aiInsight.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = reflection.aiInsight,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MindStateScatterMap(sessions: List<FlowSession>) {
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
                text = "Nodes represent logged sessions in Room DB. The diagonal path represents the optimal Flow channel.",
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 5x5 Scatter Map Grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Draw axes
                    drawLine(axisColor, Offset(0f, h), Offset(w, h), strokeWidth = 2f)
                    drawLine(axisColor, Offset(0f, 0f), Offset(0f, h), strokeWidth = 2f)

                    // Draw grid lines
                    val stepX = w / 5f
                    val stepY = h / 5f

                    for (i in 1..4) {
                        val xPos = i * stepX
                        val yPos = i * stepY
                        drawLine(gridColor.copy(alpha = 0.5f), Offset(xPos, 0f), Offset(xPos, h), strokeWidth = 1f)
                        drawLine(gridColor.copy(alpha = 0.5f), Offset(0f, yPos), Offset(w, yPos), strokeWidth = 1f)
                    }

                    // Group sessions by (skill, challenge)
                    val grouped = sessions.groupBy { it.skillLevel to it.challengeLevel }

                    grouped.forEach { (coords, list) ->
                        val (s, c) = coords
                        val normX = ((s - 1) / 4f) * w
                        val normY = h - (((c - 1) / 4f) * h)

                        val drawX = normX.coerceIn(20f, w - 20f)
                        val drawY = normY.coerceIn(20f, h - 20f)

                        val nodeWeight = list.size
                        val radius = (10f + nodeWeight * 4f).coerceAtMost(32f)

                        drawCircle(
                            color = if (s == c) secondaryAccent.copy(alpha = 0.35f) else primaryAccent.copy(alpha = 0.35f),
                            radius = radius + 6f,
                            center = Offset(drawX, drawY)
                        )

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
            }
        }
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
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${session.durationSeconds / 60}m ${session.durationSeconds % 60}s",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelMedium
                    )
                }

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
                Spacer(modifier = Modifier.height(10.dp))
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .testTag("daily_focus_trend_canvas_chart")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val bottomPadding = 30.dp.toPx()
                    val chartHeight = h - bottomPadding

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
                        val barHeight = (chartHeight * fraction).coerceAtLeast(6.dp.toPx())
                        val topY = chartHeight - barHeight

                        drawRoundRect(
                            color = surfaceVariant.copy(alpha = 0.3f),
                            topLeft = Offset(xCenter - (barWidth / 2f), 0f),
                            size = androidx.compose.ui.geometry.Size(barWidth, chartHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
                        )

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

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 2.dp),
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
    val isAppCheckVerified by viewModel.isAppCheckVerified.collectAsState()
    var showEmailDialog by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CLOUD & COMMUNITY SYNC",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Firestore Live Sync & Global Practicing Sangha",
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
                        text = if (isAppCheckVerified) "🛡️ AppCheck Active" else "🛡️ Secure",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
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
                            text = "Account: ${userAuth.displayName}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (userAuth.isAnonymous) "Local-first guest mode (${userAuth.uid.take(12)}...)" else "Email: ${userAuth.email}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        )
                    }

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

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Active Sangha", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${communityStats.activeMindfulUsers}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
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
