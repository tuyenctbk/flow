package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Context
import com.example.ui.screens.AscendScreen
import com.example.ui.screens.FlowScreen
import com.example.ui.screens.MirrorScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.*
import com.example.viewmodel.FocusState
import com.example.viewmodel.FlowViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: FlowViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            super.attachBaseContext(newBase.createAttributionContext("default"))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Setup Dynamic App Shortcuts
        com.example.util.AppShortcutManager.setupShortcuts(this)

        // Handle Widget, Notification & App Shortcut Intent Extras
        handleIncomingIntent(intent)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            MyApplicationTheme(themeMode = themeMode) {
                val context = androidx.compose.ui.platform.LocalContext.current
                val prefs = remember { context.getSharedPreferences("flow_prefs", Context.MODE_PRIVATE) }
                var showOnboarding by remember {
                    mutableStateOf(!prefs.getBoolean("onboarding_completed", false))
                }

                if (showOnboarding) {
                    OnboardingScreen(
                        onFinishOnboarding = {
                            prefs.edit().putBoolean("onboarding_completed", true).apply()
                            showOnboarding = false
                        }
                    )
                } else {
                    MainContent(viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: android.content.Intent?) {
        if (intent == null) return

        // 0. App Shortcuts Action
        val shortcutAction = intent.getStringExtra(com.example.util.AppShortcutManager.EXTRA_ACTION_SHORTCUT)
        when (shortcutAction) {
            com.example.util.AppShortcutManager.SHORTCUT_START_FLOW -> {
                viewModel.selectTab(0)
                viewModel.startCalibration()
            }
            com.example.util.AppShortcutManager.SHORTCUT_ADD_REFLECTION -> {
                viewModel.selectTab(1)
                viewModel.requestOpenReflectionDialog()
            }
            com.example.util.AppShortcutManager.SHORTCUT_OPEN_STATS -> {
                viewModel.selectTab(1)
            }
        }

        // 1. Tab navigation
        val targetTab = intent.getIntExtra(com.example.service.DailyReminderReceiver.EXTRA_NAVIGATE_TAB, -1)
        if (targetTab != -1) {
            viewModel.selectTab(targetTab)
        }

        // 2. Open Reflection Dialog action
        val openReflection = intent.getBooleanExtra(com.example.service.DailyReminderReceiver.EXTRA_ACTION_OPEN_REFLECTION, false)
        if (openReflection) {
            viewModel.requestOpenReflectionDialog()
        }

        // 3. Quick Focus / Breath Start action
        val quickDuration = intent.getIntExtra(com.example.widget.FlowQuickWidgetProvider.EXTRA_QUICK_DURATION, -1)
        val autoStart = intent.getBooleanExtra(com.example.widget.FlowQuickWidgetProvider.EXTRA_AUTO_START, false)
            || intent.getBooleanExtra(com.example.service.DailyReminderReceiver.EXTRA_ACTION_START_FOCUS, false)
            || intent.getBooleanExtra(com.example.service.DailyReminderReceiver.EXTRA_ACTION_START_BREATH, false)

        if (quickDuration > 0) {
            viewModel.setDuration(quickDuration)
            if (autoStart) {
                viewModel.startCalibration()
            }
        }
    }
}

@Composable
fun MainContent(viewModel: FlowViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val requestedTab by viewModel.requestedTab.collectAsState()

    LaunchedEffect(requestedTab) {
        requestedTab?.let {
            selectedTab = it
            viewModel.clearRequestedTab()
        }
    }

    val focusState by viewModel.focusState.collectAsState()

    // Determine if we should hide bottom navigation (highly immersive during focus rituals)
    val showBottomBar = focusState == FocusState.NONE || focusState == FocusState.COMPASS || focusState == FocusState.REFLECTION

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 0) Icons.Filled.Spa else Icons.Outlined.Spa,
                                contentDescription = "Flow Focus"
                            )
                        },
                        label = { Text(stringResource(R.string.tab_flow), fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Filled.History else Icons.Outlined.History,
                                contentDescription = "Mirror Stats"
                            )
                        },
                        label = { Text(stringResource(R.string.tab_mirror), fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == 2) Icons.Filled.SelfImprovement else Icons.Outlined.SelfImprovement,
                                contentDescription = "Ascend Progression"
                            )
                        },
                        label = { Text(stringResource(R.string.tab_ascend), fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    bottom = if (showBottomBar) innerPadding.calculateBottomPadding() else 0.dp
                )
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(400, easing = EaseInOutSine)) +
                     slideInVertically(animationSpec = tween(400), initialOffsetY = { 20 })) togetherWith
                    fadeOut(animationSpec = tween(300, easing = EaseInOutSine))
                },
                label = "MainTabTransition"
            ) { targetTab ->
                when (targetTab) {
                    0 -> FlowScreen(viewModel = viewModel)
                    1 -> MirrorScreen(viewModel = viewModel)
                    2 -> AscendScreen(viewModel = viewModel)
                }
            }
        }
    }
}
