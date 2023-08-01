package com.icure.monitoring.actions

import com.icure.monitoring.actions.payload.ActionPayload

/**
 * Base interface that all the classes that define an action to be executed for a probe trigger should implement.
 * It expects a generic parameter T that is a concrete implementation of the [ActionPayload] more suitable for this action.
 */
interface Action<T: ActionPayload> {

    /**
     * Executes the current action with the provided payload.
     * @param payload the payload to pass.
     */
    fun execute(payload: T)
}