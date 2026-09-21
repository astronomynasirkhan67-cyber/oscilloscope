package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ChannelConfig
import com.example.model.ChannelId
import com.example.model.TriggerMode
import com.example.model.TriggerSlope

class OscilloscopePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("oscilloscope_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CH1_ENABLED = "ch1_enabled"
        private const val KEY_CH1_VOLT_DIV = "ch1_volt_div"
        private const val KEY_CH1_OFFSET_DIV = "ch1_offset_div"
        private const val KEY_CH1_DIVIDER = "ch1_divider"
        private const val KEY_CH1_CAL = "ch1_cal"
        private const val KEY_CH1_OFFSET_V = "ch1_offset_v"

        private const val KEY_CH2_ENABLED = "ch2_enabled"
        private const val KEY_CH2_VOLT_DIV = "ch2_volt_div"
        private const val KEY_CH2_OFFSET_DIV = "ch2_offset_div"
        private const val KEY_CH2_DIVIDER = "ch2_divider"
        private const val KEY_CH2_CAL = "ch2_cal"
        private const val KEY_CH2_OFFSET_V = "ch2_offset_v"

        private const val KEY_TIME_DIV = "time_div"
        private const val KEY_SAMPLING_RATE = "sampling_rate"
        private const val KEY_TRIG_SOURCE = "trig_source"
        private const val KEY_TRIG_MODE = "trig_mode"
        private const val KEY_TRIG_SLOPE = "trig_slope"
        private const val KEY_TRIG_LEVEL = "trig_level"
    }

    fun loadCh1Config(): ChannelConfig {
        return ChannelConfig(
            channel = ChannelId.CH1,
            enabled = prefs.getBoolean(KEY_CH1_ENABLED, true),
            voltsPerDiv = prefs.getFloat(KEY_CH1_VOLT_DIV, 1.0f),
            verticalOffsetDiv = prefs.getFloat(KEY_CH1_OFFSET_DIV, 0.0f),
            adcMax = 4095.0f,
            vRef = 3.3f,
            dividerRatio = prefs.getFloat(KEY_CH1_DIVIDER, 1.0f),
            calMultiplier = prefs.getFloat(KEY_CH1_CAL, 1.0f),
            offsetVoltage = prefs.getFloat(KEY_CH1_OFFSET_V, 0.0f)
        )
    }

    fun saveCh1Config(config: ChannelConfig) {
        prefs.edit()
            .putBoolean(KEY_CH1_ENABLED, config.enabled)
            .putFloat(KEY_CH1_VOLT_DIV, config.voltsPerDiv)
            .putFloat(KEY_CH1_OFFSET_DIV, config.verticalOffsetDiv)
            .putFloat(KEY_CH1_DIVIDER, config.dividerRatio)
            .putFloat(KEY_CH1_CAL, config.calMultiplier)
            .putFloat(KEY_CH1_OFFSET_V, config.offsetVoltage)
            .apply()
    }

    fun loadCh2Config(): ChannelConfig {
        return ChannelConfig(
            channel = ChannelId.CH2,
            enabled = prefs.getBoolean(KEY_CH2_ENABLED, true),
            voltsPerDiv = prefs.getFloat(KEY_CH2_VOLT_DIV, 1.0f),
            verticalOffsetDiv = prefs.getFloat(KEY_CH2_OFFSET_DIV, 0.0f),
            adcMax = 4095.0f,
            vRef = 3.3f,
            dividerRatio = prefs.getFloat(KEY_CH2_DIVIDER, 1.0f),
            calMultiplier = prefs.getFloat(KEY_CH2_CAL, 1.0f),
            offsetVoltage = prefs.getFloat(KEY_CH2_OFFSET_V, 0.0f)
        )
    }

    fun saveCh2Config(config: ChannelConfig) {
        prefs.edit()
            .putBoolean(KEY_CH2_ENABLED, config.enabled)
            .putFloat(KEY_CH2_VOLT_DIV, config.voltsPerDiv)
            .putFloat(KEY_CH2_OFFSET_DIV, config.verticalOffsetDiv)
            .putFloat(KEY_CH2_DIVIDER, config.dividerRatio)
            .putFloat(KEY_CH2_CAL, config.calMultiplier)
            .putFloat(KEY_CH2_OFFSET_V, config.offsetVoltage)
            .apply()
    }

    fun loadTimePerDiv(): Float = prefs.getFloat(KEY_TIME_DIV, 50e-6f) // 50 µs/div default (optimal for 10-11 kHz signals)
    fun saveTimePerDiv(v: Float) = prefs.edit().putFloat(KEY_TIME_DIV, v).apply()

    fun loadSamplingRate(): Float = prefs.getFloat(KEY_SAMPLING_RATE, 100000f) // 100 kHz default
    fun saveSamplingRate(v: Float) = prefs.edit().putFloat(KEY_SAMPLING_RATE, v).apply()

    fun loadTriggerSource(): ChannelId =
        if (prefs.getString(KEY_TRIG_SOURCE, "CH1") == "CH2") ChannelId.CH2 else ChannelId.CH1
    fun saveTriggerSource(source: ChannelId) =
        prefs.edit().putString(KEY_TRIG_SOURCE, source.name).apply()

    fun loadTriggerMode(): TriggerMode =
        try {
            TriggerMode.valueOf(prefs.getString(KEY_TRIG_MODE, TriggerMode.AUTO.name) ?: TriggerMode.AUTO.name)
        } catch (_: Exception) {
            TriggerMode.AUTO
        }
    fun saveTriggerMode(mode: TriggerMode) =
        prefs.edit().putString(KEY_TRIG_MODE, mode.name).apply()

    fun loadTriggerSlope(): TriggerSlope =
        try {
            TriggerSlope.valueOf(prefs.getString(KEY_TRIG_SLOPE, TriggerSlope.RISING.name) ?: TriggerSlope.RISING.name)
        } catch (_: Exception) {
            TriggerSlope.RISING
        }
    fun saveTriggerSlope(slope: TriggerSlope) =
        prefs.edit().putString(KEY_TRIG_SLOPE, slope.name).apply()

    fun loadTriggerLevel(): Float = prefs.getFloat(KEY_TRIG_LEVEL, 1.65f) // 1.65V (midpoint of 3.3V)
    fun saveTriggerLevel(level: Float) = prefs.edit().putFloat(KEY_TRIG_LEVEL, level).apply()
}
