package com.icure.monitoring.probes.kraken

import com.icure.monitoring.actions.Action
import com.icure.monitoring.actions.payload.ActionPayload
import com.icure.monitoring.actions.payload.JiraActionPayload
import com.icure.monitoring.model.MetricsTags
import com.icure.monitoring.probes.RegistryProbe
import com.icure.monitoring.probes.dsl.descriptors.byTag
import com.icure.monitoring.probes.dsl.extractors.GaugeExtractor
import com.icure.monitoring.probes.dsl.filters.GenericMeterFilter
import com.icure.monitoring.probes.dsl.filters.isEqualTo
import com.icure.monitoring.probes.dsl.filters.matches
import com.icure.monitoring.probes.dsl.filters.meterIsAGauge
import com.icure.monitoring.probes.dsl.probe
import com.icure.monitoring.probes.dsl.utils.lastProducedBy
import com.icure.monitoring.test.fake.FakeJiraAction
import com.icure.monitoring.test.generators.GaugeGenerator
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.Tag
import java.time.Duration
import kotlin.sequences.forEach

class KrakenTimingProbeTest  : StringSpec({

	"Kraken timing per path test" {
		val registry = "minutelyLogsElasticProperties"
		val fakeJiraAction = FakeJiraAction()
		val probe = probe {
			probeId = "kraken_timing_per_path"
			dataSource {
				registry {
					registryId = registry
				}
			}

			filter {
				(MetricsTags.METRIC isEqualTo "total.time.path") and meterIsAGauge() and (MetricsTags.COMPONENT matches "api") and GenericMeterFilter("phi") { meter ->
					meter.id.tags.firstOrNull {
						it.key == "phi"
					}?.value?.lowercase() == "0.5"
				}
			}

			group {
				listOf(byTag(MetricsTags.PATH_CLASS))
			}

			max {
				3 lastProducedBy GaugeExtractor
			}

			fixedThreshold { 10_000.0 }

			compare { value, referenceValue -> value > referenceValue }

			action {
				jira { value, threshold, descriptors ->
					val path = descriptors.firstOrNull()?.v ?: "UNKNOWN"
					JiraActionPayload(
						ticketId = "kraken_excess_timing_on_$path",
						title = "50 percentile of response time on $path exceeded 10s",
						description = "Max is $threshold registered value is $value",
						autoCloseAfter = Duration.ofMinutes(10).toMillis(),
						value = value
					)
				}
			}
		} as RegistryProbe

		val triggerGenerator = GaugeGenerator(
			{ "api_total_time_path_class_rest/v2/classification" },
			listOf(
				Tag.of(MetricsTags.COMPONENT.tagName, "api"),
				Tag.of(MetricsTags.METRIC.tagName, "total.time.path"),
				Tag.of(MetricsTags.PATH_CLASS.tagName, "rest/v2/classification"),
				Tag.of("phi", "0.5"),
			),
			emptyList(),
			{ 13407.0 },
			byTag(MetricsTags.PATH_CLASS)
		)

		triggerGenerator.generate(5).forEach {
			probe.receiveMeter(it, registry)
		}
		probe.checkAndDispatch(listOf(fakeJiraAction as Action<ActionPayload>))
		fakeJiraAction.payloads.size shouldBe 1
		fakeJiraAction.payloads.first().ticketId shouldBe "kraken_excess_timing_on_rest/v2/classification"
	}

})