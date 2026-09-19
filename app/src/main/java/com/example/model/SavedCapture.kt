package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SavedCapture(
    val id: String = System.currentTimeMillis().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val title: String = "Capture_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}",
    val sampleCount: Int,
    val samplingRateHz: Float,
    val timePerDivSec: Float,
    val ch1VoltPerDiv: Float,
    val ch2VoltPerDiv: Float,
    val ch1DividerRatio: Float,
    val ch2DividerRatio: Float,
    val ch1OffsetVolts: Float,
    val ch2OffsetVolts: Float,
    val ch1CalMultiplier: Float,
    val ch2CalMultiplier: Float,
    val ch1Voltages: FloatArray,
    val ch2Voltages: FloatArray
) {
    val formattedDate: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp))

    fun toCsv(): String {
        val sb = StringBuilder()
        sb.appendLine("# Digital Oscilloscope Waveform Capture")
        sb.appendLine("# Capture ID: $id")
        sb.appendLine("# Date: $formattedDate")
        sb.appendLine("# Sampling Rate: ${samplingRateHz.toLong()} Hz")
        sb.appendLine("# Time/Div: $timePerDivSec s")
        sb.appendLine("# CH1 Volt/Div: $ch1VoltPerDiv V, Divider: $ch1DividerRatio, Cal: $ch1CalMultiplier, Offset: $ch1OffsetVolts V")
        sb.appendLine("# CH2 Volt/Div: $ch2VoltPerDiv V, Divider: $ch2DividerRatio, Cal: $ch2CalMultiplier, Offset: $ch2OffsetVolts V")
        sb.appendLine("SampleIndex,Time_s,CH1_Volts,CH2_Volts")

        val dt = if (samplingRateHz > 0) 1.0f / samplingRateHz else 0.00001f
        val count = minOf(ch1Voltages.size, ch2Voltages.size)
        for (i in 0 until count) {
            val t = i * dt
            sb.appendLine(String.format(Locale.US, "%d,%.7f,%.4f,%.4f", i, t, ch1Voltages[i], ch2Voltages[i]))
        }
        return sb.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SavedCapture
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
