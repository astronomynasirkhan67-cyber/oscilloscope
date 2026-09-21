# ESP32 Dual-Channel High-Speed Digital Oscilloscope Firmware

High-performance, continuous DMA-driven dual-channel oscilloscope firmware for the ESP32 DevKit, designed to pair with the Android **Digital Oscilloscope** app.

---

## 1. Specifications

- **Hardware Platform**: ESP32 Dev Module (Dual-core Xtensa 32-bit LX6 @ 240 MHz)
- **Architecture**:
  - **Core 0**: Continuous ADC DMA acquisition engine into ping-pong double buffers.
  - **Core 1**: Frequency calculation, packetization, and BLE transmission.
- **Sampling Mode**: Hardware continuous DMA (`ADC_CONV_SINGLE_UNIT_1`)
- **Sampling Rate**: Up to **100,000 samples/sec per channel** (200 kS/s aggregate).
- **ADC Resolution**: 12-bit ($0 \dots 4095$) with low-noise attenuation.
- **Communication**: Bluetooth Low Energy (BLE) with MTU 512, high-speed connection interval (7.5ms – 15ms), compact V2 binary framing, and 8-bit XOR checksum verification.

---

## 2. Pinout & Hardware Connections

| Channel | ESP32 Pin | ADC Unit & Channel | Function | Max Direct Voltage |
|---|---|---|---|---|
| **CH1** | **GPIO 34** | ADC1 Channel 6 | Input Signal (Voltage) | **3.3 V** |
| **CH2** | **GPIO 35** | ADC1 Channel 7 | Output Signal (Voltage) | **3.3 V** |
| **GND** | **GND** | System Ground | Circuit Reference Ground | **0.0 V** |

---

## 3. ⚠️ Critical Electrical Safety Notice

> 🚨 **NEVER CONNECT ARBITRARY VOLTAGES OR MAINS DIRECTLY TO ESP32 PINS!**
>
> - The ESP32 analog pins accept **0.0 V to 3.3 V maximum**.
> - Applying negative voltages or voltages exceeding 3.3 V **will permanently destroy the ESP32 microcontroller**.
> - Always connect signals through a **resistive voltage divider** with **protective clamping diodes**.

### Recommended 10:1 Attenuation Probe Circuit (For inputs up to 30V):

```
Signal Input (+) ----[ 90kΩ (1% metal film) ]----+----> To ESP32 GPIO34 (CH1)
                                                 |
                                            [ 10kΩ ]
                                                 |
                                            +----+----+----> ESP32 GND
                                            |
                                      [ BAT54S Diode ]
                                      (Clamps to 3.3V and GND)
```

In the Android app's **Calibration** settings, set the probe divider ratio to **10.0** to read actual real-world circuit voltages accurately.

---

## 4. How to Flash the Firmware

### Option A: Using Arduino IDE
1. Install **Arduino IDE** (v2.x recommended).
2. Add ESP32 board support:
   - Go to **File > Preferences**.
   - In "Additional Board Manager URLs", add:
     `https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json`
   - Open **Tools > Board > Boards Manager**, search for `esp32` by Espressif Systems, and click **Install**.
3. Open `esp32_oscilloscope.ino` in Arduino IDE.
4. Select **Tools > Board > ESP32 Arduino > ESP32 Dev Module**.
5. Connect your ESP32 board via USB. Select the correct **COM / Serial Port**.
6. Set:
   - **Upload Speed**: `921600`
   - **CPU Frequency**: `240MHz (WiFi/BT)`
   - **Flash Frequency**: `80MHz`
7. Click **Upload** (or press `Ctrl+U`).

### Option B: Using Arduino CLI (Terminal)
```bash
# Compile
arduino-cli compile --fqbn esp32:esp32:esp32 ./esp32_oscilloscope

# Upload to your port (e.g. /dev/ttyUSB0 or COM3)
arduino-cli upload -p /dev/ttyUSB0 --fqbn esp32:esp32:esp32 ./esp32_oscilloscope
```

---

## 5. Connecting to the Android App

1. Once flashed, the ESP32 starts advertising as **`ESP32-Oscilloscope`**.
2. Open the **Digital Oscilloscope** Android app.
3. Tap the Bluetooth icon in the top app bar.
4. Select **ESP32-Oscilloscope** and tap **Connect**.
5. The live dual-channel trace will immediately stream in real time at up to 100 kS/s!
