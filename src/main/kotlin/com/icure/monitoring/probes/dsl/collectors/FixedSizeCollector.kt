package com.icure.monitoring.probes.dsl.collectors

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * This concrete [Collector] stores only the last values sent to it.
 *
 * @param windowSize the maximum number of values to store.
 */
class FixedSizeCollector(
    private val windowSize: Int
): Collector {

    private val queue = Array(windowSize) { 0.0 }
    private var head: Int = 0
    private val queueMutex = Mutex()

    override suspend fun addValue(value: Double) {
        queueMutex.withLock {
            queue[head] = value
            head = (head + 1) % windowSize
        }
    }

    override fun getValues(): List<Double> = List(windowSize) { queue[(head + 1 + it) % windowSize] }

}