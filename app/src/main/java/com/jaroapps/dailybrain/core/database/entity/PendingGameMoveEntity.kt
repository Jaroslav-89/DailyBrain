package com.jaroapps.dailybrain.core.database.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "pending_game_moves",
    primaryKeys = ["ownerUserId", "roomId", "roundId", "opponentUid"],
    indices = [Index(value = ["ownerUserId", "createdAtEpochMs"])],
)
data class PendingGameMoveEntity(
    val ownerUserId: String,
    val roomId: String,
    val roundId: String,
    val opponentUid: String,
    val move: String,
    val createdAtEpochMs: Long,
)
