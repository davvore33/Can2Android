package com.can2android.domain

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
 * Lifecycle:
 * - IDLE: Created but not started
 * - ACTIVE: Running and processing signals
 * - ERROR: Failed during operation
 * - STOPPED: Cleanly stopped and resources released
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
     * Coroutine job for signal processing. Used to track and cancel the processing coroutine.
     */
    private var processingJob: Job? = null
    
    /**
     * Current lifecycle state of the Signal Stream Core.
     */
    @Volatile
    private var state: SignalStreamCoreState = SignalStreamCoreState.IDLE

    /**
     * Flow of signal updates that observers can collect.
     *
     * Emits SignalUpdate whenever a new sample is processed, containing
     * both the latest sample and the complete time-series for that signal.
     */
    val signalUpdates: SharedFlow<SignalUpdate> = _signalUpdates.asSharedFlow()

    /**
     * Check if the Signal Stream Core is currently active.
     *
     * @return true if the core is in ACTIVE state, false otherwise
     */
    fun isActive(): Boolean = state == SignalStreamCoreState.ACTIVE

    /**
     * Get the current lifecycle state.
     *
     * @return Current state of the Signal Stream Core
     */
    fun getState(): SignalStreamCoreState = state

    /**
     * Start the Signal Stream Core.
     *
     * This initiates the signal provider and begins processing incoming
     * signal samples. The core transitions from IDLE to ACTIVE state.
     *
     * @throws IllegalStateException if already started or in ERROR state
     * @throws Exception if the signal provider fails to start
     */
    fun start() {
        if (state != SignalStreamCoreState.IDLE) {
            throw IllegalStateException("Cannot start: already in state $state")
        }

        try {
            state = SignalStreamCoreState.STARTING
            
            signalProvider.start()

            processingJob = scope.launch {
                try {
                    signalProvider.observeSignals()
                        .collect { sample ->
                            processSample(sample)
                        }
                } catch (e: CancellationException) {
                    // Normal cancellation during stop() - don't log as error
                    throw e
                } catch (e: Exception) {
                    // Unexpected error during signal processing
                    handleProcessingError(e)
                    throw e
                }
            }
            
            state = SignalStreamCoreState.ACTIVE
        } catch (e: Exception) {
            state = SignalStreamCoreState.ERROR
            throw e
        }
    }

    /**
     * Stop the Signal Stream Core.
     *
     * This stops the signal provider, cancels signal processing, and clears all buffers.
     * The core transitions to STOPPED state. This operation is idempotent and will not
     * throw if called multiple times or when already stopped.
     */
    fun stop() {
        if (state == SignalStreamCoreState.STOPPED) {
            return // Already stopped, nothing to do
        }

        try {
            state = SignalStreamCoreState.STOPPING
            
            // Cancel signal processing coroutine
            processingJob?.cancel()
            processingJob = null
            
            // Stop the signal provider
            signalProvider.stop()
            
            // Clear all buffers
            synchronized(buffers) {
                buffers.clear()
            }
            
            state = SignalStreamCoreState.STOPPED
        } catch (e: Exception) {
            // Log error but still transition to stopped state
            // to prevent resource leaks
            state = SignalStreamCoreState.STOPPED
            throw e
        }
    }

    /**
     * Handle errors that occur during signal processing.
     *
     * This is called when an unexpected exception occurs in the signal
     * processing coroutine. The core transitions to ERROR state.
     *
     * @param error The exception that occurred
     */
    private fun handleProcessingError(error: Exception) {
        state = SignalStreamCoreState.ERROR
        // In PoC, we just log. Future versions could emit error events
        // or attempt recovery strategies.
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
