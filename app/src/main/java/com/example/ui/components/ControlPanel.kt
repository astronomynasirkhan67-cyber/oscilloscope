package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.AVAILABLE_TIME_DIVS
import com.example.model.AVAILABLE_VOLT_DIVS
import com.example.model.ChannelConfig
import com.example.model.ChannelId
import com.example.model.DisplayDomain
import com.example.model.RunState
import com.example.model.TriggerMode
import com.example.model.TriggerSlope
import com.example.ui.theme.ScopeCh1Yellow
import com.example.ui.theme.ScopeCh2Cyan
import com.example.ui.theme.ScopeRunGreen
import com.example.ui.theme.ScopeStopRed
import com.example.ui.theme.ScopeTriggerOrange
import com.example.ui.theme.ScopeWaitAmber
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark

@Composable
fun ControlPanel(
    runState: RunState,
    displayDomain: DisplayDomain,
    timePerDivSec: Float,
    horizontalPanDiv: Float,
    ch1Config: ChannelConfig,
    ch2Config: ChannelConfig,
    triggerSource: ChannelId,
    triggerMode: TriggerMode,
    triggerSlope: TriggerSlope,
    triggerLevelVolts: Float,
    onToggleRunStop: () -> Unit,
    onToggleFreeze: () -> Unit,
    onSingleTrigger: () -> Unit,
    onAutoSet: () -> Unit,
    onClear: () -> Unit,
    onSaveWaveform: () -> Unit,
    onDomainChange: (DisplayDomain) -> Unit,
    onTimePerDivChange: (Float) -> Unit,
    onHorizontalPanChange: (Float) -> Unit,
    onCh1EnabledChange: (Boolean) -> Unit,
    onCh2EnabledChange: (Boolean) -> Unit,
    onCh1VoltPerDivChange: (Float) -> Unit,
    onCh2VoltPerDivChange: (Float) -> Unit,
    onCh1OffsetChange: (Float) -> Unit,
    onCh2OffsetChange: (Float) -> Unit,
    onTriggerSourceChange: (ChannelId) -> Unit,
    onTriggerModeChange: (TriggerMode) -> Unit,
    onTriggerSlopeChange: (TriggerSlope) -> Unit,
    onTriggerLevelChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Horizontal/Time, 1: CH1, 2: CH2, 3: Trigger

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(12.dp))
            .border(1.dp, SurfaceVariantDark, RoundedCornerShape(12.dp))
            .padding(10.dp)
            .testTag("control_panel"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Primary Instrument Action Buttons (RUN, STOP, SINGLE, FREEZE, AUTO, FFT, SAVE, CLEAR)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // RUN / STOP Primary Button
            Button(
                onClick = onToggleRunStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (runState == RunState.RUN) ScopeRunGreen else ScopeStopRed
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
                    .testTag("run_stop_button")
            ) {
                Icon(
                    imageVector = if (runState == RunState.RUN) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (runState == RunState.RUN) "RUN" else "STOP",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            // SINGLE Trigger Button
            OutlinedButton(
                onClick = onSingleTrigger,
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("single_button")
            ) {
                Text(
                    text = "SINGLE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScopeCh2Cyan,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // FREEZE / RESUME Button
            OutlinedButton(
                onClick = onToggleFreeze,
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                modifier = Modifier
                    .weight(1.1f)
                    .height(48.dp)
                    .testTag("freeze_button")
            ) {
                Text(
                    text = if (runState == RunState.FREEZE) "RESUME" else "FREEZE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScopeWaitAmber,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // AUTO-SET Button
            OutlinedButton(
                onClick = onAutoSet,
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("autoset_button")
            ) {
                Text(
                    text = "AUTO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // FFT / TIME domain toggle button
            OutlinedButton(
                onClick = {
                    onDomainChange(
                        if (displayDomain == DisplayDomain.TIME) DisplayDomain.FFT else DisplayDomain.TIME
                    )
                },
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("fft_toggle_button")
            ) {
                Icon(
                    imageVector = if (displayDomain == DisplayDomain.TIME) Icons.Default.GraphicEq else Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = ScopeCh2Cyan,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (displayDomain == DisplayDomain.TIME) "FFT" else "TIME",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScopeCh2Cyan,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }

            // SAVE Waveform Button
            IconButton(
                onClick = onSaveWaveform,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("save_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = "Save Waveform",
                    tint = ScopeCh1Yellow
                )
            }

            // CLEAR Buffer Button
            IconButton(
                onClick = onClear,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("clear_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Clear Waveform",
                    tint = Color(0xFF94A3B8)
                )
            }
        }

        // Row 2: Secondary Controls Segmented Tabs
        val tabTitles = listOf("Timebase", "CH1", "CH2", "Trigger")
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = when (selectedTab) {
                        1 -> ScopeCh1Yellow
                        2 -> ScopeCh2Cyan
                        3 -> ScopeTriggerOrange
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            },
            divider = {}
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    modifier = Modifier.height(42.dp),
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                selectedTab == index && index == 1 -> ScopeCh1Yellow
                                selectedTab == index && index == 2 -> ScopeCh2Cyan
                                selectedTab == index && index == 3 -> ScopeTriggerOrange
                                selectedTab == index -> MaterialTheme.colorScheme.primary
                                else -> Color(0xFF94A3B8)
                            }
                        )
                    }
                )
            }
        }

        // Active Tab Content Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceVariantDark.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            when (selectedTab) {
                0 -> TimebaseControls(
                    timePerDivSec = timePerDivSec,
                    horizontalPanDiv = horizontalPanDiv,
                    onTimePerDivChange = onTimePerDivChange,
                    onHorizontalPanChange = onHorizontalPanChange
                )
                1 -> ChannelControlsTab(
                    channel = ChannelId.CH1,
                    config = ch1Config,
                    accentColor = ScopeCh1Yellow,
                    onEnabledChange = onCh1EnabledChange,
                    onVoltPerDivChange = onCh1VoltPerDivChange,
                    onOffsetChange = onCh1OffsetChange
                )
                2 -> ChannelControlsTab(
                    channel = ChannelId.CH2,
                    config = ch2Config,
                    accentColor = ScopeCh2Cyan,
                    onEnabledChange = onCh2EnabledChange,
                    onVoltPerDivChange = onCh2VoltPerDivChange,
                    onOffsetChange = onCh2OffsetChange
                )
                3 -> TriggerControlsTab(
                    source = triggerSource,
                    mode = triggerMode,
                    slope = triggerSlope,
                    levelVolts = triggerLevelVolts,
                    onSourceChange = onTriggerSourceChange,
                    onModeChange = onTriggerModeChange,
                    onSlopeChange = onTriggerSlopeChange,
                    onLevelChange = onTriggerLevelChange
                )
            }
        }
    }
}

