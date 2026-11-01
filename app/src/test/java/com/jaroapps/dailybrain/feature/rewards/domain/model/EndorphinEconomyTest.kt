package com.jaroapps.dailybrain.feature.rewards.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EndorphinEconomyTest {

    @Test
    fun achievementRewardAmountsMatchEconomyRules() {
        assertEquals(10, EndorphinEconomy.WEEKLY_STREAK_REWARD)
        assertEquals(10, EndorphinEconomy.WEEKLY_CHALLENGE_REWARD)
        assertEquals(10, EndorphinEconomy.ARITHMETIC_RECORD_REWARD)
        assertEquals(50, EndorphinEconomy.MONTHLY_STREAK_REWARD)
    }

    @Test
    fun shopCatalogKeepsPlannedPricesAndLimits() {
        assertEquals(10, EndorphinEconomy.PATCH_PRICE)
        assertEquals(5, EndorphinEconomy.MAX_PATCH_COUNT)
        assertEquals(100, EndorphinEconomy.COSTUME_PRICE)
        assertEquals(100, EndorphinEconomy.BACKGROUND_PRICE)
        assertEquals(listOf(10, 20, 30, 40, 50), EndorphinEconomy.gifts.map { it.price })
    }

    @Test
    fun negativeBalanceCanRecoverButCannotFundSpending() {
        assertFalse(EndorphinEconomy.canSpend(balance = -10, amount = 1))
        assertFalse(EndorphinEconomy.canSpend(balance = 0, amount = 1))
        assertFalse(EndorphinEconomy.canSpend(balance = 20, amount = 21))
        assertFalse(EndorphinEconomy.canSpend(balance = 20, amount = 0))
        assertTrue(EndorphinEconomy.canSpend(balance = 20, amount = 20))
    }
}
