package com.icure.monitoring.test.fake

import com.icure.monitoring.actions.Action
import com.icure.monitoring.actions.payload.ActionPayload
import com.icure.monitoring.actions.payload.JiraActionPayload

class FakeJiraAction : Action<JiraActionPayload> {

    val payloads = mutableListOf<JiraActionPayload>()

    override fun execute(payload: JiraActionPayload) {
        payloads.add(payload)
    }

    override fun acceptAndDispatch(payloads: Collection<ActionPayload>) {
        payloads.filterIsInstance<JiraActionPayload>().forEach { execute(it) }
    }
}