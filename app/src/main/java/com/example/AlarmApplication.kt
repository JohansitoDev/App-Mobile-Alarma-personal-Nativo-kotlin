package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.example.data.local.AlarmDatabase

class AlarmApplication : Application() {

    companion object {
        const val CHANNEL_ID = "alarm_clock_channel"
        const val PUSH_CHANNEL_ID = "ktor_push_notifications"
        lateinit var database: AlarmDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        database = AlarmDatabase.getInstance(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // High-importance Alarm Channel
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val alarmChannel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent alarm notifications with sound and vibration"
                setSound(alarmSound, audioAttributes)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 1000)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
            }

            // Ktor Push & Sync Channel
            val pushChannel = NotificationChannel(
                PUSH_CHANNEL_ID,
                "Ktor Push Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Persistent remote push notifications and morning briefings via Ktor"
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(alarmChannel)
            notificationManager.createNotificationChannel(pushChannel)
        }
    }
}
