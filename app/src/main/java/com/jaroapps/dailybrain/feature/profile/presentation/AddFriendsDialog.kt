package com.jaroapps.dailybrain.feature.profile.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.jaroapps.dailybrain.feature.profile.data.auth.GoogleSignInClient
import com.jaroapps.dailybrain.feature.profile.data.auth.GoogleSignInConfigurationException
import com.jaroapps.dailybrain.feature.profile.domain.model.InviteCodePolicy
import com.jaroapps.dailybrain.feature.profile.domain.model.ProfileCloudSessionStatus
import com.jaroapps.dailybrain.feature.profile.domain.model.UsernamePolicy
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun AddFriendsDialog(
    profileState: ProfileUiState,
    directoryState: UserDirectoryUiState,
    profileViewModel: ProfileViewModel,
    directoryViewModel: UserDirectoryViewModel,
    onOpenUserProfile: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var scanError by remember { mutableStateOf<String?>(null) }
    var googleSignInError by remember { mutableStateOf<String?>(null) }
    var isGoogleSignInInProgress by remember { mutableStateOf(false) }
    val googleSignInClient: GoogleSignInClient = koinInject()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val scanner = remember(context) {
        GmsBarcodeScanning.getClient(
            context,
            GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build(),
        )
    }
    val activeProfile = profileState.profile ?: directoryState.profile
    val cloudSessionStatus = if (profileState.profile != null) {
        profileState.cloudSessionStatus
    } else {
        directoryState.cloudSessionStatus
    }
    val cloudSessionReady = cloudSessionStatus == ProfileCloudSessionStatus.READY
    val requiresReauthentication = cloudSessionStatus == ProfileCloudSessionStatus.REAUTH_REQUIRED

    fun connectGoogle() {
        coroutineScope.launch {
            isGoogleSignInInProgress = true
            googleSignInError = null
            profileViewModel.clearGoogleLinkError()
            try {
                val result = googleSignInClient.signIn()
                val expectedSubject = activeProfile?.googleSubject
                if (expectedSubject != null && result.account.subject != expectedSubject) {
                    googleSignInError = "Выберите тот Google-аккаунт, который уже привязан к этому профилю."
                } else {
                    profileViewModel.linkGoogleAccount(result)
                }
            } catch (error: Exception) {
                googleSignInError = if (error is GoogleSignInConfigurationException) {
                    error.message
                } else "Не удалось выполнить вход с Google. Попробуйте ещё раз."
            } finally {
                isGoogleSignInInProgress = false
            }
        }
    }

    fun search() {
        if (selectedTab == 0) directoryViewModel.searchUserByUsername(UsernamePolicy.sanitizeInput(query))
        else directoryViewModel.searchUserByInviteCode(InviteCodePolicy.sanitizeInput(query))
    }

    fun scanQrCode() {
        scanError = null
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val code = InviteCodePolicy.fromQrPayload(barcode.rawValue.orEmpty())
                if (code == null) {
                    scanError = "В QR-коде нет действительного кода приглашения DailyBrain."
                } else {
                    selectedTab = 1
                    query = code
                    directoryViewModel.searchUserByInviteCode(code)
                }
            }
            .addOnFailureListener {
                scanError = "Не удалось открыть сканер QR-кода. Попробуйте ещё раз."
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить друзей") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!cloudSessionReady) {
                    Text(
                        if (requiresReauthentication) {
                            "Этот профиль уже связан с Google, но сейчас активно другое подключение. Войдите в привязанный аккаунт, чтобы использовать облачные функции."
                        } else {
                            "Чтобы искать пользователей и подписываться на них, сначала подключите Google-аккаунт."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    googleSignInError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    profileState.googleReauthenticationError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                    when (profileState.googleLinkError) {
                        GoogleLinkError.ALREADY_LINKED_TO_ANOTHER_PROFILE -> Text(
                            "Этот Google-аккаунт уже привязан к другому профилю на устройстве.",
                            color = MaterialTheme.colorScheme.error,
                        )
                        GoogleLinkError.FIREBASE_NOT_CONFIGURED -> Text(
                            "Firebase не настроен. Добавьте app/google-services.json и включите Google в Firebase Authentication.",
                            color = MaterialTheme.colorScheme.error,
                        )
                        GoogleLinkError.FIREBASE_AUTH_FAILED -> Text(
                            firebaseDiagnosticDescription(profileState.googleLinkDiagnostic),
                            color = MaterialTheme.colorScheme.error,
                        )
                        GoogleLinkError.SAVE_FAILED -> Text(
                            "Не удалось сохранить привязку. Попробуйте ещё раз.",
                            color = MaterialTheme.colorScheme.error,
                        )
                        null -> Unit
                    }
                    Button(
                        onClick = {
                            if (requiresReauthentication) profileViewModel.reauthenticateGoogleAccount()
                            else connectGoogle()
                        },
                        enabled = if (requiresReauthentication) {
                            !profileState.isGoogleReauthenticating
                        } else !isGoogleSignInInProgress && !profileState.isGoogleLinking,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (profileState.isGoogleReauthenticating ||
                            isGoogleSignInInProgress || profileState.isGoogleLinking
                        ) CircularProgressIndicator()
                        else Text(if (requiresReauthentication) "Войти в привязанный Google" else "Продолжить с Google")
                    }
                } else {
                    Text(
                        "Найдите человека по уникальному нику, коду приглашения или отсканируйте QR-код.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0; query = ""; directoryViewModel.resetSearch() },
                            text = { Text("По нику") },
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1; query = ""; directoryViewModel.resetSearch() },
                            text = { Text("По коду") },
                        )
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = {
                            query = if (selectedTab == 0) UsernamePolicy.sanitizeInput(it)
                            else InviteCodePolicy.sanitizeInput(it)
                            directoryViewModel.resetSearch()
                            scanError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (selectedTab == 0) "Уникальный ник" else "Код приглашения") },
                        leadingIcon = { if (selectedTab == 0) Text("@") },
                        singleLine = true,
                    )
                    Button(
                        onClick = ::search,
                        enabled = !directoryState.isSearching && if (selectedTab == 0) {
                            UsernamePolicy.isValid(query)
                        } else InviteCodePolicy.isValid(query),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (directoryState.isSearching) "Ищем..." else "Найти пользователя")
                    }
                    OutlinedButton(onClick = ::scanQrCode, modifier = Modifier.fillMaxWidth()) {
                        Text("Отсканировать QR-код")
                    }
                    scanError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    directoryState.searchError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (directoryState.searchHasRun && directoryState.searchResult == null) {
                        Text("Пользователь не найден.", style = MaterialTheme.typography.bodyMedium)
                    }
                    directoryState.searchResult?.let { user ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                directoryViewModel.resetSearch()
                                onDismiss()
                                onOpenUserProfile(user.firebaseUid)
                            },
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    user.displayName?.takeIf(String::isNotBlank)
                                        ?: "Пользователь DailyBrain",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                if (user.username.isNotBlank()) {
                                    Text("@${user.username}", color = MaterialTheme.colorScheme.primary)
                                }
                                when {
                                    user.firebaseUid == activeProfile?.firebaseUid -> Text("Это ваш профиль.")
                                    directoryState.isFollowingSearchResult -> Text(
                                        "Вы подписаны на этого пользователя.",
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    else -> Button(
                                        onClick = directoryViewModel::followSearchResult,
                                        enabled = !directoryState.isFollowingAction,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(if (directoryState.isFollowingAction) "Подписываемся..." else "Подписаться")
                                    }
                                }
                                directoryState.followError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { directoryViewModel.resetSearch(); onDismiss() }) { Text("Закрыть") }
        },
    )
}

