package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AlarmViewModel
import com.example.ui.AppTab
import com.example.ui.components.KtorPushDialog
import com.example.ui.screens.AlarmRingingScreen
import com.example.ui.screens.AlarmsTab
import com.example.ui.screens.ClockTab
import com.example.ui.screens.StopwatchTab
import com.example.ui.screens.TimerTab
import com.example.ui.splash.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PurpleAccent

class MainActivity : ComponentActivity() {

    private val viewModel: AlarmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            MyApplicationTheme {
                MainAppRoot(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val isRinging = intent?.getBooleanExtra("EXTRA_IS_RINGING", false) ?: false
        if (isRinging) {
            val alarmId = intent?.getLongExtra("EXTRA_ALARM_ID", -1L) ?: -1L
            val label = intent?.getStringExtra("EXTRA_ALARM_LABEL") ?: "Alarm"
            viewModel.setRingingState(true, alarmId, label)
        }
    }
}

@Composable
fun MainAppRoot(viewModel: AlarmViewModel) {
    var showSplash by remember { mutableStateOf(true) }
    var showKtorDialog by remember { mutableStateOf(false) }

    val selectedTab by viewModel.selectedTab.collectAsState()
    val isRinging by viewModel.isRinging.collectAsState()
    val ringingLabel by viewModel.ringingLabel.collectAsState()

    // Request Notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Crossfade(
        targetState = showSplash,
        label = "SplashCrossfade"
    ) { inSplash ->
        if (inSplash) {
            SplashScreen(
                onSplashFinished = { showSplash = false }
            )
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavigationRail = maxWidth >= 600.dp && maxHeight >= 480.dp
                Scaffold(
                    bottomBar = {
                        if (!useNavigationRail) {
                            AlarmBottomNavigation(
                                selectedTab = selectedTab,
                                onTabSelected = { viewModel.selectTab(it) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useNavigationRail) {
                            AlarmNavigationRail(
                                selectedTab = selectedTab,
                                onTabSelected = { viewModel.selectTab(it) }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                        ) {
                            when (selectedTab) {
                                AppTab.ALARM -> AlarmsTab(
                                    viewModel = viewModel,
                                    onShowKtorPushDialog = { showKtorDialog = true }
                                )
                                AppTab.CLOCK -> ClockTab()
                                AppTab.STOPWATCH -> StopwatchTab(viewModel = viewModel)
                                AppTab.TIMER -> TimerTab(viewModel = viewModel)
                            }
                        }
                    }
                }

                // Ktor Push Simulation Dialog
                if (showKtorDialog) {
                    KtorPushDialog(
                        ktorPushClient = viewModel.ktorPushClient,
                        onDismiss = { showKtorDialog = false }
                    )
                }

                // Fullscreen Alarm Ringing Overlay
                if (isRinging) {
                    AlarmRingingScreen(
                        label = ringingLabel,
                        onDismiss = { viewModel.dismissRingingAlarm() },
                        onSnooze = { viewModel.snoozeRingingAlarm() }
                    )
                    BackHandler {
                        viewModel.dismissRingingAlarm()
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmBottomNavigation(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier.testTag("bottom_nav_bar")
    ) {
        // 1. Pestaña Alarmas
        NavigationBarItem(
            selected = selectedTab == AppTab.ALARM,
            onClick = { onTabSelected(AppTab.ALARM) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == AppTab.ALARM) Icons.Filled.Alarm else Icons.Outlined.Alarm,
                    contentDescription = "Alarmas"
                )
            },
            label = { Text("Alarmas", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PurpleAccent,
                selectedTextColor = PurpleAccent,
                indicatorColor = PurpleAccent.copy(alpha = 0.16f),
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_item_alarm")
        )

        // 2. Pestaña Reloj RD
        NavigationBarItem(
            selected = selectedTab == AppTab.CLOCK,
            onClick = { onTabSelected(AppTab.CLOCK) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == AppTab.CLOCK) Icons.Filled.Schedule else Icons.Outlined.Schedule,
                    contentDescription = "Reloj RD"
                )
            },
            label = { Text("Reloj RD", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PurpleAccent,
                selectedTextColor = PurpleAccent,
                indicatorColor = PurpleAccent.copy(alpha = 0.16f),
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_item_clock")
        )

        // 3. Pestaña Cronómetro
        NavigationBarItem(
            selected = selectedTab == AppTab.STOPWATCH,
            onClick = { onTabSelected(AppTab.STOPWATCH) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == AppTab.STOPWATCH) Icons.Filled.Timer else Icons.Outlined.Timer,
                    contentDescription = "Cronómetro"
                )
            },
            label = { Text("Cronómetro", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PurpleAccent,
                selectedTextColor = PurpleAccent,
                indicatorColor = PurpleAccent.copy(alpha = 0.16f),
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_item_stopwatch")
        )

        // 4. Pestaña Temporizador
        NavigationBarItem(
            selected = selectedTab == AppTab.TIMER,
            onClick = { onTabSelected(AppTab.TIMER) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == AppTab.TIMER) Icons.Filled.HourglassBottom else Icons.Outlined.HourglassEmpty,
                    contentDescription = "Temporizador"
                )
            },
            label = { Text("Temporizador", fontSize = 12.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = PurpleAccent,
                selectedTextColor = PurpleAccent,
                indicatorColor = PurpleAccent.copy(alpha = 0.16f),
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_item_timer")
        )
    }
}

@Composable
private fun AlarmNavigationRail(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("navigation_rail")
    ) {
        NavigationRailItem(
            selected = selectedTab == AppTab.ALARM,
            onClick = { onTabSelected(AppTab.ALARM) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == AppTab.ALARM) Icons.Filled.Alarm else Icons.Outlined.Alarm,
                    contentDescription = "Alarmas"
                )
            },
            label = { Text("Alarmas") },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = PurpleAccent,
                selectedTextColor = PurpleAccent,
                indicatorColor = PurpleAccent.copy(alpha = 0.16f),
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_item_alarm")
        )
        NavigationRailItem(
            selected = selectedTab == AppTab.CLOCK,
            onClick = { onTabSelected(AppTab.CLOCK) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == AppTab.CLOCK) Icons.Filled.Schedule else Icons.Outlined.Schedule,
                    contentDescription = "Reloj RD"
                )
            },
            label = { Text("Reloj RD") },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = PurpleAccent,
                selectedTextColor = PurpleAccent,
                indicatorColor = PurpleAccent.copy(alpha = 0.16f),
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_item_clock")
        )
        NavigationRailItem(
            selected = selectedTab == AppTab.STOPWATCH,
            onClick = { onTabSelected(AppTab.STOPWATCH) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == AppTab.STOPWATCH) Icons.Filled.Timer else Icons.Outlined.Timer,
                    contentDescription = "Cronómetro"
                )
            },
            label = { Text("Cronómetro") },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = PurpleAccent,
                selectedTextColor = PurpleAccent,
                indicatorColor = PurpleAccent.copy(alpha = 0.16f),
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_item_stopwatch")
        )
        NavigationRailItem(
            selected = selectedTab == AppTab.TIMER,
            onClick = { onTabSelected(AppTab.TIMER) },
            icon = {
                Icon(
                    imageVector = if (selectedTab == AppTab.TIMER) Icons.Filled.HourglassBottom else Icons.Outlined.HourglassEmpty,
                    contentDescription = "Temporizador"
                )
            },
            label = { Text("Temporizador") },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = PurpleAccent,
                selectedTextColor = PurpleAccent,
                indicatorColor = PurpleAccent.copy(alpha = 0.16f),
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_item_timer")
        )
    }
}
