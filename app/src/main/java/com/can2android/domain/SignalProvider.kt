package com.can2android.domain

import kotlinx.coroutines.flow.Flow

/**
 * Interface for providing decoded signal samples to the Signal Stream Core.
 *
 * This is the key abstraction that enables decoder optionality. The Signal
 * Stream Core depends only on this interface, remaining agnostic to whether
 * signals were decoded locally or received pre-decoded from a remote source.
 *
 * Two implementation patterns are supported:
 *
 * 1. **With Local Decoding** (ASC files, USB CAN adapters):
 *    RawFrameProvider → SignalDecoder → SignalProvider
 *
 * 2. **Pre-Decoded Remote Source** (gRPC streams):
 *    Remote Source → SignalProvider (decoder bypassed)
 *
 * This design ensures:
 * - The decoder is not tightly coupled to data sources
 * - The decoder can be replaced or disabled
 * - Signal Stream Core does not know if decoding happened locally
 *
 * Implementations MUST:
 * - Emit signal samples in chronological order
 * - Support backpressure through Flow
 * - Preserve original timestamps from source data
 */
interface SignalProvider {
    /**
     * Start the signal provider and begin signal delivery.
     *
     * This method should initiate any necessary resources and start
     * the signal processing pipeline.
     *
     * @throws Exception if the provider cannot be started
     */
    fun start()

    /**
     * Stop the signal provider and release all resources.
     *
     * This method should gracefully terminate signal delivery and
     * clean up any allocated resources.
     */
    fun stop()

    /**
     * Observe decoded signal samples as they become available.
     *
     * The returned Flow emits signal samples in chronological order
     * and supports backpressure to prevent overwhelming downstream
     * consumers.
     *
     * @return Flow of decoded signal samples
     */
    fun observeSignals(): Flow<SignalSample>
}
