package com.can2android.domain

/**
 * Metadata about a data source.
 *
 * This class provides descriptive information about a data source,
 * useful for UI display, logging, and debugging.
 *
 * @property name Human-readable name of the data source
 * @property type Type of the data source (e.g., "ASC File", "USB CAN", "gRPC Stream")
 * @property description Optional detailed description
 * @property properties Additional key-value properties specific to the source type
 */
data class DataSourceMetadata(
    val name: String,
    val type: String,
    val description: String? = null,
    val properties: Map<String, String> = emptyMap()
) {
    companion object {
        /**
         * Create metadata for an ASC file data source.
         */
        fun forAscFile(fileName: String, filePath: String): DataSourceMetadata {
            return DataSourceMetadata(
                name = fileName,
                type = "ASC File",
                description = "Vector ASC log file",
                properties = mapOf(
                    "path" to filePath,
                    "format" to "ASC"
                )
            )
        }

        /**
         * Create metadata for a USB CAN adapter data source.
         */
        fun forUsbCan(devicePath: String, baudRate: Int): DataSourceMetadata {
            return DataSourceMetadata(
                name = "USB CAN Adapter",
                type = "USB CAN",
                description = "Live CAN bus data via USB adapter",
                properties = mapOf(
                    "device" to devicePath,
                    "baudRate" to "${baudRate / 1000} kbps"
                )
            )
        }

        /**
         * Create metadata for a gRPC stream data source.
         */
        fun forGrpcStream(host: String, port: Int): DataSourceMetadata {
            return DataSourceMetadata(
                name = "Remote Stream",
                type = "gRPC Stream",
                description = "Remote CAN data via gRPC",
                properties = mapOf(
                    "host" to host,
                    "port" to port.toString()
                )
            )
        }
    }
}
