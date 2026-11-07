package com.jaroapps.dailybrain.feature.profile.data.auth

import com.jaroapps.dailybrain.feature.profile.domain.model.GoogleAccountProfile
import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseAuthenticationRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseGoogleSignInResult
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileFirebaseUidMismatchException
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileGoogleAccountMismatchException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class ActiveProfileAuthCoordinatorTest {

    @Test
    fun matchingFirebaseSessionDoesNotPromptForGoogleSignIn() = runBlocking {
        val provider = FakeGoogleSignInProvider(googleResult("google-a", "token-a"))
        val firebase = FakeFirebaseAuthenticationRepository(currentUid = "firebase-a")
        val coordinator = ActiveProfileAuthCoordinator(provider, firebase)

        coordinator.reauthenticate(linkedProfile())

        assertEquals(0, provider.calls)
        assertEquals(0, firebase.signInCalls)
        assertEquals(0, firebase.signOutCalls)
    }

    @Test
    fun mismatchedSessionIsRestoredUsingTheLinkedGoogleSubject() = runBlocking {
        val provider = FakeGoogleSignInProvider(googleResult("google-a", "token-a"))
        val firebase = FakeFirebaseAuthenticationRepository(
            currentUid = "firebase-b",
            signInResult = FirebaseGoogleSignInResult.Success("firebase-a"),
        )
        val coordinator = ActiveProfileAuthCoordinator(provider, firebase)

        coordinator.ensureAuthenticated(linkedProfile())

        assertEquals(1, provider.calls)
        assertEquals("token-a", firebase.lastToken)
        assertEquals("google-a", firebase.lastExpectedSubject)
        assertEquals("firebase-a", firebase.currentUserId())
        assertEquals(0, firebase.signOutCalls)
    }

    @Test
    fun selectingTheWrongGoogleAccountDoesNotChangeTheFirebaseSession() = runBlocking {
        val provider = FakeGoogleSignInProvider(googleResult("google-b", "token-b"))
        val firebase = FakeFirebaseAuthenticationRepository(currentUid = "firebase-b")
        val coordinator = ActiveProfileAuthCoordinator(provider, firebase)

        assertThrows(ProfileGoogleAccountMismatchException::class.java) {
            runBlocking { coordinator.ensureAuthenticated(linkedProfile()) }
        }

        assertEquals(0, firebase.signInCalls)
        assertEquals("firebase-b", firebase.currentUserId())
    }

    @Test
    fun unexpectedFirebaseUidIsSignedOutAndRejected() = runBlocking {
        val provider = FakeGoogleSignInProvider(googleResult("google-a", "token-a"))
        val firebase = FakeFirebaseAuthenticationRepository(
            currentUid = "firebase-b",
            signInResult = FirebaseGoogleSignInResult.Success("unexpected-uid"),
        )
        val coordinator = ActiveProfileAuthCoordinator(provider, firebase)

        assertThrows(ProfileFirebaseUidMismatchException::class.java) {
            runBlocking { coordinator.ensureAuthenticated(linkedProfile()) }
        }

        assertNull(firebase.currentUserId())
        assertEquals(1, firebase.signOutCalls)
    }

    @Test
    fun unlinkedLocalProfileIsRejectedWithoutChangingFirebaseSession() = runBlocking {
        val provider = FakeGoogleSignInProvider(googleResult("google-a", "token-a"))
        val firebase = FakeFirebaseAuthenticationRepository(currentUid = "firebase-b")
        val coordinator = ActiveProfileAuthCoordinator(provider, firebase)

        assertThrows(com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileGoogleAccountNotLinkedException::class.java) {
            runBlocking { coordinator.reauthenticate(linkedProfile(firebaseUid = null, googleSubject = null)) }
        }

        assertEquals(0, provider.calls)
        assertEquals("firebase-b", firebase.currentUserId())
        assertEquals(0, firebase.signOutCalls)
    }

    private fun linkedProfile(
        firebaseUid: String? = "firebase-a",
        googleSubject: String? = "google-a",
    ) = UserProfile(
        localAccountId = "local-a",
        firebaseUid = firebaseUid,
        generatedAlias = "brain#123",
        createdAtEpochMs = 1L,
        updatedAtEpochMs = 1L,
        googleSubject = googleSubject,
    )

    private class FakeGoogleSignInProvider(
        private val result: GoogleSignInResult,
    ) : GoogleSignInProvider {
        var calls = 0
            private set

        override suspend fun signIn(): GoogleSignInResult {
            calls++
            return result
        }
    }

    private class FakeFirebaseAuthenticationRepository(
        private var currentUid: String?,
        private val signInResult: FirebaseGoogleSignInResult =
            FirebaseGoogleSignInResult.Success("firebase-a"),
    ) : FirebaseAuthenticationRepository {
        var signInCalls = 0
            private set
        var signOutCalls = 0
            private set
        var lastToken: String? = null
            private set
        var lastExpectedSubject: String? = null
            private set

        override fun currentUserId(): String? = currentUid
        override fun observeCurrentUserId(): Flow<String?> = flowOf(currentUid)

        override suspend fun signInWithGoogle(
            idToken: String,
            expectedGoogleSubject: String,
        ): FirebaseGoogleSignInResult {
            signInCalls++
            lastToken = idToken
            lastExpectedSubject = expectedGoogleSubject
            if (signInResult is FirebaseGoogleSignInResult.Success) {
                currentUid = signInResult.firebaseUid
            }
            return signInResult
        }

        override fun signOut() {
            signOutCalls++
            currentUid = null
        }
    }

    private companion object {
        fun googleResult(subject: String, token: String) = GoogleSignInResult(
            account = GoogleAccountProfile(
                subject = subject,
                email = "$subject@example.com",
                displayName = subject,
                photoUrl = null,
            ),
            idToken = token,
        )
    }
}
