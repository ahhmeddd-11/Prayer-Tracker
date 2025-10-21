package com.example.prayertracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val times = getAdhanDefaults(context)
        scheduleNotificationsForToday(context, times, getUseAlarm(context))
    }
}
