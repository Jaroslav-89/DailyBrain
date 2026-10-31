package com.jaroapps.dailybrain.feature.feed.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.feed.domain.FeedEvent
import com.jaroapps.dailybrain.feature.feed.domain.FeedRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FeedUiState(
    val events: List<FeedEvent> = emptyList(),
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

class FeedViewModel(
    private val repository: FeedRepository,
) : ViewModel() {
    private val isRefreshing = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FeedUiState> = combine(
        repository.observeFeed(),
        isRefreshing,
        errorMessage,
    ) { events, refreshing, error ->
        FeedUiState(
            events = events.sortedByDescending { it.occurredAtEpochMs },
            isRefreshing = refreshing,
            errorMessage = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FeedUiState(),
    )

    init { refresh() }

    fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch {
            isRefreshing.value = true
            errorMessage.value = null
            try {
                repository.refresh()
            } catch (_: Exception) {
                errorMessage.value = "Не удалось обновить ленту. Показываем сохранённые новости."
            } finally {
                isRefreshing.value = false
            }
        }
    }
}