@Composable
private fun TimebaseControls(
    timePerDivSec: Float,
    horizontalPanDiv: Float,
    onTimePerDivChange: (Float) -> Unit,
    onHorizontalPanChange: (Float) -> Unit
) {
    val currentTimeIndex = AVAILABLE_TIME_DIVS.indexOfFirst {
        kotlin.math.abs(it.seconds - timePerDivSec) < 1e-7f
    }.let { if (it >= 0) it else 3 }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time/Div Stepper
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "TIME / DIV",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (currentTimeIndex > 0) {
                            onTimePerDivChange(AVAILABLE_TIME_DIVS[currentTimeIndex - 1].seconds)
                        }
                    },
                    enabled = currentTimeIndex > 0,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Remove, "Faster timebase", tint = Color.White)
                }

                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceVariantDark),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = AVAILABLE_TIME_DIVS[currentTimeIndex].label,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (currentTimeIndex < AVAILABLE_TIME_DIVS.size - 1) {
                            onTimePerDivChange(AVAILABLE_TIME_DIVS[currentTimeIndex + 1].seconds)
                        }
                    },
                    enabled = currentTimeIndex < AVAILABLE_TIME_DIVS.size - 1,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Add, "Slower timebase", tint = Color.White)
                }
            }
        }

        // Horizontal Pan Stepper
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "HORIZ PAN",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onHorizontalPanChange((horizontalPanDiv - 0.5f).coerceIn(-5f, 5f)) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Pan Left", tint = Color.White)
                }

                Text(
                    text = String.format(java.util.Locale.US, "%.1f div", horizontalPanDiv),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                IconButton(
                    onClick = { onHorizontalPanChange((horizontalPanDiv + 0.5f).coerceIn(-5f, 5f)) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "Pan Right", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ChannelControlsTab(
    channel: ChannelId,
    config: ChannelConfig,
    accentColor: Color,
    onEnabledChange: (Boolean) -> Unit,
    onVoltPerDivChange: (Float) -> Unit,
    onOffsetChange: (Float) -> Unit
) {
    val currentVoltIndex = AVAILABLE_VOLT_DIVS.indexOfFirst {
        kotlin.math.abs(it.volts - config.voltsPerDiv) < 1e-4f
    }.let { if (it >= 0) it else 3 }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Channel Power Switch
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Switch(
                checked = config.enabled,
                onCheckedChange = onEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor,
                    checkedTrackColor = accentColor.copy(alpha = 0.4f)
                )
            )
            Text(
                text = if (config.enabled) "${channel.label} ON" else "${channel.label} OFF",
                color = if (config.enabled) accentColor else Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Volt/Div Stepper
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "VOLT / DIV",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        if (currentVoltIndex > 0) {
                            onVoltPerDivChange(AVAILABLE_VOLT_DIVS[currentVoltIndex - 1].volts)
                        }
                    },
                    enabled = currentVoltIndex > 0,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Remove, "Zoom in (smaller V/div)", tint = Color.White)
                }

                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Text(
                        text = AVAILABLE_VOLT_DIVS[currentVoltIndex].label,
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }

                IconButton(
                    onClick = {
                        if (currentVoltIndex < AVAILABLE_VOLT_DIVS.size - 1) {
                            onVoltPerDivChange(AVAILABLE_VOLT_DIVS[currentVoltIndex + 1].volts)
                        }
                    },
                    enabled = currentVoltIndex < AVAILABLE_VOLT_DIVS.size - 1,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Add, "Zoom out (larger V/div)", tint = Color.White)
                }
            }
        }

        // Vertical Offset Position Stepper
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "VERT POS",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onOffsetChange((config.verticalOffsetDiv - 0.5f).coerceIn(-4f, 4f)) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Remove, "Move Down", tint = Color.White)
                }
                Text(
                    text = String.format(java.util.Locale.US, "%.1f", config.verticalOffsetDiv),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                IconButton(
                    onClick = { onOffsetChange((config.verticalOffsetDiv + 0.5f).coerceIn(-4f, 4f)) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Add, "Move Up", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun TriggerControlsTab(
    source: ChannelId,
    mode: TriggerMode,
    slope: TriggerSlope,
    levelVolts: Float,
    onSourceChange: (ChannelId) -> Unit,
    onModeChange: (TriggerMode) -> Unit,
    onSlopeChange: (TriggerSlope) -> Unit,
    onLevelChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1: Source (CH1 / CH2) and Trigger Mode (AUTO / NORM / SNGL)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Source: CH1 / CH2
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "SRC:",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                ChannelId.entries.forEach { ch ->
                    PillChoice(
                        text = ch.label,
                        selected = source == ch,
                        color = if (ch == ChannelId.CH1) ScopeCh1Yellow else ScopeCh2Cyan,
                        onClick = { onSourceChange(ch) }
                    )
                }
            }

            // Mode: AUTO / NORM / SNGL
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "MODE:",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                TriggerMode.entries.forEach { m ->
                    PillChoice(
                        text = m.label.uppercase().take(4),
                        selected = mode == m,
                        color = ScopeTriggerOrange,
                        onClick = { onModeChange(m) }
                    )
                }
            }
        }

        // Row 2: Slope (RISE ↑ / FALL ↓) and Trigger Level Readout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Slope: Rising / Falling - clearly horizontal, compact, and readable
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "SLOPE:",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                PillChoice(
                    text = "RISE ↑",
                    selected = slope == TriggerSlope.RISING,
                    color = ScopeTriggerOrange,
                    onClick = { onSlopeChange(TriggerSlope.RISING) }
                )
                PillChoice(
                    text = "FALL ↓",
                    selected = slope == TriggerSlope.FALLING,
                    color = ScopeTriggerOrange,
                    onClick = { onSlopeChange(TriggerSlope.FALLING) }
                )
            }

            // Trigger Level Readout Display
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "LEVEL:",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ScopeTriggerOrange.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = String.format(java.util.Locale.US, "%.2f V", levelVolts),
                        color = ScopeTriggerOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Row 3: Level adjustment stepper buttons (-0.1V, +0.1V, 1.65V MID)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { onLevelChange((levelVolts - 0.1f).coerceIn(-100f, 100f)) },
                shape = RoundedCornerShape(6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
            ) {
                Text(
                    text = "-0.1V",
                    fontSize = 11.sp,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )
            }
            OutlinedButton(
                onClick = { onLevelChange((levelVolts + 0.1f).coerceIn(-100f, 100f)) },
                shape = RoundedCornerShape(6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
            ) {
                Text(
                    text = "+0.1V",
                    fontSize = 11.sp,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )
            }
            OutlinedButton(
                onClick = { onLevelChange(1.65f) },
                shape = RoundedCornerShape(6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .height(36.dp)
            ) {
                Text(
                    text = "1.65V (MID)",
                    fontSize = 11.sp,
                    color = ScopeTriggerOrange,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
private fun PillChoice(
    text: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        color = if (selected) color.copy(alpha = 0.25f) else SurfaceDark,
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) color else SurfaceVariantDark
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            color = if (selected) color else Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}
