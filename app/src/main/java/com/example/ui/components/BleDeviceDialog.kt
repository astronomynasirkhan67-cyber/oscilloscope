package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ble.BleManager
import com.example.model.BleDeviceItem
import com.example.model.ConnectionState
import com.example.model.DiagnosticStats
import com.example.ui.theme.ScopeCh1Yellow
import com.example.ui.theme.ScopeCh2Cyan
import com.example.ui.theme.ScopeRunGreen
import com.example.ui.theme.ScopeStopRed
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark

@Composable
fun BleDeviceDialog(
    connectionState: ConnectionState,
    devices: List<BleDeviceItem>,
    isScanning: Boolean = false,
    connectedRssi: Int? = null,
    diagnosticStats: DiagnosticStats? = null,
    onStartScan: () -> Unit,
    onStartScanForOther: () -> Unit = onStartScan,
    onStopScan: () -> Unit,
    onConnect: (String) -> Unit,
    onReconnect: () -> Unit = {},
    onDisconnect: () -> Unit,
    onDismiss: () -> Unit
) {
    var isScanningForOtherDevices by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("ble_device_dialog"),
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
                    if (isScanningForOtherDevices) {
                        IconButton(
                            onClick = { isScanningForOtherDevices = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to connected device",
                                tint = ScopeCh2Cyan
                            )
                        }
                    } else {
                        Icon(
                            imageVector = if (connectionState.isConnected) {
                                Icons.Default.BluetoothConnected
                            } else {
                                Icons.Default.Bluetooth
                            },
                            contentDescription = null,
                            tint = if (connectionState.isConnected) ScopeRunGreen else ScopeCh2Cyan
                        )
                    }
                    Text(
                        text = if (isScanningForOtherDevices) "Scan for Other Devices" else "Bluetooth BLE Devices",
                        color = Color.White,
                        fontSize = 17.sp,
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
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when {
                    // 1. Device is connected and user is NOT explicitly scanning for others
                    connectionState.isConnected && !isScanningForOtherDevices -> {
                        ConnectedDeviceContent(
                            connectionState = connectionState,
                            connectedRssi = connectedRssi,
                            diagnosticStats = diagnosticStats,
                            onDisconnect = onDisconnect,
                            onScanForOtherDevices = {
                                isScanningForOtherDevices = true
                                onStartScanForOther()
                            }
                        )
                    }

                    // 2. Connection is in progress
                    connectionState is ConnectionState.Connecting && !isScanningForOtherDevices -> {
                        ConnectingContent(
                            connectionState = connectionState,
                            onCancel = onDisconnect
                        )
                    }

                    // 3. Connection was lost
                    connectionState is ConnectionState.ConnectionLost && !isScanningForOtherDevices -> {
                        ConnectionLostContent(
                            connectionState = connectionState,
                            onReconnect = onReconnect,
                            onScan = {
                                isScanningForOtherDevices = true
                                onStartScan()
                            }
                        )
                    }

                    // 4. Disconnected or user explicitly chose "Scan for Other Devices"
                    else -> {
                        ScannerContent(
                            connectionState = connectionState,
                            devices = devices,
                            isScanning = isScanning,
                            isScanningForOther = isScanningForOtherDevices,
                            onStartScan = {
                                if (isScanningForOtherDevices) {
                                    onStartScanForOther()
                                } else {
                                    onStartScan()
                                }
                            },
                            onStopScan = onStopScan,
                            onConnect = { address ->
                                isScanningForOtherDevices = false
                                onConnect(address)
                            },
                            onBackToConnected = {
                                isScanningForOtherDevices = false
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}

/**
 * Screen displayed when ESP32 is already connected.
 * Strictly prevents automatic scan, clearly displays status, device name, RSSI,
 * Disconnect button, and Scan for Other Devices button.
 */
@Composable
private fun ConnectedDeviceContent(
    connectionState: ConnectionState,
    connectedRssi: Int?,
    diagnosticStats: DiagnosticStats?,
    onDisconnect: () -> Unit,
    onScanForOtherDevices: () -> Unit
) {
    // Current Status Card
    Surface(
        color = SurfaceVariantDark,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, ScopeRunGreen.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Current Status",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(ScopeRunGreen, CircleShape)
                )
                Text(
                    text = "CONNECTED",
                    color = ScopeRunGreen,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }

    // Connected Device Details Card
    Surface(
        color = SurfaceVariantDark,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Device Name & Address
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Device:",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = connectionState.connectedDeviceName ?: BleManager.TARGET_DEVICE_NAME,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val addr = connectionState.connectedDeviceAddress
                    if (!addr.isNullOrEmpty()) {
                        Text(
                            text = addr,
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.BluetoothConnected,
                    contentDescription = null,
                    tint = ScopeRunGreen,
                    modifier = Modifier.size(28.dp)
                )
            }

            HorizontalDivider(color = Color(0xFF334155), thickness = 1.dp)

            // Connection / Stream Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Connection:",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                val statusText = when (connectionState) {
                    is ConnectionState.ReceivingData -> "Streaming (${connectionState.fps.toInt()} pkts/s)"
                    is ConnectionState.WaitingForData -> "Connected (Waiting for data)"
                    else -> "Connected"
                }
                Text(
                    text = statusText,
                    color = ScopeRunGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            // RSSI
            val rssi = connectedRssi ?: connectionState.connectedRssi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "RSSI:",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
                Text(
                    text = if (rssi != null && rssi != 0) "$rssi dBm" else "Available",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Telemetry stats if available
            if (diagnosticStats != null && diagnosticStats.totalPacketsReceived > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Packets Received:",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${diagnosticStats.totalPacketsReceived} pkts",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }

    // Action buttons: Disconnect and Scan for Other Devices
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onDisconnect,
            colors = ButtonDefaults.buttonColors(containerColor = ScopeStopRed),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .testTag("ble_disconnect_button")
        ) {
            Text("Disconnect", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        OutlinedButton(
            onClick = onScanForOtherDevices,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .testTag("ble_scan_other_devices_button")
        ) {
            Icon(Icons.Default.BluetoothSearching, contentDescription = null, modifier = Modifier.size(16.dp), tint = ScopeCh2Cyan)
            Spacer(Modifier.width(6.dp))
            Text("Scan for Other Devices", fontSize = 12.sp, color = Color.White)
        }
    }
}

/**
 * Screen displayed when a connection is actively being established.
 */
@Composable
private fun ConnectingContent(
    connectionState: ConnectionState.Connecting,
    onCancel: () -> Unit
) {
    Surface(
        color = SurfaceVariantDark,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, ScopeCh2Cyan.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Current Status",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(ScopeCh2Cyan, CircleShape)
                )
                Text(
                    text = "CONNECTING...",
                    color = ScopeCh2Cyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }

    Surface(
        color = SurfaceVariantDark,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp),
                color = ScopeCh2Cyan
            )
            Column {
                Text(
                    text = "Connecting to ${connectionState.deviceName}...",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = connectionState.address,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }

    OutlinedButton(
        onClick = onCancel,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
    ) {
        Text("Cancel", fontSize = 12.sp, color = Color.White)
    }
}

/**
 * Screen displayed when an unexpected disconnection occurs.
 */
@Composable
private fun ConnectionLostContent(
    connectionState: ConnectionState.ConnectionLost,
    onReconnect: () -> Unit,
    onScan: () -> Unit
) {
    Surface(
        color = SurfaceVariantDark,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, ScopeStopRed.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Current Status",
                color = Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(ScopeStopRed, CircleShape)
                )
                Text(
                    text = "CONNECTION LOST",
                    color = ScopeStopRed,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }

    Surface(
        color = SurfaceVariantDark,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = ScopeStopRed, modifier = Modifier.size(24.dp))
            Column {
                Text(
                    text = "Disconnected from ${connectionState.lastDeviceName ?: BleManager.TARGET_DEVICE_NAME}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = connectionState.reason,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onReconnect,
            colors = ButtonDefaults.buttonColors(containerColor = ScopeCh2Cyan),
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .testTag("ble_reconnect_button")
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
            Spacer(Modifier.width(6.dp))
            Text("Reconnect", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }

        OutlinedButton(
            onClick = onScan,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .testTag("ble_scan_devices_button")
        ) {
            Text("Scan for Devices", fontSize = 12.sp, color = Color.White)
        }
    }
}

/**
 * Screen displayed when disconnected or when user explicitly taps "Scan for Other Devices".
 */
@Composable
private fun ScannerContent(
    connectionState: ConnectionState,
    devices: List<BleDeviceItem>,
    isScanning: Boolean,
    isScanningForOther: Boolean,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (String) -> Unit,
    onBackToConnected: () -> Unit
) {
    val scanningActive = isScanning || connectionState is ConnectionState.Scanning

    // Current Status Card
    Surface(
        color = SurfaceVariantDark,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Current Status",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (scanningActive) ScopeCh2Cyan else Color(0xFF64748B),
                                CircleShape
                            )
                    )
                    Text(
                        text = if (scanningActive) "SCANNING" else "DISCONNECTED",
                        color = if (scanningActive) ScopeCh2Cyan else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (isScanningForOther && connectionState.isConnected) {
                OutlinedButton(
                    onClick = onBackToConnected,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("← Connected Device", fontSize = 11.sp, color = ScopeRunGreen)
                }
            }
        }
    }

    // Instructions & Target UUID info
    Text(
        text = "Target Device: \"${BleManager.TARGET_DEVICE_NAME}\"\nUUID: ${BleManager.SERVICE_UUID}",
        fontSize = 11.sp,
        color = Color(0xFF94A3B8),
        fontFamily = FontFamily.Monospace
    )

    // Scan Action Row
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Discovered Devices (${devices.size})",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        if (scanningActive) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                    color = ScopeCh2Cyan
                )
                OutlinedButton(
                    onClick = onStopScan,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Stop Scan", fontSize = 11.sp, color = Color.White)
                }
            }
        } else {
            OutlinedButton(
                onClick = onStartScan,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Scan Now", fontSize = 11.sp, color = Color.White)
            }
        }
    }

    // Device List
    if (devices.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .border(1.dp, SurfaceVariantDark, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BluetoothSearching,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = if (scanningActive)
                        "Scanning for nearby BLE devices..."
                    else
                        "No devices found yet. Tap 'Scan Now'.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(devices, key = { it.address }) { device ->
                DeviceListItem(
                    device = device,
                    onSelect = { onConnect(device.address) }
                )
            }
        }
    }
}

@Composable
private fun DeviceListItem(
    device: BleDeviceItem,
    onSelect: () -> Unit
) {
    val isTarget = device.isTargetOscilloscope

    Surface(
        color = if (isTarget) Color(0xFF13284C) else SurfaceVariantDark,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            1.dp,
            if (isTarget) ScopeCh2Cyan else Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("device_item_${device.address}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (isTarget) Icons.Default.Star else Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = if (isTarget) ScopeCh1Yellow else ScopeCh2Cyan,
                    modifier = Modifier.size(20.dp)
                )

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = device.name ?: "Unknown Device",
                            color = if (isTarget) ScopeCh2Cyan else Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (isTarget) FontWeight.Bold else FontWeight.Medium
                        )
                        if (isTarget) {
                            Surface(
                                color = ScopeCh1Yellow.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "TARGET",
                                    color = ScopeCh1Yellow,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${device.address}  •  RSSI: ${device.rssi} dBm",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Button(
                onClick = onSelect,
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isTarget) ScopeCh2Cyan else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = "Connect",
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
