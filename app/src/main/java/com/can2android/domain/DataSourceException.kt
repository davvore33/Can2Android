package com.can2android.domain

/**
 * Base exception for all data source-related errors.
 *
 * This exception hierarchy allows callers to handle data source failures
 * in a structured way, distinguishing between different types of errors.
 */
sealed class DataSourceException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Thrown when a data source cannot be initialized or started.
 *
 * Examples:
 * - File not found
 * - USB device not connected
 * - Network connection failed
 * - Invalid permissions
 */
class DataSourceInitializationException(
    message: String,
    cause: Throwable? = null
) : DataSourceException(message, cause)

/**
 * Thrown when an error occurs while reading data from the source.
 *
 * Examples:
 * - I/O error while reading file
 * - USB device disconnected during operation
 * - Network timeout or connection lost
 * - Corrupted data stream
 */
class DataSourceReadException(
    message: String,
    cause: Throwable? = null
) : DataSourceException(message, cause)

/**
 * Thrown when the data format is invalid or cannot be parsed.
 *
 * Examples:
 * - Invalid ASC file format
 * - Malformed CAN frame data
 * - Unsupported file version
 */
class DataSourceFormatException(
    message: String,
    cause: Throwable? = null
) : DataSourceException(message, cause)

/**
 * Thrown when the data source is in an invalid state for the requested operation.
 *
 * Examples:
 * - Attempting to read before calling start()
 * - Attempting to start an already started source
 * - Attempting to use a closed/stopped source
 */
class DataSourceStateException(
    message: String,
    cause: Throwable? = null
) : DataSourceException(message, cause)
