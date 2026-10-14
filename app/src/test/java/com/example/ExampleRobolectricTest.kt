package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AlarmEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = AlarmApplication::class)
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Alarms", appName)
  }

  @Test
  fun `alarm entity formats 12 hour time correctly`() {
    val morningAlarm = AlarmEntity(hour = 7, minute = 30)
    val (morningTime, morningAmPm) = morningAlarm.getFormattedTime()
    assertEquals("07:30", morningTime)
    assertEquals("AM", morningAmPm)

    val eveningAlarm = AlarmEntity(hour = 17, minute = 0)
    val (eveningTime, eveningAmPm) = eveningAlarm.getFormattedTime()
    assertEquals("05:00", eveningTime)
    assertEquals("PM", eveningAmPm)
  }

  @Test
  fun `dominican republic timezone is valid and ast utc minus 4`() {
    val tz = TimeZone.getTimeZone("America/Santo_Domingo")
    assertEquals("America/Santo_Domingo", tz.id)
    assertEquals(-4 * 60 * 60 * 1000, tz.rawOffset)
  }

  @Test
  fun `launch MainActivity without crash`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java)
    val activity = controller.setup().get()
    assertNotNull(activity)
  }
}
