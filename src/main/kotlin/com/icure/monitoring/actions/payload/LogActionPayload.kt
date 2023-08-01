package com.icure.monitoring.actions.payload

import com.icure.monitoring.model.LogLevel

/**
 * Payload to be passed to the Log action to log the triggering of a probe.
 * @param log the log to publish.
 * @param logLevel the [LogLevel].
 **/
data class LogActionPayload(
    val log: String,
    val logLevel: LogLevel
) : ActionPayload
