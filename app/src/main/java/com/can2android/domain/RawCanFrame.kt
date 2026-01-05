package com.can2android.domain

/**
 * Represents a raw CAN frame as received from a data source.
 *
 * This is the fundamental unit of raw CAN data, containing only the
 * unprocessed frame information without any signal decoding or interpretation.
 *
 * @property timestamp Timestamp in microseconds since epoch
 * @property canId CAN identifier (11-bit or 29-bit extended)
 * @property dlc Data Length Code (0-8 for standard CAN, 0-64 for CAN-FD)
 * @property payload Raw data bytes
 */
data class RawCanFrame(
    val timestamp: Long,
    val canId: Int,
    val dlc: Int,
    val payload: ByteArray
) {
    /**
     * Custom equals implementation to properly compare ByteArray payload
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RawCanFrame

        if (timestamp != other.timestamp) return false
        if (canId != other.canId) return false
        if (dlc != other.dlc) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    /**
     * Custom hashCode implementation to properly hash ByteArray payload
     */
    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + canId
        result = 31 * result + dlc
        result = 31 * result + payload.contentHashCode()
        return result
    }
}
