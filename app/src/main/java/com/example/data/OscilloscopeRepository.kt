package com.example.data

import android.content.Context
import com.example.ble.BleManager
import com.example.engine.WaveformProcessor
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class OscilloscopeRepository(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    val bleManager = BleManager(context)
    val processor = WaveformProcessor(Dispatchers.Default)
    val preferences = OscilloscopePreferences(context)
    val captureRepository = CaptureRepository(context)

    // Configuration states
    private val _ch1Config = MutableStateFlow(preferences.loadCh1Config())
    val ch1Config: StateFlow<ChannelConfig> = _ch1Config.asStateFlow()

    private val _ch2Config = MutableStateFlow(preferences.loadCh2Config())
    val ch2Config: StateFlow<ChannelConfig> = _ch2Config.asStateFlow()

    private val _timePerDiv = MutableStateFlow(preferences.loadTimePerDiv())
    val timePerDiv: StateFlow<Float> = _timePerDiv.asStateFlow()

    private val _samplingRateHz = MutableStateFlow(preferences.loadSamplingRate())
    val samplingRateHz: StateFlow<Float> = _samplingRateHz.asStateFlow()

    private val _triggerSource = MutableStateFlow(preferences.loadTriggerSource())
    val triggerSource: StateFlow<ChannelId> = _triggerSource.asStateFlow()

    private val _triggerMode = MutableStateFlow(preferences.loadTriggerMode())
    val triggerMode: StateFlow<TriggerMode> = _triggerMode.asStateFlow()

    private val _triggerSlope = MutableStateFlow(preferences.loadTriggerSlope())
    val triggerSlope: StateFlow<TriggerSlope> = _triggerSlope.asStateFlow()

    private val _triggerLevelVolts = MutableStateFlow(preferences.loadTriggerLevel())
    val triggerLevelVolts: StateFlow<Float> = _triggerLevelVolts.asStateFlow()

    private val _runState = MutableStateFlow(RunState.RUN)
    val runState: StateFlow<RunState> = _runState.asStateFlow()

    private val _horizontalPanDiv = MutableStateFlow(0.0f)
    val horizontalPanDiv: StateFlow<Float> = _horizontalPanDiv.asStateFlow()

    private val _displayDomain = MutableStateFlow(DisplayDomain.TIME)
    val displayDomain: StateFlow<DisplayDomain> = _displayDomain.asStateFlow()

    // Output Data states
    private val _currentFrame = MutableStateFlow(WaveformFrame())
    val currentFrame: StateFlow<WaveformFrame> = _currentFrame.asStateFlow()

    private val _ch1Measurements = MutableStateFlow(ChannelMeasurements())
    val ch1Measurements: StateFlow<ChannelMeasurements> = _ch1Measurements.asStateFlow()

    private val _ch2Measurements = MutableStateFlow(ChannelMeasurements())
    val ch2Measurements: StateFlow<ChannelMeasurements> = _ch2Measurements.asStateFlow()

    private val _fftResult = MutableStateFlow(FftResult())
    val fftResult: StateFlow<FftResult> = _fftResult.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = bleManager.connectionState
    val discoveredDevices: StateFlow<List<BleDeviceItem>> = bleManager.discoveredDevices
    val isScanning: StateFlow<Boolean> = bleManager.isScanning
    val connectedRssi: StateFlow<Int?> = bleManager.connectedRssi
    val diagnosticStats: StateFlow<DiagnosticStats> = bleManager.diagnosticStats
    val savedCaptures: StateFlow<List<SavedCapture>> = captureRepository.savedCaptures

    init {
        // Collect incoming binary packets and feed to processor off main thread
        scope.launch {
            bleManager.rawPacketFlow.collect { packet ->
                processor.processPacket(packet, _ch1Config.value, _ch2Config.value)
            }
        }

        // Display render & measurement update loop (smooth 30fps)
        scope.launch {
            while (isActive) {
                delay(33) // ~30 fps update
                if (_runState.value != RunState.STOP && _runState.value != RunState.FREEZE) {
                    val (frame, triggered) = processor.generateDisplayFrame(
                        timePerDivSec = _timePerDiv.value,
                        samplingRateHz = _samplingRateHz.value,
                        triggerSource = _triggerSource.value,
                        triggerMode = _triggerMode.value,
                        triggerSlope = _triggerSlope.value,
                        triggerLevelVolts = _triggerLevelVolts.value,
                        runState = _runState.value,
                        horizontalPanDiv = _horizontalPanDiv.value
                    )

                    if (frame.ch1Voltages.isNotEmpty()) {
                        _currentFrame.value = frame

                        // Calculate real measurements
                        val (m1, m2) = processor.computeMeasurements(frame, _samplingRateHz.value)
                        _ch1Measurements.value = m1
                        _ch2Measurements.value = m2

                        // Calculate FFT if in FFT domain or periodically
                        if (_displayDomain.value == DisplayDomain.FFT) {
                            val fft = processor.computeFft(frame, _samplingRateHz.value)
                            _fftResult.value = fft
                        }

                        // If single mode and triggered, halt running
                        if (_runState.value == RunState.SINGLE && triggered) {
                            _runState.value = RunState.STOP
                        }
                    }
                }
            }
        }
    }

    fun startBleScan(allowScanWhileConnected: Boolean = false) = bleManager.startScan(allowScanWhileConnected)
    fun stopBleScan() = bleManager.stopScan()
    fun connectToDevice(address: String) = bleManager.connectToDevice(address)
    fun reconnect() = bleManager.reconnect()
    fun disconnect() = bleManager.disconnect()

    fun setRunState(state: RunState) {
        _runState.value = state
    }

    fun toggleRunStop() {
        _runState.value = if (_runState.value == RunState.RUN) RunState.STOP else RunState.RUN
    }

    fun toggleFreeze() {
        _runState.value = if (_runState.value == RunState.FREEZE) RunState.RUN else RunState.FREEZE
    }

    fun singleTrigger() {
        _runState.value = RunState.SINGLE
    }

    fun setTriggerMode(mode: TriggerMode) {
        _triggerMode.value = mode
        preferences.saveTriggerMode(mode)
    }

    fun setTriggerSource(source: ChannelId) {
        _triggerSource.value = source
        preferences.saveTriggerSource(source)
    }

    fun setTriggerSlope(slope: TriggerSlope) {
        _triggerSlope.value = slope
        preferences.saveTriggerSlope(slope)
    }

    fun setTriggerLevel(level: Float) {
        _triggerLevelVolts.value = level
        preferences.saveTriggerLevel(level)
    }

    fun setTimePerDiv(timeSec: Float) {
        _timePerDiv.value = timeSec
        preferences.saveTimePerDiv(timeSec)
    }

    fun setSamplingRate(rateHz: Float) {
        _samplingRateHz.value = rateHz
        preferences.saveSamplingRate(rateHz)
    }

    fun setHorizontalPan(div: Float) {
        _horizontalPanDiv.value = div
    }

    fun setDisplayDomain(domain: DisplayDomain) {
        _displayDomain.value = domain
    }

    fun updateCh1Config(config: ChannelConfig) {
        _ch1Config.value = config
        preferences.saveCh1Config(config)
    }

    fun updateCh2Config(config: ChannelConfig) {
        _ch2Config.value = config
        preferences.saveCh2Config(config)
    }

    fun setCh1Enabled(enabled: Boolean) {
        val updated = _ch1Config.value.copy(enabled = enabled)
        updateCh1Config(updated)
    }

    fun setCh2Enabled(enabled: Boolean) {
        val updated = _ch2Config.value.copy(enabled = enabled)
        updateCh2Config(updated)
    }

    fun setCh1VoltPerDiv(volts: Float) {
        val updated = _ch1Config.value.copy(voltsPerDiv = volts)
        updateCh1Config(updated)
    }

    fun setCh2VoltPerDiv(volts: Float) {
        val updated = _ch2Config.value.copy(voltsPerDiv = volts)
        updateCh2Config(updated)
    }

    fun setCh1VerticalOffset(offsetDiv: Float) {
        val updated = _ch1Config.value.copy(verticalOffsetDiv = offsetDiv)
        updateCh1Config(updated)
    }

    fun setCh2VerticalOffset(offsetDiv: Float) {
        val updated = _ch2Config.value.copy(verticalOffsetDiv = offsetDiv)
        updateCh2Config(updated)
    }

    suspend fun clearWaveforms() {
        processor.clearBuffer()
        _currentFrame.value = WaveformFrame()
        _ch1Measurements.value = ChannelMeasurements()
        _ch2Measurements.value = ChannelMeasurements()
        _fftResult.value = FftResult()
    }

    suspend fun autoSet() {
        // Automatic scale detection based on current measurements
        val m1 = _ch1Measurements.value
        val m2 = _ch2Measurements.value

        if (m1.isValid && m1.vPp > 0.05f) {
            val bestDiv = when {
                m1.vPp < 0.3f -> 0.1f
                m1.vPp < 0.6f -> 0.2f
                m1.vPp < 1.5f -> 0.5f
                m1.vPp < 3.0f -> 1.0f
                m1.vPp < 6.0f -> 2.0f
                m1.vPp < 15.0f -> 5.0f
                else -> 10.0f
            }
            setCh1VoltPerDiv(bestDiv)
            setTriggerLevel((m1.vMax + m1.vMin) / 2.0f)
            if (m1.frequencyHz > 10f) {
                val period = 1.0f / m1.frequencyHz
                val targetTimeDiv = (period * 2.0f / 10f).coerceIn(100e-6f, 100e-3f)
                setTimePerDiv(targetTimeDiv)
            }
        }

        if (m2.isValid && m2.vPp > 0.05f) {
            val bestDiv = when {
                m2.vPp < 0.3f -> 0.1f
                m2.vPp < 0.6f -> 0.2f
                m2.vPp < 1.5f -> 0.5f
                m2.vPp < 3.0f -> 1.0f
                m2.vPp < 6.0f -> 2.0f
                m2.vPp < 15.0f -> 5.0f
                else -> 10.0f
            }
            setCh2VoltPerDiv(bestDiv)
        }
    }

    suspend fun saveCurrentCapture(): SavedCapture? {
        val frame = _currentFrame.value
        if (frame.ch1Voltages.isEmpty() && frame.ch2Voltages.isEmpty()) return null

        val capture = SavedCapture(
            sampleCount = minOf(frame.ch1Voltages.size, frame.ch2Voltages.size),
            samplingRateHz = _samplingRateHz.value,
            timePerDivSec = _timePerDiv.value,
            ch1VoltPerDiv = _ch1Config.value.voltsPerDiv,
            ch2VoltPerDiv = _ch2Config.value.voltsPerDiv,
            ch1DividerRatio = _ch1Config.value.dividerRatio,
            ch2DividerRatio = _ch2Config.value.dividerRatio,
            ch1OffsetVolts = _ch1Config.value.offsetVoltage,
            ch2OffsetVolts = _ch2Config.value.offsetVoltage,
            ch1CalMultiplier = _ch1Config.value.calMultiplier,
            ch2CalMultiplier = _ch2Config.value.calMultiplier,
            ch1Voltages = frame.ch1Voltages.copyOf(),
            ch2Voltages = frame.ch2Voltages.copyOf()
        )
        captureRepository.saveCapture(capture)
        return capture
    }

    fun shareCapture(capture: SavedCapture) {
        captureRepository.shareCaptureCsv(capture)
    }

    fun deleteCapture(capture: SavedCapture) {
        captureRepository.deleteCapture(capture)
    }
}
