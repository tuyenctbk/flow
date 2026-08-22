package com.example.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.FlowAppPreferences
import kotlin.random.Random

class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_TRIGGER_DAILY_REMINDER) return

        val reminderType = intent.getStringExtra(DailyReminderScheduler.EXTRA_REMINDER_TYPE)
            ?: DailyReminderScheduler.REMINDER_TYPE_FOCUS
        val isTest = intent.getBooleanExtra("is_test", false)

        val prefs = FlowAppPreferences(context)

        DailyReminderScheduler.createNotificationChannel(context)

        val (title, body, notificationId) = when (reminderType) {
            DailyReminderScheduler.REMINDER_TYPE_REFLECTION -> {
                val prompts = listOf(
                    "Pause and observe today with clarity.",
                    "Capture one insight or feeling from your practice today.",
                    "How present were you today? Journal a reflection.",
                    "Unwind your mind. A few quiet words restore balance.",
                    "Notice what you learned today before letting the day go."
                )
                val randomPrompt = prompts[Random.nextInt(prompts.size)]
                Triple("🪞 Daily Reflection & Stillness", randomPrompt, NOTIFICATION_ID_REFLECTION)
            }
            DailyReminderScheduler.REMINDER_TYPE_BREATH -> {
                Triple(
                    "🧘 Mindful Breath Reset",
                    "Take 3 conscious breaths. Relax your shoulders and soften your jaw.",
                    NOTIFICATION_ID_BREATH
                )
            }
            else -> {
                val dailyIntention = prefs.getDailyIntention()
                val subtitle = if (dailyIntention.isNotBlank()) {
                    "Intent: \"$dailyIntention\" — Ready to enter flow?"
                } else {
                    val focusPrompts = listOf(
                        "Your daily focus sanctuary awaits. Step into clarity.",
                        "One single intention. Eliminate distractions and flow.",
                        "Cultivate uninterrupted presence for your most important work.",
                        "Step into the stillness of deep work and creativity."
                    )
                    focusPrompts[Random.nextInt(focusPrompts.size)]
                }
                Triple("🎯 Time for Flow & Focus", subtitle, NOTIFICATION_ID_FOCUS)
            }
        }

        // 1. Content Intent (Default Tap opens app to corresponding tab)
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            when (reminderType) {
                DailyReminderScheduler.REMINDER_TYPE_REFLECTION -> putExtra(EXTRA_NAVIGATE_TAB, 1)
                else -> putExtra(EXTRA_NAVIGATE_TAB, 0)
            }
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Action: 25m Focus Flow
        val focusIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION_START_FOCUS, true)
            putExtra(com.example.widget.FlowQuickWidgetProvider.EXTRA_QUICK_DURATION, 25)
            putExtra(com.example.widget.FlowQuickWidgetProvider.EXTRA_AUTO_START, true)
        }
        val focusPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 10,
            focusIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Action: Write Reflection
        val reflectionIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION_OPEN_REFLECTION, true)
            putExtra(EXTRA_NAVIGATE_TAB, 1)
        }
        val reflectionPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 20,
            reflectionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Action: 5m Breath Reset
        val breathIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_ACTION_START_BREATH, true)
            putExtra(com.example.widget.FlowQuickWidgetProvider.EXTRA_QUICK_DURATION, 5)
            putExtra(com.example.widget.FlowQuickWidgetProvider.EXTRA_AUTO_START, true)
        }
        val breathPendingIntent = PendingIntent.getActivity(
            context,
            notificationId + 30,
            breathIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, DailyReminderScheduler.CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setVibrate(longArrayOf(0, 250, 150, 250))
            .setContentIntent(contentPendingIntent)

        if (reminderType == DailyReminderScheduler.REMINDER_TYPE_REFLECTION) {
            builder.addAction(0, "✍️ Write Journal", reflectionPendingIntent)
            builder.addAction(0, "🎯 25m Focus", focusPendingIntent)
        } else {
            builder.addAction(0, "⚡ 25m Deep Flow", focusPendingIntent)
            builder.addAction(0, "🧘 5m Reset", breathPendingIntent)
            builder.addAction(0, "🪞 Reflect", reflectionPendingIntent)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())

        // Re-arm next day alarm if not a test event
        if (!isTest) {
            DailyReminderScheduler.scheduleAllReminders(context)
        }
    }

    companion object {
        const val ACTION_TRIGGER_DAILY_REMINDER = "com.example.service.ACTION_TRIGGER_DAILY_REMINDER"
        const val EXTRA_ACTION_START_FOCUS = "extra_action_start_focus"
        const val EXTRA_ACTION_OPEN_REFLECTION = "extra_action_open_reflection"
        const val EXTRA_ACTION_START_BREATH = "extra_action_start_breath"
        const val EXTRA_NAVIGATE_TAB = "extra_navigate_tab"

        const val NOTIFICATION_ID_FOCUS = 3001
        const val NOTIFICATION_ID_REFLECTION = 3002
        const val NOTIFICATION_ID_BREATH = 3003
    }
}
