#pragma once

#include <Arduino.h>
#include "ScopeProtocol.h"
#include <esp_idf_version.h>
#include <soc/soc_caps.h>
#include <driver/gpio.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include <freertos/semphr.h>

#if ESP_IDF_VERSION >= ESP_IDF_VERSION_VAL(5, 0, 0)
#include <esp_adc/adc_continuous.h>
#else
#include <driver/adc.h>
#endif

/**
 * ============================================================================
 * CRITICAL ELECTRICAL SAFETY NOTICE:
 * GPIO34 (CH1 / ADC1_CH6) and GPIO35 (CH2 / ADC1_CH7) MUST NEVER BE CONNECTED
 * DIRECTLY TO ARBITRARY OR HIGH EXTERNAL VOLTAGES.
 * 
 * The ESP32 ADC absolute maximum input range is 0.0V to 3.3V.
 * Exceeding 3.3V or applying negative voltages will cause permanent silicon damage.
 * External signals must always pass through a properly calculated resistive
 * voltage divider and protective Schottky clamping diodes before reaching the pins.
 * ============================================================================
 */

#define PIN_CH1_GPIO34        34 // ADC1_CHANNEL_6
#define PIN_CH2_GPIO35        35 // ADC1_CHANNEL_7

#define ADC_CHANNEL_CH1       ADC_CHANNEL_6
#define ADC_CHANNEL_CH2       ADC_CHANNEL_7

#ifndef ADC_ATTEN_DB_12
#define ADC_ATTEN_DB_12       ADC_ATTEN_DB_11
#endif

// Frame capacity: number of dual-channel sample pairs per transmission block
#define FRAME_SAMPLES_COUNT   100

// Internal double-buffer size for continuous DMA acquisition
#define DMA_READ_BUFFER_SIZE  1024

class AdcDmaEngine {
public:
    AdcDmaEngine() 
        : _activeBufferIndex(0),
          _samplesReadySemaphore(NULL),
          _isDmaRunning(false),
          _acquisitionTaskHandle(NULL),
          _totalSamplePairsAcquired(0),
          _samplingRateHz(TARGET_SAMPLE_RATE_HZ)
#if ESP_IDF_VERSION >= ESP_IDF_VERSION_VAL(5, 0, 0)
        , _adcHandle(NULL)
#endif
    {}

