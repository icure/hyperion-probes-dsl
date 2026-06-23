package com.icure.monitoring.test.fake

import com.icure.monitoring.actions.Action
import com.icure.monitoring.actions.payload.ActionPayload
import com.icure.monitoring.actions.payload.JiraActionPayload
import com.icure.monitoring.probes.dsl.descriptors.DescriptorElement

class FakeJiraAction : Action<JiraActionPayload> {

    val payloads = mutableListOf<JiraActionPayload>()

    override suspend fun execute(payload: JiraActionPayload) {
        payloads.add(payload)
    }

    override suspend fun acceptAndDispatch(payloads: Collection<ActionPayload>, descriptors: Set<DescriptorElement>) {
        payloads.filterIsInstance<JiraActionPayload>().forEach { execute(it) }
    }
}
