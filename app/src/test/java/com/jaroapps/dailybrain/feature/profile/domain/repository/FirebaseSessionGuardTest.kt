package com.jaroapps.dailybrain.feature.profile.domain.repository

import org.junit.Assert.assertThrows
import org.junit.Test

class FirebaseSessionGuardTest {

    @Test
    fun matchingFirebaseUidIsAllowed() {
        requireMatchingFirebaseSession(expectedUid = "firebase-a", currentUid = "firebase-a")
    }

    @Test
    fun missingOrDifferentFirebaseUidIsRejected() {
        assertThrows(FirebaseSessionMismatchException::class.java) {
            requireMatchingFirebaseSession(expectedUid = "firebase-a", currentUid = null)
        }
        assertThrows(FirebaseSessionMismatchException::class.java) {
            requireMatchingFirebaseSession(expectedUid = "firebase-a", currentUid = "firebase-b")
        }
    }
}
