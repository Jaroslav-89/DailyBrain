package com.jaroapps.dailybrain.feature.history.presentation

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.jaroapps.dailybrain.feature.profile.presentation.ProfileViewModel
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.io.FileOutputStream

@Composable
fun ShareProgressDialog(
    calendarState: CalendarUiState,
    onDismiss: () -> Unit,
    profileViewModel: ProfileViewModel = koinViewModel(),
) {
    val profileState by profileViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedTemplate by remember { mutableStateOf(ShareCardTemplate.CLASSIC) }
    val displayName = profileState.profile?.effectiveDisplayName ?: "Участник DailyBrain"
    val bitmap = remember(
        selectedTemplate,
        displayName,
        calendarState.currentStreakDays,
        calendarState.weeklyStreakProgress,
        calendarState.monthlyStreakProgress,
        calendarState.monthlyStreakTarget,
        profileState.statistics.bestStreakDays,
    ) {
        ProgressShareCardRenderer.render(
            template = selectedTemplate,
            displayName = displayName,
            currentStreak = calendarState.currentStreakDays,
            bestStreak = profileState.statistics.bestStreakDays,
            weeklyProgress = calendarState.weeklyStreakProgress,
            monthlyProgress = calendarState.monthlyStreakProgress,
            monthlyTarget = calendarState.monthlyStreakTarget,
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Поделиться прогрессом",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TemplateChoice(
                        title = "Classic",
                        selected = selectedTemplate == ShareCardTemplate.CLASSIC,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTemplate = ShareCardTemplate.CLASSIC },
                    )
                    TemplateChoice(
                        title = "Premium",
                        selected = selectedTemplate == ShareCardTemplate.PREMIUM,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTemplate = ShareCardTemplate.PREMIUM },
                    )
                }
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Предпросмотр карточки прогресса",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 5f)
                        .clip(RoundedCornerShape(16.dp)),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Закрыть")
                    }
                    Button(
                        onClick = { shareProgressImage(context, bitmap) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Поделиться")
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplateChoice(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            ),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick,
        shape = shape,
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(vertical = 11.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun shareProgressImage(context: Context, bitmap: android.graphics.Bitmap) {
    val shareDirectory = File(context.cacheDir, "share").apply { mkdirs() }
    val imageFile = File(shareDirectory, "dailybrain_progress.png")
    FileOutputStream(imageFile).use { output ->
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
    }
    val imageUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile,
    )
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, imageUri)
        putExtra(Intent.EXTRA_SUBJECT, "Мой прогресс в DailyBrain")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Поделиться прогрессом"))
}
