package com.icure.monitoring.probes.kraken

import com.icure.monitoring.actions.Action
import com.icure.monitoring.actions.payload.ActionPayload
import com.icure.monitoring.actions.payload.JiraActionPayload
import com.icure.monitoring.model.MetricsTags
import com.icure.monitoring.probes.RegistryProbe
import com.icure.monitoring.probes.dsl.aggregators.MaxAggregator
import com.icure.monitoring.probes.dsl.collectors.FixedSizeCollector
import com.icure.monitoring.probes.dsl.descriptors.byTag
import com.icure.monitoring.probes.dsl.extractors.extractor
import com.icure.monitoring.probes.dsl.filters.isEqualTo
import com.icure.monitoring.probes.dsl.filters.matches
import com.icure.monitoring.probes.dsl.filters.meterIsADistribution
import com.icure.monitoring.probes.dsl.probe
import com.icure.monitoring.probes.dsl.utils.AggregatorParams
import com.icure.monitoring.test.fake.FakeJiraAction
import com.icure.monitoring.test.fake.PercentileDistributionSummary
import com.icure.monitoring.test.generateGauge
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.Tag
import java.time.Duration

private const val REGISTRY = "minutelyLogsElasticProperties"

/**
 * The parameters that differ between the Kraken timing probes defined in hyperion-probes.
 */
private data class KrakenTimingProbeSpec(
	val probeId: String,
	val metric: String,
	val groupTag: MetricsTags,
	val percentile: Double,
	val windowSize: Int,
	val threshold: Double,
	val ticketPrefix: String,
)

private val specs = listOf(
	KrakenTimingProbeSpec(
		probeId = "kraken_timing_per_path",
		metric = "total.time.path",
		groupTag = MetricsTags.PATH_CLASS,
		percentile = 0.5,
		windowSize = 3,
		threshold = 5_000.0,
		ticketPrefix = "kraken_excess_timing_on_",
	),
	KrakenTimingProbeSpec(
		probeId = "kraken_90p_timing_per_path",
		metric = "total.time.path",
		groupTag = MetricsTags.PATH_CLASS,
		percentile = 0.9,
		windowSize = 4,
		threshold = 10_000.0,
		ticketPrefix = "kraken_excess_90p_timing_on_",
	),
	KrakenTimingProbeSpec(
		probeId = "kraken_timing_per_node",
		metric = "total.time.node",
		groupTag = MetricsTags.BACKEND,
		percentile = 0.9,
		windowSize = 4,
		threshold = 3_000.0,
		ticketPrefix = "kraken_excess_timing_on_node_",
	),
)

/**
 * Mirrors the structure of the probes in io.hyperion.probes.kraken.KrakenTimingProbes.
 */
private fun KrakenTimingProbeSpec.buildProbe() = probe {
	probeId = this@buildProbe.probeId
	dataSource {
		registry {
			registryId = REGISTRY
		}
	}

	filter {
		(MetricsTags.METRIC isEqualTo metric) and meterIsADistribution() and (MetricsTags.COMPONENT matches "api")
	}

	group {
		listOf(byTag(groupTag))
	}

	customAggregation {
		AggregatorParams(
			aggregator = MaxAggregator,
			extractor = extractor { meter ->
				if (meter is DistributionSummary) {
					meter.takeSnapshot().percentileValues().firstNotNullOfOrNull {
						if (it.percentile() == percentile) {
							it.value()
						} else {
							null
						}
					}
				} else {
					null
				}
			},
			collectorProducer = { FixedSizeCollector(windowSize) }
		)
	}

	fixedThreshold { this@buildProbe.threshold }

	compare { value, referenceValue -> value > referenceValue }

	action {
		jira { value, threshold, descriptors ->
			val group = descriptors.firstOrNull()?.v ?: "UNKNOWN"
			JiraActionPayload(
				ticketId = "$ticketPrefix$group",
				title = "Response time on $group exceeded threshold",
				description = "Max is $threshold registered value is $value",
				autoCloseAfter = Duration.ofMinutes(10).toMillis(),
				value = value
			)
		}
	}
} as RegistryProbe

/**
 * Creates a summary shaped like the ones registered by GenericApiHaProxyPathParser, that publishes the 0.5, 0.9 and
 * 0.99 percentiles.
 */
