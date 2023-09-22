package com.icure.monitoring.probes.dsl.filters

import com.icure.monitoring.exceptions.UnsupportedDataSourceException
import io.micrometer.core.instrument.Meter

/**
 * This [Filter] can filter [Meter]s based on a generic function defined when instantiated.
 * Note: this can only be used on registry data sources and will throw an error if applied to remote data sources.
 *
 * @param description a description for the filter
 * @param filter a function that takes as input a [Meter] and returns true if the meter should be considered in the probe
 * and false otherwise
 */
data class GenericMeterFilter (
    val description: String = "Custom meter filter",
    val filter: (meter: Meter) -> Boolean
) : SimpleFilter() {
    override fun matches(meter: Meter): Boolean = filter(meter)
    override fun toString(): String = description
    override fun toElasticQuery(): String {
        throw UnsupportedDataSourceException("This filter is not compatible with a remote ES datasource.")
    }
}