internal fun firebaseDiagnosticDescription(code: String?): String = when (code) {
    "ERROR_OPERATION_NOT_ALLOWED" -> "В Firebase Authentication отключён вход через Google. Включите Google provider в Sign-in method."
    "ERROR_INVALID_CREDENTIAL", "GOOGLE_ID_TOKEN_SUBJECT_MISSING" -> "Firebase отклонил Google ID token. Проверьте Web OAuth Client ID и обновите google-services.json."
    "ERROR_APP_NOT_AUTHORIZED" -> "Firebase не авторизовал это Android-приложение. Проверьте package name и SHA-1 сертификата."
    "ERROR_INVALID_API_KEY", "CONFIGURATION_NOT_FOUND" -> "Похоже, приложение подключено к неверному Firebase-проекту или использует устаревший google-services.json."
    "ERROR_NETWORK_REQUEST_FAILED" -> "Не удалось связаться с Firebase. Проверьте интернет-соединение и повторите попытку."
    "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> "В Firebase уже существует аккаунт с этим email и другим способом входа. Проверьте способы входа в Authentication."
    "GOOGLE_SUBJECT_MISMATCH" -> "Google ID token и выбранный Google-аккаунт не совпали. Проверьте Web OAuth Client ID и повторите вход."
    "GOOGLE_TOKEN_OR_SUBJECT_EMPTY" -> "Google не вернул обязательные данные для проверки аккаунта. Повторите вход."
    "LOCAL_PROFILE_LINK_FAILED" -> "Firebase авторизовал аккаунт, но не удалось сохранить привязку к локальному профилю."
    else -> "Firebase вернул ошибку авторизации. Повторите вход."
}
