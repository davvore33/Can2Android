package com.can2android.domain

/**
 * Represents an update to a signal's time-series buffer.
 *
 * This is the data type emitted by the Signal Stream Core to observers,
 * providing both the new sample and the complete time-series history
 * for a specific signal.
 *
 * @property signalName Unique identifier for the signal
 * @property latestSample The most recent sample added to the buffer
 * @property timeSeries Complete time-series data within the current time window
 */
data class SignalUpdate(
    val signalName: String,
    val latestSample: SignalSample,
    val timeSeries: List<SignalSample>
)
