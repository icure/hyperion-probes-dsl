package com.icure.monitoring.actions.payload

/**
 * Payload to be passed to the Jira action to create or update Jira tickets.
 * @param probeId the id of the probe. Will be set as the Probe_ID field in the Jira ticket and used for the successive
 * retrieving of tickets.
 * @param title the title of the Jira ticket to create.
 * @param description the description of the Jira ticket to create. If the ticket is already opened or just reopened,
 * it will be the content of the comment that will be added.
 * @param autoCloseAfter if set, the number of milliseconds after the last modification of the ticket after which the
 * ticket will be auto closed. If null, the ticket will never be closed automatically.
 */
data class JiraActionPayload(
    val probeId: String,
    val title: String,
    val description: String,
    val autoCloseAfter: Long?
): ActionPayload
