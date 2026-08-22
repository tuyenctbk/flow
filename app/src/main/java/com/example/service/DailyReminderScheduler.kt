package com.example.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.FlowAppPreferences
import java.util.Calendar

object DailyReminderScheduler {
    const val CHANNEL_ID_REMINDERS = "flow_daily_reminders"
    const val CHANNEL_NAME_REMINDERS = "Daily Flow & Reflection Reminders"

    const val REQUEST_CODE_FOCUS_REMINDER = 2001
    const val REQUEST_CODE_REFLECTION_REMINDER = 2002
    const val REQUEST_CODE_TEST_NOTIFICATION = 2099

    const val EXTRA_REMINDER_TYPE = "extra_reminder_type"
    const val REMINDER_TYPE_FOCUS = "FOCUS"
    const val REMINDER_TYPE_REFLECTION = "REFLECTION"
    const val REMINDER_TYPE_BREATH = "BREATH"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                CHANNEL_NAME_REMINDERS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily notifications prompting you to focus, reflect, or practice mindful breathing."
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleAllReminders(context: Context) {
        createNotificationChannel(context)
        val prefs = FlowAppPreferences(context)

        // 1. Schedule Focus Session Reminder
        if (prefs.isFocusReminderEnabled()) {
            scheduleSingleReminder(
                context = context,
                requestCode = REQUEST_CODE_FOCUS_REMINDER,
                hour = prefs.getFocusReminderHour(),
                minute = prefs.getFocusReminderMinute(),
                reminderType = REMINDER_TYPE_FOCUS
            )
        } else {
            cancelReminder(context, REQUEST_CODE_FOCUS_REMINDER)
        }

        // 2. Schedule Evening Reflection Reminder
        if (prefs.isReflectionReminderEnabled()) {
            scheduleSingleReminder(
                context = context,
                requestCode = REQUEST_CODE_REFLECTION_REMINDER,
                hour = prefs.getReflectionReminderHour(),
                minute = prefs.getReflectionReminderMinute(),
                reminderType = REMINDER_TYPE_REFLECTION
            )
        } else {
            cancelReminder(context, REQUEST_CODE_REFLECTION_REMINDER)
        }
    }

    fun scheduleSingleReminder(
        context: Context,
        requestCode: Int,
        hour: Int,
        minute: Int,
        reminderType: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = DailyReminderReceiver.ACTION_TRIGGER_DAILY_REMINDER
            putExtra(EXTRA_REMINDER_TYPE, reminderType)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If the time has already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d("DailyReminderScheduler", "Scheduled reminder ($reminderType) at ${calendar.time}")
        } catch (e: SecurityException) {
            // Fallback for Android 12+ if exact alarm permission is restricted
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.w("DailyReminderScheduler", "Exact alarm permission missing, fell back to standard alarm", e)
        } catch (e: Exception) {
            Log.e("DailyReminderScheduler", "Failed to schedule alarm", e)
        }
    }

    fun cancelReminder(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = DailyReminderReceiver.ACTION_TRIGGER_DAILY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("DailyReminderScheduler", "Cancelled reminder with code $requestCode")
        }
    }

    fun triggerTestNotification(context: Context, reminderType: String) {
        createNotificationChannel(context)
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = DailyReminderReceiver.ACTION_TRIGGER_DAILY_REMINDER
            putExtra(EXTRA_REMINDER_TYPE, reminderType)
            putExtra("is_test", true)
        }
        context.sendBroadcast(intent)
    }
}
