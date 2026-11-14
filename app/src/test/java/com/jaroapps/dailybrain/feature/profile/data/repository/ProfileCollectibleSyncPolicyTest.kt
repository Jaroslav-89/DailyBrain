package com.jaroapps.dailybrain.feature.profile.data.repository

import com.jaroapps.dailybrain.core.database.entity.UserBadgeEntity
import com.jaroapps.dailybrain.feature.profile.domain.model.ProfileCollectibleKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ProfileCollectibleSyncPolicyTest {
    @Test
    fun sameBadgeTypeFromDifferentPeriodsUsesDifferentFirestoreDocuments() {
        val novemberFirstWeek = publicCollectibleDocumentId(
            ProfileCollectibleKind.BADGE,
            "weekly_streak:2026-11-02",
        )
        val novemberSecondWeek = publicCollectibleDocumentId(
            ProfileCollectibleKind.BADGE,
            "weekly_streak:2026-11-09",
        )

        assertNotEquals(novemberFirstWeek, novemberSecondWeek)
    }

    @Test
    fun onlyExplicitlyRevokedBadgesAreScheduledForDeletion() {
        val badges = listOf(
            badge(id = "weekly_streak:2026-11-02", revoked = false),
            badge(id = "weekly_streak:2026-11-09", revoked = true),
        )

        assertEquals(
            listOf("weekly_streak:2026-11-09"),
            revokedBadgeIdsToDelete(
                badges = badges,
                activeBadgeIds = setOf("weekly_streak:2026-11-02"),
            ),
        )
    }

    @Test
    fun activeBadgeIsNotDeletedWhenRevocationSnapshotIsStale() {
        val badges = listOf(badge(id = "weekly_streak:2026-11-02", revoked = true))

        assertEquals(
            emptyList<String>(),
            revokedBadgeIdsToDelete(
                badges = badges,
                activeBadgeIds = setOf("weekly_streak:2026-11-02"),
            ),
        )
    }

    private fun badge(id: String, revoked: Boolean) = UserBadgeEntity(
        ownerUserId = "local-profile",
        badgeId = id,
        badgeType = "WEEKLY_STREAK",
        periodKey = id.substringAfter(':'),
        awardedAtEpochMs = 1L,
        triggerWorkoutId = null,
        isRevoked = revoked,
    )
}
