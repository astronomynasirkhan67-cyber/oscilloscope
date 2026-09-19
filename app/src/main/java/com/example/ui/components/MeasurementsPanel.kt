package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChannelConfig
import com.example.model.ChannelId
import com.example.model.ChannelMeasurements
import com.example.ui.theme.ScopeCh1Yellow
import com.example.ui.theme.ScopeCh2Cyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import java.util.Locale

@Composable
fun MeasurementsPanel(
    ch1Measurements: ChannelMeasurements,
    ch2Measurements: ChannelMeasurements,
    ch1Config: ChannelConfig,
    ch2Config: ChannelConfig,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("measurements_panel"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (ch1Config.enabled) {
            ChannelMeasurementCard(
                title = "CH1 (GPIO34)",
                color = ScopeCh1Yellow,
                measurements = ch1Measurements,
                modifier = Modifier.weight(1f)
            )
        }
        if (ch2Config.enabled) {
            ChannelMeasurementCard(
                title = "CH2 (GPIO35)",
                color = ScopeCh2Cyan,
                measurements = ch2Measurements,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ChannelMeasurementCard(
    title: String,
    color: Color,
    measurements: ChannelMeasurements,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(SurfaceDark, RoundedCornerShape(8.dp))
            .border(1.dp, SurfaceVariantDark, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Header with channel accent
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(14.dp)
                        .background(color, RoundedCornerShape(2.dp))
                )
                Text(
                    text = title,
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Measurement Grid (2 rows x 3 columns)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MeasurementItem(label = "Vpp", value = formatVolt(measurements.vPp))
                MeasurementItem(label = "Vmax", value = formatVolt(measurements.vMax))
                MeasurementItem(label = "Vmin", value = formatVolt(measurements.vMin))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MeasurementItem(label = "RMS", value = formatVolt(measurements.rms))
                MeasurementItem(label = "Freq", value = formatFrequency(measurements.frequencyHz))
                MeasurementItem(label = "Period", value = formatPeriod(measurements.periodSec))
            }
        }
    }
}

@Composable
private fun MeasurementItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            color = Color(0xFF64748B),
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = Color(0xFFF1F5F9),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun formatVolt(v: Float): String {
    val absV = kotlin.math.abs(v)
    return when {
        absV >= 1.0f -> String.format(Locale.US, "%.2fV", v)
        absV >= 0.001f -> String.format(Locale.US, "%.0fmV", v * 1000f)
        else -> "0.0V"
    }
}

private fun formatFrequency(hz: Float): String {
    return when {
        hz <= 0f -> "-- Hz"
        hz >= 1e6f -> String.format(Locale.US, "%.2fMHz", hz / 1e6f)
        hz >= 1e3f -> String.format(Locale.US, "%.2fkHz", hz / 1e3f)
        else -> String.format(Locale.US, "%.1fHz", hz)
    }
}

private fun formatPeriod(sec: Float): String {
    return when {
        sec <= 0f -> "-- s"
        sec >= 1.0f -> String.format(Locale.US, "%.2fs", sec)
        sec >= 1e-3f -> String.format(Locale.US, "%.2fms", sec * 1e3f)
        sec >= 1e-6f -> String.format(Locale.US, "%.1fµs", sec * 1e6f)
        else -> String.format(Locale.US, "%.0fns", sec * 1e9f)
    }
}
