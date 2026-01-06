package com.can2android.domain

/**
 * Time-series buffer for a single signal.
 *
 * This class maintains a time-windowed history of signal samples,
 * automatically evicting samples that fall outside the configured
 * time window.
 *
 * @property timeWindowMicros Time window duration in microseconds
 */
class SignalBuffer(
    private val timeWindowMicros: Long
) {
    private val samples = mutableListOf<SignalSample>()

    /**
     * Add a new sample to the buffer.
     *
     * This will automatically evict samples that fall outside the
     * time window based on the new sample's timestamp.
     *
     * @param sample The signal sample to add
     */
    fun addSample(sample: SignalSample) {
        synchronized(samples) {
            samples.add(sample)
            evictOldSamples(sample.timestamp)
        }
    }

    /**
     * Get all samples currently in the buffer.
     *
     * @return Immutable copy of samples in chronological order
     */
    fun getSamples(): List<SignalSample> {
        synchronized(samples) {
            return samples.toList()
        }
    }

    /**
     * Get the most recent sample, or null if buffer is empty.
     *
     * @return The latest signal sample, or null
     */
    fun getLatestSample(): SignalSample? {
        synchronized(samples) {
            return samples.lastOrNull()
        }
    }

    /**
     * Get the number of samples currently in the buffer.
     *
     * @return Sample count
     */
    fun size(): Int {
        synchronized(samples) {
            return samples.size
        }
    }

    /**
     * Clear all samples from the buffer.
     */
    fun clear() {
        synchronized(samples) {
            samples.clear()
        }
    }

    /**
     * Evict samples that fall outside the time window.
     *
     * @param currentTimestamp Reference timestamp for window calculation
     */
    private fun evictOldSamples(currentTimestamp: Long) {
        val windowStart = currentTimestamp - timeWindowMicros
        samples.removeAll { it.timestamp < windowStart }
    }
}