    bool init() {
        Serial.println("[ADC-DMA] Initializing ESP32 Continuous ADC with DMA...");
        Serial.printf("[ADC-DMA] CH1: GPIO%d (ADC1_CH%d), CH2: GPIO%d (ADC1_CH%d)\n",
                      PIN_CH1_GPIO34, ADC_CHANNEL_CH1, PIN_CH2_GPIO35, ADC_CHANNEL_CH2);
        Serial.printf("[ADC-DMA] Target sampling rate: %.0f Hz per channel (%.0f Hz aggregate)\n",
                      TARGET_SAMPLE_RATE_HZ, AGGREGATE_ADC_RATE_HZ);

        // Configure input pins
        pinMode(PIN_CH1_GPIO34, INPUT);
        pinMode(PIN_CH2_GPIO35, INPUT);

        _samplesReadySemaphore = xSemaphoreCreateBinary();

#if ESP_IDF_VERSION >= ESP_IDF_VERSION_VAL(5, 0, 0)
        // ESP-IDF 5.x Continuous ADC API
        adc_continuous_handle_cfg_t adc_config = {
            .max_store_buf_size = 4096,
            .conv_frame_size = 512,
        };

        esp_err_t err = adc_continuous_new_handle(&adc_config, &_adcHandle);
        if (err != ESP_OK) {
            Serial.printf("[ADC-DMA] ERROR: Failed to create continuous ADC handle (0x%x)\n", err);
            return false;
        }

        adc_continuous_pattern_config_t pattern[2];
        memset(pattern, 0, sizeof(pattern));

        // Pattern 0: CH1 (GPIO34)
        pattern[0].atten = ADC_ATTEN_DB_12;
        pattern[0].channel = ADC_CHANNEL_CH1;
        pattern[0].unit = ADC_UNIT_1;
        pattern[0].bit_width = 12;

        // Pattern 1: CH2 (GPIO35)
        pattern[1].atten = ADC_ATTEN_DB_12;
        pattern[1].channel = ADC_CHANNEL_CH2;
        pattern[1].unit = ADC_UNIT_1;
        pattern[1].bit_width = 12;

        adc_continuous_config_t dig_cfg = {
            .pattern_num = 2,
            .adc_pattern = pattern,
            .sample_freq_hz = (uint32_t)AGGREGATE_ADC_RATE_HZ,
            .conv_mode = ADC_CONV_SINGLE_UNIT_1,
            .format = ADC_DIGI_OUTPUT_FORMAT_TYPE1,
        };

        err = adc_continuous_config(_adcHandle, &dig_cfg);
        if (err != ESP_OK) {
            Serial.printf("[ADC-DMA] ERROR: Failed to config continuous ADC (0x%x)\n", err);
            return false;
        }
#else
        // ESP-IDF 4.x / Arduino Core 2.x Continuous ADC API
        adc_digi_init_config_t adc_init_config = {
            .max_store_buf_size = 4096,
            .conv_num_each_intr = 512,
            .adc1_chan_mask = (uint32_t)((1 << ADC_CHANNEL_CH1) | (1 << ADC_CHANNEL_CH2)),
            .adc2_chan_mask = 0,
        };

        esp_err_t err = adc_digi_initialize(&adc_init_config);
        if (err != ESP_OK) {
            Serial.printf("[ADC-DMA] ERROR: Failed to initialize digital ADC (0x%x)\n", err);
            return false;
        }

        adc_digi_pattern_config_t pattern[2];
        memset(pattern, 0, sizeof(pattern));

        // Pattern 0: CH1 (GPIO34)
        pattern[0].atten = ADC_ATTEN_DB_11;
        pattern[0].channel = ADC_CHANNEL_CH1;
        pattern[0].unit = 0; // ADC_UNIT_1
        pattern[0].bit_width = 12;

        // Pattern 1: CH2 (GPIO35)
        pattern[1].atten = ADC_ATTEN_DB_11;
        pattern[1].channel = ADC_CHANNEL_CH2;
        pattern[1].unit = 0; // ADC_UNIT_1
        pattern[1].bit_width = 12;

        adc_digi_configuration_t dig_cfg = {
            .conv_limit_en = false,
            .conv_limit_num = 250,
            .pattern_num = 2,
            .adc_pattern = pattern,
            .sample_freq_hz = (uint32_t)AGGREGATE_ADC_RATE_HZ,
            .conv_mode = ADC_CONV_SINGLE_UNIT_1,
            .format = ADC_DIGI_OUTPUT_FORMAT_TYPE1,
        };

        err = adc_digi_controller_configure(&dig_cfg);
        if (err != ESP_OK) {
            Serial.printf("[ADC-DMA] ERROR: Failed to configure ADC digital controller (0x%x)\n", err);
            return false;
        }
#endif

        Serial.println("[ADC-DMA] Continuous ADC configured successfully at 100 kS/s per channel.");
        return true;
    }

    bool start() {
        esp_err_t err = ESP_OK;
#if ESP_IDF_VERSION >= ESP_IDF_VERSION_VAL(5, 0, 0)
        if (!_adcHandle) return false;
        err = adc_continuous_start(_adcHandle);
#else
        err = adc_digi_start();
#endif
        if (err != ESP_OK) {
            Serial.printf("[ADC-DMA] ERROR: Failed to start continuous ADC (0x%x)\n", err);
            return false;
        }

        _isDmaRunning = true;

        // Spawn dedicated high-priority ADC acquisition worker pinned to Core 0
        // (FreeRTOS Core 0 executes DMA acquisition; Core 1 handles BLE stack & packetization)
        xTaskCreatePinnedToCore(
            acquisitionTaskStatic,
            "AdcAcquisitionTask",
            4096,
            this,
            configMAX_PRIORITIES - 2,
            &_acquisitionTaskHandle,
            0 // Core 0
        );

        Serial.println("[ADC-DMA] Continuous acquisition task started on Core 0.");
        return true;
    }

    void stop() {
        _isDmaRunning = false;
        if (_acquisitionTaskHandle) {
            vTaskDelete(_acquisitionTaskHandle);
            _acquisitionTaskHandle = NULL;
        }
#if ESP_IDF_VERSION >= ESP_IDF_VERSION_VAL(5, 0, 0)
        if (_adcHandle) {
            adc_continuous_stop(_adcHandle);
        }
#else
        adc_digi_stop();
#endif
    }

