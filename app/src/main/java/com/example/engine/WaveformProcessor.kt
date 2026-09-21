package com.example.engine

import com.example.model.ChannelConfig
import com.example.model.ChannelId
import com.example.model.ChannelMeasurements
import com.example.model.FftResult
import com.example.model.RawWaveformPacket
import com.example.model.RunState
import com.example.model.TriggerMode
import com.example.model.TriggerSlope
import com.example.model.WaveformFrame
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WaveformProcessor(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    companion object {
        const val BUFFER_CAPACITY = 65536
        const val GRID_HORIZONTAL_DIVS = 10
    }

    // Circular buffers for raw & calibrated values
    private val ch1Buffer = FloatArray(BUFFER_CAPACITY)
    private val ch2Buffer = FloatArray(BUFFER_CAPACITY)
    private var bufferHead = 0
    private var totalSamplesBuffered = 0

    // Frozen snapshot for FREEZE / STOP
    private var frozenFrame: WaveformFrame? = null

    suspend fun processPacket(
        packet: RawWaveformPacket,
        ch1Config: ChannelConfig,
        ch2Config: ChannelConfig
    ) = withContext(dispatcher) {
        val count = packet.numSamples
        for (i in 0 until count) {
            val v1 = ch1Config.adcToVoltage(packet.ch1Raw[i])
            val v2 = ch2Config.adcToVoltage(packet.ch2Raw[i])

            ch1Buffer[bufferHead] = v1
            ch2Buffer[bufferHead] = v2

            bufferHead = (bufferHead + 1) % BUFFER_CAPACITY
            if (totalSamplesBuffered < BUFFER_CAPACITY) {
                totalSamplesBuffered++
            }
        }
    }

    suspend fun clearBuffer() = withContext(dispatcher) {
        bufferHead = 0
        totalSamplesBuffered = 0
        frozenFrame = null
    }

    suspend fun generateDisplayFrame(
        timePerDivSec: Float,
        samplingRateHz: Float,
        triggerSource: ChannelId,
        triggerMode: TriggerMode,
        triggerSlope: TriggerSlope,
        triggerLevelVolts: Float,
        runState: RunState,
        horizontalPanDiv: Float
    ): Pair<WaveformFrame, Boolean> = withContext(dispatcher) {
        if (runState == RunState.FREEZE || runState == RunState.STOP) {
            frozenFrame?.let { return@withContext Pair(it, it.isTriggered) }
        }

        if (totalSamplesBuffered < 16) {
            return@withContext Pair(WaveformFrame(samplingRateHz = samplingRateHz), false)
        }

        // Linearize circular buffer into contiguous arrays (oldest to newest)
        val available = totalSamplesBuffered
        val linearCh1 = FloatArray(available)
        val linearCh2 = FloatArray(available)

        val startIdx = if (totalSamplesBuffered == BUFFER_CAPACITY) bufferHead else 0
        for (i in 0 until available) {
            val ringIdx = (startIdx + i) % BUFFER_CAPACITY
            linearCh1[i] = ch1Buffer[ringIdx]
            linearCh2[i] = ch2Buffer[ringIdx]
        }

        // Compute required window size for 10 horizontal divisions
        val windowDurationSec = GRID_HORIZONTAL_DIVS * timePerDivSec
        val targetWindowPoints = (windowDurationSec * samplingRateHz).toInt().coerceIn(16, available)

        val triggerSourceArray = if (triggerSource == ChannelId.CH1) linearCh1 else linearCh2

        val trigResult = TriggerEngine.process(
            voltages = triggerSourceArray,
            triggerLevelVolts = triggerLevelVolts,
            slope = triggerSlope,
            mode = triggerMode,
            windowSize = targetWindowPoints
        )

        // Apply horizontal pan (in divisions converted to sample offset)
        val panSamples = (horizontalPanDiv * timePerDivSec * samplingRateHz).toInt()

        val rawStart = if (trigResult.triggerIndex >= 0) {
            trigResult.triggerIndex + panSamples
        } else {
            (available - targetWindowPoints + panSamples)
        }
        val safeStart = rawStart.coerceIn(0, available - targetWindowPoints)

        val frameCh1 = FloatArray(targetWindowPoints)
        val frameCh2 = FloatArray(targetWindowPoints)
        System.arraycopy(linearCh1, safeStart, frameCh1, 0, targetWindowPoints)
        System.arraycopy(linearCh2, safeStart, frameCh2, 0, targetWindowPoints)

        val frame = WaveformFrame(
            ch1Voltages = frameCh1,
            ch2Voltages = frameCh2,
            samplingRateHz = samplingRateHz,
            triggerIndex = targetWindowPoints / 2,
            isTriggered = trigResult.triggered
        )

        if (runState == RunState.SINGLE && trigResult.triggered) {
            frozenFrame = frame
        } else if (runState == RunState.STOP || runState == RunState.FREEZE) {
            frozenFrame = frame
        }

        Pair(frame, trigResult.triggered)
    }

    suspend fun computeMeasurements(
        frame: WaveformFrame,
        samplingRateHz: Float
    ): Pair<ChannelMeasurements, ChannelMeasurements> = withContext(dispatcher) {
        val m1 = MeasurementEngine.calculate(frame.ch1Voltages, samplingRateHz)
        val m2 = MeasurementEngine.calculate(frame.ch2Voltages, samplingRateHz)
        Pair(m1, m2)
    }

    suspend fun computeFft(
        frame: WaveformFrame,
        samplingRateHz: Float
    ): FftResult = withContext(dispatcher) {
        FftEngine.compute(frame.ch1Voltages, frame.ch2Voltages, samplingRateHz)
    }
}
