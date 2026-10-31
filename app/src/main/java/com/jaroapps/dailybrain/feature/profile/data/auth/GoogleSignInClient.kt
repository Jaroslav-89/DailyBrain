package com.jaroapps.dailybrain.feature.profile.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.feature.profile.domain.model.GoogleAccountProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

class GoogleSignInConfigurationException(message: String) : IllegalStateException(message)

data class GoogleSignInResult(
    val account: GoogleAccountProfile,
    /** Kept in memory only; never persisted in Room or logged. */
    val idToken: String,
)

/**
 * Android Google Credential Manager entry point.
 * MainActivity owns the Activity lifecycle reference, following FunnyMail's pattern.
 */
class GoogleSignInClient(
    private val context: Context,
) {
    @Volatile
    private var activityReference: WeakReference<Activity>? = null

    fun attachActivity(activity: Activity?) {
        activityReference = activity?.let(::WeakReference)
    }

    suspend fun signIn(): GoogleSignInResult = withContext(Dispatchers.Main.immediate) {
        val activity = activityReference?.get()
            ?: throw IllegalStateException("Не удалось открыть Google Sign-In. Перезапустите экран приложения.")

        val generatedClientIdResource = activity.resources.getIdentifier(
            "default_web_client_id",
            "string",
            activity.packageName,
        )
        val webClientId = if (generatedClientIdResource != 0) {
            activity.getString(generatedClientIdResource).trim()
        } else {
            context.getString(R.string.google_web_client_id).trim()
        }
        if (webClientId.isBlank() || webClientId.startsWith("YOUR_")) {
            throw GoogleSignInConfigurationException(
                "Укажите Web OAuth Client ID и добавьте app/google-services.json по инструкции GOOGLE_SIGN_IN_SETUP.md.",
            )
        }

        val option = GetSignInWithGoogleOption.Builder(webClientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        val response = try {
            CredentialManager.create(activity).getCredential(
                context = activity,
                request = request,
            )
        } catch (error: GetCredentialException) {
            Log.e(LOG_TAG, "Google Credential Manager failed: " + error.javaClass.simpleName, error)
            throw error
        }

        val credential = response.credential as? CustomCredential
            ?: throw IllegalStateException("Google не вернул ID-токен.")
        if (credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw IllegalStateException("Получен неподдерживаемый тип Google-учётных данных.")
        }

        val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val idToken = googleCredential.idToken
        val subject = googleCredential.uniqueId.trim()
        if (idToken.isBlank() || subject.isBlank()) {
            throw IllegalStateException("Google не вернул необходимые данные аккаунта.")
        }

        GoogleSignInResult(
            account = GoogleAccountProfile(
                subject = subject,
                email = googleCredential.email?.takeIf { it.isNotBlank() },
                displayName = googleCredential.displayName?.takeIf { it.isNotBlank() },
                photoUrl = googleCredential.profilePictureUri?.toString(),
            ),
            idToken = idToken,
        )
    }

    private companion object {
        const val LOG_TAG = "DailyBrainGoogleAuth"
    }
}
