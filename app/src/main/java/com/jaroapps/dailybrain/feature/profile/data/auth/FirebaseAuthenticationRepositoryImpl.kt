package com.jaroapps.dailybrain.feature.profile.data.auth

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseAuthenticationRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseGoogleSignInResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Firebase Auth adapter based on the Google ID-token exchange used by FunnyMail.
 *
 * Firebase configuration is resolved lazily so an unconfigured checkout still builds
 * and launches; authentication reports NotConfigured until google-services.json exists.
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
            return FirebaseGoogleSignInResult.Failed
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
            Log.e(LOG_TAG, "Firebase Google authentication failed.", error)
            FirebaseGoogleSignInResult.Failed
        }
    }

    override fun signOut() {
        runCatching { firebaseAuthOrNull()?.signOut() }
    }

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
