package com.icure.monitoring.probes.dsl.collectors

/**
 * Base interface for all the collectors.
 * Concrete implementation will store the values from the meters according to different strategies and will provide
 * them to the aggregators.
 */
sealed interface Collector {

    /**
     * Registers a new value extracted from a meter to the collector.
     * The concrete implementations should be thread-safe.
     *
     * @param value the value to register.
     */
    suspend fun addValue(value: Double)

    /**
     * The concrete implementations of this method do not consume the values: if called multiple times without
     * adding new values to the collector, the result should be the same.
     *
     * @return a [List] of [Double] that contains all the values currently stored in this collector.
     */
    fun getValues(): List<Double>

}