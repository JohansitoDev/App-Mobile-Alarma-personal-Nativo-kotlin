package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlarmEntity
import com.example.ui.theme.CoralRed
import com.example.ui.theme.PurpleAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditDialog(
    alarm: AlarmEntity?,
    onDismiss: () -> Unit,
    onSave: (id: Long, hour: Int, minute: Int, label: String, daysOfWeek: List<Int>, vibrate: Boolean) -> Unit,
    onTestAlarm: (hour: Int, minute: Int, label: String) -> Unit
) {
    val initialHour24 = alarm?.hour ?: 7
    val initialMinute = alarm?.minute ?: 0
    val initialIsPm = initialHour24 >= 12
    val initialHour12 = when {
        initialHour24 == 0 -> 12
        initialHour24 > 12 -> initialHour24 - 12
        else -> initialHour24
    }

    var hour12 by remember { mutableIntStateOf(initialHour12) }
    var minute by remember { mutableIntStateOf(initialMinute) }
    var isPm by remember { mutableStateOf(initialIsPm) }
    var label by remember { mutableStateOf(alarm?.label ?: "Wake up") }
    var vibrate by remember { mutableStateOf(alarm?.vibrate ?: true) }

    val initialDays = remember {
        alarm?.daysOfWeek?.split(",")?.mapNotNull { it.trim().toIntOrNull() } ?: listOf(2, 3, 4, 5, 6)
    }
    val selectedDays = remember { mutableStateListOf<Int>().apply { addAll(initialDays) } }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("alarm_edit_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (alarm == null) "Nueva Alarma" else "Editar Alarma",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Time Selector (Wheel / Number adjuster)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Hour Adjuster
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(onClick = { hour12 = if (hour12 == 12) 1 else hour12 + 1 }) {
                        Text("▲", fontSize = 18.sp, color = PurpleAccent)
                    }
                    Text(
                        text = String.format("%02d", hour12),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { hour12 = if (hour12 == 1) 12 else hour12 - 1 }) {
                        Text("▼", fontSize = 18.sp, color = PurpleAccent)
                    }
                }

                Text(
                    text = ":",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // Minute Adjuster
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    TextButton(onClick = { minute = (minute + 1) % 60 }) {
                        Text("▲", fontSize = 18.sp, color = PurpleAccent)
                    }
                    Text(
                        text = String.format("%02d", minute),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { minute = if (minute == 0) 59 else minute - 1 }) {
                        Text("▼", fontSize = 18.sp, color = PurpleAccent)
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // AM / PM Toggle
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isPm) PurpleAccent else Color.Transparent)
                            .clickable { isPm = false }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "AM",
                            fontWeight = FontWeight.Bold,
                            color = if (!isPm) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isPm) PurpleAccent else Color.Transparent)
                            .clickable { isPm = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "PM",
                            fontWeight = FontWeight.Bold,
                            color = if (isPm) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Label text field
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Nombre de la alarma") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PurpleAccent,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("alarm_label_input")
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Repeat Days Section
            Text(
                text = "Repetir",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(8.dp))

            val dayLabels = listOf(
                2 to "L", 3 to "M", 4 to "M", 5 to "J", 6 to "V", 7 to "S", 1 to "D"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayLabels.forEach { (dayInt, dayStr) ->
                    val isSelected = selectedDays.contains(dayInt)
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) PurpleAccent else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                if (isSelected) selectedDays.remove(dayInt) else selectedDays.add(dayInt)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayStr,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Preset shortcuts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        selectedDays.clear()
                        selectedDays.addAll(listOf(1, 2, 3, 4, 5, 6, 7))
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Todos los días", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = {
                        selectedDays.clear()
                        selectedDays.addAll(listOf(2, 3, 4, 5, 6))
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Laborables", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = {
                        selectedDays.clear()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Una vez", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Vibration switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Vibración",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = vibrate,
                    onCheckedChange = { vibrate = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = PurpleAccent
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Test Button
            OutlinedButton(
                onClick = {
                    val finalHour = when {
                        isPm && hour12 < 12 -> hour12 + 12
                        !isPm && hour12 == 12 -> 0
                        else -> hour12
                    }
                    onTestAlarm(finalHour, minute, label)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("test_alarm_button"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = CoralRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Probar Alarma Ahora", color = CoralRed)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    val finalHour = when {
                        isPm && hour12 < 12 -> hour12 + 12
                        !isPm && hour12 == 12 -> 0
                        else -> hour12
                    }
                    onSave(alarm?.id ?: 0L, finalHour, minute, label, selectedDays.toList(), vibrate)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_alarm_button"),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
            ) {
                Text("Guardar Alarma", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
