package com.can2android.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * Implementation of SignalProvider that uses a SignalDecoder to transform
 * raw CAN frames into signal samples.
 *
 * This is the standard implementation for data sources that provide raw
 * CAN frames (ASC files, USB CAN adapters, etc.).
 *
 * Data flow:
 * RawFrameProvider → SignalDecoder → SignalProvider → Signal Stream Core
 *
 * Lifecycle:
 * - Delegates lifecycle to the underlying RawFrameProvider
 * - Handles decoding errors gracefully by logging and skipping problematic frames
 * - Propagates fatal errors from the RawFrameProvider
 *
 * @property rawFrameProvider Source of raw CAN frames
 * @property signalDecoder Decoder to transform frames into signals
 */
class DecodingSignalProvider(
    private val rawFrameProvider: RawFrameProvider,
    private val signalDecoder: SignalDecoder
) : SignalProvider {

    @Volatile
    private var state: SignalProviderState = SignalProviderState.IDLE

    /**
     * Get the current lifecycle state.
     *
     * @return Current state of the signal provider
     */
    fun getState(): SignalProviderState = state

    /**
     * Start the signal provider.
     *
     * This starts the underlying RawFrameProvider, which begins emitting
     * raw CAN frames that will be decoded into signal samples.
     *
     * @throws IllegalStateException if already started
     * @throws DataSourceException if the RawFrameProvider fails to start
     */
    override fun start() {
        if (state != SignalProviderState.IDLE) {
            throw IllegalStateException("Cannot start: already in state $state")
        }

        try {
            state = SignalProviderState.STARTING
            rawFrameProvider.start()
            state = SignalProviderState.ACTIVE
        } catch (e: Exception) {
            state = SignalProviderState.ERROR
            throw e
        }
    }

    /**
     * Stop the signal provider.
     *
     * This stops the underlying RawFrameProvider, releasing all resources.
     * This operation is idempotent.
     */
    override fun stop() {
        if (state == SignalProviderState.STOPPED) {
            return // Already stopped
        }

        try {
            state = SignalProviderState.STOPPING
            rawFrameProvider.stop()
            state = SignalProviderState.STOPPED
        } catch (e: Exception) {
            // Still transition to stopped to prevent resource leaks
            state = SignalProviderState.STOPPED
            throw e
        }
    }

    /**
     * Observe decoded signal samples.
     *
     * This Flow receives raw CAN frames from the RawFrameProvider,
     * decodes them using the SignalDecoder, and emits individual
     * signal samples.
     *
     * Error handling:
     * - Decoding errors are logged but processing continues (skip bad frames)
     * - Fatal errors from RawFrameProvider are propagated to the collector
     *
     * @return Flow of decoded signal samples
     */
    override fun observeSignals(): Flow<SignalSample> {
        return rawFrameProvider.observeFrames()
            .map { frame ->
                try {
                    signalDecoder.decode(frame)
                } catch (e: Exception) {
                    // Log decoding error but continue processing
                    // In PoC, we just print to stderr
                    // In production, use proper logging
                    System.err.println("Error decoding frame ${frame.canId}: ${e.message}")
                    emptyList<SignalSample>() // Skip this frame
                }
            }
            // Flatten the list of samples into individual emissions
            .let { flow ->
                kotlinx.coroutines.flow.flow {
                    flow.collect { samples ->
                        samples.forEach { sample ->
                            emit(sample)
                        }
                    }
                }
            }
            // Catch and log errors from the RawFrameProvider
            .catch { e ->
                state = SignalProviderState.ERROR
                System.err.println("Fatal error in signal provider: ${e.message}")
                throw e // Re-throw to propagate to Signal Stream Core
            }
    }
}
