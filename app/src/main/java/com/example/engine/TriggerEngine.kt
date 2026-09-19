package com.example.engine

import com.example.model.TriggerMode
import com.example.model.TriggerSlope

object TriggerEngine {

    data class TriggerResult(
        val triggered: Boolean,
        val triggerIndex: Int
    )

    /**
     * Searches for trigger event in the source waveform buffer.
     *
     * @param voltages Voltage samples of the selected trigger source (CH1 or CH2)
     * @param triggerLevelVolts Selected trigger threshold level in Volts
     * @param slope Rising or Falling edge
     * @param mode Auto, Normal, Single
     * @param windowSize Desired display frame size
     * @return TriggerResult containing whether trigger succeeded and the alignment index
     */
    fun process(
        voltages: FloatArray,
        triggerLevelVolts: Float,
        slope: TriggerSlope,
        mode: TriggerMode,
        windowSize: Int
    ): TriggerResult {
        if (voltages.size < windowSize) {
            return TriggerResult(triggered = false, triggerIndex = 0)
        }

        // We want the trigger point to ideally appear at the horizontal center or 1/4 of the display
        val halfWindow = windowSize / 2
        val maxSearchIndex = voltages.size - halfWindow - 1
        val minSearchIndex = 1

        if (maxSearchIndex <= minSearchIndex) {
            val fallbackIndex = (voltages.size - windowSize).coerceAtLeast(0)
            return TriggerResult(triggered = false, triggerIndex = fallbackIndex)
        }

        var foundIndex = -1

        for (i in minSearchIndex until maxSearchIndex) {
            val v0 = voltages[i - 1]
            val v1 = voltages[i]

            when (slope) {
                TriggerSlope.RISING -> {
                    if (v0 <= triggerLevelVolts && v1 > triggerLevelVolts) {
                        foundIndex = i
                        break
                    }
                }
                TriggerSlope.FALLING -> {
                    if (v0 >= triggerLevelVolts && v1 < triggerLevelVolts) {
                        foundIndex = i
                        break
                    }
                }
            }
        }

        return if (foundIndex != -1) {
            // Align so found trigger point is at the center of the window
            val alignedStart = (foundIndex - halfWindow).coerceIn(0, voltages.size - windowSize)
            TriggerResult(triggered = true, triggerIndex = alignedStart)
        } else {
            // No trigger found
            when (mode) {
                TriggerMode.AUTO -> {
                    // Auto mode: free-run using newest available window
                    val latestStart = (voltages.size - windowSize).coerceAtLeast(0)
                    TriggerResult(triggered = false, triggerIndex = latestStart)
                }
                TriggerMode.NORMAL, TriggerMode.SINGLE -> {
                    // Normal / Single mode: retain previous or report not triggered
                    TriggerResult(triggered = false, triggerIndex = -1)
                }
            }
        }
    }
}
