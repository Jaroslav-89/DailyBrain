package com.jaroapps.dailybrain.feature.profile.data.auth

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.GoogleAuthProvider
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseAuthenticationRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseGoogleSignInResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Firebase Auth adapter based on the Google ID-token exchange used by FunnyMail.
 * Diagnostic codes are safe to show in the UI: no token, email or UID is included.
 */
class FirebaseAuthenticationRepositoryImpl(
    context: Context,
) : FirebaseAuthenticationRepository {
    private val applicationContext = context.applicationContext

    override suspend fun signInWithGoogle(
        idToken: String,
        expectedGoogleSubject: String,
    ): FirebaseGoogleSignInResult {
        if (idToken.isBlank() || expectedGoogleSubject.isBlank()) {
            return FirebaseGoogleSignInResult.Failed("GOOGLE_TOKEN_OR_SUBJECT_EMPTY")
        }

        // This comparison is only a consistency check; Firebase performs trusted token validation.
        val tokenSubject = subjectFromIdToken(idToken)
            ?: return FirebaseGoogleSignInResult.Failed("GOOGLE_ID_TOKEN_SUBJECT_MISSING")
        if (tokenSubject != expectedGoogleSubject) {
            Log.e(LOG_TAG, "Google subject mismatch between Credential Manager identity and ID token.")
            return FirebaseGoogleSignInResult.Failed("GOOGLE_SUBJECT_MISMATCH")
        }

        val auth = firebaseAuthOrNull() ?: return FirebaseGoogleSignInResult.NotConfigured
        return try {
            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).awaitResult()
            val firebaseUser = authResult.user ?: error("Firebase returned no user after Google sign-in.")
            FirebaseGoogleSignInResult.Success(firebaseUser.uid)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            val diagnosticCode = diagnosticCode(error)
            Log.e(LOG_TAG, "Firebase Google authentication failed: $diagnosticCode", error)
            FirebaseGoogleSignInResult.Failed(diagnosticCode)
        }
    }

    override fun currentUserId(): String? = firebaseAuthOrNull()?.currentUser?.uid

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCurrentUserId(): Flow<String?> = callbackFlow {
        val auth = firebaseAuthOrNull()
        if (auth == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = FirebaseAuth.AuthStateListener { currentAuth ->
            trySend(currentAuth.currentUser?.uid)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun deleteCurrentUser(expectedFirebaseUid: String) {
        val auth = firebaseAuthOrNull()
            ?: throw IllegalStateException("Firebase is not configured.")
        val user = auth.currentUser
            ?: throw IllegalStateException("There is no signed-in Firebase user.")
        if (user.uid != expectedFirebaseUid) {
            throw IllegalStateException("The Firebase session does not match the profile being deleted.")
        }
        user.delete().awaitResult()
    }

    override fun signOut() {
        runCatching { firebaseAuthOrNull()?.signOut() }
    }

    private fun diagnosticCode(error: Exception): String = when (error) {
        is FirebaseNetworkException -> "ERROR_NETWORK_REQUEST_FAILED"
        is FirebaseAuthException -> error.errorCode
        else -> error.javaClass.simpleName.takeIf { it.isNotBlank() } ?: "UNKNOWN_FIREBASE_ERROR"
    }

    private fun subjectFromIdToken(idToken: String): String? = runCatching {
        val payload = idToken.split('.').getOrNull(1) ?: return null
        val decoded = Base64.decode(
            payload,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
        JSONObject(String(decoded, Charsets.UTF_8))
            .optString("sub")
            .takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun firebaseAuthOrNull(): FirebaseAuth? = runCatching {
        val firebaseApp = FirebaseApp.getApps(applicationContext).firstOrNull()
        firebaseApp?.let { FirebaseAuth.getInstance(it) }
    }.getOrNull()

    private suspend fun <T> Task<T>.awaitResult(): T =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (!continuation.isActive) return@addOnCompleteListener
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(
                        task.exception ?: IllegalStateException("Firebase authentication failed."),
                    )
                }
            }
        }

    private companion object {
        const val LOG_TAG = "DailyBrainFirebaseAuth"
    }
}
