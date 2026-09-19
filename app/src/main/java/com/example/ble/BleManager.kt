package com.example.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.model.BleDeviceItem
import com.example.model.ConnectionState
import com.example.model.DiagnosticStats
import com.example.model.RawWaveformPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class BleManager(private val context: Context) {

    companion object {
        private const val TAG = "BleManager"
        const val TARGET_DEVICE_NAME = "ESP32-Oscilloscope"
        val SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        val WAVEFORM_CHAR_UUID: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        private const val PACKET_TIMEOUT_MS = 2000L
        private const val SCAN_TIMEOUT_MS = 15000L
    }

    private val scope = CoroutineScope(Dispatchers.Default + Job())

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private var bluetoothGatt: BluetoothGatt? = null
    private var lastConnectedDevice: BluetoothDevice? = null
    private var userInitiatedDisconnect = false
    private var reconnectAttempts = 0
    private var reconnectJob: Job? = null
    private var watchdogJob: Job? = null

    // State Flows
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BleDeviceItem>>(emptyList())
    val discoveredDevices: StateFlow<List<BleDeviceItem>> = _discoveredDevices.asStateFlow()

    private val _rawPacketFlow = MutableSharedFlow<RawWaveformPacket>(extraBufferCapacity = 128)
    val rawPacketFlow: SharedFlow<RawWaveformPacket> = _rawPacketFlow.asSharedFlow()

    private val _diagnosticStats = MutableStateFlow(DiagnosticStats())
    val diagnosticStats: StateFlow<DiagnosticStats> = _diagnosticStats.asStateFlow()

    // Diagnostic accumulators
    private var totalPackets = 0L
    private var droppedPackets = 0L
    private var corruptedPackets = 0L
    private var lastSequenceNumber = -1
    private var lastPacketTime = 0L
    private var rateCalcStartTime = System.currentTimeMillis()
    private var packetsInCurrentWindow = 0
    private var samplesInCurrentWindow = 0

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    private var isScanning = false
    private var scanCallback: ScanCallback? = null

    init {
        startWatchdog()
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled) return

        val scanner: BluetoothLeScanner = adapter.bluetoothLeScanner ?: return
        if (isScanning) return

        _discoveredDevices.value = emptyList()
        _connectionState.value = ConnectionState.Scanning

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                handleScanResult(result)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { handleScanResult(it) }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e(TAG, "BLE scan failed with error code $errorCode")
                isScanning = false
                if (_connectionState.value == ConnectionState.Scanning) {
                    _connectionState.value = ConnectionState.Disconnected
                }
            }
        }

        try {
            isScanning = true
            scanner.startScan(scanCallback)
            Log.d(TAG, "BLE scan started")

            // Auto-stop scan after timeout
            scope.launch {
                delay(SCAN_TIMEOUT_MS)
                if (isScanning) {
                    stopScan()
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException starting BLE scan", e)
            isScanning = false
            _connectionState.value = ConnectionState.Disconnected
        } catch (e: Exception) {
            Log.e(TAG, "Error starting BLE scan", e)
            isScanning = false
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isScanning) return
        try {
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            Log.d(TAG, "BLE scan stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping scan", e)
        } finally {
            isScanning = false
            scanCallback = null
            if (_connectionState.value == ConnectionState.Scanning) {
                _connectionState.value = ConnectionState.Disconnected
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleScanResult(result: ScanResult) {
        val device = result.device ?: return
        val name = device.name ?: result.scanRecord?.deviceName
        val address = device.address ?: return
        val rssi = result.rssi
        val isTarget = (name == TARGET_DEVICE_NAME)

        val currentList = _discoveredDevices.value.toMutableList()
        val index = currentList.indexOfFirst { it.address == address }
        val item = BleDeviceItem(
            name = name,
            address = address,
            rssi = rssi,
            isTargetOscilloscope = isTarget
        )

        if (index >= 0) {
            currentList[index] = item
        } else {
            // Put target oscilloscope at the top
            if (isTarget) {
                currentList.add(0, item)
            } else {
                currentList.add(item)
            }
        }
        _discoveredDevices.value = currentList
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceAddress: String) {
        stopScan()
        userInitiatedDisconnect = false
        reconnectAttempts = 0
        reconnectJob?.cancel()

        val adapter = bluetoothAdapter ?: return
        val device = try {
            adapter.getRemoteDevice(deviceAddress)
        } catch (e: Exception) {
            Log.e(TAG, "Invalid Bluetooth address $deviceAddress", e)
            return
        }

        lastConnectedDevice = device
        val devName = device.name ?: TARGET_DEVICE_NAME
        _connectionState.value = ConnectionState.Connecting(devName, device.address)

        try {
            bluetoothGatt?.close()
            bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(context, false, gattCallback)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException connecting to device", e)
            _connectionState.value = ConnectionState.ConnectionLost("Permission denied")
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to device", e)
            _connectionState.value = ConnectionState.ConnectionLost(e.message ?: "Connection failed")
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDefaultOscilloscope() {
        val target = _discoveredDevices.value.firstOrNull { it.isTargetOscilloscope }
        if (target != null) {
            connectToDevice(target.address)
        } else {
            startScan()
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        userInitiatedDisconnect = true
        reconnectJob?.cancel()
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        } finally {
            bluetoothGatt = null
            lastPacketTime = 0L
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceName = gatt.device?.name ?: TARGET_DEVICE_NAME
            val deviceAddress = gatt.device?.address ?: ""

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to GATT server. Discovering services...")
                _connectionState.value = ConnectionState.Connected(deviceName, deviceAddress)
                reconnectAttempts = 0

                // Request MTU 512 for high-throughput waveform transfer
                try {
                    gatt.requestMtu(512)
                } catch (e: Exception) {
                    Log.w(TAG, "requestMtu failed, proceeding with service discovery", e)
                }
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w(TAG, "Disconnected from GATT server with status $status")
                gatt.close()
                bluetoothGatt = null

                if (userInitiatedDisconnect) {
                    _connectionState.value = ConnectionState.Disconnected
                } else {
                    _connectionState.value = ConnectionState.ConnectionLost("Status code: $status")
                    scheduleAutoReconnect()
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.d(TAG, "MTU changed to $mtu, status: $status")
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                if (service != null) {
                    val characteristic = service.getCharacteristic(WAVEFORM_CHAR_UUID)
                    if (characteristic != null) {
                        subscribeToWaveform(gatt, characteristic)
                    } else {
                        Log.e(TAG, "Waveform characteristic not found: $WAVEFORM_CHAR_UUID")
                    }
                } else {
                    Log.e(TAG, "Oscilloscope service not found: $SERVICE_UUID")
                }
            } else {
                Log.e(TAG, "onServicesDiscovered received status: $status")
            }
        }

        // Android 13+ (API 33+) callback
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == WAVEFORM_CHAR_UUID) {
                processWaveformPacket(value)
            }
        }

        // Legacy callback for Android <= 12
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                if (characteristic.uuid == WAVEFORM_CHAR_UUID) {
                    @Suppress("DEPRECATION")
                    val value = characteristic.value
                    if (value != null) {
                        processWaveformPacket(value)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun subscribeToWaveform(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        val success = gatt.setCharacteristicNotification(characteristic, true)
        Log.d(TAG, "setCharacteristicNotification success: $success")

        val descriptor = characteristic.getDescriptor(CCCD_UUID)
        if (descriptor != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
            Log.d(TAG, "Wrote CCCD descriptor to enable notifications")
        } else {
            Log.w(TAG, "CCCD descriptor not found on waveform characteristic")
        }

        val devName = gatt.device?.name ?: TARGET_DEVICE_NAME
        _connectionState.value = ConnectionState.WaitingForData(devName)
    }

    /**
     * Binary packet decoder strictly according to specification:
     * - Little-endian encoding
     * - Byte 0-1: sequence number (uint16)
     * - Byte 2: number of samples N (uint8)
     * - Then N samples:
     *   - CH1 uint16 (2 bytes)
     *   - CH2 uint16 (2 bytes)
     *
     * Validates packet size, sequence gaps, corrupted packets.
     * Safe execution: Never crashes on malformed data.
     */
    private fun processWaveformPacket(bytes: ByteArray) {
        if (bytes.size < 3) {
            corruptedPackets++
            updateDiagnostics(0)
            return
        }

        val seqNumber = (bytes[0].toInt() and 0xFF) or ((bytes[1].toInt() and 0xFF) shl 8)
        val numSamples = bytes[2].toInt() and 0xFF
        val expectedLength = 3 + (numSamples * 4)

        if (bytes.size < expectedLength) {
            // Corrupted or incomplete packet
            corruptedPackets++
            updateDiagnostics(0)
            return
        }

        // Check for sequence continuity & dropped packets
        if (lastSequenceNumber >= 0) {
            val diff = (seqNumber - lastSequenceNumber) and 0xFFFF
            if (diff > 1) {
                val dropped = (diff - 1).coerceAtMost(1000)
                droppedPackets += dropped
            }
        }
        lastSequenceNumber = seqNumber
        totalPackets++
        lastPacketTime = System.currentTimeMillis()

        val ch1Raw = IntArray(numSamples)
        val ch2Raw = IntArray(numSamples)

        var offset = 3
        for (i in 0 until numSamples) {
            val ch1Val = (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)
            val ch2Val = (bytes[offset + 2].toInt() and 0xFF) or ((bytes[offset + 3].toInt() and 0xFF) shl 8)
            ch1Raw[i] = ch1Val
            ch2Raw[i] = ch2Val
            offset += 4
        }

        val packet = RawWaveformPacket(
            sequenceNumber = seqNumber,
            numSamples = numSamples,
            ch1Raw = ch1Raw,
            ch2Raw = ch2Raw
        )

        _rawPacketFlow.tryEmit(packet)
        updateDiagnostics(numSamples)
    }

    private fun updateDiagnostics(samplesInPacket: Int) {
        packetsInCurrentWindow++
        samplesInCurrentWindow += samplesInPacket

        val now = System.currentTimeMillis()
        val elapsedSec = (now - rateCalcStartTime) / 1000f

        if (elapsedSec >= 1.0f) {
            val pps = packetsInCurrentWindow / elapsedSec
            val sps = samplesInCurrentWindow / elapsedSec
            val lossRate = if (totalPackets + droppedPackets > 0) {
                droppedPackets.toFloat() / (totalPackets + droppedPackets).toFloat()
            } else 0f

            _diagnosticStats.value = DiagnosticStats(
                totalPacketsReceived = totalPackets,
                droppedPackets = droppedPackets,
                corruptedPackets = corruptedPackets,
                packetsPerSec = pps,
                samplesPerSec = sps
            )

            // Update ConnectionState with live rate
            if (_connectionState.value is ConnectionState.ReceivingData || _connectionState.value is ConnectionState.WaitingForData || _connectionState.value is ConnectionState.Connected) {
                _connectionState.value = ConnectionState.ReceivingData(
                    sampleCount = samplesInCurrentWindow,
                    fps = pps,
                    packetLossRate = lossRate
                )
            }

            packetsInCurrentWindow = 0
            samplesInCurrentWindow = 0
            rateCalcStartTime = now
        }
    }

    /**
     * Watchdog monitors received packet timestamps.
     * If connected but no packets arrive for > 2 seconds, transition state to WaitingForData.
     */
    private fun startWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = scope.launch {
            while (isActive) {
                delay(1000)
                val state = _connectionState.value
                val now = System.currentTimeMillis()

                if (state is ConnectionState.ReceivingData) {
                    if (now - lastPacketTime > PACKET_TIMEOUT_MS) {
                        val devName = lastConnectedDevice?.name ?: TARGET_DEVICE_NAME
                        _connectionState.value = ConnectionState.WaitingForData(devName)
                    }
                }
            }
        }
    }

    /**
     * Automatic reconnection logic on unexpected disconnect.
     */
    @SuppressLint("MissingPermission")
    private fun scheduleAutoReconnect() {
        if (userInitiatedDisconnect) return
        val device = lastConnectedDevice ?: return

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            reconnectAttempts++
            val backoffMs = (reconnectAttempts * 2000L).coerceAtMost(10000L)
            Log.d(TAG, "Will attempt reconnect #$reconnectAttempts in ${backoffMs}ms")
            delay(backoffMs)

            if (!userInitiatedDisconnect && _connectionState.value is ConnectionState.ConnectionLost) {
                val devName = device.name ?: TARGET_DEVICE_NAME
                _connectionState.value = ConnectionState.Connecting(devName, device.address)
                try {
                    bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                    } else {
                        device.connectGatt(context, false, gattCallback)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Auto-reconnect failed", e)
                    scheduleAutoReconnect()
                }
            }
        }
    }
}
