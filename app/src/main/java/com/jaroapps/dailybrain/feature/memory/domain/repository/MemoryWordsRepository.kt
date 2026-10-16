package com.jaroapps.dailybrain.feature.memory.domain.repository

import com.jaroapps.dailybrain.feature.memory.domain.model.MemoryWord

interface MemoryWordsRepository {

    suspend fun getAllWords(): List<MemoryWord>
}