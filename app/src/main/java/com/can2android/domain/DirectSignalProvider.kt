package com.can2android.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Implementation of SignalProvider that receives pre-decoded signals directly,
 * bypassing the decoder layer entirely.
 *
 * This implementation is intended for data sources that provide already-decoded
 * signal samples (e.g., remote gRPC streams, pre-processed data feeds).
 *
 * Data flow:
 * Remote Source → SignalProvider → Signal Stream Core
 * (SignalDecoder bypassed)
 *
 * This demonstrates the decoder optionality principle: the Signal Stream Core
 * operates on SignalSample objects without knowing or caring whether they were
 * decoded locally or received pre-decoded.
 *
 * Lifecycle:
 * - Uses callbacks for start/stop operations
 * - Handles errors from the signal source gracefully
 * - Transitions to ERROR state on fatal errors
 *
 * @property signalSource Function that provides the Flow of pre-decoded signals
 * @property onStart Optional callback to execute when starting (e.g., open connection)
 * @property onStop Optional callback to execute when stopping (e.g., close connection)
 */
class DirectSignalProvider(
    private val signalSource: () -> Flow<SignalSample>,
    private val onStart: (() -> Unit)? = null,
    private val onStop: (() -> Unit)? = null
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
     * Invokes the onStart callback if provided.
     *
     * @throws IllegalStateException if already started
     * @throws Exception if the start callback fails
     */
    override fun start() {
        if (state != SignalProviderState.IDLE) {
            throw IllegalStateException("Cannot start: already in state $state")
        }

        try {
            state = SignalProviderState.STARTING
            onStart?.invoke()
            state = SignalProviderState.ACTIVE
        } catch (e: Exception) {
            state = SignalProviderState.ERROR
            throw e
        }
    }

    /**
     * Stop the signal provider.
     *
     * Invokes the onStop callback if provided.
     * This operation is idempotent.
     */
    override fun stop() {
        if (state == SignalProviderState.STOPPED) {
            return // Already stopped
        }

        try {
            state = SignalProviderState.STOPPING
            onStop?.invoke()
            state = SignalProviderState.STOPPED
        } catch (e: Exception) {
            // Still transition to stopped to prevent resource leaks
            state = SignalProviderState.STOPPED
            throw e
        }
    }

    /**
     * Observe pre-decoded signal samples.
     *
     * Returns the Flow from the signal source function.
     * Errors from the source are caught and logged.
     *
     * @return Flow of pre-decoded signal samples
     */
    override fun observeSignals(): Flow<SignalSample> {
        return signalSource()
            .catch { e ->
                state = SignalProviderState.ERROR
                System.err.println("Fatal error in direct signal provider: ${e.message}")
                throw e // Re-throw to propagate to Signal Stream Core
            }
    }
}
