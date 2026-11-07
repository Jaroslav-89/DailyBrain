package com.jaroapps.dailybrain.feature.feed.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestoreException
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.feed.domain.FeedEvent
import com.jaroapps.dailybrain.feature.feed.domain.FeedRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseSessionMismatchException
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseAuthenticationRepository
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveUserProfileUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
    observeUserProfile: ObserveUserProfileUseCase,
    firebaseAuthenticationRepository: FirebaseAuthenticationRepository,
) : ViewModel() {
    private val events = MutableStateFlow<List<FeedEvent>>(emptyList())
    private val isRefreshing = MutableStateFlow(false)
    private val isLoadingMore = MutableStateFlow(false)
    private val hasMore = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)
    private var refreshPending = false

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
        viewModelScope.launch {
            combine(
                observeUserProfile(),
                firebaseAuthenticationRepository.observeCurrentUserId(),
            ) { profile, firebaseUid ->
                profile.localAccountId to profile.firebaseUid?.takeIf {
                    it.isNotBlank() && it == firebaseUid
                }
            }
                .distinctUntilChanged()
                .collect {
                    // Always recalculate the selected profile's local rewards. The repository
                    // skips cloud work until the active Firebase UID matches the stored binding.
                    // A later matching auth-state emission retries the deferred cloud sync.
                    refresh()
                }
        }
    }

    fun refresh() {
        if (isRefreshing.value) {
            refreshPending = true
            return
        }
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
                val firestoreError = generateSequence<Throwable>(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                val diagnosticCode = firestoreError?.code?.name
                    ?: (error.message?.substringBefore(':')?.takeIf { it.matches(Regex("[A-Z0-9_]+")) })
                errorMessage.value = when {
                    error is FirebaseSessionMismatchException ->
                        "Войдите в Google-аккаунт, привязанный к этому профилю. Показываем сохранённую ленту."
                    diagnosticCode != null ->
                        "Не удалось обновить ленту ($diagnosticCode). Показываем сохранённые новости."
                    else -> "Не удалось обновить ленту. Показываем сохранённые новости."
                }
            } finally {
                isRefreshing.value = false
                if (refreshPending) {
                    refreshPending = false
                    refresh()
                }
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
