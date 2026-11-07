package com.jaroapps.dailybrain.feature.announcements.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.announcements.domain.model.AppAnnouncement
import com.jaroapps.dailybrain.feature.announcements.domain.repository.AnnouncementRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AnnouncementViewModel(
    private val repository: AnnouncementRepository,
) : ViewModel() {
    val currentAnnouncement: StateFlow<AppAnnouncement?> = repository.observeNext()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun dismiss(announcementId: String) {
        viewModelScope.launch { repository.dismiss(announcementId) }
    }
}
