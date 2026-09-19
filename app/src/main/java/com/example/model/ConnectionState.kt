package com.example.model

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Scanning : ConnectionState
    data class Connecting(val deviceName: String, val address: String) : ConnectionState
    data class Connected(val deviceName: String, val address: String) : ConnectionState
    data class ReceivingData(val sampleCount: Int, val fps: Float, val packetLossRate: Float) : ConnectionState
    data class WaitingForData(val deviceName: String) : ConnectionState
    data class ConnectionLost(val reason: String) : ConnectionState

    val displayText: String
        get() = when (this) {
            is Disconnected -> "Disconnected"
            is Scanning -> "Scanning for BLE..."
            is Connecting -> "Connecting to $deviceName..."
            is Connected -> "Connected"
            is ReceivingData -> "Receiving Data ($sampleCount pts)"
            is WaitingForData -> "Connected — Waiting for waveform data"
            is ConnectionLost -> "Connection Lost: $reason"
        }

    val isConnected: Boolean
        get() = this is Connected || this is ReceivingData || this is WaitingForData
}
