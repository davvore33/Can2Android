package com.can2android.domain

import java.io.File

/**
 * Configuration for a data source.
 *
 * This sealed interface defines the configuration types for different
 * data sources. Each data source type has its own configuration class.
 *
 * This abstraction allows the system to support multiple data source
 * types while maintaining type safety and clear configuration contracts.
 */
sealed interface DataSourceConfig {
    /**
     * Configuration for ASC file-based data source.
     *
     * @property file The ASC file to read CAN frames from
     * @property replaySpeed Speed multiplier for frame replay (1.0 = real-time, 2.0 = 2x speed, etc.)
     *                       null = read as fast as possible
     */
    data class AscFile(
        val file: File,
        val replaySpeed: Double? = 1.0
    ) : DataSourceConfig

    /**
     * Configuration for USB CAN adapter data source.
     *
     * @property devicePath Path to the USB device (e.g., "/dev/ttyUSB0")
     * @property baudRate CAN bus baud rate (e.g., 500000 for 500 kbps)
     * @property bufferSize Size of the read buffer in bytes
     */
    data class UsbCan(
        val devicePath: String,
        val baudRate: Int,
        val bufferSize: Int = 8192
    ) : DataSourceConfig

    /**
     * Configuration for remote gRPC stream data source.
     *
     * @property host Server hostname or IP address
     * @property port Server port
     * @property useTls Whether to use TLS encryption
     * @property authToken Optional authentication token
     */
    data class GrpcStream(
        val host: String,
        val port: Int,
        val useTls: Boolean = true,
        val authToken: String? = null
    ) : DataSourceConfig
}
