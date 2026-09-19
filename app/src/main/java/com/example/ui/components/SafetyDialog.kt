package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import com.example.ui.theme.ScopeStopRed
import com.example.ui.theme.ScopeWaitAmber
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark

@Composable
fun SafetyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("safety_dialog"),
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
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = ScopeStopRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "ELECTRICAL SAFETY",
                        color = ScopeStopRed,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Warning Banner
                Surface(
                    color = Color(0x33FF1744),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, ScopeStopRed),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "WARNING",
                            color = ScopeStopRed,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Never connect 220V AC mains directly to the ESP32.",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Safety Rules
                SafetyRuleItem(
                    title = "ADC Voltage Limits",
                    description = "Never exceed the safe electrical input range of the ESP32 ADC (0.0 V to 3.3 V maximum hardware rating). Voltages above 3.3V or negative voltages will permanently destroy the microcontroller."
                )

                SafetyRuleItem(
                    title = "Conditioning & Dividers",
                    description = "Use a correctly designed voltage divider, clamp diodes, and protection circuit for any signal exceeding 3.3V peak."
                )

                SafetyRuleItem(
                    title = "Galvanic Isolation",
                    description = "Use proper optical or transformer isolation when measuring mains or other dangerous/high-voltage circuits."
                )

                SafetyRuleItem(
                    title = "Shared Grounds",
                    description = "CH1 and CH2 are not electrically isolated unless isolated hardware is specifically added. Both share the ESP32 system GND."
                )

                Surface(
                    color = SurfaceVariantDark,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Expected ESP32 ADC range: 0–3.3 V maximum. The app utilizes software calibration multipliers and divider ratios to scale the readings to the measured circuit voltage.",
                        color = ScopeWaitAmber,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ScopeStopRed),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text("I Understand", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun SafetyRuleItem(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "• $title",
            color = Color(0xFFF1F5F9),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = description,
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
