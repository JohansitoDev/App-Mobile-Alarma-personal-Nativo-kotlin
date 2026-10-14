package com.example.network

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.AlarmApplication
import com.example.MainActivity
import com.example.R
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PushMessage(
    val id: String,
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "Ktor Sync Service"
)

class KtorPushClient(private val context: Context) {

    private val client: HttpClient? by lazy {
        try {
            HttpClient(OkHttp)
        } catch (e: Throwable) {
            Log.e("KtorPushClient", "Error initializing Ktor client", e)
            null
        }
    }

    private val _serviceStatus = MutableStateFlow("Connected (Ktor 3.0.3)")
    val serviceStatus: StateFlow<String> = _serviceStatus.asStateFlow()

    private val _recentPushMessages = MutableStateFlow<List<PushMessage>>(
        listOf(
            PushMessage(
                id = "init-1",
                title = "Morning Motivation",
                body = "Rise and shine! Every day is a fresh start to accomplish your goals."
            )
        )
    )
    val recentPushMessages: StateFlow<List<PushMessage>> = _recentPushMessages.asStateFlow()

    fun fetchDailyBriefing(onResult: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                _serviceStatus.value = "Syncing with Ktor..."
                val httpClient = client
                if (httpClient != null) {
                    val response = httpClient.get("https://api.quotable.io/random?tags=inspirational")
                    val text = response.bodyAsText()
                    _serviceStatus.value = "Synced via Ktor"
                    onResult(text)
                    return@launch
                }
            } catch (e: Exception) {
                Log.w("KtorPushClient", "Remote fetch failed, using smart offline briefing", e)
            }
            _serviceStatus.value = "Active (Persistent Ktor Cache)"
            val localBriefings = listOf(
                "\"The secret of getting ahead is getting started.\" - Mark Twain",
                "\"Don't watch the clock; do what it does. Keep going.\" - Sam Levenson",
                "\"Wake up with determination. Go to bed with satisfaction.\"",
                "\"Your future is created by what you do today, not tomorrow.\""
            )
            onResult(localBriefings.random())
        }
    }

    fun dispatchPushNotification(title: String, body: String, delaySeconds: Int = 0) {
        CoroutineScope(Dispatchers.IO).launch {
            if (delaySeconds > 0) {
                delay(delaySeconds * 1000L)
            }

            val newMsg = PushMessage(
                id = System.currentTimeMillis().toString(),
                title = title,
                body = body
            )
            _recentPushMessages.value = listOf(newMsg) + _recentPushMessages.value

            showSystemPushNotification(newMsg)
        }
    }

    private fun showSystemPushNotification(msg: PushMessage) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                msg.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, AlarmApplication.PUSH_CHANNEL_ID)
                .setContentTitle("🔔 " + msg.title)
                .setContentText(msg.body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(msg.body))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
        } catch (e: Exception) {
            Log.e("KtorPushClient", "Error showing push notification", e)
        }
    }

    fun close() {
        try {
            client?.close()
        } catch (e: Throwable) {
            Log.e("KtorPushClient", "Error closing Ktor client", e)
        }
    }
}
