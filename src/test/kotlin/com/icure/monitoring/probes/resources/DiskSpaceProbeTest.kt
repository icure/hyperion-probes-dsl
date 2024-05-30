package com.icure.monitoring.probes.resources

import com.icure.monitoring.actions.Action
import com.icure.monitoring.actions.payload.ActionPayload
import com.icure.monitoring.actions.payload.JiraActionPayload
import com.icure.monitoring.model.MetricsTags
import com.icure.monitoring.probes.RegistryProbe
import com.icure.monitoring.probes.dsl.aggregators.aggregator
import com.icure.monitoring.probes.dsl.descriptors.byTag
import com.icure.monitoring.probes.dsl.extractors.GaugeValue
import com.icure.monitoring.probes.dsl.filters.isEqualTo
import com.icure.monitoring.probes.dsl.probe
import com.icure.monitoring.probes.dsl.utils.aggregateUsing
import com.icure.monitoring.probes.dsl.utils.lastProducedBy
import com.icure.monitoring.test.fake.FakeJiraAction
import com.icure.monitoring.test.generators.GaugeGenerator
import com.icure.monitoring.test.generators.VariableTag
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.micrometer.core.instrument.Tag
import kotlin.random.Random

class DiskSpaceProbeTest : StringSpec({

	"HealthCheckProbe test" {
		val registry = "minutelyLogsElasticProperties"
		val fakeJiraAction = FakeJiraAction()
		val probe = probe {
			probeId = "disk_space_exceeded_limit"
			dataSource {
				registry {
					registryId = "minutelyLogsElasticProperties"
				}
			}

			filter {
				MetricsTags.METRIC isEqualTo "z_pool_used_space_percentage"
			}

			group {
				listOf(byTag(MetricsTags.NODE_ID))
			}

			customAggregation {
				3 lastProducedBy GaugeValue aggregateUsing aggregator { collector ->
					collector.getValues()?.takeIf { it.size > 2 }?.average()
				}
			}

			fixedThreshold { 83.0 }

			compare { value, referenceValue -> value >= referenceValue }

			action {
				jira { value, threshold, descriptors ->
					val node = descriptors.firstOrNull()?.v ?: "UNKNOWN"
					JiraActionPayload(
						ticketId = "disk_space_exceeded_limit_$node",
						title = "Disk space critical $value% on $node",
						description = "Threshold value is $threshold registered value is $value",
						autoCloseAfter = null,
						value = value
					)
				}
			}
		} as RegistryProbe

		val triggerGenerator = GaugeGenerator(
			{ "couchdb-01-lim-05_z_pool_used_space_percentage" },
			listOf(Tag.of(MetricsTags.METRIC.tagName, "z_pool_used_space_percentage"), Tag.of(MetricsTags.NODE_ID.tagName, "couchdb-01-lim-05"), Tag.of(MetricsTags.STORAGE_NAME.tagName, "tank")),
			emptyList(),
			{ 83.0 },
			byTag(MetricsTags.NODE_ID)
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

		triggerGenerator.generate(4).forEach {
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