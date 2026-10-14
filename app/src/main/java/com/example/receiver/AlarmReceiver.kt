package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.AlarmApplication
import com.example.scheduler.AlarmScheduler
import com.example.service.AlarmRingingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_ALARM_LABEL = "extra_alarm_label"
        const val EXTRA_ALARM_HOUR = "extra_alarm_hour"
        const val EXTRA_ALARM_MINUTE = "extra_alarm_minute"
        const val EXTRA_ALARM_VIBRATE = "extra_alarm_vibrate"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("AlarmReceiver", "Received intent action: $action")

        if (Intent.ACTION_BOOT_COMPLETED == action) {
            // Device rebooted -> reschedule all active alarms from Room database
            rescheduleAllAlarms(context)
            return
        }

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val label = intent.getStringExtra(EXTRA_ALARM_LABEL) ?: "Alarm"
        val hour = intent.getIntExtra(EXTRA_ALARM_HOUR, 0)
        val minute = intent.getIntExtra(EXTRA_ALARM_MINUTE, 0)
        val vibrate = intent.getBooleanExtra(EXTRA_ALARM_VIBRATE, true)

        // Start ringing foreground service
        val serviceIntent = Intent(context, AlarmRingingService::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_LABEL, label)
            putExtra(EXTRA_ALARM_HOUR, hour)
            putExtra(EXTRA_ALARM_MINUTE, minute)
            putExtra(EXTRA_ALARM_VIBRATE, vibrate)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // Handle alarm repeat vs one-off
        if (alarmId > 0) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = com.example.data.local.AlarmDatabase.getInstance(context)
                    val alarm = db.alarmDao().getAlarmById(alarmId)
                    if (alarm != null) {
                        if (alarm.daysOfWeek.isBlank()) {
                            // Single time alarm: disable after firing
                            db.alarmDao().updateAlarmStatus(alarmId, false)
                        } else {
                            // Repeating alarm: schedule next occurrence
                            val scheduler = AlarmScheduler(context)
                            scheduler.schedule(alarm)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AlarmReceiver", "Error updating alarm status after trigger", e)
                }
            }
        }
    }

    private fun rescheduleAllAlarms(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = com.example.data.local.AlarmDatabase.getInstance(context)
                val enabledAlarms = db.alarmDao().getEnabledAlarms()
                val scheduler = AlarmScheduler(context)
                for (alarm in enabledAlarms) {
                    scheduler.schedule(alarm)
                }
                Log.d("AlarmReceiver", "Rescheduled ${enabledAlarms.size} alarms after reboot")
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error rescheduling alarms after reboot", e)
            }
        }
    }
}
