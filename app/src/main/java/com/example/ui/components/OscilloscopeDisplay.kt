package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChannelConfig
import com.example.model.ConnectionState
import com.example.model.RunState
import com.example.model.WaveformFrame
import com.example.ui.theme.ScopeCh1Yellow
import com.example.ui.theme.ScopeCh1YellowDim
import com.example.ui.theme.ScopeCh2Cyan
import com.example.ui.theme.ScopeCh2CyanDim
import com.example.ui.theme.ScopeGridAxis
import com.example.ui.theme.ScopeGridMajor
import com.example.ui.theme.ScopeGridMinor
import com.example.ui.theme.ScopeRunGreen
import com.example.ui.theme.ScopeScreenBackground
import com.example.ui.theme.ScopeStopRed
import com.example.ui.theme.ScopeTriggerOrange
import com.example.ui.theme.ScopeWaitAmber

private const val HORIZONTAL_DIVS = 10
private const val VERTICAL_DIVS = 8

@Composable
fun OscilloscopeDisplay(
    frame: WaveformFrame,
    ch1Config: ChannelConfig,
    ch2Config: ChannelConfig,
    timePerDivSec: Float,
    triggerLevelVolts: Float,
    isTriggered: Boolean,
    runState: RunState,
    connectionState: ConnectionState,
    onTriggerLevelChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .testTag("oscilloscope_screen")
            .background(ScopeScreenBackground, RoundedCornerShape(8.dp))
            .border(1.dp, ScopeGridAxis, RoundedCornerShape(8.dp))
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        val divWidth = width / HORIZONTAL_DIVS
        val divHeight = height / VERTICAL_DIVS
        val centerY = height / 2.0f

        val dashedPathEffect = remember {
            PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(height, ch1Config.voltsPerDiv, ch1Config.verticalOffsetDiv) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // Dragging vertically adjusts trigger level (or channel offset)
                        val voltsPerPixel = (ch1Config.voltsPerDiv * VERTICAL_DIVS) / height
                        val deltaVolts = -dragAmount.y * voltsPerPixel
                        onTriggerLevelChange(triggerLevelVolts + deltaVolts)
                    }
                }
        ) {
            // 1. Draw Oscilloscope Reticle Grid
            drawGrid(width, height, divWidth, divHeight, centerY)

            // 2. Draw Trigger Level Line (Dashed)
            if (ch1Config.voltsPerDiv > 0f) {
                // Trigger level relative to center
                val trigOffsetY = centerY - ((triggerLevelVolts / ch1Config.voltsPerDiv) + ch1Config.verticalOffsetDiv) * divHeight
                if (trigOffsetY in 0f..height) {
                    drawLine(
                        color = ScopeTriggerOrange.copy(alpha = 0.6f),
                        start = Offset(0f, trigOffsetY),
                        end = Offset(width, trigOffsetY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashedPathEffect
                    )

                    // Right-side Trigger Level Indicator (Orange 'T▶')
                    drawTriggerMarker(width, trigOffsetY)
                }
            }

            // 3. Draw Zero-Ground Level Markers on Left Edge
            if (ch1Config.enabled) {
                val gnd1Y = centerY - (ch1Config.verticalOffsetDiv * divHeight)
                drawChannelGndMarker(gnd1Y, ScopeCh1Yellow, "1")
            }
            if (ch2Config.enabled) {
                val gnd2Y = centerY - (ch2Config.verticalOffsetDiv * divHeight)
                drawChannelGndMarker(gnd2Y, ScopeCh2Cyan, "2")
            }

            // 4. Draw Waveform Traces (Only if real data present)
            if (ch1Config.enabled && frame.ch1Voltages.isNotEmpty()) {
                drawWaveform(
                    voltages = frame.ch1Voltages,
                    width = width,
                    height = height,
                    divHeight = divHeight,
                    centerY = centerY,
                    voltsPerDiv = ch1Config.voltsPerDiv,
                    verticalOffsetDiv = ch1Config.verticalOffsetDiv,
                    traceColor = ScopeCh1Yellow,
                    glowColor = ScopeCh1YellowDim
                )
            }

            if (ch2Config.enabled && frame.ch2Voltages.isNotEmpty()) {
                drawWaveform(
                    voltages = frame.ch2Voltages,
                    width = width,
                    height = height,
                    divHeight = divHeight,
                    centerY = centerY,
                    voltsPerDiv = ch2Config.voltsPerDiv,
                    verticalOffsetDiv = ch2Config.verticalOffsetDiv,
                    traceColor = ScopeCh2Cyan,
                    glowColor = ScopeCh2CyanDim
                )
            }
        }

        // Top Status Bar Overlay on Grid (Professional Test Instrument OSD)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Top Left: Run Mode & Trigger status
            Surface(
                color = ScopeScreenBackground.copy(alpha = 0.85f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = when (runState) {
                        RunState.RUN -> "RUN"
                        RunState.STOP -> "STOP"
                        RunState.SINGLE -> "SINGLE"
                        RunState.FREEZE -> "FREEZE"
                    } + if (isTriggered) " • Trig'd" else " • Auto",
                    color = when (runState) {
                        RunState.RUN -> ScopeRunGreen
                        RunState.STOP -> ScopeStopRed
                        RunState.SINGLE -> ScopeCh2Cyan
                        RunState.FREEZE -> ScopeWaitAmber
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Top Right: Timebase & Trigger level
            Surface(
                color = ScopeScreenBackground.copy(alpha = 0.85f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(
                    text = "M: ${formatTime(timePerDivSec)}  T: ${formatVoltage(triggerLevelVolts)}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Bottom Left: Channel Scales
            Surface(
                color = ScopeScreenBackground.copy(alpha = 0.85f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Text(
                    text = "CH1: ${formatVoltage(ch1Config.voltsPerDiv)}/div  |  CH2: ${formatVoltage(ch2Config.voltsPerDiv)}/div",
                    color = Color(0xFFE2E8F0),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Center Status Banner when not receiving data
            if (connectionState is ConnectionState.WaitingForData) {
                Surface(
                    color = Color(0xDD2A1E00),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ScopeWaitAmber),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        text = "Connected — Waiting for waveform data",
                        color = ScopeWaitAmber,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            } else if (!connectionState.isConnected && connectionState !is ConnectionState.Scanning) {
                Surface(
                    color = Color(0xDD111827),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF374151)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Text(
                        text = "Disconnected — Tap Connect for ESP32-Oscilloscope",
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawGrid(
    width: Float,
    height: Float,
    divWidth: Float,
    divHeight: Float,
    centerY: Float
) {
    val centerX = width / 2.0f

    // Major grid lines
    for (i in 0..HORIZONTAL_DIVS) {
        val x = i * divWidth
        val isCenter = (i == HORIZONTAL_DIVS / 2)
        drawLine(
            color = if (isCenter) ScopeGridAxis else ScopeGridMajor,
            start = Offset(x, 0f),
            end = Offset(x, height),
            strokeWidth = if (isCenter) 1.5.dp.toPx() else 0.8.dp.toPx()
        )
    }

    for (j in 0..VERTICAL_DIVS) {
        val y = j * divHeight
        val isCenter = (j == VERTICAL_DIVS / 2)
        drawLine(
            color = if (isCenter) ScopeGridAxis else ScopeGridMajor,
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = if (isCenter) 1.5.dp.toPx() else 0.8.dp.toPx()
        )
    }

    // Minor graduation ticks on center axes (5 subdivisions per division)
    val tickSize = 3.dp.toPx()
    val subDivX = divWidth / 5.0f
    val subDivY = divHeight / 5.0f

    var currX = 0f
    while (currX <= width) {
        drawLine(
            color = ScopeGridMinor,
            start = Offset(currX, centerY - tickSize),
            end = Offset(currX, centerY + tickSize),
            strokeWidth = 1.dp.toPx()
        )
        currX += subDivX
    }

    var currY = 0f
    while (currY <= height) {
        drawLine(
            color = ScopeGridMinor,
            start = Offset(centerX - tickSize, currY),
            end = Offset(centerX + tickSize, currY),
            strokeWidth = 1.dp.toPx()
        )
        currY += subDivY
    }
}

private fun DrawScope.drawWaveform(
    voltages: FloatArray,
    width: Float,
    height: Float,
    divHeight: Float,
    centerY: Float,
    voltsPerDiv: Float,
    verticalOffsetDiv: Float,
    traceColor: Color,
    glowColor: Color
) {
    val count = voltages.size
    if (count < 2 || voltsPerDiv <= 0f) return

    val path = Path()
    val maxPixels = (width.toInt()).coerceIn(300, 1000)

    if (count <= maxPixels) {
        // Direct rendering when point count is within display pixel resolution
        val dx = width / (count - 1).toFloat()
        val firstY = centerY - ((voltages[0] / voltsPerDiv) + verticalOffsetDiv) * divHeight
        path.moveTo(0f, firstY.coerceIn(-50f, height + 50f))

        for (i in 1 until count) {
            val x = i * dx
            val v = voltages[i]
            val y = centerY - ((v / voltsPerDiv) + verticalOffsetDiv) * divHeight
            path.lineTo(x, y.coerceIn(-50f, height + 50f))
        }
    } else {
        // High-Speed Peak-Detect (Min/Max Envelope) Decimation:
        // Preserves fast transients, peak-to-peak extrema, transitions, and waveform shape
        // while bounding rendered vertices to at most 2 * maxPixels for 60 FPS rendering.
        val numBins = maxPixels
        val dx = width / numBins.toFloat()
        var hasMoved = false

        for (bin in 0 until numBins) {
            val startIdx = (bin.toLong() * count / numBins).toInt()
            val endIdx = (((bin + 1).toLong() * count / numBins).toInt()).coerceIn(startIdx + 1, count)

            var minV = voltages[startIdx]
            var maxV = voltages[startIdx]
            var minIdx = startIdx
            var maxIdx = startIdx

            for (i in startIdx + 1 until endIdx) {
                val v = voltages[i]
                if (v < minV) {
                    minV = v
                    minIdx = i
                }
                if (v > maxV) {
                    maxV = v
                    maxIdx = i
                }
            }

            val x = bin * dx
            val yMin = centerY - ((minV / voltsPerDiv) + verticalOffsetDiv) * divHeight
            val yMax = centerY - ((maxV / voltsPerDiv) + verticalOffsetDiv) * divHeight

            // Preserve temporal transition order within the bin
            val (yFirst, ySecond) = if (minIdx <= maxIdx) {
                Pair(yMin, yMax)
            } else {
                Pair(yMax, yMin)
            }

            if (!hasMoved) {
                path.moveTo(x, yFirst.coerceIn(-50f, height + 50f))
                hasMoved = true
            } else {
                path.lineTo(x, yFirst.coerceIn(-50f, height + 50f))
            }

            if (minV != maxV) {
                path.lineTo(x, ySecond.coerceIn(-50f, height + 50f))
            }
        }
    }

    // Phosphor glow layer
    drawPath(
        path = path,
        color = glowColor,
        style = Stroke(
            width = 4.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )

    // Crisp high-intensity trace
    drawPath(
        path = path,
        color = traceColor,
        style = Stroke(
            width = 1.8.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawTriggerMarker(width: Float, y: Float) {
    val path = Path().apply {
        moveTo(width, y)
        lineTo(width - 12.dp.toPx(), y - 6.dp.toPx())
        lineTo(width - 12.dp.toPx(), y + 6.dp.toPx())
        close()
    }
    drawPath(path, ScopeTriggerOrange)
}

private fun DrawScope.drawChannelGndMarker(y: Float, color: Color, label: String) {
    val path = Path().apply {
        moveTo(0f, y)
        lineTo(12.dp.toPx(), y - 6.dp.toPx())
        lineTo(12.dp.toPx(), y + 6.dp.toPx())
        close()
    }
    drawPath(path, color)
}

fun formatVoltage(volts: Float): String {
    val absV = kotlin.math.abs(volts)
    return when {
        absV >= 1.0f -> String.format(java.util.Locale.US, "%.2f V", volts)
        absV >= 0.001f -> String.format(java.util.Locale.US, "%.0f mV", volts * 1000f)
        else -> "0.00 V"
    }
}

fun formatTime(sec: Float): String {
    return when {
        sec >= 1.0f -> String.format(java.util.Locale.US, "%.2f s", sec)
        sec >= 1e-3f -> String.format(java.util.Locale.US, "%.0f ms", sec * 1e3f)
        else -> String.format(java.util.Locale.US, "%.0f µs", sec * 1e6f)
    }
}
