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

    private var queue = mutableListOf<Double>()
    private val queueMutex = Mutex()

    override suspend fun addValue(value: Double) {
        queueMutex.withLock {
            queue.add(value)
            if(queue.size > windowSize) {
                queue = queue.drop(queue.size - windowSize).toMutableList()
            }
        }
    }

    override fun getValues(): List<Double> = queue
}