package com.icure.monitoring.probes.resources

import com.icure.monitoring.actions.Action
import com.icure.monitoring.actions.payload.ActionPayload
import com.icure.monitoring.actions.payload.JiraActionPayload
import com.icure.monitoring.model.MetricsTags
import com.icure.monitoring.probes.RegistryProbe
import com.icure.monitoring.probes.dsl.aggregators.aggregator
import com.icure.monitoring.probes.dsl.descriptors.byTag
import com.icure.monitoring.probes.dsl.extractors.GaugeExtractor
import com.icure.monitoring.probes.dsl.extractors.GaugeValue
import com.icure.monitoring.probes.dsl.filters.isEqualTo
import com.icure.monitoring.probes.dsl.filters.metricNameIs
import com.icure.monitoring.probes.dsl.probe
import com.icure.monitoring.probes.dsl.utils.aggregateUsing
import com.icure.monitoring.probes.dsl.utils.lastProducedBy
import com.icure.monitoring.probes.dsl.utils.over
import com.icure.monitoring.test.fake.FakeJiraAction
import com.icure.monitoring.test.generators.GaugeGenerator
import com.icure.monitoring.test.generators.VariableTag
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.Tag
import java.time.Duration
import kotlin.random.Random

class PodRestartProbeTest : StringSpec({

	"PodRestart probe test" {
		val registry = "minutelyLogsElasticProperties"
		val fakeJiraAction = FakeJiraAction()
		val probe = probe {
			probeId = "k8s_pod_restart"
			dataSource {
				registry {
					registryId = "minutelyLogsElasticProperties"
				}
			}

			filter {
				metricNameIs("restart")
			}

			group {
				listOf(byTag(MetricsTags.POD_ID))
			}

			max {
				GaugeExtractor over Duration.ofMinutes(1)
			}

			compare { value, referenceValue ->
				value >= referenceValue
			}

			fixedThreshold { 0.9 }

			action {
				jira { value, threshold, descriptors ->
					val podName = descriptors.firstOrNull()?.v ?: "UNKNOWN"
					JiraActionPayload(
						ticketId = "k8s_pod_restart",
						title = "Pod $podName restarted",
						description = "Limit value is $threshold, registered value is $value",
						autoCloseAfter = Duration.ofMinutes(30).toMillis(),
						value = value
					)
				}
			}
		} as RegistryProbe

		val triggerGenerator = GaugeGenerator(
			{ "icure.kraken-kraken-cloud-kraken-85fbff4d86-d9x8d.restartCount" },
			listOf(Tag.of(MetricsTags.METRIC.tagName, "restart"), Tag.of(MetricsTags.POD_ID.tagName, "kraken-kraken-cloud-kraken-85fbff4d86-d9x8d"), Tag.of(MetricsTags.NODE_ID.tagName, "doc-cr-app01.icure.ch")),
			emptyList(),
			{ 1.0 },
			byTag(MetricsTags.POD_ID)
		)

		val generator = GaugeGenerator(
			{ "couchdb-01-${listOf("lim", "rbx").random()}-0${Random.nextInt(1,4)}_z_pool_used_space_percentage" },
			listOf(Tag.of(MetricsTags.METRIC.tagName, "z_pool_used_space_percentage"), Tag.of(MetricsTags.STORAGE_NAME.tagName, "tank")),
			listOf(
				VariableTag(MetricsTags.NODE_ID) { "couchdb-01-${listOf("lim", "rbx").random()}-0${Random.nextInt(1,4)}" },
			),
			{ 42.0 },
			byTag(MetricsTags.NODE_ID)
		)

		triggerGenerator.generate(1).forEach {
			probe.receiveMeter(it, registry)
		}
		generator.generate(40).forEach {
			probe.receiveMeter(it, registry)
		}
		@Suppress("UNCHECKED_CAST")
		probe.checkAndDispatch(listOf(fakeJiraAction as Action<ActionPayload>))
		fakeJiraAction.payloads.size shouldBe 1
		fakeJiraAction.payloads.first().ticketId shouldBe "disk_space_exceeded_limit_couchdb-01-lim-05"
	}

})