package com.example.model

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Scanning : ConnectionState
    data class Connecting(val deviceName: String, val address: String) : ConnectionState
    data class Connected(
        val deviceName: String,
        val address: String,
        val rssi: Int? = null
    ) : ConnectionState
    data class ReceivingData(
        val deviceName: String = "ESP32-Oscilloscope",
        val address: String = "",
        val sampleCount: Int,
        val fps: Float,
        val packetLossRate: Float,
        val rssi: Int? = null
    ) : ConnectionState
    data class WaitingForData(
        val deviceName: String = "ESP32-Oscilloscope",
        val address: String = "",
        val rssi: Int? = null
    ) : ConnectionState
    data object Disconnecting : ConnectionState
    data class ConnectionLost(
        val reason: String,
        val lastDeviceAddress: String? = null,
        val lastDeviceName: String? = null
    ) : ConnectionState

    val displayText: String
        get() = when (this) {
            is Disconnected -> "Disconnected"
            is Scanning -> "Scanning for BLE..."
            is Connecting -> "Connecting to $deviceName..."
            is Connected -> "Connected"
            is ReceivingData -> "Receiving Data ($sampleCount pts)"
            is WaitingForData -> "Connected — Waiting for waveform data"
            is Disconnecting -> "Disconnecting..."
            is ConnectionLost -> "Connection Lost: $reason"
        }

    val isConnected: Boolean
        get() = this is Connected || this is ReceivingData || this is WaitingForData

    val connectedDeviceName: String?
        get() = when (this) {
            is Connected -> deviceName
            is ReceivingData -> deviceName
            is WaitingForData -> deviceName
            is Connecting -> deviceName
            is ConnectionLost -> lastDeviceName
            else -> null
        }

    val connectedDeviceAddress: String?
        get() = when (this) {
            is Connected -> address
            is ReceivingData -> address
            is WaitingForData -> address
            is Connecting -> address
            is ConnectionLost -> lastDeviceAddress
            else -> null
        }

    val connectedRssi: Int?
        get() = when (this) {
            is Connected -> rssi
            is ReceivingData -> rssi
            is WaitingForData -> rssi
            else -> null
        }
}
