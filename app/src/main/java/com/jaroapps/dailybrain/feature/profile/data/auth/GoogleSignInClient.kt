package com.jaroapps.dailybrain.feature.profile.data.auth

import android.content.Context
import android.util.Base64
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.feature.profile.domain.model.GoogleAccountProfile
import java.security.SecureRandom

class GoogleSignInConfigurationException(message: String) : IllegalStateException(message)

class GoogleSignInClient(
    private val context: Context,
) {
    suspend fun signIn(activityContext: Context): GoogleAccountProfile {
        val webClientId = context.getString(R.string.google_web_client_id).trim()
        if (webClientId.isBlank() || webClientId.startsWith("YOUR_")) {
            throw GoogleSignInConfigurationException(
                "Сначала укажите Web OAuth Client ID в res/values/google_sign_in.xml.",
            )
        }

        val option = GetSignInWithGoogleOption.Builder(webClientId)
            .setNonce(createNonce())
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val response = CredentialManager.create(activityContext)
            .getCredential(activityContext, request)
        val credential = response.credential as? CustomCredential
            ?: throw IllegalStateException("Google не вернул ID-токен.")

        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw IllegalStateException("Получен неподдерживаемый тип Google-учётных данных.")
        }

        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val subject = googleCredential.uniqueId.trim()
        if (subject.isBlank()) {
            throw IllegalStateException("Google не вернул уникальный идентификатор аккаунта.")
        }

        return GoogleAccountProfile(
            subject = subject,
            email = googleCredential.email.takeIf { it.isNotBlank() },
            displayName = googleCredential.displayName?.takeIf { it.isNotBlank() },
            photoUrl = googleCredential.profilePictureUri?.toString(),
        )
    }

    private fun createNonce(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
