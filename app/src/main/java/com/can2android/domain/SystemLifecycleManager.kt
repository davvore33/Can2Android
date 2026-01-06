package com.can2android.domain

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/**
 * Coordinates the lifecycle of the entire signal processing pipeline.
 *
 * This manager orchestrates the startup and shutdown of all components
 * in the correct order, ensuring:
 * - Dependencies are satisfied during startup (bottom-up initialization)
 * - Resources are released during shutdown (top-down cleanup)
 * - Failures in one component don't prevent cleanup of others
 * - Clean state transitions for the entire system
 *
 * Lifecycle phases:
 * 1. **Startup**: RawFrameProvider → SignalProvider → SignalStreamCore
 * 2. **Shutdown**: SignalStreamCore → SignalProvider → RawFrameProvider
 *
 * Usage example:
 * ```
 * val manager = SystemLifecycleManager(
 *     rawFrameProvider = ascFileProvider,
 *     signalProvider = decodingSignalProvider,
 *     signalStreamCore = signalStreamCore
 * )
 *
 * try {
 *     manager.startAll()
 *     // System is running
 * } catch (e: Exception) {
 *     // Startup failed, components are cleaned up
 * } finally {
 *     manager.stopAll()
 * }
 * ```
 *
 * @property rawFrameProvider The raw CAN frame provider (data source)
 * @property signalProvider The signal provider (with optional decoder)
 * @property signalStreamCore The core signal processing and buffering component
 */
class SystemLifecycleManager(
    private val rawFrameProvider: RawFrameProvider,
    private val signalProvider: SignalProvider,
    private val signalStreamCore: SignalStreamCore
) {
    /**
     * Current lifecycle state of the system.
     */
    @Volatile
    private var state: SystemState = SystemState.IDLE

    /**
     * Get the current system state.
     *
     * @return Current lifecycle state
     */
    fun getState(): SystemState = state

    /**
     * Check if the system is currently running.
     *
     * @return true if all components are active, false otherwise
     */
    fun isRunning(): Boolean = state == SystemState.RUNNING

    /**
     * Start all components in the correct order.
     *
     * The startup sequence is:
     * 1. RawFrameProvider (data source)
     * 2. SignalProvider (decoder, if applicable)
     * 3. SignalStreamCore (buffering and distribution)
     *
     * If any component fails to start, all previously started components
     * are stopped to ensure clean state.
     *
     * @throws IllegalStateException if the system is not in IDLE state
     * @throws DataSourceException if the data source fails to start
     * @throws Exception for any other startup failures
     */
    suspend fun startAll() {
        if (state != SystemState.IDLE) {
            throw IllegalStateException("Cannot start: system is in state $state")
        }

        state = SystemState.STARTING

        try {
            // Phase 1: Start raw frame provider (data source)
            rawFrameProvider.start()

            try {
                // Phase 2: Start signal provider (decoder)
                signalProvider.start()

                try {
                    // Phase 3: Start signal stream core (buffering)
                    signalStreamCore.start()

                    // All components started successfully
                    state = SystemState.RUNNING
                } catch (e: Exception) {
                    // Core failed to start - clean up provider and source
                    tryStopComponent("SignalProvider") { signalProvider.stop() }
                    tryStopComponent("RawFrameProvider") { rawFrameProvider.stop() }
                    throw e
                }
            } catch (e: Exception) {
                // Provider failed to start - clean up source
                tryStopComponent("RawFrameProvider") { rawFrameProvider.stop() }
                throw e
            }
        } catch (e: Exception) {
            state = SystemState.ERROR
            throw e
        }
    }

    /**
     * Stop all components in reverse order.
     *
     * The shutdown sequence is:
     * 1. SignalStreamCore (stop buffering and distribution)
     * 2. SignalProvider (stop decoder)
     * 3. RawFrameProvider (close data source)
     *
     * This method continues shutdown even if individual components fail,
     * ensuring resources are released. It uses NonCancellable context to
     * prevent cancellation from interrupting cleanup.
     *
     * The method is idempotent - calling it multiple times is safe.
     */
    suspend fun stopAll() {
        if (state == SystemState.STOPPED) {
            return // Already stopped
        }

        state = SystemState.STOPPING

        // Use NonCancellable to ensure cleanup completes even if parent scope is cancelled
        withContext(NonCancellable) {
            // Phase 1: Stop signal stream core
            tryStopComponent("SignalStreamCore") { signalStreamCore.stop() }

            // Phase 2: Stop signal provider
            tryStopComponent("SignalProvider") { signalProvider.stop() }

            // Phase 3: Stop raw frame provider
            tryStopComponent("RawFrameProvider") { rawFrameProvider.stop() }

            state = SystemState.STOPPED
        }
    }

    /**
     * Attempt to stop a component, logging errors but continuing shutdown.
     *
     * This helper ensures that failures in one component don't prevent
     * cleanup of other components.
     *
     * @param componentName Name of the component for logging
     * @param stopAction The stop action to execute
     */
    private suspend fun tryStopComponent(componentName: String, stopAction: suspend () -> Unit) {
        try {
            stopAction()
        } catch (e: Exception) {
            // Log error but continue shutdown
            // In PoC, we just swallow the exception
            // In production, this would be logged properly
            System.err.println("Error stopping $componentName: ${e.message}")
        }
    }
}

/**
 * Represents the overall lifecycle state of the system.
 */
enum class SystemState {
    /**
     * System has been created but not started.
     * All components are in their initial state.
     */
    IDLE,

    /**
     * System is starting up.
     * Components are being initialized in sequence.
     */
    STARTING,

    /**
     * System is fully running.
     * All components are active and data is flowing.
     */
    RUNNING,

    /**
     * System is shutting down.
     * Components are being stopped in reverse order.
     */
    STOPPING,

    /**
     * System has stopped cleanly.
     * All resources have been released.
     */
    STOPPED,

    /**
     * System encountered an error during startup or operation.
     * Cleanup may have been performed, but manual intervention
     * may be required.
     */
    ERROR
}
