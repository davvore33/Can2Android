package com.can2android.domain

/**
 * Interface for decoding raw CAN frames into physical signal samples.
 *
 * This interface represents the Parser/Decoder layer in the architecture,
 * transforming raw CAN frames into meaningful signal values using DBC
 * definitions or other decoding mechanisms.
 *
 * Implementations MUST:
 * - Decode signals at runtime (not pre-process)
 * - Preserve timestamps from the source frames
 * - Handle unknown CAN IDs gracefully (return empty list)
 * - Support stream-based operation
 *
 * This component is OPTIONAL and can be bypassed when the data source
 * provides already-decoded signals (e.g., from a remote gRPC stream).
 */
interface SignalDecoder {
    /**
     * Decode a raw CAN frame into zero or more signal samples.
     *
     * A single CAN frame may contain multiple signals. If the CAN ID is
     * not recognized or no signals are defined for it, an empty list
     * should be returned.
     *
     * @param frame The raw CAN frame to decode
     * @return List of decoded signal samples (may be empty)
     */
    fun decode(frame: RawCanFrame): List<SignalSample>
}
