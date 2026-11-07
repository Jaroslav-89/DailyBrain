package com.jaroapps.dailybrain.feature.announcements.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun AnnouncementHost(
    viewModel: AnnouncementViewModel = koinViewModel(),
) {
    val announcement by viewModel.currentAnnouncement.collectAsStateWithLifecycle()
    announcement?.let { item ->
        AnnouncementDialog(
            announcement = item,
            onDismiss = { viewModel.dismiss(item.id) },
        )
    }
}
