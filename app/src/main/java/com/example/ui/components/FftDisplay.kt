package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChannelConfig
import com.example.model.FftResult
import com.example.ui.theme.ScopeCh1Yellow
import com.example.ui.theme.ScopeCh1YellowDim
import com.example.ui.theme.ScopeCh2Cyan
import com.example.ui.theme.ScopeCh2CyanDim
import com.example.ui.theme.ScopeGridAxis
import com.example.ui.theme.ScopeGridMajor
import com.example.ui.theme.ScopeScreenBackground
import java.util.Locale
import kotlin.math.max

@Composable
fun FftDisplay(
    fftResult: FftResult,
    ch1Config: ChannelConfig,
    ch2Config: ChannelConfig,
    samplingRateHz: Float,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .testTag("fft_display_screen")
            .background(ScopeScreenBackground, RoundedCornerShape(8.dp))
            .border(1.dp, ScopeGridAxis, RoundedCornerShape(8.dp))
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawFftGrid(width, height)

            val maxMag = max(
                max(fftResult.maxMagnitudeCh1, fftResult.maxMagnitudeCh2),
                0.5f
            )

            if (ch1Config.enabled && fftResult.magnitudesCh1.isNotEmpty()) {
                drawFftSpectrum(
                    mags = fftResult.magnitudesCh1,
                    width = width,
                    height = height,
                    maxMag = maxMag,
                    color = ScopeCh1Yellow,
                    glowColor = ScopeCh1YellowDim
                )
            }

            if (ch2Config.enabled && fftResult.magnitudesCh2.isNotEmpty()) {
                drawFftSpectrum(
                    mags = fftResult.magnitudesCh2,
                    width = width,
                    height = height,
                    maxMag = maxMag,
                    color = ScopeCh2Cyan,
                    glowColor = ScopeCh2CyanDim
                )
            }
        }

        // Overlay readouts
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Header & Dominant Peak Readouts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ScopeScreenBackground.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "FFT SPECTRUM (0 – ${formatFreq(samplingRateHz / 2f)})",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (ch1Config.enabled) {
                        Surface(
                            color = ScopeScreenBackground.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "CH1 Peak: ${formatFreq(fftResult.dominantFreqCh1)}",
                                color = ScopeCh1Yellow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (ch2Config.enabled) {
                        Surface(
                            color = ScopeScreenBackground.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "CH2 Peak: ${formatFreq(fftResult.dominantFreqCh2)}",
                                color = ScopeCh2Cyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Frequency Axis Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "0 Hz",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = formatFreq(samplingRateHz / 4f),
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = formatFreq(samplingRateHz / 2f),
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

private fun DrawScope.drawFftGrid(width: Float, height: Float) {
    val hDivs = 8
    val vDivs = 6
    val dx = width / hDivs
    val dy = height / vDivs

    for (i in 0..hDivs) {
        drawLine(
            color = ScopeGridMajor,
            start = Offset(i * dx, 0f),
            end = Offset(i * dx, height),
            strokeWidth = 0.8.dp.toPx()
        )
    }

    for (j in 0..vDivs) {
        drawLine(
            color = if (j == vDivs) ScopeGridAxis else ScopeGridMajor,
            start = Offset(0f, j * dy),
            end = Offset(width, j * dy),
            strokeWidth = if (j == vDivs) 1.5.dp.toPx() else 0.8.dp.toPx()
        )
    }
}

private fun DrawScope.drawFftSpectrum(
    mags: FloatArray,
    width: Float,
    height: Float,
    maxMag: Float,
    color: Color,
    glowColor: Color
) {
    if (mags.size < 2 || maxMag <= 0f) return

    val path = Path()
    val count = mags.size
    val dx = width / (count - 1).toFloat()
    val bottomY = height - 4f

    val startY = bottomY - (mags[0] / maxMag) * (height - 20f)
    path.moveTo(0f, startY.coerceIn(10f, bottomY))

    for (i in 1 until count) {
        val x = i * dx
        val y = bottomY - (mags[i] / maxMag) * (height - 20f)
        path.lineTo(x, y.coerceIn(10f, bottomY))
    }

    drawPath(
        path = path,
        color = glowColor,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

fun formatFreq(hz: Float): String {
    return when {
        hz >= 1e6f -> String.format(Locale.US, "%.2f MHz", hz / 1e6f)
        hz >= 1e3f -> String.format(Locale.US, "%.2f kHz", hz / 1e3f)
        else -> String.format(Locale.US, "%.1f Hz", hz)
    }
}
