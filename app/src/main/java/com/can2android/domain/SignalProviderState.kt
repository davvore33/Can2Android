package com.can2android.domain

/**
 * Represents the lifecycle state of a SignalProvider.
 *
 * This enum tracks the operational state of signal providers,
 * enabling proper lifecycle management and error handling.
 */
enum class SignalProviderState {
    /**
     * Initial state. The provider has been created but not started.
     * No resources are allocated, no signals are being provided.
     */
    IDLE,

    /**
     * Transitional state during startup.
     * Resources are being initialized (connections, decoders, etc.).
     */
    STARTING,

    /**
     * The provider is active and emitting signal samples.
     * Resources are allocated and data is flowing.
     */
    ACTIVE,

    /**
     * Transitional state during shutdown.
     * Resources are being released and signal emission is stopping.
     */
    STOPPING,

    /**
     * The provider has stopped cleanly.
     * All resources have been released and signal emission has ceased.
     * Can be restarted if needed.
     */
    STOPPED,

    /**
     * The provider encountered an error during operation.
     * This typically indicates an unexpected exception in the
     * signal pipeline. Manual intervention may be required.
     */
    ERROR
}
