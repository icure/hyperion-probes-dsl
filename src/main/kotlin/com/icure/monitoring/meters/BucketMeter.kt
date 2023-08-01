package com.icure.monitoring.meters

import io.micrometer.core.instrument.Meter

/**
 * Interface that defines the behaviour of all the meters that can save data related to multiple time windows.
 */
interface BucketMeter<T> : Meter {

    /**
     * Retrieves the data for all the time windows.
     */
    fun getBuckets(): Map<Long, T>

    /**
     * Gets the timestamps of all the completed buckets.
     */
    fun getTimestamps(): List<Long>

    /**
     * Removes the buckets related to the timestamps passed as parameter
     */
    fun clearBuckets(ids: Collection<Long>)

}