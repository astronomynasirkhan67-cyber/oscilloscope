package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.MeasurementEngine
import com.example.engine.TriggerEngine
import com.example.model.TriggerMode
import com.example.model.TriggerSlope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.sin

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Digital Oscilloscope", appName)
    }

    @Test
    fun `test measurement engine with sine wave`() {
        val samplingRate = 100000f
        val freq = 1000f // 1 kHz
        val samples = FloatArray(1000) { i ->
            val t = i / samplingRate
            (1.65f + 1.0f * sin(2.0 * Math.PI * freq * t)).toFloat()
        }

        val result = MeasurementEngine.calculate(samples, samplingRate)
        assertTrue(result.isValid)
        assertEquals(2.65f, result.vMax, 0.05f)
        assertEquals(0.65f, result.vMin, 0.05f)
        assertEquals(2.0f, result.vPp, 0.1f)
        assertEquals(1000f, result.frequencyHz, 50f)
    }

    @Test
    fun `test trigger engine rising edge`() {
        val samples = floatArrayOf(0.5f, 0.8f, 1.2f, 1.8f, 2.2f, 2.0f, 1.5f, 0.9f)
        val trig = TriggerEngine.process(
            voltages = samples,
            triggerLevelVolts = 1.5f,
            slope = TriggerSlope.RISING,
            mode = TriggerMode.NORMAL,
            windowSize = 4
        )
        assertTrue(trig.triggered)
        assertEquals(1, trig.triggerIndex) // Window start centered around rising edge at index 3
    }
}

