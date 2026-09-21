#pragma once

#include <Arduino.h>
#include <stdint.h>

/**
 * ============================================================================
 * ESP32 DUAL-CHANNEL OSCILLOSCOPE PROTOCOL SPECIFICATION
 * ============================================================================
 * 
 * Electrical Pin Assignment:
 *   - CH1 (Input Signal) : GPIO34 (ADC1_CHANNEL_6)
 *   - CH2 (Output Signal): GPIO35 (ADC1_CHANNEL_7)
 * 
 * Safety & Electrical Constraints:
 *   - ESP32 ADC absolute maximum input range is 0.0V to 3.3V.
 *   - Exceeding 3.3V or negative voltages will permanently damage the silicon.
 *   - Always use a 10:1 or 100:1 resistive voltage divider with Schottky
 *     protection clamping diodes (e.g. BAT54S to GND & 3.3V) for signals > 3.3V.
 * ============================================================================
 */

#define SCOPE_MAGIC_0            0xAA
#define SCOPE_MAGIC_1            0x55
#define SCOPE_PROTOCOL_VER       0x02

#define SCOPE_CHAN_CH1_ONLY      0x01
#define SCOPE_CHAN_CH2_ONLY      0x02
#define SCOPE_CHAN_DUAL          0x03

#define TARGET_SAMPLE_RATE_HZ    100000.0f  // 100 kS/s per channel
#define AGGREGATE_ADC_RATE_HZ    200000.0f  // 200 kS/s aggregate (CH1 + CH2 interleaved)

#pragma pack(push, 1)

/**
 * Standard 8-byte Header for Oscilloscope Packets (Protocol V2)
 */
struct ScopePacketHeader {
    uint8_t magic0;            // 0xAA
    uint8_t magic1;            // 0x55
    uint8_t version;           // 0x02
    uint8_t channel;           // 0x03 for dual channel
    uint16_t sequenceNumber;   // Monotonically increasing packet sequence
    uint16_t sampleCount;      // Number of DualSample pairs in this packet
};

/**
 * Dual-channel raw ADC sample pair (12-bit, 0..4095)
 */
struct DualSample {
    uint16_t ch1;  // GPIO34 (ADC1_CH6) raw 12-bit ADC value
    uint16_t ch2;  // GPIO35 (ADC1_CH7) raw 12-bit ADC value
};

#pragma pack(pop)

/**
 * Compute 8-bit XOR checksum over packet data
 */
static inline uint8_t computePacketChecksum(const uint8_t* data, size_t length) {
    uint8_t checksum = 0;
    for (size_t i = 0; i < length; i++) {
        checksum ^= data[i];
    }
    return checksum;
}

/**
 * Compute signal frequency on ESP32 using zero-crossing detection
 */
static inline float calculateSignalFrequency(const uint16_t* samples, size_t count, float sampleRateHz) {
    if (count < 8 || sampleRateHz <= 0.0f) return 0.0f;

    uint32_t sum = 0;
    uint16_t minVal = 4095;
    uint16_t maxVal = 0;

    for (size_t i = 0; i < count; i++) {
        uint16_t v = samples[i];
        sum += v;
        if (v < minVal) minVal = v;
        if (v > maxVal) maxVal = v;
    }

    // Require minimum peak-to-peak voltage to avoid noise triggering
    if ((maxVal - minVal) < 60) { // ~50mV pk-pk threshold
        return 0.0f;
    }

    uint16_t midpoint = (uint16_t)(sum / count);
    uint16_t hysteresis = (maxVal - minVal) / 10;
    if (hysteresis < 8) hysteresis = 8;

    int firstCrossing = -1;
    int lastCrossing = -1;
    int crossingCount = 0;
    bool wasBelow = (samples[0] < midpoint);

    for (size_t i = 1; i < count; i++) {
        uint16_t val = samples[i];
        if (wasBelow && val > (midpoint + hysteresis)) {
            // Positive zero-crossing
            if (firstCrossing < 0) {
                firstCrossing = (int)i;
            } else {
                lastCrossing = (int)i;
                crossingCount++;
            }
            wasBelow = false;
        } else if (!wasBelow && val < (midpoint - hysteresis)) {
            wasBelow = true;
        }
    }

    if (crossingCount > 0 && lastCrossing > firstCrossing) {
        float avgPeriodSamples = (float)(lastCrossing - firstCrossing) / (float)crossingCount;
        if (avgPeriodSamples > 0.0f) {
            return sampleRateHz / avgPeriodSamples;
        }
    }

    return 0.0f;
}
