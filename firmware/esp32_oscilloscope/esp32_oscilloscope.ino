/**
 * ============================================================================
 * ESP32 DUAL-CHANNEL HIGH-SPEED OSCILLOSCOPE FIRMWARE (100 kS/s DMA)
 * ============================================================================
 * 
 * Hardware Configuration:
 *   - Board: ESP32 Dev Module (Dual-core Xtensa 32-bit LX6 @ 240 MHz)
 *   - CH1 Input: GPIO34 (ADC1_CHANNEL_6)
 *   - CH2 Input: GPIO35 (ADC1_CHANNEL_7)
 *   - Resolution: 12-bit (0 - 4095)
 *   - ADC Sampling Mode: Continuous DMA / Hardware Clocked
 *   - Target Sampling Rate: 100,000 samples/sec per channel (200 kS/s aggregate)
 *   - Architecture:
 *       Core 0: Continuous ADC/DMA acquisition filling ping-pong double buffer
 *       Core 1: On-device frequency calculation, BLE packetizer, & notification
 * 
 * Electrical Safety Warning:
 *   - GPIO34 and GPIO35 MUST NEVER receive external voltages above 3.3V or below 0V.
 *   - Always use an external resistive voltage divider (e.g. 10:1 or 100:1 probe)
 *     with clamping protection diodes before connecting signals to GPIO34/GPIO35.
 * 
 * BLE Specifications:
 *   - Device Name: "ESP32-Oscilloscope"
 *   - Service UUID: 6e400001-b5a3-f393-e0a9-e50e24dcca9e
 *   - Waveform Characteristic UUID: 6e400003-b5a3-f393-e0a9-e50e24dcca9e
 *   - Packet Format: V2 Compact Binary framing with XOR checksum
 * ============================================================================
 */

#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#include "ScopeProtocol.h"
#include "AdcDmaEngine.h"

#define SERVICE_UUID           "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
#define WAVEFORM_CHAR_UUID     "6e400003-b5a3-f393-e0a9-e50e24dcca9e"
#define DEVICE_NAME            "ESP32-Oscilloscope"

// Global instances
static AdcDmaEngine adcEngine;
static BLEServer* pServer = nullptr;
static BLECharacteristic* pWaveformCharacteristic = nullptr;
static bool deviceConnected = false;
static bool oldDeviceConnected = false;
static uint16_t packetSequence = 0;

// High-speed packet buffer:
// Header (8 bytes) + (100 * 4 bytes DualSample) + Checksum (1 byte) = 409 bytes
#define PACKET_PAYLOAD_SIZE (sizeof(ScopePacketHeader) + (FRAME_SAMPLES_COUNT * sizeof(DualSample)) + 1)
static uint8_t bleTransmitPacket[PACKET_PAYLOAD_SIZE];

// BLE Server Callbacks
class ScopeServerCallbacks: public BLEServerCallbacks {
    void onConnect(BLEServer* pServer, esp_ble_gatts_cb_param_t *param) {
        deviceConnected = true;
        Serial.println("[BLE] Client connected. High-speed connection parameters requested.");
        pServer->updateConnParams(param->connect.remote_bda, 6, 12, 0, 400);
    }

    void onDisconnect(BLEServer* pServer) {
        deviceConnected = false;
        Serial.println("[BLE] Client disconnected. Restarting advertising...");
    }
};

void setup() {
    Serial.begin(115200);
    delay(500);

    Serial.println("\n========================================================");
    Serial.println("  ESP32 HIGH-SPEED DUAL-CHANNEL DIGITAL OSCILLOSCOPE   ");
    Serial.println("========================================================");
    Serial.println("[SAFETY] WARNING: Ensure external voltage divider is connected!");
    Serial.println("[SAFETY] Absolute max pin voltage is 3.3V. NEVER connect mains!");
    Serial.printf("[CONFIG] CH1 Pin: GPIO%d (ADC1_CH6)\n", PIN_CH1_GPIO34);
    Serial.printf("[CONFIG] CH2 Pin: GPIO%d (ADC1_CH7)\n", PIN_CH2_GPIO35);
    Serial.printf("[CONFIG] Target Rate: %.0f Samples/Sec/Channel (Continuous DMA)\n", TARGET_SAMPLE_RATE_HZ);
    Serial.println("========================================================\n");

    // 1. Initialize BLE stack
    Serial.println("[BLE] Initializing BLE Device...");
    BLEDevice::init(DEVICE_NAME);
    BLEDevice::setMTU(512);

    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new ScopeServerCallbacks());

    BLEService* pService = pServer->createService(SERVICE_UUID);

    pWaveformCharacteristic = pService->createCharacteristic(
        WAVEFORM_CHAR_UUID,
        BLECharacteristic::PROPERTY_READ |
        BLECharacteristic::PROPERTY_NOTIFY
    );

    BLE2902* pCccd = new BLE2902();
    pCccd->setNotifications(true);
    pWaveformCharacteristic->addDescriptor(pCccd);

    pService->start();

    BLEAdvertising* pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->setMinPreferred(0x06); // 7.5ms
    pAdvertising->setMinPreferred(0x12); // 22.5ms
    BLEDevice::startAdvertising();
    Serial.println("[BLE] Advertising started. Waiting for Android Oscilloscope connection...");

    // 2. Initialize ADC DMA Engine
    if (!adcEngine.init()) {
        Serial.println("[ADC-DMA] FATAL: Failed to initialize ADC continuous DMA driver!");
    } else {
        adcEngine.start();
        Serial.println("[ADC-DMA] Dual-channel continuous acquisition running at 100 kS/s per channel.");
    }
}

