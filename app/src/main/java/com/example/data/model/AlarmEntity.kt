package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar
import java.util.TimeZone

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val label: String = "Alarma",
    val daysOfWeek: String = "", // "2,3,4,5,6" (Lun-Vie) o vacío para "Una sola vez"
    val vibrate: Boolean = true,
    val snoozeMinutes: Int = 10,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getFormattedTime(): Pair<String, String> {
        val amPm = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val formattedHour = String.format("%02d", displayHour)
        val formattedMin = String.format("%02d", minute)
        return Pair("$formattedHour:$formattedMin", amPm)
    }

    fun getDaysDescription(): String {
        if (daysOfWeek.isBlank()) return "Una sola vez"
        val days = daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.sorted()
        return when {
            days.size == 7 -> "Todos los días"
            days == listOf(2, 3, 4, 5, 6) -> "Lun a Vie"
            days == listOf(2, 3, 4, 5) -> "Lun a Jue"
            days == listOf(1, 7) -> "Fines de semana"
            else -> {
                val dayNames = mapOf(
                    1 to "Dom", 2 to "Lun", 3 to "Mar", 4 to "Mié",
                    5 to "Jue", 6 to "Vie", 7 to "Sáb"
                )
                days.mapNotNull { dayNames[it] }.joinToString(", ")
            }
        }
    }

    fun getNextTriggerMillis(): Long {
        val dominicanTz = TimeZone.getTimeZone("America/Santo_Domingo")
        val now = Calendar.getInstance(dominicanTz)
        val target = Calendar.getInstance(dominicanTz).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val days = if (daysOfWeek.isNotBlank()) {
            daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
        } else {
            emptyList()
        }

        if (days.isEmpty()) {
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        } else {
            var daysToAdd = 0
            while (daysToAdd < 8) {
                val candidate = (target.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, daysToAdd)
                }
                val candidateDayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
                if (days.contains(candidateDayOfWeek)) {
                    if (candidate.timeInMillis > now.timeInMillis) {
                        return candidate.timeInMillis
                    }
                }
                daysToAdd++
            }
            return target.timeInMillis
        }
    }
}
