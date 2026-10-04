package com.icure.monitoring.probes.dsl.filters

import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Meter
import kotlinx.serialization.Serializable

/**
 * Ensure that the statistical support of the meter (i.e. the sample size) is greater than the configured value.
 * Warning: it currently works only on DistributionSummaries
 */
@Serializable
data class StatisticalSupportFilter(
	val support: Long,
) : SimpleFilter() {

	override fun matches(meter: Meter): Boolean = when (meter.id.type) {
		Meter.Type.GAUGE,
			Meter.Type.COUNTER,
			Meter.Type.LONG_TASK_TIMER,
			Meter.Type.TIMER,
			Meter.Type.OTHER -> false
		Meter.Type.DISTRIBUTION_SUMMARY ->
			((meter as? DistributionSummary)?.takeSnapshot()?.count() ?: 0) >= support
	}
	override fun toString(): String = "support is greater than $support"
	override fun toElasticQuery(): String = throw UnsupportedOperationException("This filter is not yet supported on ES queries")
}

fun supportIsGreaterThan(value: Long) = StatisticalSupportFilter(value)