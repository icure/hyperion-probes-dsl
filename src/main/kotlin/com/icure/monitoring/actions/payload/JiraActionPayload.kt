package com.icure.monitoring.actions.payload

/**
 * Payload to be passed to the Jira action to create or update Jira tickets.
 * @param ticketId the id of the probe. Will be set as the Probe_ID field in the Jira ticket and used for the successive
 * retrieving of tickets. A new ticket will be created for each different ticketId value.
 * @param title the title of the Jira ticket to create.
 * @param description the description of the Jira ticket to create. If the ticket is already opened or just reopened,
 * it will be the content of the comment that will be added.
 * @param autoCloseAfter if set, the number of milliseconds after the last modification of the ticket after which the
 * ticket will be auto closed. If null, the ticket will never be closed automatically.
 * @param customFields a [Map] containing additional custom fields to set in the ticket
 * @param value the value that triggered the action.
 * @param threshold the threshold value.
 */
data class JiraActionPayload(
    val ticketId: String,
    val title: String,
    val description: String,
    val autoCloseAfter: Long?,
    val issueType: String? = null,
    val customFields: Map<String, String> = emptyMap(),
    val value: Double,
    val threshold: Double? = null
): ActionPayload
