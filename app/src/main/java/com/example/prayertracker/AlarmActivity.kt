package com.example.prayertracker

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class AlarmActivity : ComponentActivity() {
    private var player: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            run { window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON) }
        }
        // Ensure volume keys control the alarm stream
        volumeControlStream = AudioManager.STREAM_ALARM
        startSound()
        val title = intent.getStringExtra("title") ?: "Alarm"
        setContent {
            Surface { AlarmScreen(title, onStop = { stopAndFinish() }, onSnooze = { snoozeAndFinish() }) }
        }
    }

    private fun startSound() {
        val selectedSoundType = getSelectedAlarmSound(this)
        val customUri = getCustomAlarmSound(this)
        
        val alarmUri = when {
            selectedSoundType == "custom" && customUri != null -> {
                try {
                    android.net.Uri.parse(customUri)
                } catch (e: Exception) {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                }
            }
            selectedSoundType.startsWith("content://") || selectedSoundType.startsWith("android.resource://") -> {
                try {
                    android.net.Uri.parse(selectedSoundType)
                } catch (e: Exception) {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                }
            }
            else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        player = MediaPlayer().apply {
            try {
                setAudioAttributes(attrs)
                setDataSource(this@AlarmActivity, alarmUri)
                isLooping = true
                prepare()
                start()
            } catch (e: Exception) {
                // Fallback to default alarm if custom sound fails
                val fallbackUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setDataSource(this@AlarmActivity, fallbackUri)
                isLooping = true
                prepare()
                start()
            }
        }
    }

    private fun stopAndFinish() {
        player?.stop(); player?.release(); player = null
        finish()
    }

    private fun snoozeAndFinish() {
        // Snooze 5 minutes by scheduling the receiver again with alarm flag
        val trigger = System.currentTimeMillis() + 5*60*1000
        scheduleAlarm(this, trigger, "Snoozed Alarm", "Time to pray")
        stopAndFinish()
    }
}

@Composable
private fun AlarmScreen(title: String, onStop: () -> Unit, onSnooze: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onStop) { Text("Stop") }
            OutlinedButton(onClick = onSnooze) { Text("Snooze 5 min") }
        }
    }
}
