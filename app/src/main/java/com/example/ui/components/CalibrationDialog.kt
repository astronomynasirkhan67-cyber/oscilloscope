package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChannelConfig
import com.example.ui.theme.ScopeCh1Yellow
import com.example.ui.theme.ScopeCh2Cyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import java.util.Locale

@Composable
fun CalibrationDialog(
    ch1Config: ChannelConfig,
    ch2Config: ChannelConfig,
    samplingRateHz: Float,
    onSaveCh1: (dividerRatio: Float, calMultiplier: Float, offsetVoltage: Float) -> Unit,
    onSaveCh2: (dividerRatio: Float, calMultiplier: Float, offsetVoltage: Float) -> Unit,
    onSaveSamplingRate: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedChannelTab by remember { mutableIntStateOf(0) } // 0: CH1, 1: CH2, 2: Sampling Rate

    var ch1Divider by remember { mutableStateOf(String.format(Locale.US, "%.2f", ch1Config.dividerRatio)) }
    var ch1Multiplier by remember { mutableStateOf(String.format(Locale.US, "%.4f", ch1Config.calMultiplier)) }
    var ch1Offset by remember { mutableStateOf(String.format(Locale.US, "%.3f", ch1Config.offsetVoltage)) }

    var ch2Divider by remember { mutableStateOf(String.format(Locale.US, "%.2f", ch2Config.dividerRatio)) }
    var ch2Multiplier by remember { mutableStateOf(String.format(Locale.US, "%.4f", ch2Config.calMultiplier)) }
    var ch2Offset by remember { mutableStateOf(String.format(Locale.US, "%.3f", ch2Config.offsetVoltage)) }

    var sampleRateText by remember { mutableStateOf(samplingRateHz.toLong().toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("calibration_dialog"),
        containerColor = SurfaceDark,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = ScopeCh1Yellow
                    )
                    Text(
                        text = "Hardware Calibration",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedChannelTab,
                    containerColor = Color.Transparent,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedChannelTab]),
                            color = when (selectedChannelTab) {
                                0 -> ScopeCh1Yellow
                                1 -> ScopeCh2Cyan
                                else -> MaterialTheme.colorScheme.primary
                            }
                        )
                    },
                    divider = {}
                ) {
                    Tab(
                        selected = selectedChannelTab == 0,
                        onClick = { selectedChannelTab = 0 },
                        text = { Text("CH1 (GPIO34)", color = ScopeCh1Yellow, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedChannelTab == 1,
                        onClick = { selectedChannelTab = 1 },
                        text = { Text("CH2 (GPIO35)", color = ScopeCh2Cyan, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedChannelTab == 2,
                        onClick = { selectedChannelTab = 2 },
                        text = { Text("Sampling Rate", color = Color.White) }
                    )
                }

                when (selectedChannelTab) {
                    0 -> ChannelCalFields(
                        divider = ch1Divider,
                        onDividerChange = { ch1Divider = it },
                        multiplier = ch1Multiplier,
                        onMultiplierChange = { ch1Multiplier = it },
                        offset = ch1Offset,
                        onOffsetChange = { ch1Offset = it },
                        accentColor = ScopeCh1Yellow,
                        onPreset1X = { ch1Divider = "1.00" },
                        onPreset10X = { ch1Divider = "10.00" }
                    )
                    1 -> ChannelCalFields(
                        divider = ch2Divider,
                        onDividerChange = { ch2Divider = it },
                        multiplier = ch2Multiplier,
                        onMultiplierChange = { ch2Multiplier = it },
                        offset = ch2Offset,
                        onOffsetChange = { ch2Offset = it },
                        accentColor = ScopeCh2Cyan,
                        onPreset1X = { ch2Divider = "1.00" },
                        onPreset10X = { ch2Divider = "10.00" }
                    )
                    2 -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "ADC Sampling Rate (Hz)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Configured ESP32 internal sampling clock. Used to calculate accurate timebase, frequency, period, and FFT frequency bins.",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                        OutlinedTextField(
                            value = sampleRateText,
                            onValueChange = { sampleRateText = it },
                            label = { Text("Rate in Hz") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFF475569)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PresetPill("50 kHz") { sampleRateText = "50000" }
                            PresetPill("100 kHz") { sampleRateText = "100000" }
                            PresetPill("200 kHz") { sampleRateText = "200000" }
                        }
                    }
                }

                Surface(
                    color = SurfaceVariantDark,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "V = (((ADC / 4095) * 3.3V * Divider) + Offset) * CalMultiplier",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Save CH1
                    val d1 = ch1Divider.toFloatOrNull() ?: ch1Config.dividerRatio
                    val m1 = ch1Multiplier.toFloatOrNull() ?: ch1Config.calMultiplier
                    val o1 = ch1Offset.toFloatOrNull() ?: ch1Config.offsetVoltage
                    onSaveCh1(d1, m1, o1)

                    // Save CH2
                    val d2 = ch2Divider.toFloatOrNull() ?: ch2Config.dividerRatio
                    val m2 = ch2Multiplier.toFloatOrNull() ?: ch2Config.calMultiplier
                    val o2 = ch2Offset.toFloatOrNull() ?: ch2Config.offsetVoltage
                    onSaveCh2(d2, m2, o2)

                    // Save Sampling Rate
                    val sr = sampleRateText.toFloatOrNull() ?: samplingRateHz
                    onSaveSamplingRate(sr)

                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("Save Settings")
            }
        }
    )
}

@Composable
private fun ChannelCalFields(
    divider: String,
    onDividerChange: (String) -> Unit,
    multiplier: String,
    onMultiplierChange: (String) -> Unit,
    offset: String,
    onOffsetChange: (String) -> Unit,
    accentColor: Color,
    onPreset1X: () -> Unit,
    onPreset10X: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Voltage Divider Ratio
        Text(
            text = "Voltage Divider Ratio (e.g. 1.0 for direct, 10.0 for 10:1 probe)",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = divider,
                onValueChange = onDividerChange,
                label = { Text("Divider Ratio") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = Color(0xFF475569)
                ),
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                PresetPill("1X", onPreset1X)
                PresetPill("10X", onPreset10X)
            }
        }

        // Calibration Multiplier
        Text(
            text = "Calibration Multiplier (Trim factor, default 1.000)",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        OutlinedTextField(
            value = multiplier,
            onValueChange = onMultiplierChange,
            label = { Text("Multiplier") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color(0xFF475569)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Zero Offset Voltage
        Text(
            text = "Voltage Offset in Volts (e.g. 0.000 V)",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        OutlinedTextField(
            value = offset,
            onValueChange = onOffsetChange,
            label = { Text("Offset (V)") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Color(0xFF475569)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PresetPill(text: String, onClick: () -> Unit) {
    Surface(
        color = SurfaceVariantDark,
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
        modifier = Modifier.height(36.dp)
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
        ) {
            Text(text, fontSize = 11.sp, color = Color.White)
        }
    }
}
