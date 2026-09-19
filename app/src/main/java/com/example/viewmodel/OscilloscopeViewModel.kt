package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.OscilloscopeRepository
import com.example.model.BleDeviceItem
import com.example.model.ChannelConfig
import com.example.model.ChannelId
import com.example.model.ChannelMeasurements
import com.example.model.ConnectionState
import com.example.model.DiagnosticStats
import com.example.model.DisplayDomain
import com.example.model.FftResult
import com.example.model.RunState
import com.example.model.SavedCapture
import com.example.model.TriggerMode
import com.example.model.TriggerSlope
import com.example.model.WaveformFrame
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class OscilloscopeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = OscilloscopeRepository(application.applicationContext)

    val connectionState: StateFlow<ConnectionState> = repository.connectionState
    val discoveredDevices: StateFlow<List<BleDeviceItem>> = repository.discoveredDevices
    val isScanning: StateFlow<Boolean> = repository.isScanning
    val connectedRssi: StateFlow<Int?> = repository.connectedRssi
    val diagnosticStats: StateFlow<DiagnosticStats> = repository.diagnosticStats

    val ch1Config: StateFlow<ChannelConfig> = repository.ch1Config
    val ch2Config: StateFlow<ChannelConfig> = repository.ch2Config

    val timePerDiv: StateFlow<Float> = repository.timePerDiv
    val samplingRateHz: StateFlow<Float> = repository.samplingRateHz

    val triggerSource: StateFlow<ChannelId> = repository.triggerSource
    val triggerMode: StateFlow<TriggerMode> = repository.triggerMode
    val triggerSlope: StateFlow<TriggerSlope> = repository.triggerSlope
    val triggerLevelVolts: StateFlow<Float> = repository.triggerLevelVolts

    val runState: StateFlow<RunState> = repository.runState
    val horizontalPanDiv: StateFlow<Float> = repository.horizontalPanDiv
    val displayDomain: StateFlow<DisplayDomain> = repository.displayDomain

    val currentFrame: StateFlow<WaveformFrame> = repository.currentFrame
    val ch1Measurements: StateFlow<ChannelMeasurements> = repository.ch1Measurements
    val ch2Measurements: StateFlow<ChannelMeasurements> = repository.ch2Measurements
    val fftResult: StateFlow<FftResult> = repository.fftResult

    val savedCaptures: StateFlow<List<SavedCapture>> = repository.savedCaptures

    private val _userMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    fun startBleScan(allowScanWhileConnected: Boolean = false) {
        repository.startBleScan(allowScanWhileConnected)
    }

    fun stopBleScan() {
        repository.stopBleScan()
    }

    fun connectToDevice(address: String) {
        repository.connectToDevice(address)
    }

    fun reconnect() {
        repository.reconnect()
    }

    fun disconnect() {
        repository.disconnect()
    }

    fun setRunState(state: RunState) {
        repository.setRunState(state)
    }

    fun toggleRunStop() {
        repository.toggleRunStop()
    }

    fun toggleFreeze() {
        repository.toggleFreeze()
    }

    fun singleTrigger() {
        repository.singleTrigger()
    }

    fun autoSet() {
        viewModelScope.launch {
            repository.autoSet()
            _userMessage.tryEmit("Auto-set applied")
        }
    }

    fun clearWaveform() {
        viewModelScope.launch {
            repository.clearWaveforms()
            _userMessage.tryEmit("Waveform cleared")
        }
    }

    fun setTimePerDiv(timeSec: Float) {
        repository.setTimePerDiv(timeSec)
    }

    fun setSamplingRate(rateHz: Float) {
        repository.setSamplingRate(rateHz)
    }

    fun setHorizontalPan(panDiv: Float) {
        repository.setHorizontalPan(panDiv)
    }

    fun setDisplayDomain(domain: DisplayDomain) {
        repository.setDisplayDomain(domain)
    }

    fun setTriggerMode(mode: TriggerMode) {
        repository.setTriggerMode(mode)
    }

    fun setTriggerSource(source: ChannelId) {
        repository.setTriggerSource(source)
    }

    fun setTriggerSlope(slope: TriggerSlope) {
        repository.setTriggerSlope(slope)
    }

    fun setTriggerLevel(levelVolts: Float) {
        repository.setTriggerLevel(levelVolts)
    }

    fun setCh1Enabled(enabled: Boolean) {
        repository.setCh1Enabled(enabled)
    }

    fun setCh2Enabled(enabled: Boolean) {
        repository.setCh2Enabled(enabled)
    }

    fun setCh1VoltPerDiv(volts: Float) {
        repository.setCh1VoltPerDiv(volts)
    }

    fun setCh2VoltPerDiv(volts: Float) {
        repository.setCh2VoltPerDiv(volts)
    }

    fun setCh1VerticalOffset(offsetDiv: Float) {
        repository.setCh1VerticalOffset(offsetDiv)
    }

    fun setCh2VerticalOffset(offsetDiv: Float) {
        repository.setCh2VerticalOffset(offsetDiv)
    }

    fun updateCh1Calibration(
        dividerRatio: Float,
        calMultiplier: Float,
        offsetVoltage: Float
    ) {
        val updated = ch1Config.value.copy(
            dividerRatio = dividerRatio,
            calMultiplier = calMultiplier,
            offsetVoltage = offsetVoltage
        )
        repository.updateCh1Config(updated)
        _userMessage.tryEmit("CH1 calibration saved")
    }

    fun updateCh2Calibration(
        dividerRatio: Float,
        calMultiplier: Float,
        offsetVoltage: Float
    ) {
        val updated = ch2Config.value.copy(
            dividerRatio = dividerRatio,
            calMultiplier = calMultiplier,
            offsetVoltage = offsetVoltage
        )
        repository.updateCh2Config(updated)
        _userMessage.tryEmit("CH2 calibration saved")
    }

    fun saveWaveformCapture() {
        viewModelScope.launch {
            val capture = repository.saveCurrentCapture()
            if (capture != null) {
                _userMessage.tryEmit("Waveform saved (${capture.title})")
            } else {
                _userMessage.tryEmit("No waveform data to save")
            }
        }
    }

    fun shareCapture(capture: SavedCapture) {
        repository.shareCapture(capture)
    }

    fun deleteCapture(capture: SavedCapture) {
        repository.deleteCapture(capture)
        _userMessage.tryEmit("Capture deleted")
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnect()
    }
}
