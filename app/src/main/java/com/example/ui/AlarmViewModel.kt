package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.AlarmApplication
import com.example.data.model.AlarmEntity
import com.example.network.KtorPushClient
import com.example.network.PushMessage
import com.example.scheduler.AlarmScheduler
import com.example.service.AlarmRingingService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AppTab {
    ALARM, CLOCK, STOPWATCH, TIMER
}

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val alarmDao = com.example.data.local.AlarmDatabase.getInstance(application).alarmDao()
    private val scheduler = AlarmScheduler(application)
    val ktorPushClient = KtorPushClient(application)

    val alarms: StateFlow<List<AlarmEntity>> = alarmDao.getAllAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTab = MutableStateFlow(AppTab.ALARM)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    // Alarm ringing state
    private val _isRinging = MutableStateFlow(false)
    val isRinging: StateFlow<Boolean> = _isRinging.asStateFlow()

    private val _ringingLabel = MutableStateFlow("")
    val ringingLabel: StateFlow<String> = _ringingLabel.asStateFlow()

    private val _ringingAlarmId = MutableStateFlow(-1L)
    val ringingAlarmId: StateFlow<Long> = _ringingAlarmId.asStateFlow()

    // Stopwatch state
    private val _stopwatchMillis = MutableStateFlow(0L)
    val stopwatchMillis: StateFlow<Long> = _stopwatchMillis.asStateFlow()

    private val _isStopwatchRunning = MutableStateFlow(false)
    val isStopwatchRunning: StateFlow<Boolean> = _isStopwatchRunning.asStateFlow()

    private val _stopwatchLaps = MutableStateFlow<List<Long>>(emptyList())
    val stopwatchLaps: StateFlow<List<Long>> = _stopwatchLaps.asStateFlow()

    private var stopwatchJob: Job? = null

    // Timer state
    private val _timerRemainingSeconds = MutableStateFlow(300) // Default 5 mins
    val timerRemainingSeconds: StateFlow<Int> = _timerRemainingSeconds.asStateFlow()

    private val _timerTotalSeconds = MutableStateFlow(300)
    val timerTotalSeconds: StateFlow<Int> = _timerTotalSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private var timerJob: Job? = null

    // Mensaje matutino diario
    private val _dailyBriefing = MutableStateFlow<String?>("Sincronización activa • Hora oficial de República Dominicana (UTC-4)")
    val dailyBriefing: StateFlow<String?> = _dailyBriefing.asStateFlow()

    init {
        seedInitialAlarmsIfEmpty()
        fetchMorningBriefing()
    }

    private fun seedInitialAlarmsIfEmpty() {
        viewModelScope.launch {
            val existing = alarmDao.getEnabledAlarms()
            if (existing.isEmpty()) {
                val sampleAlarms = listOf(
                    AlarmEntity(hour = 5, minute = 0, isEnabled = true, label = "Despertar", daysOfWeek = "1,2,3,4,5,6,7"),
                    AlarmEntity(hour = 7, minute = 0, isEnabled = true, label = "Trabajo", daysOfWeek = "2,3,4,5"),
                    AlarmEntity(hour = 10, minute = 0, isEnabled = false, label = "Reunión", daysOfWeek = ""),
                    AlarmEntity(hour = 13, minute = 0, isEnabled = false, label = "Almuerzo", daysOfWeek = ""),
                    AlarmEntity(hour = 17, minute = 0, isEnabled = false, label = "Gimnasio", daysOfWeek = "")
                )
                sampleAlarms.forEach { alarm ->
                    val id = alarmDao.insertAlarm(alarm)
                    if (alarm.isEnabled) {
                        scheduler.schedule(alarm.copy(id = id))
                    }
                }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun toggleAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            val newStatus = !alarm.isEnabled
            val updated = alarm.copy(isEnabled = newStatus)
            alarmDao.updateAlarm(updated)
            if (newStatus) {
                scheduler.schedule(updated)
            } else {
                scheduler.cancel(alarm.id)
            }
        }
    }

    fun saveAlarm(
        id: Long,
        hour: Int,
        minute: Int,
        label: String,
        daysOfWeek: List<Int>,
        vibrate: Boolean,
        isEnabled: Boolean = true
    ) {
        viewModelScope.launch {
            val daysString = daysOfWeek.sorted().joinToString(",")
            val alarm = AlarmEntity(
                id = if (id > 0) id else 0,
                hour = hour,
                minute = minute,
                label = label.ifBlank { "Alarm" },
                daysOfWeek = daysString,
                vibrate = vibrate,
                isEnabled = isEnabled
            )

            val savedId = if (id > 0) {
                alarmDao.updateAlarm(alarm)
                id
            } else {
                alarmDao.insertAlarm(alarm)
            }

            val scheduledAlarm = alarm.copy(id = savedId)
            if (isEnabled) {
                scheduler.schedule(scheduledAlarm)
            } else {
                scheduler.cancel(savedId)
            }
        }
    }

    fun deleteAlarm(alarm: AlarmEntity) {
        viewModelScope.launch {
            scheduler.cancel(alarm.id)
            alarmDao.deleteAlarm(alarm)
        }
    }

    fun dismissRingingAlarm() {
        val id = _ringingAlarmId.value
        _isRinging.value = false
        _ringingAlarmId.value = -1L
        _ringingLabel.value = ""
        val context = getApplication<Application>()
        val intent = android.content.Intent(context, com.example.receiver.AlarmActionReceiver::class.java).apply {
            action = com.example.receiver.AlarmActionReceiver.ACTION_DISMISS
            putExtra(com.example.receiver.AlarmReceiver.EXTRA_ALARM_ID, id)
        }
        context.sendBroadcast(intent)
    }

    fun snoozeRingingAlarm() {
        val id = _ringingAlarmId.value
        val label = _ringingLabel.value
        _isRinging.value = false
        _ringingAlarmId.value = -1L
        _ringingLabel.value = ""
        val context = getApplication<Application>()
        val intent = android.content.Intent(context, com.example.receiver.AlarmActionReceiver::class.java).apply {
            action = com.example.receiver.AlarmActionReceiver.ACTION_SNOOZE
            putExtra(com.example.receiver.AlarmReceiver.EXTRA_ALARM_ID, id)
            putExtra(com.example.receiver.AlarmReceiver.EXTRA_ALARM_LABEL, label)
        }
        context.sendBroadcast(intent)
    }

    fun setRingingState(ringing: Boolean, alarmId: Long = -1L, label: String = "Alarm") {
        _isRinging.value = ringing
        _ringingAlarmId.value = alarmId
        _ringingLabel.value = label
    }

    // Stopwatch Controls
    fun startStopwatch() {
        if (_isStopwatchRunning.value) return
        _isStopwatchRunning.value = true
        val startTime = System.currentTimeMillis() - _stopwatchMillis.value
        stopwatchJob = viewModelScope.launch {
            while (_isStopwatchRunning.value) {
                _stopwatchMillis.value = System.currentTimeMillis() - startTime
                delay(30)
            }
        }
    }

    fun pauseStopwatch() {
        _isStopwatchRunning.value = false
        stopwatchJob?.cancel()
    }

    fun resetStopwatch() {
        pauseStopwatch()
        _stopwatchMillis.value = 0L
        _stopwatchLaps.value = emptyList()
    }

    fun lapStopwatch() {
        if (_isStopwatchRunning.value && _stopwatchMillis.value > 0) {
            _stopwatchLaps.value = listOf(_stopwatchMillis.value) + _stopwatchLaps.value
        }
    }

    // Timer Controls
    fun setTimerDuration(seconds: Int) {
        if (!_isTimerRunning.value) {
            _timerTotalSeconds.value = seconds
            _timerRemainingSeconds.value = seconds
        }
    }

    fun startTimer() {
        if (_isTimerRunning.value || _timerRemainingSeconds.value <= 0) return
        _isTimerRunning.value = true
        timerJob = viewModelScope.launch {
            while (_isTimerRunning.value && _timerRemainingSeconds.value > 0) {
                delay(1000)
                _timerRemainingSeconds.value -= 1
            }
            if (_timerRemainingSeconds.value <= 0) {
                _isTimerRunning.value = false
                // Trigger push notification on timer completion
                ktorPushClient.dispatchPushNotification(
                    title = "Timer Finished!",
                    body = "Your ${_timerTotalSeconds.value / 60} min countdown has ended."
                )
            }
        }
    }

    fun pauseTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resetTimer() {
        pauseTimer()
        _timerRemainingSeconds.value = _timerTotalSeconds.value
    }

    fun fetchMorningBriefing() {
        ktorPushClient.fetchDailyBriefing { text ->
            _dailyBriefing.value = text
        }
    }

    fun sendTestPushNotification(title: String = "Alarms Push Alert", body: String = "Test notification sent via persistent Ktor service.") {
        ktorPushClient.dispatchPushNotification(title, body)
    }

    fun getNextAlarmCountdown(): String? {
        val activeAlarms = alarms.value.filter { it.isEnabled }
        if (activeAlarms.isEmpty()) return null

        val now = System.currentTimeMillis()
        val nextMillis = activeAlarms.map { it.getNextTriggerMillis() }
            .filter { it > now }
            .minOrNull() ?: return null

        val diffMillis = nextMillis - now
        val hours = diffMillis / (1000 * 60 * 60)
        val minutes = (diffMillis / (1000 * 60)) % 60

        return when {
            hours > 0 -> "Próxima alarma en ${hours}h ${minutes}m"
            minutes > 0 -> "Próxima alarma en ${minutes}m"
            else -> "Próxima alarma en menos de 1m"
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopwatchJob?.cancel()
        timerJob?.cancel()
        ktorPushClient.close()
    }
}
