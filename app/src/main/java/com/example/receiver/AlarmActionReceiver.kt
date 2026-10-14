package com.example.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.scheduler.AlarmScheduler
import com.example.service.AlarmRingingService

class AlarmActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISS = "com.example.ACTION_DISMISS"
        const val ACTION_SNOOZE = "com.example.ACTION_SNOOZE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L)
        val label = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_LABEL) ?: "Alarm"

        Log.d("AlarmActionReceiver", "Action received: $action for alarmId: $alarmId")

        // Stop the ringing service
        val stopServiceIntent = Intent(context, AlarmRingingService::class.java)
        context.stopService(stopServiceIntent)

        // Cancel notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(AlarmRingingService.NOTIFICATION_ID)

        if (action == ACTION_SNOOZE) {
            val scheduler = AlarmScheduler(context)
            scheduler.scheduleSnooze(alarmId, label, 10)
        }
    }
}
