package com.jaroapps.dailybrain.feature.feed.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestoreException
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.feed.domain.FeedEvent
import com.jaroapps.dailybrain.feature.feed.domain.FeedRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FeedUiState(
    val events: List<FeedEvent> = emptyList(),
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val errorMessage: String? = null,
)

class FeedViewModel(
    private val repository: FeedRepository,
) : ViewModel() {
    private val events = MutableStateFlow<List<FeedEvent>>(emptyList())
    private val isRefreshing = MutableStateFlow(false)
    private val isLoadingMore = MutableStateFlow(false)
    private val hasMore = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FeedUiState> = combine(
        events,
        isRefreshing,
        isLoadingMore,
        hasMore,
        errorMessage,
    ) { currentEvents, refreshing, loadingMore, more, error ->
        FeedUiState(
            events = currentEvents,
            isRefreshing = refreshing,
            isLoadingMore = loadingMore,
            hasMore = more,
            errorMessage = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FeedUiState(),
    )

    init {
        viewModelScope.launch {
            repository.observeFeed().collect { firstPageWithLookahead ->
                val newest = firstPageWithLookahead.take(PAGE_SIZE)
                val alreadyLoadedOlder = events.value.drop(PAGE_SIZE)
                events.value = (newest + alreadyLoadedOlder)
                    .distinctBy { it.eventId }
                    .sortedWith(
                        compareByDescending<FeedEvent> { it.occurredAtEpochMs }
                            .thenByDescending { it.eventId },
                    )
                hasMore.value = firstPageWithLookahead.size > PAGE_SIZE || alreadyLoadedOlder.isNotEmpty()
            }
        }
        refresh()
    }

    fun refresh() {
        if (isRefreshing.value) return
        viewModelScope.launch {
            isRefreshing.value = true
            errorMessage.value = null
            // Drop previously paged items before refreshing the cache. The first 30 remain
            // visible immediately; subsequent pages will be read from the updated Room cache.
            val hadMore = hasMore.value || events.value.size > PAGE_SIZE
            events.value = events.value.take(PAGE_SIZE)
            hasMore.value = hadMore
            try {
                withContext(Dispatchers.IO) { repository.refresh() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e(LOG_TAG, "Feed refresh failed.", error)
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                val diagnosticCode = firestoreError?.code?.name
                    ?: (error.message?.substringBefore(':')?.takeIf { it.matches(Regex("[A-Z0-9_]+")) })
                errorMessage.value = if (diagnosticCode != null) {
                    "Не удалось обновить ленту ($diagnosticCode). Показываем сохранённые новости."
                } else {
                    "Не удалось обновить ленту. Показываем сохранённые новости."
                }
            } finally {
                isRefreshing.value = false
            }
        }
    }

    fun loadMore() {
        if (isLoadingMore.value || isRefreshing.value || !hasMore.value) return
        val cursor = events.value.lastOrNull() ?: return
        viewModelScope.launch {
            isLoadingMore.value = true
            try {
                val pageWithLookahead = withContext(Dispatchers.IO) {
                    repository.loadOlderEvents(
                        beforeOccurredAtEpochMs = cursor.occurredAtEpochMs,
                        beforeEventId = cursor.eventId,
                        pageSize = PAGE_SIZE,
                    )
                }
                val page = pageWithLookahead.take(PAGE_SIZE)
                val existingIds = events.value.mapTo(mutableSetOf()) { it.eventId }
                events.value = events.value + page.filterNot { it.eventId in existingIds }
                hasMore.value = pageWithLookahead.size > PAGE_SIZE && page.isNotEmpty()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e(LOG_TAG, "Loading older feed events failed.", error)
                hasMore.value = false
                errorMessage.value = "Не удалось загрузить следующие новости из сохранённой истории."
            } finally {
                isLoadingMore.value = false
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 30
        const val LOG_TAG = "DailyBrainFeed"
    }
}
