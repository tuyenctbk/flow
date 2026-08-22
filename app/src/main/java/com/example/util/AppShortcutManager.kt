package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R

/**
 * Manages Dynamic Android App Shortcuts for Long-Press menu on launcher icon:
 * - Start Flow Focus
 * - Add Journal Reflection
 * - Open Mirror Stats
 */
object AppShortcutManager {
    const val EXTRA_ACTION_SHORTCUT = "com.example.ACTION_SHORTCUT"
    const val SHORTCUT_START_FLOW = "start_flow"
    const val SHORTCUT_ADD_REFLECTION = "add_reflection"
    const val SHORTCUT_OPEN_STATS = "open_stats"

    fun setupShortcuts(context: Context) {
        try {
            val startFlowIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra(EXTRA_ACTION_SHORTCUT, SHORTCUT_START_FLOW)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val shortcutStartFlow = ShortcutInfoCompat.Builder(context, SHORTCUT_START_FLOW)
                .setShortLabel(context.getString(R.string.shortcut_start_flow_short))
                .setLongLabel(context.getString(R.string.shortcut_start_flow_long))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_flow))
                .setIntent(startFlowIntent)
                .build()

            val addReflectionIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra(EXTRA_ACTION_SHORTCUT, SHORTCUT_ADD_REFLECTION)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val shortcutAddReflection = ShortcutInfoCompat.Builder(context, SHORTCUT_ADD_REFLECTION)
                .setShortLabel(context.getString(R.string.shortcut_add_reflection_short))
                .setLongLabel(context.getString(R.string.shortcut_add_reflection_long))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_reflection))
                .setIntent(addReflectionIntent)
                .build()

            val openStatsIntent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                putExtra(EXTRA_ACTION_SHORTCUT, SHORTCUT_OPEN_STATS)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }

            val shortcutOpenStats = ShortcutInfoCompat.Builder(context, SHORTCUT_OPEN_STATS)
                .setShortLabel(context.getString(R.string.shortcut_open_stats_short))
                .setLongLabel(context.getString(R.string.shortcut_open_stats_long))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_stats))
                .setIntent(openStatsIntent)
                .build()

            ShortcutManagerCompat.addDynamicShortcuts(
                context,
                listOf(shortcutStartFlow, shortcutAddReflection, shortcutOpenStats)
            )
        } catch (e: Exception) {
            android.util.Log.e("AppShortcutManager", "Error setting up dynamic shortcuts", e)
        }
    }
}
