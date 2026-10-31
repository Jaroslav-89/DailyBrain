package com.jaroapps.dailybrain.feature.feed.domain

data class FeedEvent(
    val eventId: String,
    val authorFirebaseUid: String?,
    val authorName: String,
    val authorUsername: String?,
    val eventType: String,
    val occurredAtEpochMs: Long,
    val periodKey: String? = null,
    val exerciseType: String? = null,
    val valueLong: Long? = null,
    val valueInt: Int? = null,
    val accuracyPercent: Double? = null,
)
