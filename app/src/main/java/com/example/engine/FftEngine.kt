package com.example.engine

import com.example.model.FftResult
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object FftEngine {

    private const val FFT_SIZE = 512

    /**
     * Computes real-time Fast Fourier Transform for CH1 and CH2 waveforms.
     * Uses Radix-2 Cooley-Tukey with Hann windowing.
     */
    fun compute(
        ch1Voltages: FloatArray,
        ch2Voltages: FloatArray,
        samplingRateHz: Float
    ): FftResult {
        if (samplingRateHz <= 0f) return FftResult()

        val n = FFT_SIZE
        val halfN = n / 2

        val freqs = FloatArray(halfN)
        val binWidth = samplingRateHz / n
        for (i in 0 until halfN) {
            freqs[i] = i * binWidth
        }

        val (mag1, dom1, maxMag1) = computeSingleChannel(ch1Voltages, n, freqs)
        val (mag2, dom2, maxMag2) = computeSingleChannel(ch2Voltages, n, freqs)

        return FftResult(
            frequencies = freqs,
            magnitudesCh1 = mag1,
            magnitudesCh2 = mag2,
            dominantFreqCh1 = dom1,
            dominantFreqCh2 = dom2,
            maxMagnitudeCh1 = maxMag1,
            maxMagnitudeCh2 = maxMag2
        )
    }

    private data class ChannelFftOutput(
        val magnitudes: FloatArray,
        val dominantFreq: Float,
        val maxMagnitude: Float
    )

    private fun computeSingleChannel(
        voltages: FloatArray,
        n: Int,
        freqs: FloatArray
    ): ChannelFftOutput {
        val halfN = n / 2
        val mags = FloatArray(halfN)

        if (voltages.isEmpty()) {
            return ChannelFftOutput(mags, 0f, 0f)
        }

        val real = DoubleArray(n)
        val imag = DoubleArray(n)

        // Window & copy: Use latest available samples up to n
        val sourceLen = voltages.size
        val start = (sourceLen - n).coerceAtLeast(0)
        val actualCount = minOf(n, sourceLen - start)

        for (i in 0 until actualCount) {
            val v = voltages[start + i].toDouble()
            // Hann window
            val w = 0.5 * (1.0 - cos(2.0 * PI * i / (n - 1)))
            real[i] = v * w
            imag[i] = 0.0
        }

        // Radix-2 FFT
        fftRadix2(real, imag, n)

        var peakMag = 0f
        var dominantFreq = 0f

        // Single-sided amplitude spectrum
        for (k in 0 until halfN) {
            val mag = (sqrt(real[k] * real[k] + imag[k] * imag[k]) * 2.0 / n).toFloat()
            mags[k] = mag

            // Ignore DC bin (k=0 and k=1) to prevent 0Hz offset from dominating AC signal
            if (k >= 2 && mag > peakMag) {
                peakMag = mag
                dominantFreq = freqs[k]
            }
        }

        return ChannelFftOutput(mags, dominantFreq, peakMag)
    }

    /**
     * In-place Radix-2 Decimation-In-Time Cooley-Tukey FFT
     */
    private fun fftRadix2(real: DoubleArray, imag: DoubleArray, n: Int) {
        // Bit-reversal permutation
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tempR = real[i]
                real[i] = real[j]
                real[j] = tempR

                val tempI = imag[i]
                imag[i] = imag[j]
                imag[j] = tempI
            }
            var k = n shr 1
            while (k <= j) {
                j -= k
                k = k shr 1
            }
            j += k
        }

        // Cooley-Tukey Butterflies
        var len = 2
        while (len <= n) {
            val halfLen = len shr 1
            val angle = -2.0 * PI / len
            val wStepR = cos(angle)
            val wStepI = sin(angle)

            var i = 0
            while (i < n) {
                var wR = 1.0
                var wI = 0.0
                for (m in 0 until halfLen) {
                    val uR = real[i + m]
                    val uI = imag[i + m]
                    val vR = real[i + m + halfLen] * wR - imag[i + m + halfLen] * wI
                    val vI = real[i + m + halfLen] * wI + imag[i + m + halfLen] * wR

                    real[i + m] = uR + vR
                    imag[i + m] = uI + vI
                    real[i + m + halfLen] = uR - vR
                    imag[i + m + halfLen] = uI - vI

                    val nextWR = wR * wStepR - wI * wStepI
                    val nextWI = wR * wStepI + wI * wStepR
                    wR = nextWR
                    wI = nextWI
                }
                i += len
            }
            len = len shl 1
        }
    }
}
