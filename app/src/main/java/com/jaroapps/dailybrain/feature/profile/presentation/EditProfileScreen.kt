package com.jaroapps.dailybrain.feature.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.feature.profile.domain.model.UserGender
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = state.profile
    var displayName by remember(profile?.displayName) { mutableStateOf(profile?.displayName.orEmpty()) }
    var gender by remember(profile?.gender) { mutableStateOf(profile?.gender?.name) }
    var birthDate by remember(profile?.birthDate) { mutableStateOf(profile?.birthDate?.toString().orEmpty()) }
    var country by remember(profile?.country) { mutableStateOf(profile?.country.orEmpty()) }
    var city by remember(profile?.city) { mutableStateOf(profile?.city.orEmpty()) }
    var genderMenuExpanded by remember { mutableStateOf(false) }
    var birthDateError by remember { mutableStateOf(false) }
    var datePickerVisible by remember { mutableStateOf(false) }
    val initialDateMillis = remember(birthDate) {
        runCatching { LocalDate.parse(birthDate).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        TopAppBar(
            title = { Text(stringResource(R.string.profile_edit_title)) },
            navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.action_cancel)) } },
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.profile_name)) },
            supportingText = { Text(stringResource(R.string.profile_name_optional)) },
            singleLine = true,
        )
        Spacer(Modifier.height(8.dp))
        Column {
            OutlinedTextField(
                value = gender?.let { genderLabel(it) }.orEmpty(),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.profile_gender)) },
                trailingIcon = { TextButton(onClick = { genderMenuExpanded = true }) { Text("▾") } },
            )
            DropdownMenu(expanded = genderMenuExpanded, onDismissRequest = { genderMenuExpanded = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.profile_gender_not_set)) }, onClick = { gender = null; genderMenuExpanded = false })
                listOf(UserGender.MALE, UserGender.FEMALE).forEach { option ->
                    DropdownMenuItem(text = { Text(genderLabel(option.name)) }, onClick = { gender = option.name; genderMenuExpanded = false })
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = birthDate,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.profile_birth_date)) },
            placeholder = { Text(stringResource(R.string.profile_birth_date_hint)) },
            supportingText = { Text(stringResource(R.string.profile_birth_date_hint)) },
            isError = birthDateError,
            singleLine = true,
            trailingIcon = {
                TextButton(onClick = { datePickerVisible = true }) {
                    Text(stringResource(R.string.profile_choose_date))
                }
            },
        )
        if (datePickerVisible) {
            DatePickerDialog(
                onDismissRequest = { datePickerVisible = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            birthDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                            birthDateError = false
                        } ?: run { birthDate = "" }
                        datePickerVisible = false
                    }) { Text(stringResource(R.string.action_done)) }
                },
                dismissButton = {
                    TextButton(onClick = { datePickerVisible = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            ) {
                DatePicker(state = datePickerState)
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = country, onValueChange = { country = it }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.profile_country)) }, singleLine = true)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = city, onValueChange = { city = it }, modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.profile_city)) }, singleLine = true)
        if (state.saveError) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.profile_save_error), color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                val normalizedBirthDate = birthDate.trim()
                val validBirthDate = normalizedBirthDate.isEmpty() || runCatching {
                    val parsed = LocalDate.parse(normalizedBirthDate)
                    !parsed.isAfter(LocalDate.now())
                }.getOrDefault(false)
                if (!validBirthDate) {
                    birthDateError = true
                } else {
                    viewModel.saveProfile(
                        displayName = displayName,
                        gender = gender,
                        birthDate = normalizedBirthDate,
                        country = country,
                        city = city,
                        onSaved = onBack,
                    )
                }
            },
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.isSaving) stringResource(R.string.status_saving) else stringResource(R.string.action_save))
        }
    }
}

@Composable
private fun genderLabel(value: String): String = when (value) {
    UserGender.FEMALE.name -> stringResource(R.string.profile_gender_female)
    UserGender.MALE.name -> stringResource(R.string.profile_gender_male)
    else -> stringResource(R.string.profile_gender_not_set)
}