private fun KrakenTimingProbeSpec.summary(
	group: String,
	p50: Double,
	p90: Double,
	p99: Double,
	component: String = "api",
	metric: String = this.metric,
) = PercentileDistributionSummary(
	name = "${component}_${metric.replace('.', '_')}_$group",
	tags = listOf(
		Tag.of(MetricsTags.COMPONENT.tagName, component),
		Tag.of(MetricsTags.METRIC.tagName, metric),
		Tag.of(groupTag.tagName, group),
	),
	percentiles = mapOf(0.5 to p50, 0.9 to p90, 0.99 to p99)
)

/**
 * Creates a summary where only the percentile monitored by the probe has the specified value, while the others are
 * kept below the threshold.
 */
private fun KrakenTimingProbeSpec.summaryWithMonitoredPercentile(group: String, value: Double) = summary(
	group = group,
	p50 = if (percentile == 0.5) value else 1.0,
	p90 = if (percentile == 0.9) value else 1.0,
	p99 = 1.0,
)

@Suppress("UNCHECKED_CAST")
private suspend fun RegistryProbe.dispatchTo(action: FakeJiraAction) =
	checkAndDispatch(listOf(action as Action<ActionPayload>))

class KrakenTimingProbeTest : StringSpec({

	specs.forEach { spec ->

		"${spec.probeId} - triggers when the monitored percentile exceeds the threshold" {
			val probe = spec.buildProbe()
			val fakeJiraAction = FakeJiraAction()
			probe.receiveMeter(spec.summaryWithMonitoredPercentile("group-a", spec.threshold + 1), REGISTRY)
			probe.dispatchTo(fakeJiraAction)
			fakeJiraAction.payloads.size shouldBe 1
			fakeJiraAction.payloads.first().ticketId shouldBe "${spec.ticketPrefix}group-a"
			fakeJiraAction.payloads.first().value shouldBe spec.threshold + 1
		}

		"${spec.probeId} - does not trigger when the monitored percentile is below the threshold" {
			val probe = spec.buildProbe()
			val fakeJiraAction = FakeJiraAction()
			repeat(spec.windowSize) {
				probe.receiveMeter(spec.summaryWithMonitoredPercentile("group-a", spec.threshold - 1), REGISTRY)
			}
			probe.dispatchTo(fakeJiraAction)
			fakeJiraAction.payloads.shouldBeEmpty()
		}

		"${spec.probeId} - does not trigger when the value is exactly the threshold" {
			val probe = spec.buildProbe()
			val fakeJiraAction = FakeJiraAction()
			probe.receiveMeter(spec.summaryWithMonitoredPercentile("group-a", spec.threshold), REGISTRY)
			probe.dispatchTo(fakeJiraAction)
			fakeJiraAction.payloads.shouldBeEmpty()
		}

		"${spec.probeId} - ignores the percentiles it does not monitor" {
			val probe = spec.buildProbe()
			val fakeJiraAction = FakeJiraAction()
			val high = spec.threshold * 10
			probe.receiveMeter(
				spec.summary(
					group = "group-a",
					p50 = if (spec.percentile == 0.5) 1.0 else high,
					p90 = if (spec.percentile == 0.9) 1.0 else high,
					p99 = high,
				),
				REGISTRY
			)
			probe.dispatchTo(fakeJiraAction)
			fakeJiraAction.payloads.shouldBeEmpty()
		}

		"${spec.probeId} - ignores the percentile gauges written to elastic" {
			// These are the documents found on elastic: gauges with a phi tag. They are never published to the probes,
			// and even if they were the probe should only consider the distribution summaries.
			val probe = spec.buildProbe()
			val fakeJiraAction = FakeJiraAction()
			probe.receiveMeter(
				generateGauge(
					name = "api_${spec.metric.replace('.', '_')}_group-a",
					tags = listOf(
						Tag.of(MetricsTags.COMPONENT.tagName, "api"),
						Tag.of(MetricsTags.METRIC.tagName, spec.metric),
						Tag.of(spec.groupTag.tagName, "group-a"),
						Tag.of("phi", "${spec.percentile}"),
					),
					value = spec.threshold * 10
				),
				REGISTRY
			)
			probe.dispatchTo(fakeJiraAction)
			fakeJiraAction.payloads.shouldBeEmpty()
		}

		"${spec.probeId} - ignores other components, other metrics and other registries" {
			val probe = spec.buildProbe()
			val fakeJiraAction = FakeJiraAction()
			val high = spec.threshold * 10
			probe.receiveMeter(spec.summary("group-a", high, high, high, component = "couchdb"), REGISTRY)
			probe.receiveMeter(spec.summary("group-a", high, high, high, metric = "total.time.group"), REGISTRY)
			probe.receiveMeter(spec.summary("group-a", high, high, high), "hourlyLogsElasticProperties")
			probe.dispatchTo(fakeJiraAction)
			fakeJiraAction.payloads.shouldBeEmpty()
		}

		"${spec.probeId} - ignores summaries that do not publish percentiles" {
			val probe = spec.buildProbe()
			val fakeJiraAction = FakeJiraAction()
			probe.receiveMeter(
				PercentileDistributionSummary(
					tags = listOf(
						Tag.of(MetricsTags.COMPONENT.tagName, "api"),
						Tag.of(MetricsTags.METRIC.tagName, spec.metric),
						Tag.of(spec.groupTag.tagName, "group-a"),
					),
					percentiles = emptyMap()
				),
				REGISTRY
			)
			probe.dispatchTo(fakeJiraAction)
			fakeJiraAction.payloads.shouldBeEmpty()
		}

		"${spec.probeId} - opens one ticket for each group over the threshold" {
			val probe = spec.buildProbe()
			val fakeJiraAction = FakeJiraAction()
			probe.receiveMeter(spec.summaryWithMonitoredPercentile("group-a", spec.threshold + 1), REGISTRY)
			probe.receiveMeter(spec.summaryWithMonitoredPercentile("group-b", spec.threshold - 1), REGISTRY)
			probe.receiveMeter(spec.summaryWithMonitoredPercentile("group-c", spec.threshold * 2), REGISTRY)
			probe.dispatchTo(fakeJiraAction)
			fakeJiraAction.payloads.map { it.ticketId } shouldContainExactlyInAnyOrder listOf(
				"${spec.ticketPrefix}group-a",
				"${spec.ticketPrefix}group-c",
			)
		}

		"${spec.probeId} - keeps triggering while a spike is within the last ${spec.windowSize} values" {
			val probe = spec.buildProbe()
			val fakeJiraAction = FakeJiraAction()
			probe.receiveMeter(spec.summaryWithMonitoredPercentile("group-a", spec.threshold * 2), REGISTRY)
			repeat(spec.windowSize - 1) {
				probe.receiveMeter(spec.summaryWithMonitoredPercentile("group-a", 1.0), REGISTRY)
			}
			probe.dispatchTo(fakeJiraAction)
			fakeJiraAction.payloads.size shouldBe 1
			fakeJiraAction.payloads.first().value shouldBe spec.threshold * 2
		}

		"${spec.probeId} - stops triggering once a spike is out of the last ${spec.windowSize} values" {
			val probe = spec.buildProbe()
			val fakeJiraAction = FakeJiraAction()
			probe.receiveMeter(spec.summaryWithMonitoredPercentile("group-a", spec.threshold * 2), REGISTRY)
			repeat(spec.windowSize) {
				probe.receiveMeter(spec.summaryWithMonitoredPercentile("group-a", 1.0), REGISTRY)
			}
			probe.dispatchTo(fakeJiraAction)
			fakeJiraAction.payloads.shouldBeEmpty()
		}

		"${spec.probeId} - does not dispatch again until new data is received" {
			val probe = spec.buildProbe()
			val fakeJiraAction = FakeJiraAction()
			probe.receiveMeter(spec.summaryWithMonitoredPercentile("group-a", spec.threshold + 1), REGISTRY)
			probe.dispatchTo(fakeJiraAction)
			probe.dispatchTo(fakeJiraAction)
			fakeJiraAction.payloads.size shouldBe 1
			probe.receiveMeter(spec.summaryWithMonitoredPercentile("group-a", spec.threshold + 1), REGISTRY)
			probe.dispatchTo(fakeJiraAction)
			fakeJiraAction.payloads.size shouldBe 2
		}
	}

})
