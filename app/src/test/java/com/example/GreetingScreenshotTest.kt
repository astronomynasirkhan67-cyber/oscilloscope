package com.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.ChannelConfig
import com.example.model.ChannelId
import com.example.model.ConnectionState
import com.example.model.RunState
import com.example.model.WaveformFrame
import com.example.ui.components.OscilloscopeDisplay
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun oscilloscope_screen_screenshot() {
        val testCh1 = FloatArray(100) { i ->
            (1.65f + 1.2f * kotlin.math.sin(i * 0.2f))
        }
        val testCh2 = FloatArray(100) { i ->
            (1.65f + 0.8f * kotlin.math.cos(i * 0.2f))
        }

        composeTestRule.setContent {
            MyApplicationTheme {
                OscilloscopeDisplay(
                    frame = WaveformFrame(
                        ch1Voltages = testCh1,
                        ch2Voltages = testCh2,
                        samplingRateHz = 100000f,
                        isTriggered = true
                    ),
                    ch1Config = ChannelConfig(channel = ChannelId.CH1, voltsPerDiv = 1.0f),
                    ch2Config = ChannelConfig(channel = ChannelId.CH2, voltsPerDiv = 1.0f),
                    timePerDivSec = 1e-3f,
                    triggerLevelVolts = 1.65f,
                    isTriggered = true,
                    runState = RunState.RUN,
                    connectionState = ConnectionState.ReceivingData(sampleCount = 100, fps = 30f, packetLossRate = 0f),
                    onTriggerLevelChange = {},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/oscilloscope.png")
    }
}

