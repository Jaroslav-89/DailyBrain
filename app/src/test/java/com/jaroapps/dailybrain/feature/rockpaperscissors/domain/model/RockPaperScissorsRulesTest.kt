package com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RockPaperScissorsRulesTest {
    @Test
    fun rockBeatsScissorsAndAwardsThreePoints() {
        val result = RockPaperScissorsRules.outcome(
            RockPaperScissorsMove.ROCK,
            RockPaperScissorsMove.SCISSORS,
        )
        assertEquals(RockPaperScissorsOutcome.WIN, result)
        assertEquals(3, RockPaperScissorsRules.points(result))
    }

    @Test
    fun paperBeatsRock() {
        assertEquals(
            RockPaperScissorsOutcome.WIN,
            RockPaperScissorsRules.outcome(
                RockPaperScissorsMove.PAPER,
                RockPaperScissorsMove.ROCK,
            ),
        )
    }

    @Test
    fun scissorsBeatsPaper() {
        assertEquals(
            RockPaperScissorsOutcome.WIN,
            RockPaperScissorsRules.outcome(
                RockPaperScissorsMove.SCISSORS,
                RockPaperScissorsMove.PAPER,
            ),
        )
    }

    @Test
    fun equalMovesAwardTwoPoints() {
        val result = RockPaperScissorsRules.outcome(
            RockPaperScissorsMove.PAPER,
            RockPaperScissorsMove.PAPER,
        )
        assertEquals(RockPaperScissorsOutcome.DRAW, result)
        assertEquals(2, RockPaperScissorsRules.points(result))
    }

    @Test
    fun lossAwardsOnePoint() {
        val result = RockPaperScissorsRules.outcome(
            RockPaperScissorsMove.SCISSORS,
            RockPaperScissorsMove.ROCK,
        )
        assertEquals(RockPaperScissorsOutcome.LOSS, result)
        assertEquals(1, RockPaperScissorsRules.points(result))
    }

    @Test
    fun roomRequestIsNormalizedAndValidated() {
        val request = CreateGameRoomRequest(
            name = "  Weekend  ",
            visibility = GameRoomVisibility.PRIVATE,
            maxParticipants = 8,
            targetScore = 50,
        )
        assertEquals("Weekend", RockPaperScissorsRules.validateRoomRequest(request).name)
    }

    @Test
    fun roomRequestRejectsInvalidCapacity() {
        val request = CreateGameRoomRequest(
            name = "Room",
            visibility = GameRoomVisibility.PUBLIC,
            maxParticipants = 9,
            targetScore = 50,
        )
        assertThrows(IllegalArgumentException::class.java) {
            RockPaperScissorsRules.validateRoomRequest(request)
        }
    }

    @Test
    fun pairIdIsIndependentOfArgumentOrder() {
        assertEquals(
            RockPaperScissorsRules.pairId("userA", "userB"),
            RockPaperScissorsRules.pairId("userB", "userA"),
        )
    }
}
