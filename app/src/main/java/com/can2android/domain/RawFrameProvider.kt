package com.can2android.domain

import kotlinx.coroutines.flow.Flow

/**
 * Interface for providing raw CAN frames from various data sources.
 *
 * This is the abstraction layer between data sources (ASC files, USB CAN adapters,
 * gRPC streams, etc.) and the rest of the system.
 *
 * Implementations MUST:
 * - Provide only raw, uninterpreted CAN frames
 * - Emit frames in chronological order
 * - Support backpressure through Flow
 * - NOT perform any parsing or signal decoding
 *
 * This interface serves as the entry point for all raw CAN data in the system.
 */
interface RawFrameProvider {
    /**
     * Start the frame provider and begin data acquisition.
     *
     * This method should initiate any necessary resources (file handles,
     * USB connections, network streams, etc.).
     *
     * @throws Exception if the provider cannot be started
     */
    fun start()

    /**
     * Stop the frame provider and release all resources.
     *
     * This method should gracefully terminate data acquisition and
     * clean up any allocated resources.
     */
    fun stop()

    /**
     * Observe raw CAN frames as they become available.
     *
     * The returned Flow emits frames in chronological order and supports
     * backpressure to prevent overwhelming downstream consumers.
     *
     * @return Flow of raw CAN frames
     */
    fun observeFrames(): Flow<RawCanFrame>
}
