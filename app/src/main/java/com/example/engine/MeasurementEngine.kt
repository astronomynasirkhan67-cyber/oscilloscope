package com.example.engine

import com.example.model.ChannelMeasurements
import kotlin.math.max
import kotlin.math.sqrt

object MeasurementEngine {

    /**
     * Calculates authentic oscilloscope measurements from actual received voltage samples.
     * Never uses hardcoded or random values.
     *
     * @param voltages Calibrated voltage samples
     * @param samplingRateHz Hardware sampling rate
     * @return ChannelMeasurements with Vmax, Vmin, Vpp, RMS, Frequency, and Period.
     */
    fun calculate(voltages: FloatArray, samplingRateHz: Float): ChannelMeasurements {
        if (voltages.isEmpty() || voltages.size < 4) {
            return ChannelMeasurements()
        }

        var min = Float.MAX_VALUE
        var max = -Float.MAX_VALUE
        var sumSquares = 0.0

        for (v in voltages) {
            if (v < min) min = v
            if (v > max) max = v
            sumSquares += (v.toDouble() * v.toDouble())
        }

        val vMin = min
        val vMax = max
        val vPp = max - min
        val rms = sqrt(sumSquares / voltages.size).toFloat()

        // Noise gate: if peak-to-peak is below 30mV, treat as flatline/noise
        if (vPp < 0.03f || samplingRateHz <= 0f) {
            return ChannelMeasurements(
                vMax = vMax,
                vMin = vMin,
                vPp = vPp,
                rms = rms,
                frequencyHz = 0f,
                periodSec = 0f,
                isValid = true
            )
        }

        // Frequency & Period calculation using Schmitt-trigger midpoint zero-crossing with linear interpolation
        val midThreshold = (vMax + vMin) / 2.0f
        val hysteresis = max(0.04f * vPp, 0.015f)
        val highThresh = midThreshold + hysteresis
        val lowThresh = midThreshold - hysteresis

        val risingCrossingIndices = ArrayList<Float>()
        var stateIsLow = voltages[0] < midThreshold

        for (i in 0 until voltages.size - 1) {
            val v0 = voltages[i]
            val v1 = voltages[i + 1]

            if (stateIsLow) {
                if (v1 > highThresh) {
                    // Upward crossing detected between i and i+1
                    // Linear interpolation to determine sub-sample exact crossing point
                    val fraction = if (v1 != v0) {
                        ((midThreshold - v0) / (v1 - v0)).coerceIn(0f, 1f)
                    } else 0.5f
                    risingCrossingIndices.add(i + fraction)
                    stateIsLow = false
                }
            } else {
                if (v1 < lowThresh) {
                    stateIsLow = true
                }
            }
        }

        val (freq, period) = if (risingCrossingIndices.size >= 2) {
            var totalPeriodSamples = 0.0
            val cycleCount = risingCrossingIndices.size - 1
            for (k in 0 until cycleCount) {
                totalPeriodSamples += (risingCrossingIndices[k + 1] - risingCrossingIndices[k])
            }
            val avgPeriodSamples = totalPeriodSamples / cycleCount
            val periodSec = (avgPeriodSamples / samplingRateHz).toFloat()
            val freqHz = if (periodSec > 0f) 1.0f / periodSec else 0f
            Pair(freqHz, periodSec)
        } else {
            Pair(0f, 0f)
        }

        return ChannelMeasurements(
            vMax = vMax,
            vMin = vMin,
            vPp = vPp,
            rms = rms,
            frequencyHz = freq,
            periodSec = period,
            isValid = true
        )
    }
}
