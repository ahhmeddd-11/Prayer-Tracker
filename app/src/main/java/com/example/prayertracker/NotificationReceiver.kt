package com.example.prayertracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Prayer Reminder"
        val body = intent.getStringExtra("body") ?: ""
        val isAlarm = intent.getBooleanExtra("alarm", false)
        
        // Check if this alarm is still relevant (not too old)
        val scheduledTime = intent.getLongExtra("scheduled_time", 0)
        val now = System.currentTimeMillis()
        
        // If the alarm was scheduled more than 30 minutes ago, skip it
        if (scheduledTime > 0 && (now - scheduledTime) > 30 * 60 * 1000) {
            return // Don't show old alarms
        }
        
        ensureChannel(context)
        val id = System.currentTimeMillis().toInt()

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (isAlarm) {
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
            val fsIntent = Intent(context, AlarmActivity::class.java).apply {
                putExtra("title", title)
            }
            val fsPi = PendingIntent.getActivity(
                context, 77777, fsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setFullScreenIntent(fsPi, true)
            fsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fsIntent)
        }

        NotificationManagerCompat.from(context).notify(id, builder.build())

        // After firing one reminder, schedule the next set based on saved defaults
        scheduleNotificationsForToday(context, getAdhanDefaults(context), getUseAlarm(context))
    }

    companion object {
        const val CHANNEL_ID = "prayer_alerts"
        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= 26) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                    nm.createNotificationChannel(
                        NotificationChannel(
                            CHANNEL_ID,
                            "Prayer Alerts",
                            NotificationManager.IMPORTANCE_HIGH
                        ).apply { description = "Alerts 5 minutes before Adhan" }
                    )
                }
            }
        }
    }
}
