package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.pow
import kotlin.random.Random

enum class SmartPromptType {
    NONE,
    RATING,
    SHARE,
    UPDATE
}

data class SmartPromptInfo(
    val type: SmartPromptType,
    val title: String = "",
    val message: String = "",
    val sessionsCompleted: Int = 0,
    val totalMinutes: Int = 0
)

class FlowAppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("flow_app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAUNCH_COUNT = "launch_count"
        private const val KEY_COMPLETED_SESSIONS = "completed_sessions"
        private const val KEY_TOTAL_FOCUS_SECONDS = "total_focus_seconds"
        private const val KEY_HAS_RATED = "has_rated"
        private const val KEY_DONT_ASK_RATING = "dont_ask_rating"
        private const val KEY_LAST_RATING_PROMPT_SESSION = "last_rating_prompt_session"
        private const val KEY_LAST_SHARE_PROMPT_SESSION = "last_share_prompt_session"
        private const val KEY_LAST_UPDATE_CHECK_TIME = "last_update_check_time"
        private const val KEY_LAST_UPDATE_DISMISSED_TIME = "last_update_dismissed_time"
        private const val KEY_CUSTOM_INTENTS = "custom_intents_set"
        private const val KEY_THEME_MODE = "theme_mode_preference"
        private const val KEY_HAPTIC_FEEDBACK = "haptic_feedback_enabled"
        private const val KEY_DAILY_INTENTION = "daily_intention_preference"
        private const val KEY_TIMER_PALETTE = "timer_palette_preference"
        private const val KEY_INTENSITY_SHIFTING = "intensity_shifting_enabled"
        
        // Daily Reminder Keys
        private const val KEY_FOCUS_REMINDER_ENABLED = "focus_reminder_enabled"
        private const val KEY_FOCUS_REMINDER_HOUR = "focus_reminder_hour"
        private const val KEY_FOCUS_REMINDER_MINUTE = "focus_reminder_minute"
        private const val KEY_REFLECTION_REMINDER_ENABLED = "reflection_reminder_enabled"
        private const val KEY_REFLECTION_REMINDER_HOUR = "reflection_reminder_hour"
        private const val KEY_REFLECTION_REMINDER_MINUTE = "reflection_reminder_minute"

        val DEFAULT_INTENT_POOL = listOf(
            "Deep Reading",
            "Creative Writing",
            "Mindful Reflection",
            "Strategic Planning",
            "Zen Meditation",
            "Study & Research",
            "Language Practice",
            "Visual Design",
            "Calm Breathing",
            "Solitude & Silence",
            "Problem Solving",
            "Mind Mapping",
            "Journaling",
            "Decluttering",
            "Math & Logic"
        )
    }

    private val _suggestedIntents = MutableStateFlow<List<String>>(emptyList())
    val suggestedIntents: StateFlow<List<String>> = _suggestedIntents.asStateFlow()

    private val _activePrompt = MutableStateFlow<SmartPromptInfo>(SmartPromptInfo(SmartPromptType.NONE))
    val activePrompt: StateFlow<SmartPromptInfo> = _activePrompt.asStateFlow()

    init {
        recordAppLaunch()
        refreshSuggestedIntents()
        evaluateSmartPrompts()
    }

    private fun recordAppLaunch() {
        val launches = prefs.getInt(KEY_LAUNCH_COUNT, 0) + 1
        prefs.edit().putInt(KEY_LAUNCH_COUNT, launches).apply()
    }

    /**
     * Record selected intent to boost its probability next time
     */
    fun recordIntentSelection(intentName: String) {
        if (intentName.isBlank()) return
        val cleanIntent = intentName.trim()
        val key = "intent_count_${cleanIntent.lowercase()}"
        val currentCount = prefs.getInt(key, 0)
        prefs.edit().putInt(key, currentCount + 1).apply()

        // Also save to custom intents set if not in default pool
        if (!DEFAULT_INTENT_POOL.any { it.equals(cleanIntent, ignoreCase = true) }) {
            val customSet = prefs.getStringSet(KEY_CUSTOM_INTENTS, emptySet())?.toMutableSet() ?: mutableSetOf()
            customSet.add(cleanIntent)
            prefs.edit().putStringSet(KEY_CUSTOM_INTENTS, customSet).apply()
        }

        refreshSuggestedIntents()
    }

    /**
     * Calculates weighted probability for each intent based on selection count
     * Next time user opens home screen, selected/frequent intents have higher chance of being picked.
     */
    fun refreshSuggestedIntents() {
        val customSet = prefs.getStringSet(KEY_CUSTOM_INTENTS, emptySet()) ?: emptySet()
        val allCandidates = (DEFAULT_INTENT_POOL + customSet).distinct()

        val weightedCandidates = allCandidates.map { intent ->
            val key = "intent_count_${intent.lowercase()}"
            val selectCount = prefs.getInt(key, 0)
            // Base weight = 1.0, each selection adds 3.0 weight multiplier
            val weight = 1.0 + (selectCount * 3.0)
            intent to weight
        }

        // Weighted random sampling without replacement (Efraimidis & Spirakis algorithm)
        val sampled = weightedCandidates.map { (intent, weight) ->
            val u = Random.nextDouble().coerceIn(0.0001, 0.9999)
            val score = u.pow(1.0 / weight)
            intent to score
        }.sortedByDescending { it.second }
            .take(4)
            .map { it.first }

        _suggestedIntents.value = sampled
    }

    fun recordSessionCompleted(
        durationSeconds: Int,
        flowScore: Int,
        rateMinSessions: Int = 3,
        rateMinMinutes: Int = 60,
        shareTargetSession: Int = 5
    ) {
        val totalSessions = prefs.getInt(KEY_COMPLETED_SESSIONS, 0) + 1
        val totalSecs = prefs.getInt(KEY_TOTAL_FOCUS_SECONDS, 0) + durationSeconds

        prefs.edit()
            .putInt(KEY_COMPLETED_SESSIONS, totalSessions)
            .putInt(KEY_TOTAL_FOCUS_SECONDS, totalSecs)
            .apply()

        evaluateSmartPrompts(
            latestFlowScore = flowScore,
            rateMinSessions = rateMinSessions,
            rateMinMinutes = rateMinMinutes,
            shareTargetSession = shareTargetSession
        )
    }

    /**
     * Smart evaluation to decide when to show Rating, Share, or Update prompts
     * Thresholds are dynamically configured via Firebase Remote Config
     */
    fun evaluateSmartPrompts(
        latestFlowScore: Int = 0,
        rateMinSessions: Int = 3,
        rateMinMinutes: Int = 60,
        shareTargetSession: Int = 5
    ) {
        val completedSessions = prefs.getInt(KEY_COMPLETED_SESSIONS, 0)
        val totalSeconds = prefs.getInt(KEY_TOTAL_FOCUS_SECONDS, 0)
        val totalMins = totalSeconds / 60
        val hasRated = prefs.getBoolean(KEY_HAS_RATED, false)
        val dontAskRating = prefs.getBoolean(KEY_DONT_ASK_RATING, false)
        val lastRatingSession = prefs.getInt(KEY_LAST_RATING_PROMPT_SESSION, 0)
        val lastShareSession = prefs.getInt(KEY_LAST_SHARE_PROMPT_SESSION, 0)
        val lastUpdateCheck = prefs.getLong(KEY_LAST_UPDATE_CHECK_TIME, 0L)
        val lastUpdateDismissed = prefs.getLong(KEY_LAST_UPDATE_DISMISSED_TIME, 0L)
        val now = System.currentTimeMillis()

        // 1. App Update Suggestion Prompt (Smart check: after 5 app opens or 1 day since last check)
        val updateAvailable = checkAppUpdateAvailable(now, lastUpdateCheck)
        if (updateAvailable && (now - lastUpdateDismissed > 24 * 3600 * 1000L)) {
            _activePrompt.value = SmartPromptInfo(
                type = SmartPromptType.UPDATE,
                title = "New Version 1.2.0 Available",
                message = "Enhanced ambient audio synthesis, improved breathing rituals, and smoother animations."
            )
            return
        }

        // 2. 5-Star Rating Prompt Logic
        // Triggers ONLY after user has performed at least rateMinSessions (3) separate session logs
        // AND spent over rateMinMinutes (60) total focus time
        if (!hasRated && !dontAskRating && completedSessions >= rateMinSessions && totalMins >= rateMinMinutes && (completedSessions - lastRatingSession >= 2)) {
            _activePrompt.value = SmartPromptInfo(
                type = SmartPromptType.RATING,
                title = "Finding Stillness in FLOW?",
                message = "If FLOW helps you cultivate focus and calm, a 5-star rating helps others discover it too.",
                sessionsCompleted = completedSessions,
                totalMinutes = totalMins
            )
            prefs.edit().putInt(KEY_LAST_RATING_PROMPT_SESSION, completedSessions).apply()
            return
        }

        // 3. Share Flow Popup Logic
        // Appears after user completes their 5th focus session (or milestone session matching shareTargetSession)
        if (completedSessions >= shareTargetSession && (completedSessions - lastShareSession >= shareTargetSession || completedSessions == shareTargetSession)) {
            _activePrompt.value = SmartPromptInfo(
                type = SmartPromptType.SHARE,
                title = "Share Your Focus Milestone",
                message = "You've completed $completedSessions focus sessions ($totalMins mins total in flow). Invite friends to focus with you!",
                sessionsCompleted = completedSessions,
                totalMinutes = totalMins
            )
            prefs.edit().putInt(KEY_LAST_SHARE_PROMPT_SESSION, completedSessions).apply()
            return
        }

        _activePrompt.value = SmartPromptInfo(SmartPromptType.NONE)
    }

    private fun checkAppUpdateAvailable(now: Long, lastCheckTime: Long): Boolean {
        prefs.edit().putLong(KEY_LAST_UPDATE_CHECK_TIME, now).apply()
        val launches = prefs.getInt(KEY_LAUNCH_COUNT, 0)
        val daysSinceFirstCheck = (now - lastCheckTime) / (1000 * 3600 * 24)
        return launches >= 5 && daysSinceFirstCheck >= 1
    }

    fun triggerSharePromptManually() {
        val completedSessions = prefs.getInt(KEY_COMPLETED_SESSIONS, 0)
        val totalSeconds = prefs.getInt(KEY_TOTAL_FOCUS_SECONDS, 0)
        val totalMins = totalSeconds / 60
        _activePrompt.value = SmartPromptInfo(
            type = SmartPromptType.SHARE,
            title = "Share Your Focus Milestone",
            message = "You've completed $completedSessions focus sessions ($totalMins mins total in flow). Invite friends to focus with you!",
            sessionsCompleted = completedSessions,
            totalMinutes = totalMins
        )
    }

    fun triggerRatePromptManually() {
        val completedSessions = prefs.getInt(KEY_COMPLETED_SESSIONS, 0)
        val totalSeconds = prefs.getInt(KEY_TOTAL_FOCUS_SECONDS, 0)
        val totalMins = totalSeconds / 60
        _activePrompt.value = SmartPromptInfo(
            type = SmartPromptType.RATING,
            title = "Finding Stillness in FLOW?",
            message = "If FLOW helps you cultivate focus and calm, a 5-star rating helps others discover it too.",
            sessionsCompleted = completedSessions,
            totalMinutes = totalMins
        )
    }

    fun dismissActivePrompt() {
        if (_activePrompt.value.type == SmartPromptType.UPDATE) {
            prefs.edit().putLong(KEY_LAST_UPDATE_DISMISSED_TIME, System.currentTimeMillis()).apply()
        }
        _activePrompt.value = SmartPromptInfo(SmartPromptType.NONE)
    }

    fun onRated() {
        prefs.edit().putBoolean(KEY_HAS_RATED, true).apply()
        _activePrompt.value = SmartPromptInfo(SmartPromptType.NONE)
    }

    fun onNeverAskRating() {
        prefs.edit().putBoolean(KEY_DONT_ASK_RATING, true).apply()
        _activePrompt.value = SmartPromptInfo(SmartPromptType.NONE)
    }

    fun getSavedThemeMode(): com.example.viewmodel.FlowThemeMode {
        val saved = prefs.getString(KEY_THEME_MODE, com.example.viewmodel.FlowThemeMode.CIRCADIAN.name)
            ?: com.example.viewmodel.FlowThemeMode.CIRCADIAN.name
        return try {
            com.example.viewmodel.FlowThemeMode.valueOf(saved)
        } catch (e: Exception) {
            com.example.viewmodel.FlowThemeMode.CIRCADIAN
        }
    }

    fun saveThemeMode(mode: com.example.viewmodel.FlowThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun getTimerPalette(): com.example.ui.theme.FlowTimerPalette {
        val saved = prefs.getString(KEY_TIMER_PALETTE, com.example.ui.theme.FlowTimerPalette.SOLAR_AMBER.name)
            ?: com.example.ui.theme.FlowTimerPalette.SOLAR_AMBER.name
        return try {
            com.example.ui.theme.FlowTimerPalette.valueOf(saved)
        } catch (e: Exception) {
            com.example.ui.theme.FlowTimerPalette.SOLAR_AMBER
        }
    }

    fun saveTimerPalette(palette: com.example.ui.theme.FlowTimerPalette) {
        prefs.edit().putString(KEY_TIMER_PALETTE, palette.name).apply()
    }

    fun isIntensityShiftingEnabled(): Boolean {
        return prefs.getBoolean(KEY_INTENSITY_SHIFTING, true)
    }

    fun saveIntensityShiftingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_INTENSITY_SHIFTING, enabled).apply()
    }

    fun getHapticFeedbackEnabled(): Boolean {
        return prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
    }

    fun saveHapticFeedbackEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, enabled).apply()
    }

    fun clearHistoryPreferences() {
        prefs.edit()
            .putInt(KEY_COMPLETED_SESSIONS, 0)
            .putInt(KEY_TOTAL_FOCUS_SECONDS, 0)
            .apply()
    }

    fun getDailyIntention(): String {
        return prefs.getString(KEY_DAILY_INTENTION, "") ?: ""
    }

    fun saveDailyIntention(intention: String) {
        prefs.edit().putString(KEY_DAILY_INTENTION, intention).apply()
    }

    // Daily Focus Reminder Settings
    fun isFocusReminderEnabled(): Boolean {
        return prefs.getBoolean(KEY_FOCUS_REMINDER_ENABLED, true)
    }

    fun saveFocusReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FOCUS_REMINDER_ENABLED, enabled).apply()
    }

    fun getFocusReminderHour(): Int {
        return prefs.getInt(KEY_FOCUS_REMINDER_HOUR, 9) // Default 9:00 AM
    }

    fun getFocusReminderMinute(): Int {
        return prefs.getInt(KEY_FOCUS_REMINDER_MINUTE, 0)
    }

    fun saveFocusReminderTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_FOCUS_REMINDER_HOUR, hour)
            .putInt(KEY_FOCUS_REMINDER_MINUTE, minute)
            .apply()
    }

    // Daily Reflection Reminder Settings
    fun isReflectionReminderEnabled(): Boolean {
        return prefs.getBoolean(KEY_REFLECTION_REMINDER_ENABLED, true)
    }

    fun saveReflectionReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REFLECTION_REMINDER_ENABLED, enabled).apply()
    }

    fun getReflectionReminderHour(): Int {
        return prefs.getInt(KEY_REFLECTION_REMINDER_HOUR, 20) // Default 8:30 PM (20:30)
    }

    fun getReflectionReminderMinute(): Int {
        return prefs.getInt(KEY_REFLECTION_REMINDER_MINUTE, 30)
    }

    fun saveReflectionReminderTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_REFLECTION_REMINDER_HOUR, hour)
            .putInt(KEY_REFLECTION_REMINDER_MINUTE, minute)
            .apply()
    }
}
