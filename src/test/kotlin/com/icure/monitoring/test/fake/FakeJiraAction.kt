package com.icure.monitoring.test.fake

import com.icure.monitoring.actions.Action
import com.icure.monitoring.actions.payload.ActionPayload
import com.icure.monitoring.actions.payload.JiraActionPayload
import com.icure.monitoring.probes.dsl.descriptors.DescriptorElement

class FakeJiraAction : Action<JiraActionPayload> {

    val payloads = mutableListOf<JiraActionPayload>()

    override fun execute(payload: JiraActionPayload, descriptors: Set<DescriptorElement>) {
        payloads.add(payload)
    }

    override fun acceptAndDispatch(payloads: Collection<ActionPayload>, descriptors: Set<DescriptorElement>) {
        payloads.filterIsInstance<JiraActionPayload>().forEach { execute(it, descriptors) }
    }
}
