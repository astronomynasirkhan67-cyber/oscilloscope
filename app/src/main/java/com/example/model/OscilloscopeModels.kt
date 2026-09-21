package com.example.model

enum class ChannelId(val label: String, val pinName: String, val colorHex: Long) {
    CH1("CH1", "GPIO34 / ADC1_CH6", 0xFFFFE500), // Yellow trace
    CH2("CH2", "GPIO35 / ADC1_CH7", 0xFF00E5FF)  // Cyan trace
}

enum class TriggerMode(val label: String) {
    AUTO("Auto"),
    NORMAL("Normal"),
    SINGLE("Single")
}

enum class TriggerSlope(val label: String) {
    RISING("Rising /"),
    FALLING("Falling \\")
}

enum class RunState(val label: String) {
    RUN("RUN"),
    STOP("STOP"),
    SINGLE("SINGLE"),
    FREEZE("FREEZE")
}

enum class DisplayDomain(val label: String) {
    TIME("Time Domain"),
    FFT("Frequency Domain (FFT)")
}

data class TimeDivOption(val seconds: Float, val label: String)

val AVAILABLE_TIME_DIVS = listOf(
    TimeDivOption(10e-6f, "10 µs"),
    TimeDivOption(20e-6f, "20 µs"),
    TimeDivOption(50e-6f, "50 µs"),
    TimeDivOption(100e-6f, "100 µs"),
    TimeDivOption(200e-6f, "200 µs"),
    TimeDivOption(500e-6f, "500 µs"),
    TimeDivOption(1e-3f, "1 ms"),
    TimeDivOption(2e-3f, "2 ms"),
    TimeDivOption(5e-3f, "5 ms"),
    TimeDivOption(10e-3f, "10 ms"),
    TimeDivOption(20e-3f, "20 ms"),
    TimeDivOption(50e-3f, "50 ms"),
    TimeDivOption(100e-3f, "100 ms")
)

data class VoltDivOption(val volts: Float, val label: String)

val AVAILABLE_VOLT_DIVS = listOf(
    VoltDivOption(0.1f, "100 mV"),
    VoltDivOption(0.2f, "200 mV"),
    VoltDivOption(0.5f, "500 mV"),
    VoltDivOption(1.0f, "1 V"),
    VoltDivOption(2.0f, "2 V"),
    VoltDivOption(5.0f, "5 V"),
    VoltDivOption(10.0f, "10 V"),
    VoltDivOption(20.0f, "20 V")
)

data class ChannelConfig(
    val channel: ChannelId,
    val enabled: Boolean = true,
    val voltsPerDiv: Float = 1.0f,
    val verticalOffsetDiv: Float = 0.0f,
    // Calibration parameters
    val adcMax: Float = 4095.0f,          // ESP32 12-bit ADC (0 - 4095)
    val vRef: Float = 3.3f,               // ESP32 3.3V reference
    val dividerRatio: Float = 1.0f,       // External voltage-divider ratio (1.0 = direct, 10.0 = 10:1 probe)
    val calMultiplier: Float = 1.0f,      // Fine calibration multiplier
    val offsetVoltage: Float = 0.0f       // Calibrated zero-offset in Volts
) {
    /**
     * Converts a raw uint16 ADC code from ESP32 into actual calibrated input voltage.
     * Formula: Voltage = (((ADC / adcMax) * vRef * dividerRatio) + offsetVoltage) * calMultiplier
     */
    fun adcToVoltage(rawAdc: Int): Float {
        val clampedAdc = rawAdc.coerceIn(0, adcMax.toInt())
        val uncalibratedV = (clampedAdc.toFloat() / adcMax) * vRef * dividerRatio
        return (uncalibratedV + offsetVoltage) * calMultiplier
    }
}

data class ChannelMeasurements(
    val vMax: Float = 0.0f,
    val vMin: Float = 0.0f,
    val vPp: Float = 0.0f,
    val rms: Float = 0.0f,
    val frequencyHz: Float = 0.0f,
    val periodSec: Float = 0.0f,
    val isValid: Boolean = false
)

data class BleDeviceItem(
    val name: String?,
    val address: String,
    val rssi: Int = 0,
    val isTargetOscilloscope: Boolean = false
)

data class RawWaveformPacket(
    val sequenceNumber: Int,
    val numSamples: Int,
    val ch1Raw: IntArray,
    val ch2Raw: IntArray,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as RawWaveformPacket
        return sequenceNumber == other.sequenceNumber &&
                numSamples == other.numSamples &&
                ch1Raw.contentEquals(other.ch1Raw) &&
                ch2Raw.contentEquals(other.ch2Raw)
    }

    override fun hashCode(): Int {
        var result = sequenceNumber
        result = 31 * result + numSamples
        result = 31 * result + ch1Raw.contentHashCode()
        result = 31 * result + ch2Raw.contentHashCode()
        return result
    }
}

data class WaveformFrame(
    val ch1Voltages: FloatArray = FloatArray(0),
    val ch2Voltages: FloatArray = FloatArray(0),
    val samplingRateHz: Float = 100000.0f,
    val triggerIndex: Int = 0,
    val isTriggered: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as WaveformFrame
        return ch1Voltages.contentEquals(other.ch1Voltages) &&
                ch2Voltages.contentEquals(other.ch2Voltages) &&
                samplingRateHz == other.samplingRateHz &&
                triggerIndex == other.triggerIndex &&
                isTriggered == other.isTriggered
    }

    override fun hashCode(): Int {
        var result = ch1Voltages.contentHashCode()
        result = 31 * result + ch2Voltages.contentHashCode()
        result = 31 * result + samplingRateHz.hashCode()
        result = 31 * result + triggerIndex
        result = 31 * result + isTriggered.hashCode()
        return result
    }
}

data class FftResult(
    val frequencies: FloatArray = FloatArray(0),
    val magnitudesCh1: FloatArray = FloatArray(0),
    val magnitudesCh2: FloatArray = FloatArray(0),
    val dominantFreqCh1: Float = 0.0f,
    val dominantFreqCh2: Float = 0.0f,
    val maxMagnitudeCh1: Float = 0.0f,
    val maxMagnitudeCh2: Float = 0.0f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FftResult
        return frequencies.contentEquals(other.frequencies) &&
                magnitudesCh1.contentEquals(other.magnitudesCh1) &&
                magnitudesCh2.contentEquals(other.magnitudesCh2)
    }

    override fun hashCode(): Int {
        var result = frequencies.contentHashCode()
        result = 31 * result + magnitudesCh1.contentHashCode()
        result = 31 * result + magnitudesCh2.contentHashCode()
        return result
    }
}

data class DiagnosticStats(
    val totalPacketsReceived: Long = 0,
    val droppedPackets: Long = 0,
    val corruptedPackets: Long = 0,
    val packetsPerSec: Float = 0.0f,
    val samplesPerSec: Float = 0.0f
)
