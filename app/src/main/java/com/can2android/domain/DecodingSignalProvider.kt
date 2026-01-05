package com.can2android.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of SignalProvider that uses a SignalDecoder to transform
 * raw CAN frames into signal samples.
 *
 * This is the standard implementation for data sources that provide raw
 * CAN frames (ASC files, USB CAN adapters, etc.).
 *
 * Data flow:
 * RawFrameProvider → SignalDecoder → SignalProvider → Signal Stream Core
 *
 * @property rawFrameProvider Source of raw CAN frames
 * @property signalDecoder Decoder to transform frames into signals
 */
class DecodingSignalProvider(
    private val rawFrameProvider: RawFrameProvider,
    private val signalDecoder: SignalDecoder
) : SignalProvider {

    override fun start() {
        rawFrameProvider.start()
    }

    override fun stop() {
        rawFrameProvider.stop()
    }

    override fun observeSignals(): Flow<SignalSample> {
        return rawFrameProvider.observeFrames()
            .map { frame -> signalDecoder.decode(frame) }
            .map { samples -> samples }
            // Flatten the list of samples into individual emissions
            .let { flow ->
                kotlinx.coroutines.flow.flow {
                    flow.collect { samples ->
                        samples.forEach { sample ->
                            emit(sample)
                        }
                    }
                }
            }
    }
}
