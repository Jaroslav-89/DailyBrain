package com.jaroapps.dailybrain.feature.memory.data

import android.content.Context
import com.jaroapps.dailybrain.feature.memory.domain.model.MemoryWord
import com.jaroapps.dailybrain.feature.memory.domain.repository.MemoryWordsRepository
import org.json.JSONArray

class MemoryWordsRepositoryImpl(
    private val context: Context
) : MemoryWordsRepository {

    override suspend fun getAllWords(): List<MemoryWord> {
        val json = context.assets
            .open(WORDS_FILE)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

        val jsonArray = JSONArray(json)

        return buildList {
            for (index in 0 until jsonArray.length()) {
                val value = jsonArray.optString(index, "").trim()

                if (value.isNotEmpty()) {
                    add(MemoryWord(value))
                }
            }
        }
    }

    private companion object {
        const val WORDS_FILE = "memory_words.json"
    }
}