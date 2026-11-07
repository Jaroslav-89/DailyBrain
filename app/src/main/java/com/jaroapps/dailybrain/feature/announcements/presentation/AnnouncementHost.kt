package com.jaroapps.dailybrain.feature.announcements.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import com.jaroapps.dailybrain.core.presentation.FeedbackController
import com.jaroapps.dailybrain.feature.announcements.domain.model.AppAnnouncementType
import org.koin.compose.koinInject
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun AnnouncementHost(
    viewModel: AnnouncementViewModel = koinViewModel(),
) {
    val announcement by viewModel.currentAnnouncement.collectAsStateWithLifecycle()
    val feedbackController: FeedbackController = koinInject()
    var lastFeedbackAnnouncementId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(announcement?.id) {
        val item = announcement
        if (item != null && item.id != lastFeedbackAnnouncementId) {
            lastFeedbackAnnouncementId = item.id
            feedbackController.onAnnouncementShown(
                playFanfare = item.type == AppAnnouncementType.ACHIEVEMENTS ||
                    item.type == AppAnnouncementType.COLLECTIBLE,
            )
        }
    }
    announcement?.let { item ->
        AnnouncementDialog(
            announcement = item,
            onDismiss = { viewModel.dismiss(item.id) },
        )
    }
}
