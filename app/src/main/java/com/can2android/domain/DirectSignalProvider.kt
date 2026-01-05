package com.can2android.domain

import kotlinx.coroutines.flow.Flow

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
 * @property signalSource Function that provides the Flow of pre-decoded signals
 * @property onStart Optional callback to execute when starting (e.g., open connection)
 * @property onStop Optional callback to execute when stopping (e.g., close connection)
 */
class DirectSignalProvider(
    private val signalSource: () -> Flow<SignalSample>,
    private val onStart: (() -> Unit)? = null,
    private val onStop: (() -> Unit)? = null
) : SignalProvider {

    override fun start() {
        onStart?.invoke()
    }

    override fun stop() {
        onStop?.invoke()
    }

    override fun observeSignals(): Flow<SignalSample> {
        return signalSource()
    }
}
