package com.example.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.model.ConnectionState
import com.example.model.DisplayDomain
import com.example.ui.components.AboutDialog
import com.example.ui.components.BleDeviceDialog
import com.example.ui.components.CalibrationDialog
import com.example.ui.components.ControlPanel
import com.example.ui.components.FftDisplay
import com.example.ui.components.MeasurementsPanel
import com.example.ui.components.OscilloscopeDisplay
import com.example.ui.components.SafetyDialog
import com.example.ui.components.SavedCapturesDialog
import com.example.ui.theme.ScopeCh1Yellow
import com.example.ui.theme.ScopeCh2Cyan
import com.example.ui.theme.ScopeDarkBackground
import com.example.ui.theme.ScopeRunGreen
import com.example.ui.theme.ScopeStopRed
import com.example.ui.theme.ScopeWaitAmber
import com.example.ui.theme.SurfaceDark
import com.example.viewmodel.OscilloscopeViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OscilloscopeScreen(
    viewModel: OscilloscopeViewModel,
    modifier: Modifier = Modifier
) {
    val connectionState by viewModel.connectionState.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val diagnosticStats by viewModel.diagnosticStats.collectAsState()

    val ch1Config by viewModel.ch1Config.collectAsState()
    val ch2Config by viewModel.ch2Config.collectAsState()

    val timePerDiv by viewModel.timePerDiv.collectAsState()
    val samplingRateHz by viewModel.samplingRateHz.collectAsState()

    val triggerSource by viewModel.triggerSource.collectAsState()
    val triggerMode by viewModel.triggerMode.collectAsState()
    val triggerSlope by viewModel.triggerSlope.collectAsState()
    val triggerLevelVolts by viewModel.triggerLevelVolts.collectAsState()

    val runState by viewModel.runState.collectAsState()
    val horizontalPanDiv by viewModel.horizontalPanDiv.collectAsState()
    val displayDomain by viewModel.displayDomain.collectAsState()

    val currentFrame by viewModel.currentFrame.collectAsState()
    val ch1Measurements by viewModel.ch1Measurements.collectAsState()
    val ch2Measurements by viewModel.ch2Measurements.collectAsState()
    val fftResult by viewModel.fftResult.collectAsState()
    val savedCaptures by viewModel.savedCaptures.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog Visibility states
    var showBleDialog by remember { mutableStateOf(false) }
    var showCalDialog by remember { mutableStateOf(false) }
    var showSafetyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showCapturesDialog by remember { mutableStateOf(false) }

    // BLE Runtime Permission Launcher
    val blePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val allGranted = grants.values.all { it }
        if (allGranted) {
            viewModel.startBleScan()
        }
    }

    // Snackbar event collector
    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ScopeDarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Digital Oscilloscope",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        // Live connection badge
                        Surface(
                            color = SurfaceDark,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when (connectionState) {
                                    is ConnectionState.Connected,
                                    is ConnectionState.ReceivingData -> ScopeRunGreen
                                    is ConnectionState.Scanning,
                                    is ConnectionState.Connecting -> ScopeCh2Cyan
                                    is ConnectionState.WaitingForData -> ScopeWaitAmber
                                    is ConnectionState.ConnectionLost -> ScopeStopRed
                                    is ConnectionState.Disconnected -> Color(0xFF475569)
                                }
                            ),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            when (connectionState) {
                                                is ConnectionState.Connected,
                                                is ConnectionState.ReceivingData -> ScopeRunGreen
                                                is ConnectionState.Scanning,
                                                is ConnectionState.Connecting -> ScopeCh2Cyan
                                                is ConnectionState.WaitingForData -> ScopeWaitAmber
                                                is ConnectionState.ConnectionLost -> ScopeStopRed
                                                is ConnectionState.Disconnected -> Color(0xFF64748B)
                                            },
                                            CircleShape
                                        )
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (connectionState is ConnectionState.ReceivingData)
                                        "${diagnosticStats.packetsPerSec} pkts/s"
                                    else
                                        connectionState.displayText,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark),
                actions = {
                    // BLE Connect Icon
                    IconButton(
                        onClick = {
                            showBleDialog = true
                            permissionLauncher.launch(blePermissions)
                        },
                        modifier = Modifier.testTag("action_ble")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = "Bluetooth Connection",
                            tint = if (connectionState.isConnected) ScopeRunGreen else ScopeCh2Cyan
                        )
                    }

                    // Saved Captures Icon
                    IconButton(
                        onClick = { showCapturesDialog = true },
                        modifier = Modifier.testTag("action_captures")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "Saved Captures",
                            tint = ScopeCh1Yellow
                        )
                    }

                    // Calibration Icon
                    IconButton(
                        onClick = { showCalDialog = true },
                        modifier = Modifier.testTag("action_calibration")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Hardware Calibration",
                            tint = Color(0xFFCBD5E1)
                        )
                    }

                    // Safety Icon
                    IconButton(
                        onClick = { showSafetyDialog = true },
                        modifier = Modifier.testTag("action_safety")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Electrical Safety",
                            tint = ScopeStopRed
                        )
                    }

                    // About Icon
                    IconButton(
                        onClick = { showAboutDialog = true },
                        modifier = Modifier.testTag("action_about")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Oscilloscope Display Canvas (Time domain or FFT domain)
            if (displayDomain == DisplayDomain.TIME) {
                OscilloscopeDisplay(
                    frame = currentFrame,
                    ch1Config = ch1Config,
                    ch2Config = ch2Config,
                    timePerDivSec = timePerDiv,
                    triggerLevelVolts = triggerLevelVolts,
                    isTriggered = currentFrame.isTriggered,
                    runState = runState,
                    connectionState = connectionState,
                    onTriggerLevelChange = viewModel::setTriggerLevel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                )
            } else {
                FftDisplay(
                    fftResult = fftResult,
                    ch1Config = ch1Config,
                    ch2Config = ch2Config,
                    samplingRateHz = samplingRateHz,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                )
            }

            // 2. Real-time Telemetry Measurements Panel (CH1 & CH2 Vmax, Vmin, Vpp, RMS, Freq, Period)
            MeasurementsPanel(
                ch1Measurements = ch1Measurements,
                ch2Measurements = ch2Measurements,
                ch1Config = ch1Config,
                ch2Config = ch2Config
            )

            // 3. Hardware Control Panel (Run/Stop, Auto, Single, Freeze, Volt/Div, Time/Div, Trigger, etc.)
            ControlPanel(
                runState = runState,
                displayDomain = displayDomain,
                timePerDivSec = timePerDiv,
                horizontalPanDiv = horizontalPanDiv,
                ch1Config = ch1Config,
                ch2Config = ch2Config,
                triggerSource = triggerSource,
                triggerMode = triggerMode,
                triggerSlope = triggerSlope,
                triggerLevelVolts = triggerLevelVolts,
                onToggleRunStop = viewModel::toggleRunStop,
                onToggleFreeze = viewModel::toggleFreeze,
                onSingleTrigger = viewModel::singleTrigger,
                onAutoSet = viewModel::autoSet,
                onClear = viewModel::clearWaveform,
                onSaveWaveform = viewModel::saveWaveformCapture,
                onDomainChange = viewModel::setDisplayDomain,
                onTimePerDivChange = viewModel::setTimePerDiv,
                onHorizontalPanChange = viewModel::setHorizontalPan,
                onCh1EnabledChange = viewModel::setCh1Enabled,
                onCh2EnabledChange = viewModel::setCh2Enabled,
                onCh1VoltPerDivChange = viewModel::setCh1VoltPerDiv,
                onCh2VoltPerDivChange = viewModel::setCh2VoltPerDiv,
                onCh1OffsetChange = viewModel::setCh1VerticalOffset,
                onCh2OffsetChange = viewModel::setCh2VerticalOffset,
                onTriggerSourceChange = viewModel::setTriggerSource,
                onTriggerModeChange = viewModel::setTriggerMode,
                onTriggerSlopeChange = viewModel::setTriggerSlope,
                onTriggerLevelChange = viewModel::setTriggerLevel
            )
        }
    }

    // Modal Dialogs
    if (showBleDialog) {
        BleDeviceDialog(
            connectionState = connectionState,
            devices = discoveredDevices,
            onStartScan = {
                permissionLauncher.launch(blePermissions)
            },
            onStopScan = viewModel::stopBleScan,
            onConnect = { addr ->
                viewModel.connectToDevice(addr)
            },
            onDisconnect = viewModel::disconnect,
            onDismiss = { showBleDialog = false }
        )
    }

    if (showCalDialog) {
        CalibrationDialog(
            ch1Config = ch1Config,
            ch2Config = ch2Config,
            samplingRateHz = samplingRateHz,
            onSaveCh1 = viewModel::updateCh1Calibration,
            onSaveCh2 = viewModel::updateCh2Calibration,
            onSaveSamplingRate = viewModel::setSamplingRate,
            onDismiss = { showCalDialog = false }
        )
    }

    if (showSafetyDialog) {
        SafetyDialog(onDismiss = { showSafetyDialog = false })
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showCapturesDialog) {
        SavedCapturesDialog(
            captures = savedCaptures,
            onShareCapture = viewModel::shareCapture,
            onDeleteCapture = viewModel::deleteCapture,
            onDismiss = { showCapturesDialog = false }
        )
    }
}
