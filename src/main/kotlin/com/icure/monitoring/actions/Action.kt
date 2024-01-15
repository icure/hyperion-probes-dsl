package com.icure.monitoring.actions

import com.icure.monitoring.actions.payload.ActionPayload
import com.icure.monitoring.probes.dsl.descriptors.DescriptorElement

/**
 * Base interface that all the classes that define an action to be executed for a probe trigger should implement.
 * It expects a generic parameter T that is a concrete implementation of the [ActionPayload] more suitable for this action.
 */
interface Action<T: ActionPayload> {

    /**
     * Executes the current action with the provided payload.
     * @param payload the payload to pass.
     */
    fun execute(payload: T, descriptors: Set<DescriptorElement>)

    /**
     * This function receives a collection of generic [ActionPayload] and will dispatch only the ones that can actually
     * be executed by the concrete implementation of this action.
     *
     * @param payloads a [Collection] of [ActionPayload].
     */
    fun acceptAndDispatch(payloads: Collection<ActionPayload>, descriptors: Set<DescriptorElement>)
}
