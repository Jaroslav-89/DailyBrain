package com.jaroapps.dailybrain.feature.rewards.data

import com.jaroapps.dailybrain.core.database.ActiveUserProvider
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.feature.rewards.domain.model.RewardBalanceType
import com.jaroapps.dailybrain.feature.rewards.domain.model.RewardHistoryEntry
import com.jaroapps.dailybrain.feature.rewards.domain.model.RewardHistorySnapshot
import com.jaroapps.dailybrain.feature.rewards.domain.repository.RewardHistoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class RewardHistoryRepositoryImpl(
    private val database: AppDatabase,
    private val activeUserProvider: ActiveUserProvider,
) : RewardHistoryRepository {

    override fun observeHistory(type: RewardBalanceType): Flow<RewardHistorySnapshot> =
        activeUserProvider.observeUserId().flatMapLatest { localAccountId ->
            combine(
                database.userProfileDao().observeProfile(localAccountId),
                database.rewardLedgerDao().observeForUser(localAccountId),
            ) { profile, ledger ->
                val entries = ledger.asSequence()
                    .filter { it.isActive }
                    .mapNotNull { operation ->
                        val amount = when (type) {
                            RewardBalanceType.XP -> operation.deltaXp
                            RewardBalanceType.ENDORPHINS -> operation.deltaEndorphins
                        }
                        if (amount == 0) null else RewardHistoryEntry(
                            id = operation.rewardKey,
                            amount = amount,
                            reason = operation.reason,
                            occurredAtEpochMs = operation.occurredAtEpochMs,
                        )
                    }
                    .sortedWith(
                        compareByDescending<RewardHistoryEntry> { it.occurredAtEpochMs }
                            .thenByDescending { it.id },
                    )
                    .toList()

                val balance = when (type) {
                    RewardBalanceType.XP -> profile?.xpPoints ?: 0
                    RewardBalanceType.ENDORPHINS -> profile?.endorphinBalance ?: 0
                }
                RewardHistorySnapshot(balance = balance, entries = entries)
            }
        }
}