    /**
     * Waits for the next populated ping-pong double buffer.
     * Guaranteed thread-safe between Core 0 (ADC DMA) and Core 1 (BLE task).
     */
    bool waitForBuffer(DualSample** outBuffer, size_t* outCount, TickType_t waitTicks = portMAX_DELAY) {
        if (xSemaphoreTake(_samplesReadySemaphore, waitTicks) == pdTRUE) {
            // Buffer ready: return the inactive buffer index that was just completed
            uint8_t readyIndex = 1 - _activeBufferIndex;
            *outBuffer = _doubleBuffer[readyIndex];
            *outCount = FRAME_SAMPLES_COUNT;
            return true;
        }
        return false;
    }

    float getActualSampleRateHz() const {
        return _samplingRateHz;
    }

    uint64_t getTotalSamplePairsAcquired() const {
        return _totalSamplePairsAcquired;
    }

private:
    static void acquisitionTaskStatic(void* arg) {
        static_cast<AdcDmaEngine*>(arg)->acquisitionTaskLoop();
    }

    void acquisitionTaskLoop() {
        uint8_t dmaRawBytes[DMA_READ_BUFFER_SIZE];
        uint32_t bytesRead = 0;
        size_t currentPairIndex = 0;

        uint16_t pendingCh1 = 0;
        uint16_t pendingCh2 = 0;
        bool hasCh1 = false;
        bool hasCh2 = false;

        while (_isDmaRunning) {
            esp_err_t ret = ESP_OK;

#if ESP_IDF_VERSION >= ESP_IDF_VERSION_VAL(5, 0, 0)
            ret = adc_continuous_read(
                _adcHandle,
                dmaRawBytes,
                sizeof(dmaRawBytes),
                &bytesRead,
                portMAX_DELAY
            );
#else
            ret = adc_digi_read_bytes(
                dmaRawBytes,
                sizeof(dmaRawBytes),
                &bytesRead,
                ADC_MAX_DELAY
            );
#endif

            if (ret == ESP_OK && bytesRead > 0) {
                // Parse ADC_DIGI_OUTPUT_FORMAT_TYPE1 data
                // Format: uint16_t with 12 bits data (0..4095) and 4 bits channel (0..15)
                for (size_t i = 0; i < bytesRead; i += sizeof(adc_digi_output_data_t)) {
                    adc_digi_output_data_t* p = (adc_digi_output_data_t*)&dmaRawBytes[i];
                    uint16_t chan = p->type1.channel;
                    uint16_t rawAdc = p->type1.data;

                    // Strictly route channel to prevent swapping
                    if (chan == ADC_CHANNEL_CH1) {
                        pendingCh1 = rawAdc;
                        hasCh1 = true;
                    } else if (chan == ADC_CHANNEL_CH2) {
                        pendingCh2 = rawAdc;
                        hasCh2 = true;
                    }

                    // Once we have a synchronized pair from both channels:
                    if (hasCh1 && hasCh2) {
                        _doubleBuffer[_activeBufferIndex][currentPairIndex].ch1 = pendingCh1;
                        _doubleBuffer[_activeBufferIndex][currentPairIndex].ch2 = pendingCh2;
                        currentPairIndex++;
                        _totalSamplePairsAcquired++;

                        hasCh1 = false;
                        hasCh2 = false;

                        // Check if active buffer is full
                        if (currentPairIndex >= FRAME_SAMPLES_COUNT) {
                            // Swap ping-pong double buffer
                            _activeBufferIndex = 1 - _activeBufferIndex;
                            currentPairIndex = 0;

                            // Notify consumer task on Core 1
                            xSemaphoreGive(_samplesReadySemaphore);
                        }
                    }
                }
            } else {
                // Yield briefly on timeout or error
                vTaskDelay(1);
            }
        }

        vTaskDelete(NULL);
    }

#if ESP_IDF_VERSION >= ESP_IDF_VERSION_VAL(5, 0, 0)
    adc_continuous_handle_t _adcHandle;
#endif

    // Double buffer architecture: 2 buffers of FRAME_SAMPLES_COUNT DualSample pairs
    DualSample _doubleBuffer[2][FRAME_SAMPLES_COUNT];
    volatile uint8_t _activeBufferIndex;

    SemaphoreHandle_t _samplesReadySemaphore;
    volatile bool _isDmaRunning;
    TaskHandle_t _acquisitionTaskHandle;
    volatile uint64_t _totalSamplePairsAcquired;
    float _samplingRateHz;
};
