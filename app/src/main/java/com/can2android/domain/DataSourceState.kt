package com.can2android.domain

/**
 * Represents the current state of a data source.
 *
 * This enum defines the lifecycle states that a RawFrameProvider
 * can be in, allowing observers to track the data source status.
 */
enum class DataSourceState {
    /**
     * Initial state. The data source has been created but not started.
     * No resources are allocated yet.
     */
    IDLE,

    /**
     * The data source is initializing and allocating resources.
     * Transition state between IDLE and ACTIVE.
     */
    STARTING,

    /**
     * The data source is active and emitting frames.
     * Resources are allocated and data is flowing.
     */
    ACTIVE,

    /**
     * The data source is paused. Resources remain allocated but
     * no frames are being emitted.
     *
     * Note: Not all data sources support pausing. File-based sources
     * may support this, while live sources typically do not.
     */
    PAUSED,

    /**
     * The data source is stopping and releasing resources.
     * Transition state between ACTIVE/PAUSED and STOPPED.
     */
    STOPPING,

    /**
     * The data source has stopped cleanly and released all resources.
     * Can transition back to IDLE or STARTING to restart.
     */
    STOPPED,

    /**
     * The data source encountered an error and is in a failed state.
     * Resources may or may not be released. Manual intervention
     * or restart is required.
     */
    ERROR
}
