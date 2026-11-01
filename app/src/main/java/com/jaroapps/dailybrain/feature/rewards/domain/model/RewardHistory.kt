package com.jaroapps.dailybrain.feature.rewards.domain.model

enum class RewardBalanceType(val routeValue: String) {
    XP("xp"),
    ENDORPHINS("endorphins");

    companion object {
        fun fromRouteValue(value: String?): RewardBalanceType =
            entries.firstOrNull { it.routeValue == value } ?: XP
    }
}

data class RewardHistoryEntry(
    val id: String,
    val amount: Int,
    val reason: String,
    val occurredAtEpochMs: Long,
)

data class RewardHistorySnapshot(
    val balance: Int,
    val entries: List<RewardHistoryEntry>,
)
