package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.FirebaseIntegration
import com.example.data.FlowAppPreferences
import com.example.data.FlowAudioSynthesizer
import com.example.data.FlowRepository
import com.example.data.FlowSession
import com.example.data.UserReflection
import com.example.data.SmartPromptInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.data.AmbientSoundProfile
import com.example.data.AuthenticatedUser
import com.example.data.FirestoreSyncManager
import com.example.data.SyncState
import com.example.data.SyncStatus
import com.example.ui.theme.FlowTimerPalette
import com.example.ui.util.FlowHapticManager
import com.example.R

enum class FocusState {
    NONE,       // Idle screen
    COMPASS,    // Flow Compass calibration (Challenge vs Skill)
    RITUAL,     // Threshold breathing gesture
    DEEP_SHIELD,// AMOLED focus mode
    REFLECTION  // Post-session Metacognitive Mirror
}

enum class BreathPhase {
    IN, HOLD, OUT, COMPLETE
}

enum class FlowThemeMode {
    CIRCADIAN,
    DAWN,
    DUSK,
    MIDNIGHT
}

class FlowViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = FlowRepository(db.flowSessionDao(), db.userReflectionDao())
    val appPreferences = FlowAppPreferences(application)
    val firebaseIntegration = FirebaseIntegration(application)
    val firestoreSyncManager = FirestoreSyncManager(application)

    val syncState: StateFlow<SyncState> = firestoreSyncManager.syncState
    val currentUser: StateFlow<AuthenticatedUser?> = firestoreSyncManager.currentUser

    val communityStats = firebaseIntegration.communityStats
    val userAuth = firebaseIntegration.userAuth
    val remoteConfig = firebaseIntegration.remoteConfig
    val isAppCheckVerified = firebaseIntegration.isAppCheckVerified
    val aiMantra = firebaseIntegration.aiMantra
    val isAiLoading = firebaseIntegration.isAiLoading

    fun signInAnonymously() {
        firebaseIntegration.signInAnonymously()
    }

    fun signInWithEmail(email: String) {
        firebaseIntegration.signInWithEmail(email)
    }

    fun signOut() {
        firebaseIntegration.signOut()
    }

    fun fetchRemoteConfig() {
        viewModelScope.launch {
            val config = firebaseIntegration.fetchRemoteConfig()
            _selectedDurationMinutes.value = config.defaultFocusMinutes
        }
    }

    fun verifyAppCheckToken() {
        firebaseIntegration.verifyAppCheckToken()
    }

    val suggestedIntents: StateFlow<List<String>> = appPreferences.suggestedIntents
    val activePrompt: StateFlow<SmartPromptInfo> = appPreferences.activePrompt

    // History Flows
    val allSessions: StateFlow<List<FlowSession>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allReflections: StateFlow<List<UserReflection>> = repository.allReflections
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _reflectionQuery = MutableStateFlow("")
    val reflectionQuery = _reflectionQuery.asStateFlow()

    fun setReflectionQuery(query: String) {
        _reflectionQuery.value = query
    }

    val filteredReflections: StateFlow<List<UserReflection>> = combine(allReflections, _reflectionQuery) { list, query ->
        if (query.isBlank()) {
            list
        } else {
            list.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.content.contains(query, ignoreCase = true) ||
                it.tags.contains(query, ignoreCase = true) ||
                it.frictionNotes.contains(query, ignoreCase = true) ||
                it.breakthroughNotes.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )



    // Theme Mode Selection State
    private val _themeMode = MutableStateFlow(appPreferences.getSavedThemeMode())
    val themeMode = _themeMode.asStateFlow()

    private val _hapticFeedbackEnabled = MutableStateFlow(appPreferences.getHapticFeedbackEnabled())
    val hapticFeedbackEnabled = _hapticFeedbackEnabled.asStateFlow()

    private val _dailyIntention = MutableStateFlow(appPreferences.getDailyIntention())
    val dailyIntention = _dailyIntention.asStateFlow()

    private val _firstTimerStarted = MutableStateFlow(false)
    val firstTimerStarted = _firstTimerStarted.asStateFlow()

    fun setDailyIntention(intention: String) {
        _dailyIntention.value = intention
        appPreferences.saveDailyIntention(intention)
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        _hapticFeedbackEnabled.value = enabled
        appPreferences.saveHapticFeedbackEnabled(enabled)
    }

    fun setThemeMode(mode: FlowThemeMode) {
        _themeMode.value = mode
        appPreferences.saveThemeMode(mode)
    }

    // Flow Timer Palette & Intensity Shifting State
    private val _timerPalette = MutableStateFlow(appPreferences.getTimerPalette())
    val timerPalette: StateFlow<FlowTimerPalette> = _timerPalette.asStateFlow()

    private val _intensityShiftingEnabled = MutableStateFlow(appPreferences.isIntensityShiftingEnabled())
    val intensityShiftingEnabled: StateFlow<Boolean> = _intensityShiftingEnabled.asStateFlow()

    fun setTimerPalette(palette: FlowTimerPalette) {
        _timerPalette.value = palette
        appPreferences.saveTimerPalette(palette)
    }

    fun setIntensityShiftingEnabled(enabled: Boolean) {
        _intensityShiftingEnabled.value = enabled
        appPreferences.saveIntensityShiftingEnabled(enabled)
    }

    // Compute current Focus Intensity (0.0f - 1.0f)
    fun calculateFocusIntensity(): Float {
        if (!_intensityShiftingEnabled.value) return 0.5f
        val totalSecs = _selectedDurationMinutes.value * 60
        if (totalSecs <= 0) return 0f
        val elapsed = (totalSecs - _remainingSeconds.value).toFloat()
        val elapsedRatio = (elapsed / totalSecs.toFloat()).coerceIn(0f, 1f)
        
        // Intensity curve: starts calm (0.1), ascends progressively towards peak (1.0)
        val calibrationBonus = (_challengeLevel.value + _skillLevel.value - 2) / 8f * 0.2f
        return (elapsedRatio * 0.8f + 0.1f + calibrationBonus).coerceIn(0.1f, 1.0f)
    }

    // Cloud Firestore Sync Actions
    fun syncWithFirestore() {
        viewModelScope.launch {
            firestoreSyncManager.performTwoWaySync(
                localSessions = allSessions.value,
                localReflections = allReflections.value,
                onSessionPulled = { session ->
                    repository.insertSession(session)
                },
                onReflectionPulled = { reflection ->
                    repository.insertReflection(reflection)
                }
            )
        }
    }

    fun signInWithGoogle(activityContext: android.content.Context) {
        viewModelScope.launch {
            val result = firestoreSyncManager.signInWithGoogle(activityContext)
            if (result.isSuccess) {
                syncWithFirestore()
            }
        }
    }

    fun signInQuick(displayName: String = "Zen Practitioner", email: String = "zen@mindfulflow.app") {
        firestoreSyncManager.signInQuick(displayName, email)
        syncWithFirestore()
    }

    fun signOutFromCloud() {
        firestoreSyncManager.signOut()
    }

    fun getEffectiveThemeMode(): FlowThemeMode {
        return when (_themeMode.value) {
            FlowThemeMode.CIRCADIAN -> {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                when (hour) {
                    in 6..16 -> FlowThemeMode.DAWN       // 6 AM to 4:59 PM is Dawn (Light)
                    in 17..20 -> FlowThemeMode.DUSK     // 5 PM to 8:59 PM is Dusk (Slate)
                    else -> FlowThemeMode.MIDNIGHT      // 9 PM to 5:59 AM is Midnight (Pure Black)
                }
            }
            else -> _themeMode.value
        }
    }

    // Navigation & Deep link triggers
    private val _requestedTab = MutableStateFlow<Int?>(null)
    val requestedTab = _requestedTab.asStateFlow()

    private val _triggerNewReflectionDialog = MutableStateFlow(false)
    val triggerNewReflectionDialog = _triggerNewReflectionDialog.asStateFlow()

    fun selectTab(tab: Int) {
        _requestedTab.value = tab
    }

    fun clearRequestedTab() {
        _requestedTab.value = null
    }

    fun requestOpenReflectionDialog() {
        _requestedTab.value = 1
        _triggerNewReflectionDialog.value = true
    }

    fun clearTriggerNewReflectionDialog() {
        _triggerNewReflectionDialog.value = false
    }

    // Current Navigation/Focus State
    private val _focusState = MutableStateFlow(FocusState.NONE)
    val focusState = _focusState.asStateFlow()

    // Intent Input
    private val _currentIntent = MutableStateFlow("")
    val currentIntent = _currentIntent.asStateFlow()

    // Chosen Focus Duration (in minutes)
    private val _selectedDurationMinutes = MutableStateFlow(25)
    val selectedDurationMinutes = _selectedDurationMinutes.asStateFlow()

    // Countdown state (in seconds)
    private val _remainingSeconds = MutableStateFlow(1500)
    val remainingSeconds = _remainingSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning = _isTimerRunning.asStateFlow()

    // Calibration levels (1 to 5)
    private val _challengeLevel = MutableStateFlow(3)
    val challengeLevel = _challengeLevel.asStateFlow()

    private val _skillLevel = MutableStateFlow(3)
    val skillLevel = _skillLevel.asStateFlow()

    // Sound Synthesizer & Multi-layer Soundscape Mixer
    val synthesizer = FlowAudioSynthesizer()

    private val _selectedAmbientProfile = MutableStateFlow(AmbientSoundProfile.BROWN_NOISE)
    val selectedAmbientProfile = _selectedAmbientProfile.asStateFlow()

    private val _binauralLayerVol = MutableStateFlow(0.35f)
    val binauralLayerVol = _binauralLayerVol.asStateFlow()

    private val _noiseLayerVol = MutableStateFlow(0.40f)
    val noiseLayerVol = _noiseLayerVol.asStateFlow()

    private val _singingBowlVol = MutableStateFlow(0.60f)
    val singingBowlVol = _singingBowlVol.asStateFlow()

    private val _masterVolume = MutableStateFlow(0.70f)
    val masterVolume = _masterVolume.asStateFlow()

    private val _binauralMode = MutableStateFlow(40.0) // 40Hz Gamma default
    val binauralMode = _binauralMode.asStateFlow()

    val isSoundscapeTransitioning: StateFlow<Boolean> = synthesizer.isTransitioning
    val soundscapeTransitioningTo: StateFlow<AmbientSoundProfile?> = synthesizer.transitioningTo

    fun setAmbientProfile(profile: AmbientSoundProfile) {
        _selectedAmbientProfile.value = profile
        synthesizer.transitionToPreset(profile)
        recordUserActivity()
    }

    fun setBinauralLayerVol(volume: Float) {
        _binauralLayerVol.value = volume
        synthesizer.binauralBeatVolume = volume
        if (volume > 0.01f && synthesizer.activeProfile != AmbientSoundProfile.NONE) {
            synthesizer.start()
        }
        recordUserActivity()
    }

    fun setNoiseLayerVol(volume: Float) {
        _noiseLayerVol.value = volume
        synthesizer.noiseTextureVolume = volume
    }

    fun setSingingBowlVol(volume: Float) {
        _singingBowlVol.value = volume
        synthesizer.singingBowlVolume = volume
    }

    fun setMasterVolume(volume: Float) {
        _masterVolume.value = volume
        synthesizer.masterVolume = volume
    }

    fun setBinauralFrequency(freqHz: Double) {
        _binauralMode.value = freqHz
        synthesizer.binauralBeatFreq = freqHz
    }

    // Sound Layer toggles
    private val _gammaEnabled = MutableStateFlow(false)
    val gammaEnabled = _gammaEnabled.asStateFlow()

    private val _rainEnabled = MutableStateFlow(false)
    val rainEnabled = _rainEnabled.asStateFlow()

    private val _bowlEnabled = MutableStateFlow(true) // Bowl strike active by default
    val bowlEnabled = _bowlEnabled.asStateFlow()

    // Breathing Threshold Ritual State
    private val _holdingCircle = MutableStateFlow(false)
    val holdingCircle = _holdingCircle.asStateFlow()

    // Daily Reminder State Management
    private val _isFocusReminderEnabled = MutableStateFlow(appPreferences.isFocusReminderEnabled())
    val isFocusReminderEnabled = _isFocusReminderEnabled.asStateFlow()

    private val _focusReminderHour = MutableStateFlow(appPreferences.getFocusReminderHour())
    val focusReminderHour = _focusReminderHour.asStateFlow()

    private val _focusReminderMinute = MutableStateFlow(appPreferences.getFocusReminderMinute())
    val focusReminderMinute = _focusReminderMinute.asStateFlow()

    private val _isReflectionReminderEnabled = MutableStateFlow(appPreferences.isReflectionReminderEnabled())
    val isReflectionReminderEnabled = _isReflectionReminderEnabled.asStateFlow()

    private val _reflectionReminderHour = MutableStateFlow(appPreferences.getReflectionReminderHour())
    val reflectionReminderHour = _reflectionReminderHour.asStateFlow()

    private val _reflectionReminderMinute = MutableStateFlow(appPreferences.getReflectionReminderMinute())
    val reflectionReminderMinute = _reflectionReminderMinute.asStateFlow()

    private val _breathPhase = MutableStateFlow(BreathPhase.IN)
    val breathPhase = _breathPhase.asStateFlow()

    private val _breathCount = MutableStateFlow(1) // 1, 2, 3 breaths
    val breathCount = _breathCount.asStateFlow()

    private val _breathProgress = MutableStateFlow(0f) // 0.0 to 1.0 within active phase
    val breathProgress = _breathProgress.asStateFlow()

    // Timer Job
    private var countdownJob: Job? = null
    private var breathingJob: Job? = null

    // For Mindful Reminders (Stage 2)
    private val _currentReminderId = MutableStateFlow(R.string.reminder_1)
    val currentReminderId = _currentReminderId.asStateFlow()

    // 10-Minute (600s) Idle State Detector
    private val _idleSeconds = MutableStateFlow(0)
    val idleSeconds = _idleSeconds.asStateFlow()

    private val _isIdleNudgeActive = MutableStateFlow(false)
    val isIdleNudgeActive = _isIdleNudgeActive.asStateFlow()

    private val _isIdleNudgeDismissed = MutableStateFlow(false)
    val isIdleNudgeDismissed = _isIdleNudgeDismissed.asStateFlow()

    private val reminders = listOf(
        R.string.reminder_1,
        R.string.reminder_2,
        R.string.reminder_3,
        R.string.reminder_4,
        R.string.reminder_5,
        R.string.reminder_6,
        R.string.reminder_7,
        R.string.reminder_8
    )

    init {
        synthesizer.start()
        updateSynthVolumes()
        com.example.service.DailyReminderScheduler.scheduleAllReminders(getApplication())

        // Idle Detector 1-second loop
        viewModelScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(1000L)
                if (_focusState.value == FocusState.NONE && !_isTimerRunning.value) {
                    _idleSeconds.value += 1
                    if (_idleSeconds.value >= 600 && !_isIdleNudgeDismissed.value) {
                        _isIdleNudgeActive.value = true
                    }
                } else {
                    _idleSeconds.value = 0
                    _isIdleNudgeActive.value = false
                    _isIdleNudgeDismissed.value = false
                }
            }
        }

        viewModelScope.launch {
            com.example.service.FlowServiceController.cancelSessionRequests.collect {
                cancelSessionFlow()
            }
        }
    }

    fun recordUserActivity() {
        _idleSeconds.value = 0
        if (_isIdleNudgeActive.value) {
            _isIdleNudgeActive.value = false
        }
    }

    fun dismissIdleNudge() {
        _isIdleNudgeActive.value = false
        _isIdleNudgeDismissed.value = true
        _idleSeconds.value = 0
    }

    fun triggerTestIdleNudge() {
        _idleSeconds.value = 600
        _isIdleNudgeDismissed.value = false
        _isIdleNudgeActive.value = true
    }

    fun startFlowFromIdleNudge() {
        _isIdleNudgeActive.value = false
        _isIdleNudgeDismissed.value = false
        _idleSeconds.value = 0
        startCalibration()
    }

    fun setIntent(intent: String) {
        _currentIntent.value = intent
    }

    fun setDuration(minutes: Int) {
        _selectedDurationMinutes.value = minutes
        _remainingSeconds.value = minutes * 60
    }

    fun setChallengeLevel(level: Int) {
        _challengeLevel.value = level
    }

    fun setSkillLevel(level: Int) {
        _skillLevel.value = level
    }

    fun toggleGamma(enabled: Boolean) {
        _gammaEnabled.value = enabled
        updateSynthVolumes()
    }

    fun toggleRain(enabled: Boolean) {
        _rainEnabled.value = enabled
        updateSynthVolumes()
    }

    fun strikeBowl() {
        synthesizer.strikeSingingBowl()
    }

    private fun updateSynthVolumes() {
        synthesizer.binauralBeatVolume = if (_gammaEnabled.value) _binauralLayerVol.value else 0.0f
        synthesizer.noiseTextureVolume = if (_rainEnabled.value) _noiseLayerVol.value else 0.0f
        synthesizer.singingBowlVolume = if (_bowlEnabled.value) _singingBowlVol.value else 0.0f
    }

    // Start Focus Session Flow
    fun startCalibration() {
        _firstTimerStarted.value = true
        _focusState.value = FocusState.COMPASS
        FlowHapticManager.performClickHaptic(getApplication(), _hapticFeedbackEnabled.value)
    }

    fun proceedToRitual() {
        _focusState.value = FocusState.RITUAL
        resetBreathing()
        FlowHapticManager.performClickHaptic(getApplication(), _hapticFeedbackEnabled.value)
    }

    fun cancelSessionFlow() {
        resetBreathing()
        stopFocusTimer()
        com.example.service.FlowForegroundService.stopService(getApplication())
        _focusState.value = FocusState.NONE
        appPreferences.refreshSuggestedIntents()
        FlowHapticManager.performClickHaptic(getApplication(), _hapticFeedbackEnabled.value)
    }

    // Breathing Gesture Logic
    fun startHoldingBreath() {
        _holdingCircle.value = true
        breathingJob?.cancel()
        breathingJob = viewModelScope.launch {
            // We need 3 full breaths. Each breath has: IN (4s), HOLD (4s), OUT (4s)
            val stepMs = 50L
            val phaseDurationMs = 4000L

            while (_holdingCircle.value) {
                // Increment phase progress
                var progress = _breathProgress.value + (stepMs.toFloat() / phaseDurationMs)
                if (progress >= 1.0f) {
                    progress = 0f
                    // Move to next phase
                    when (_breathPhase.value) {
                        BreathPhase.IN -> {
                            _breathPhase.value = BreathPhase.HOLD
                            FlowHapticManager.performBreathPhaseHaptic(getApplication(), _hapticFeedbackEnabled.value)
                        }
                        BreathPhase.HOLD -> {
                            _breathPhase.value = BreathPhase.OUT
                            FlowHapticManager.performBreathPhaseHaptic(getApplication(), _hapticFeedbackEnabled.value)
                        }
                        BreathPhase.OUT -> {
                            if (_breathCount.value < 3) {
                                _breathCount.value += 1
                                _breathPhase.value = BreathPhase.IN
                                FlowHapticManager.performBreathPhaseHaptic(getApplication(), _hapticFeedbackEnabled.value)
                                synthesizer.strikeSingingBowl() // Accentuate the turn
                            } else {
                                // Completed 3 breaths!
                                _breathPhase.value = BreathPhase.COMPLETE
                                FlowHapticManager.performSessionStartHaptic(getApplication(), _hapticFeedbackEnabled.value)
                                delay(200)
                                enterDeepShield()
                                break
                            }
                        }
                        else -> {}
                    }
                }
                _breathProgress.value = progress
                delay(stepMs)
            }
        }
    }

    fun releaseHoldingBreath() {
        _holdingCircle.value = false
        breathingJob?.cancel()
        // Decay breathing progress back to 0 slowly
        viewModelScope.launch {
            while (!_holdingCircle.value && _breathProgress.value > 0f) {
                _breathProgress.value = (_breathProgress.value - 0.05f).coerceAtLeast(0f)
                delay(20)
            }
        }
    }

    private fun resetBreathing() {
        _holdingCircle.value = false
        _breathPhase.value = BreathPhase.IN
        _breathCount.value = 1
        _breathProgress.value = 0f
        breathingJob?.cancel()
    }

    // Enter AMOLED Focus Mode
    fun proceedToShield() {
        enterDeepShield()
        startFocusTimer()
    }

    private fun enterDeepShield() {
        val selectedIntent = _currentIntent.value.ifBlank { "Deep Focus" }
        appPreferences.recordIntentSelection(selectedIntent)
        _focusState.value = FocusState.DEEP_SHIELD
        _remainingSeconds.value = _selectedDurationMinutes.value * 60
        _isTimerRunning.value = true
        FlowHapticManager.performSessionStartHaptic(getApplication(), _hapticFeedbackEnabled.value)
        synthesizer.strikeSingingBowl() // Strike bowl at session start!
        com.example.service.FlowForegroundService.startService(
            getApplication(),
            selectedIntent,
            _remainingSeconds.value
        )
        startFocusTimer()
    }

    private fun startFocusTimer() {
        countdownJob?.cancel()
        _isTimerRunning.value = true
        countdownJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000)
                _remainingSeconds.value -= 1
                
                // Update foreground service notification timer
                com.example.service.FlowForegroundService.updateService(
                    getApplication(),
                    _currentIntent.value.ifBlank { "Deep Focus" },
                    _remainingSeconds.value
                )

                // Strike bowl at regular intervals to keep the mind grounded (e.g., every 5 minutes or 1 minute before completion)
                if (_remainingSeconds.value == 60) {
                    synthesizer.strikeSingingBowl() // 1 minute warning
                    FlowHapticManager.performBreathPhaseHaptic(getApplication(), _hapticFeedbackEnabled.value)
                }
            }
            // Session complete!
            _isTimerRunning.value = false
            FlowHapticManager.performSessionCompleteHaptic(getApplication(), _hapticFeedbackEnabled.value)
            synthesizer.strikeSingingBowl() // Masterful finishing strike
            val sessionName = _currentIntent.value.ifBlank { "Deep Focus" }
            com.example.service.FlowForegroundService.sendSessionCompleteNotification(getApplication(), sessionName)
            com.example.service.FlowForegroundService.stopService(getApplication())
            fadeOutAmbientSounds()
            _focusState.value = FocusState.REFLECTION
        }
    }

    private suspend fun fadeOutAmbientSounds() {
        val originalVolume = synthesizer.masterVolume
        val originalBinaural = synthesizer.binauralBeatVolume
        val originalNoise = synthesizer.noiseTextureVolume
        val steps = 25
        val delayMs = 100L // 2.5 seconds total fade-out
        for (i in 1..steps) {
            val factor = (steps - i).toFloat() / steps
            synthesizer.masterVolume = originalVolume * factor
            synthesizer.binauralBeatVolume = originalBinaural * factor
            synthesizer.noiseTextureVolume = originalNoise * factor
            delay(delayMs)
        }
        // Completely stop ambient and restore original settings for next time
        synthesizer.activeProfile = com.example.data.AmbientSoundProfile.NONE
        _selectedAmbientProfile.value = com.example.data.AmbientSoundProfile.NONE
        synthesizer.masterVolume = originalVolume
        synthesizer.binauralBeatVolume = originalBinaural
        synthesizer.noiseTextureVolume = originalNoise
    }

    private fun stopFocusTimer() {
        countdownJob?.cancel()
        countdownJob = null
        _isTimerRunning.value = false
    }

    fun toggleTimerPlayPause() {
        if (_isTimerRunning.value) {
            stopFocusTimer()
        } else {
            startFocusTimer()
        }
    }

    fun autoPauseTimer() {
        if (_isTimerRunning.value) {
            stopFocusTimer()
            com.example.service.FlowForegroundService.sendAutoPauseNotification(getApplication())
        }
    }

    fun resetTimer() {
        stopFocusTimer()
        _remainingSeconds.value = _selectedDurationMinutes.value * 60
    }

    fun updateFocusInterval(minutes: Int) {
        setDuration(minutes)
        _remainingSeconds.value = minutes * 60
    }

    // Post Session Reflection
    fun saveSession(flowScore: Int, notes: String, frictionNotes: String = "", breakthroughNotes: String = "") {
        viewModelScope.launch {
            val elapsedSeconds = (_selectedDurationMinutes.value * 60) - _remainingSeconds.value
            val intentStr = _currentIntent.value.ifBlank { "Center" }
            val session = FlowSession(
                intent = intentStr,
                durationSeconds = elapsedSeconds,
                flowScore = flowScore,
                challengeLevel = _challengeLevel.value,
                skillLevel = _skillLevel.value,
                notes = notes
            )
            val sessionId = repository.insertSession(session).toInt()
            val savedSessionWithId = session.copy(id = sessionId)

            // Direct sync to Cloud Firestore
            firestoreSyncManager.uploadSingleSession(savedSessionWithId)

            // If notes, friction, or breakthrough was provided, store a linked UserReflection automatically
            if (notes.isNotBlank() || frictionNotes.isNotBlank() || breakthroughNotes.isNotBlank()) {
                val reflection = UserReflection(
                    title = "Post-Focus Mirror: $intentStr",
                    content = if (notes.isNotBlank()) notes else "Flow calibration rating: $flowScore/5",
                    moodRating = flowScore,
                    frictionNotes = frictionNotes,
                    breakthroughNotes = breakthroughNotes,
                    tags = "Focus, $intentStr",
                    linkedSessionId = sessionId
                )
                val reflectionId = repository.insertReflection(reflection)
                firestoreSyncManager.uploadSingleReflection(reflection.copy(id = reflectionId))
            }
            
            // Log Firebase Analytics Event for Session Completion
            firebaseIntegration.logEvent("session_completed", mapOf(
                "duration_seconds" to elapsedSeconds,
                "flow_score" to flowScore,
                "intent" to intentStr
            ))

            // Sync to Firebase Cloud Data Service & Record session completed
            firebaseIntegration.syncSessionToCloud((elapsedSeconds / 60).coerceAtLeast(1))
            val config = remoteConfig.value
            appPreferences.recordSessionCompleted(
                durationSeconds = elapsedSeconds,
                flowScore = flowScore,
                rateMinSessions = config.ratePromptMinSessions,
                rateMinMinutes = config.ratePromptMinMinutes,
                shareTargetSession = config.sharePromptTargetSession
            )
            
            // Clean up states
            FlowHapticManager.performSessionCompleteHaptic(getApplication(), _hapticFeedbackEnabled.value)
            com.example.service.FlowForegroundService.stopService(getApplication())
            com.example.widget.FlowQuickWidgetProvider.notifyUpdate(getApplication())
            _currentIntent.value = ""
            _focusState.value = FocusState.NONE
            appPreferences.refreshSuggestedIntents()
        }
    }

    // Direct User Reflection Management (Room Database)
    fun createReflection(
        title: String,
        content: String,
        moodRating: Int = 3,
        frictionNotes: String = "",
        breakthroughNotes: String = "",
        tags: String = "Reflection",
        aiInsight: String = "",
        linkedSessionId: Int? = null
    ) {
        viewModelScope.launch {
            val reflection = UserReflection(
                title = title.ifBlank { "Mindful Reflection" },
                content = content,
                moodRating = moodRating,
                frictionNotes = frictionNotes,
                breakthroughNotes = breakthroughNotes,
                tags = tags.ifBlank { "Journal" },
                aiInsight = aiInsight,
                linkedSessionId = linkedSessionId
            )
            val reflectionId = repository.insertReflection(reflection)
            val savedReflectionWithId = reflection.copy(id = reflectionId)
            
            // Sync to Cloud Firestore
            firestoreSyncManager.uploadSingleReflection(savedReflectionWithId)
            FlowHapticManager.performClickHaptic(getApplication(), _hapticFeedbackEnabled.value)
            firebaseIntegration.logEvent("reflection_created", mapOf("mood" to moodRating))
        }
    }

    fun updateReflection(reflection: UserReflection) {
        viewModelScope.launch {
            repository.updateReflection(reflection)
        }
    }

    fun deleteReflection(reflection: UserReflection) {
        viewModelScope.launch {
            repository.deleteReflection(reflection)
        }
    }

    fun clearAllReflections() {
        viewModelScope.launch {
            repository.clearAllReflections()
        }
    }

    suspend fun generateAiJournalInsight(title: String, content: String, friction: String, breakthrough: String): String {
        val prompt = "User reflection: '$title'. Details: '$content'. Friction encountered: '$friction'. Breakthroughs: '$breakthrough'."
        return firebaseIntegration.generateReflectionInsight(
            intent = title.ifBlank { "Daily Mindfulness Reflection" },
            durationMinutes = 15,
            flowScore = 4,
            frictionNotes = prompt
        )
    }

    fun generateAiMantra() {
        viewModelScope.launch {
            firebaseIntegration.generateFocusMantra(_currentIntent.value)
        }
    }

    suspend fun getAiReflectionInsight(flowScore: Int, frictionNotes: String): String {
        return firebaseIntegration.generateReflectionInsight(
            intent = _currentIntent.value.ifBlank { "Focus Session" },
            durationMinutes = _selectedDurationMinutes.value,
            flowScore = flowScore,
            frictionNotes = frictionNotes
        )
    }

    fun discardSession() {
        firebaseIntegration.logEvent("session_discarded")
        _currentIntent.value = ""
        _focusState.value = FocusState.NONE
        appPreferences.refreshSuggestedIntents()
    }

    fun dismissSmartPrompt() {
        val currentPrompt = activePrompt.value
        firebaseIntegration.logEvent("growth_prompt_action", mapOf(
            "prompt_type" to currentPrompt.type.name,
            "action" to "dismissed"
        ))
        appPreferences.dismissActivePrompt()
    }

    fun triggerSharePrompt() {
        firebaseIntegration.logEvent("growth_prompt_triggered_manually", mapOf("type" to "SHARE"))
        appPreferences.triggerSharePromptManually()
    }

    fun triggerRatePrompt() {
        firebaseIntegration.logEvent("growth_prompt_triggered_manually", mapOf("type" to "RATING"))
        appPreferences.triggerRatePromptManually()
    }

    fun onRatedApp() {
        firebaseIntegration.logEvent("growth_prompt_action", mapOf(
            "prompt_type" to "RATING",
            "action" to "rated_5_stars"
        ))
        appPreferences.onRated()
    }

    fun onSharedApp() {
        firebaseIntegration.logEvent("growth_prompt_action", mapOf(
            "prompt_type" to "SHARE",
            "action" to "shared_app"
        ))
        appPreferences.dismissActivePrompt()
    }

    fun onNeverAskRating() {
        firebaseIntegration.logEvent("growth_prompt_action", mapOf(
            "prompt_type" to "RATING",
            "action" to "dont_ask_again"
        ))
        appPreferences.onNeverAskRating()
    }

    fun deleteSessionItem(session: FlowSession) {
        viewModelScope.launch {
            repository.delete(session)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
            appPreferences.clearHistoryPreferences()
        }
    }

    // Stage 2 Reminder Generator
    fun generateReminder() {
        var next = reminders.random()
        while (next == _currentReminderId.value) {
            next = reminders.random()
        }
        _currentReminderId.value = next
    }



    // Export last 7 days of sessions data as CSV or JSON
    fun exportSessionsShareIntent(context: android.content.Context, format: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val last7DaysMs = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
                val recentSessions = allSessions.value.filter { it.timestamp >= last7DaysMs }

                val ext = format.lowercase()
                val fileName = "flow_sessions_7days.$ext"
                val file = java.io.File(context.cacheDir, fileName)

                val content = if (ext == "json") {
                    val jsonArray = org.json.JSONArray()
                    recentSessions.forEach { s ->
                        val obj = org.json.JSONObject().apply {
                            put("id", s.id)
                            put("timestamp", s.timestamp)
                            put("date", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(s.timestamp)))
                            put("intent", s.intent)
                            put("durationSeconds", s.durationSeconds)
                            put("flowScore", s.flowScore)
                            put("challengeLevel", s.challengeLevel)
                            put("skillLevel", s.skillLevel)
                            put("notes", s.notes)
                        }
                        jsonArray.put(obj)
                    }
                    jsonArray.toString(2)
                } else {
                    val sb = StringBuilder()
                    sb.append("ID,Date,Intent,DurationSeconds,FlowScore,ChallengeLevel,SkillLevel,Notes\n")
                    recentSessions.forEach { s ->
                        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(s.timestamp))
                        val safeIntent = "\"" + s.intent.replace("\"", "\"\"") + "\""
                        val safeNotes = "\"" + s.notes.replace("\"", "\"\"") + "\""
                        sb.append("${s.id},$dateStr,$safeIntent,${s.durationSeconds},${s.flowScore},${s.challengeLevel},${s.skillLevel},$safeNotes\n")
                    }
                    sb.toString()
                }

                file.writeText(content)

                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = if (ext == "json") "application/json" else "text/csv"
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Flow Sessions Export ($format)")
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = android.content.Intent.createChooser(shareIntent, "Export Flow Sessions ($format)")
                chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (e: Exception) {
                android.util.Log.e("FlowViewModel", "Export failed", e)
            }
        }
    }

    fun updateFocusReminder(enabled: Boolean, hour: Int, minute: Int) {
        appPreferences.saveFocusReminderEnabled(enabled)
        appPreferences.saveFocusReminderTime(hour, minute)
        _isFocusReminderEnabled.value = enabled
        _focusReminderHour.value = hour
        _focusReminderMinute.value = minute
        com.example.service.DailyReminderScheduler.scheduleAllReminders(getApplication())
    }

    fun updateReflectionReminder(enabled: Boolean, hour: Int, minute: Int) {
        appPreferences.saveReflectionReminderEnabled(enabled)
        appPreferences.saveReflectionReminderTime(hour, minute)
        _isReflectionReminderEnabled.value = enabled
        _reflectionReminderHour.value = hour
        _reflectionReminderMinute.value = minute
        com.example.service.DailyReminderScheduler.scheduleAllReminders(getApplication())
    }

    fun triggerTestNotification(type: String) {
        com.example.service.DailyReminderScheduler.triggerTestNotification(getApplication(), type)
    }

    override fun onCleared() {
        super.onCleared()
        synthesizer.stop()
    }
}
