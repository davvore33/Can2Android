package com.can2android.domain

/**
 * Represents the lifecycle state of the Signal Stream Core.
 *
 * This enum tracks the operational state of the core component,
 * enabling proper lifecycle management and error handling.
 */
enum class SignalStreamCoreState {
    /**
     * Initial state. The core has been created but not started.
     * No resources are allocated, no processing is occurring.
     */
    IDLE,

    /**
     * Transitional state during startup.
     * The signal provider is being started and the processing
     * coroutine is being launched.
     */
    STARTING,

    /**
     * The core is active and processing signals.
     * Buffers are being updated and SignalUpdates are being emitted.
     */
    ACTIVE,

    /**
     * Transitional state during shutdown.
     * The signal provider is being stopped, processing coroutine
     * is being cancelled, and buffers are being cleared.
     */
    STOPPING,

    /**
     * The core has stopped cleanly.
     * All resources have been released, all coroutines cancelled,
     * and all buffers cleared. Can be restarted if needed.
     */
    STOPPED,

    /**
     * The core encountered an error during operation.
     * This typically indicates an unexpected exception in the
     * signal processing pipeline. Manual intervention may be required.
     */
    ERROR
}
