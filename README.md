# Digital Oscilloscope

A high-performance two-channel digital oscilloscope Android application built with Kotlin, Jetpack Compose, and Material 3. The application connects over Bluetooth Low Energy (BLE) to an ESP32 DevKit to stream, trigger, measure, and analyze real dual-channel ADC waveforms in real time.

---

## 1. Hardware Architecture

- **Microcontroller**: ESP32 DevKit (Dual-core Xtensa 32-bit LX6)
- **Channel 1 (CH1 - Input Voltage)**: ESP32 `GPIO34` (`ADC1_CH6`)
- **Channel 2 (CH2 - Output Voltage)**: ESP32 `GPIO35` (`ADC1_CH7`)
- **Analog Reference ($V_{ref}$)**: 3.3 V
- **ADC Resolution**: 12-bit ($0 - 4095$)
- **Communication**: Bluetooth Low Energy (BLE 4.2 / 5.0)

---

## 2. Bluetooth Low Energy (BLE) Specifications

- **Target Device Name**: `ESP32-Oscilloscope`
- **Nordic UART Service UUID**: `6e400001-b5a3-f393-e0a9-e50e24dcca9e`
- **RX/Waveform Characteristic UUID**: `6e400003-b5a3-f393-e0a9-e50e24dcca9e`
- **Client Characteristic Configuration Descriptor (CCCD)**: `00002902-0000-1000-8000-00805f9b34fb`
- **Requested MTU**: 512 bytes

---

## 3. Binary Packet Protocol

Data is transmitted in compact, little-endian binary packets via BLE notifications:

| Field | Size | Data Type | Description |
|---|---|---|---|
| **Sequence Number** | 2 bytes | `uint16_t` (little-endian) | Monotonically increasing packet sequence ($0 - 65535$) for packet loss detection |
| **Sample Count ($N$)** | 1 byte | `uint8_t` | Number of dual-channel sample pairs in this packet ($1 \le N \le 124$) |
| **Interleaved Samples** | $4 \times N$ bytes | `uint16_t` pairs | Dual-channel raw ADC pairs: `[CH1_0, CH2_0, CH1_1, CH2_1, ...]` |

### Packet Size Calculation
$$\text{Total Packet Length} = 3 + (4 \times N) \text{ bytes}$$
For a typical burst of $N = 100$ samples:
$$\text{Length} = 3 + 400 = 403 \text{ bytes}$$

---

## 4. Voltage Conversion & Calibration Formula

The application scales raw 12-bit ADC integer values ($0 \dots 4095$) to actual circuit voltages using configurable channel calibration parameters:

$$V_{measured} = \left[ \left( \frac{\text{Raw ADC}}{4095.0} \times 3.3\text{ V} \times \text{DividerRatio} \right) + \text{OffsetVoltage} \right] \times \text{CalMultiplier}$$

Where:
- **`DividerRatio`**: External resistive voltage divider ratio (e.g., $1.0$ for direct 1X, $10.0$ for 10:1 attenuator probe).
- **`OffsetVoltage`**: DC offset voltage trim (in Volts).
- **`CalMultiplier`**: Fine calibration gain multiplier (default: $1.0000$).

---

## 5. Key Application Features

1. **Oscilloscope Reticle Grid**:
   - 8 vertical divisions $\times$ 10 horizontal divisions with center axis subdivisions.
   - Dual-channel phosphor-glow traces: CH1 (Yellow), CH2 (Cyan).
   - Zero-reference ground markers and trigger level horizontal dashed line with interactive drag adjustments.
2. **Software Trigger Engine**:
   - Trigger Sources: **CH1** or **CH2**.
   - Trigger Modes: **Auto**, **Normal**, **Single**.
   - Trigger Slopes: **Rising Edge ($\uparrow$)**, **Falling Edge ($\downarrow$)**.
   - Adjustable trigger voltage threshold with instant snap-to-midpoint button.
3. **Timebase & Voltage Scaling**:
   - Time/Div: $100\ \mu\text{s/div}$ up to $100\ \text{ms/div}$.
   - Volt/Div: $0.1\ \text{V/div}$ up to $20\ \text{V/div}$.
   - Horizontal waveform panning.
4. **Real-Time Automated Measurements**:
   - Computes $V_{max}$, $V_{min}$, $V_{pp}$, True RMS, Frequency ($\text{Hz}$), and Period ($\text{s}$) using zero-crossing algorithms.
5. **Fast Fourier Transform (FFT)**:
   - Radix-2 FFT with Hann windowing for frequency domain spectrum visualization ($0$ to Nyquist frequency).
   - Instant readout of dominant peak frequencies for both channels.
6. **Local Data Persistence & CSV Export**:
   - Save waveform captures locally with timestamp, calibration parameters, timebase, and sample arrays.
   - One-tap CSV export and sharing via standard Android share sheet.
7. **Hardware Calibration Screen**:
   - Independent probe divider ratios, trim multipliers, voltage offsets, and sampling rate configuration.

---

## 6. How to Connect and Use

1. **Power up the ESP32**: Ensure the ESP32 is loaded with the dual-channel sampling firmware and advertising as `ESP32-Oscilloscope`.
2. **Launch Digital Oscilloscope**: Open the app on your Android device.
3. **Scan & Connect**:
   - Tap the Bluetooth icon in the top toolbar.
   - Grant Bluetooth permissions if prompted.
   - The app highlights `ESP32-Oscilloscope` (Target Device). Tap **Connect**.
4. **Monitor & Adjust**:
   - The live status indicator will display connection and packet throughput.
   - Use **AUTO** for rapid auto-ranging.
   - Use **RUN/STOP** or **FREEZE** to hold and examine waveforms.
   - Switch between **TIME** and **FFT** views to inspect signal frequency content.

---

## 7. Electrical Safety Warnings

> ⚠️ **CRITICAL SAFETY WARNING**
> - **NEVER connect 220V AC mains directly to the ESP32.**
> - **Never exceed the safe electrical input range of the ESP32 ADC (0.0 V to 3.3 V maximum).** Exceeding 3.3V or inputting negative voltages will permanently destroy the ESP32.
> - **Always use a properly calculated voltage divider and protection circuit (clamping Zener/Schottky diodes) for signals exceeding 3.3V peak.**
> - **CH1 and CH2 share the ESP32 system GND and are NOT electrically isolated.** Use optical or transformer isolation when measuring dangerous or high-voltage circuits.

---

## 8. CI/CD & Automated GitHub APK Builds

This project includes automated GitHub Actions workflows:

### Automated Debug APK (`build-apk.yml`)
- **Workflow**: `Build Digital Oscilloscope APK`
- **Triggers**: Any push or pull request to `main`/`master`, or manual trigger via `workflow_dispatch`.
- **Artifact**: `Digital-Oscilloscope-APK` containing `Digital-Oscilloscope-debug.apk`.

### Release APK (`release-apk.yml`)
- **Workflow**: `Release Digital Oscilloscope APK`
- **Trigger**: Pushing a version tag such as `v1.0.0`.
- **Output**: Automatically creates a GitHub Release and attaches `Digital-Oscilloscope-v1.0.0.apk` directly to the release page.

