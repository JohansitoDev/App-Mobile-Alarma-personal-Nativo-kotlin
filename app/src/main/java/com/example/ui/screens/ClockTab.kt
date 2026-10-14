package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoralRed
import com.example.ui.theme.PurpleAccent
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ClockTab() {
    val dominicanTimeZone = remember { TimeZone.getTimeZone("America/Santo_Domingo") }
    val esLocale = remember { Locale("es", "DO") }

    var currentTime by remember { mutableStateOf(Date()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Date()
            delay(1000)
        }
    }

    val timeFormat = remember {
        SimpleDateFormat("hh:mm:ss", esLocale).apply { timeZone = dominicanTimeZone }
    }
    val amPmFormat = remember {
        SimpleDateFormat("a", esLocale).apply { timeZone = dominicanTimeZone }
    }
    val dateFormat = remember {
        SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", esLocale).apply { timeZone = dominicanTimeZone }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .testTag("clock_tab_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Título Principal
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "República Dominicana",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = CoralRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Santo Domingo • Hora Oficial AST (UTC-4)",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Reloj Analógico estilizado para República Dominicana
        item {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                DominicanAnalogClockFace(date = currentTime, timeZone = dominicanTimeZone)
            }
        }

        // Hora Digital de República Dominicana
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = timeFormat.format(currentTime),
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = amPmFormat.format(currentTime).uppercase(esLocale),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PurpleAccent,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateFormat.format(currentTime).replaceFirstChar { it.uppercase(esLocale) },
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Tarjeta Informativa de la Zona Horaria Dominicana
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(PurpleAccent.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = PurpleAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Zona Horaria Oficial",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "America/Santo_Domingo",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CoralRed.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "UTC -4",
                                color = CoralRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DominicanTimeStat(label = "Estándar", value = "AST")
                        DominicanTimeStat(label = "Horario Verano", value = "No aplica")
                        DominicanTimeStat(label = "Estado", value = "Sincronizado")
                    }
                }
            }
        }

        // Regiones de República Dominicana (Todas comparten la misma hora oficial)
        item {
            Text(
                text = "Regiones del País",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        val dominicanRegions = listOf(
            "Santo Domingo (Distrito Nacional)" to "Sede de Gobierno",
            "Santiago de los Caballeros" to "Región del Cibao",
            "Punta Cana / Bávaro" to "Región Este",
            "Puerto Plata" to "Costa Norte",
            "La Romana" to "Costa Sureste"
        )

        items(dominicanRegions.size) { index ->
            val (city, region) = dominicanRegions[index]
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = city,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = region,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = timeFormat.format(currentTime) + " " + amPmFormat.format(currentTime).uppercase(esLocale),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleAccent
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun DominicanTimeStat(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun DominicanAnalogClockFace(date: Date, timeZone: TimeZone) {
    val cal = Calendar.getInstance(timeZone).apply { time = date }
    val hours = cal.get(Calendar.HOUR)
    val minutes = cal.get(Calendar.MINUTE)
    val seconds = cal.get(Calendar.SECOND)

    val dialColor = MaterialTheme.colorScheme.surfaceVariant
    val hourHandColor = MaterialTheme.colorScheme.onSurface
    val secondHandColor = CoralRed

    Canvas(modifier = Modifier.size(180.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.width / 2

        // Dial
        drawCircle(color = dialColor, radius = radius)

        // Marcas horarias
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30).toDouble())
            val start = Offset(
                (center.x + (radius - 12.dp.toPx()) * sin(angle)).toFloat(),
                (center.y - (radius - 12.dp.toPx()) * cos(angle)).toFloat()
            )
            val end = Offset(
                (center.x + (radius - 4.dp.toPx()) * sin(angle)).toFloat(),
                (center.y - (radius - 4.dp.toPx()) * cos(angle)).toFloat()
            )
            drawLine(
                color = hourHandColor.copy(alpha = 0.45f),
                start = start,
                end = end,
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Manecilla de Horas
        val hourAngle = Math.toRadians(((hours + minutes / 60f) * 30).toDouble())
        val hourHandEnd = Offset(
            (center.x + (radius * 0.5f) * sin(hourAngle)).toFloat(),
            (center.y - (radius * 0.5f) * cos(hourAngle)).toFloat()
        )
        drawLine(
            color = hourHandColor,
            start = center,
            end = hourHandEnd,
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Manecilla de Minutos
        val minuteAngle = Math.toRadians((minutes * 6).toDouble())
        val minuteHandEnd = Offset(
            (center.x + (radius * 0.72f) * sin(minuteAngle)).toFloat(),
            (center.y - (radius * 0.72f) * cos(minuteAngle)).toFloat()
        )
        drawLine(
            color = PurpleAccent,
            start = center,
            end = minuteHandEnd,
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Manecilla de Segundos
        val secondAngle = Math.toRadians((seconds * 6).toDouble())
        val secondHandEnd = Offset(
            (center.x + (radius * 0.85f) * sin(secondAngle)).toFloat(),
            (center.y - (radius * 0.85f) * cos(secondAngle)).toFloat()
        )
        drawLine(
            color = secondHandColor,
            start = center,
            end = secondHandEnd,
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Punto central
        drawCircle(color = secondHandColor, radius = 4.dp.toPx(), center = center)
    }
}
