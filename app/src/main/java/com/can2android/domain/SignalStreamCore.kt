package com.can2android.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Core component for managing signal state, buffering, and time-windowing.
 *
 * This is the central component of the signal processing pipeline, responsible
 * for maintaining time-series buffers for multiple signals and distributing
 * updates to observers.
 *
 * Key responsibilities:
 * - Buffer decoded signals in memory
 * - Support real-time updates
 * - Support multiple signals simultaneously
 * - Operate exclusively on physical values (SignalSample), never raw frames
 *
 * The Signal Stream Core is data-source agnostic and has no knowledge of:
 * - CAN frames or CAN IDs
 * - ASC file format
 * - DBC definitions
 * - Hardware interfaces
 *
 * It operates purely on the abstraction of time-stamped physical signal values.
 *
 * @property signalProvider Source of signal samples (may be decoded locally or pre-decoded)
 * @property timeWindowMicros Time window duration in microseconds (default: 60 seconds)
 * @property scope CoroutineScope for managing coroutines
 */
class SignalStreamCore(
    private val signalProvider: SignalProvider,
    private val timeWindowMicros: Long = 60_000_000L, // 60 seconds default
    private val scope: CoroutineScope
) {
    private val buffers = mutableMapOf<String, SignalBuffer>()
    private val _signalUpdates = MutableSharedFlow<SignalUpdate>(replay = 0)

    /**
     * Flow of signal updates that observers can collect.
     *
     * Emits SignalUpdate whenever a new sample is processed, containing
     * both the latest sample and the complete time-series for that signal.
     */
    val signalUpdates: SharedFlow<SignalUpdate> = _signalUpdates.asSharedFlow()

    /**
     * Start the Signal Stream Core.
     *
     * This initiates the signal provider and begins processing incoming
     * signal samples.
     */
    fun start() {
        signalProvider.start()

        scope.launch {
            signalProvider.observeSignals()
                .collect { sample ->
                    processSample(sample)
                }
        }
    }

    /**
     * Stop the Signal Stream Core.
     *
     * This stops the signal provider and clears all buffers.
     */
    fun stop() {
        signalProvider.stop()
        synchronized(buffers) {
            buffers.clear()
        }
    }

    /**
     * Get the current time-series for a specific signal.
     *
     * @param signalName Name of the signal
     * @return List of samples in chronological order, or empty list if signal not found
     */
    fun getSignalHistory(signalName: String): List<SignalSample> {
        synchronized(buffers) {
            return buffers[signalName]?.getSamples() ?: emptyList()
        }
    }

    /**
     * Get the most recent sample for a specific signal.
     *
     * @param signalName Name of the signal
     * @return Latest sample, or null if signal not found or no samples received
     */
    fun getLatestSample(signalName: String): SignalSample? {
        synchronized(buffers) {
            return buffers[signalName]?.getLatestSample()
        }
    }

    /**
     * Get the names of all signals currently being tracked.
     *
     * @return Set of signal names
     */
    fun getTrackedSignals(): Set<String> {
        synchronized(buffers) {
            return buffers.keys.toSet()
        }
    }

    /**
     * Process an incoming signal sample.
     *
     * This adds the sample to the appropriate buffer and emits a SignalUpdate.
     *
     * @param sample The signal sample to process
     */
    private suspend fun processSample(sample: SignalSample) {
        val buffer = getOrCreateBuffer(sample.signalName)
        buffer.addSample(sample)

        val update = SignalUpdate(
            signalName = sample.signalName,
            latestSample = sample,
            timeSeries = buffer.getSamples()
        )

        _signalUpdates.emit(update)
    }

    /**
     * Get or create a buffer for a signal.
     *
     * @param signalName Name of the signal
     * @return The signal buffer
     */
    private fun getOrCreateBuffer(signalName: String): SignalBuffer {
        synchronized(buffers) {
            return buffers.getOrPut(signalName) {
                SignalBuffer(timeWindowMicros)
            }
        }
    }
}