void loop() {
    // Handle BLE connection state transitions & advertising restart
    if (!deviceConnected && oldDeviceConnected) {
        delay(200); // Give Bluetooth stack time to reset
        pServer->startAdvertising();
        Serial.println("[BLE] Re-advertising...");
        oldDeviceConnected = deviceConnected;
    }
    if (deviceConnected && !oldDeviceConnected) {
        oldDeviceConnected = deviceConnected;
        packetSequence = 0;
    }

    // Wait for the next complete frame of 100 dual-channel sample pairs from ADC DMA
    DualSample* sampleBuffer = nullptr;
    size_t sampleCount = 0;

    // Timeout of 25ms ensures continuous operation even if no client is listening
    if (adcEngine.waitForBuffer(&sampleBuffer, &sampleCount, pdMS_TO_TICKS(25))) {
        if (deviceConnected && pWaveformCharacteristic != nullptr && sampleBuffer != nullptr) {
            // 1. Assemble Compact V2 Binary Packet
            ScopePacketHeader* header = (ScopePacketHeader*)bleTransmitPacket;
            header->magic0 = SCOPE_MAGIC_0;
            header->magic1 = SCOPE_MAGIC_1;
            header->version = SCOPE_PROTOCOL_VER;
            header->channel = SCOPE_CHAN_DUAL;
            header->sequenceNumber = packetSequence++;
            header->sampleCount = (uint16_t)sampleCount;

            // 2. Copy dual-channel sample pairs (CH1, CH2)
            DualSample* packetSamples = (DualSample*)(bleTransmitPacket + sizeof(ScopePacketHeader));
            memcpy(packetSamples, sampleBuffer, sampleCount * sizeof(DualSample));

            // 3. Compute 8-bit XOR checksum over Header + Samples
            size_t payloadBeforeChecksum = sizeof(ScopePacketHeader) + (sampleCount * sizeof(DualSample));
            uint8_t csum = computePacketChecksum(bleTransmitPacket, payloadBeforeChecksum);
            bleTransmitPacket[payloadBeforeChecksum] = csum;

            size_t totalPacketLength = payloadBeforeChecksum + 1;

            // 4. Send via BLE notification
            pWaveformCharacteristic->setValue(bleTransmitPacket, totalPacketLength);
            pWaveformCharacteristic->notify();

            // Periodic telemetry reporting over Serial (every ~1000 ms)
            static uint32_t lastReportTime = 0;
            uint32_t now = millis();
            if (now - lastReportTime >= 1000) {
                lastReportTime = now;

                // Extract CH1 & CH2 samples for on-device frequency measurement
                uint16_t ch1Array[FRAME_SAMPLES_COUNT];
                uint16_t ch2Array[FRAME_SAMPLES_COUNT];
                for (size_t i = 0; i < sampleCount; i++) {
                    ch1Array[i] = sampleBuffer[i].ch1;
                    ch2Array[i] = sampleBuffer[i].ch2;
                }

                // Compute real frequency directly from acquired samples array
                float ch1Freq = calculateSignalFrequency(ch1Array, sampleCount, TARGET_SAMPLE_RATE_HZ);
                float ch2Freq = calculateSignalFrequency(ch2Array, sampleCount, TARGET_SAMPLE_RATE_HZ);

                Serial.printf("[STATUS] Sent pkt #%u | CH1: %u raw (Freq: %.1f Hz) | CH2: %u raw (Freq: %.1f Hz)\n",
                              header->sequenceNumber, ch1Array[0], ch1Freq, ch2Array[0], ch2Freq);
            }
        }
    }
}
