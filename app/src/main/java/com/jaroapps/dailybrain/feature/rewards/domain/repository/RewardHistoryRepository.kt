package com.jaroapps.dailybrain.feature.rewards.domain.repository

import com.jaroapps.dailybrain.feature.rewards.domain.model.RewardBalanceType
import com.jaroapps.dailybrain.feature.rewards.domain.model.RewardHistorySnapshot
import kotlinx.coroutines.flow.Flow

interface RewardHistoryRepository {
    fun observeHistory(type: RewardBalanceType): Flow<RewardHistorySnapshot>
}
