package com.can2android.domain

/**
 * Represents a decoded CAN signal sample with physical value.
 *
 * This is the fundamental unit of processed signal data after decoding
 * raw CAN frames using DBC definitions or other decoding mechanisms.
 *
 * SignalSample is the primary data type used by the Signal Stream Core
 * and Visualization layers.
 *
 * @property signalName Unique identifier for the signal (e.g., "EngineSpeed", "VehicleSpeed")
 * @property physicalValue Decoded physical value after applying scale and offset
 * @property unit Physical unit of measurement (e.g., "km/h", "rpm", "°C"), null if unitless
 * @property timestamp Timestamp in microseconds since epoch (same as source RawCanFrame)
 */
data class SignalSample(
    val signalName: String,
    val physicalValue: Double,
    val unit: String?,
    val timestamp: Long
)
