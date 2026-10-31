# Google Sign-In and Firebase Authentication setup

DailyBrain uses the Android authentication sequence from FunnyMail:

1. Android Credential Manager displays the explicit Continue with Google flow.
2. Google returns an ID token. DailyBrain keeps it in memory only and exchanges it with Firebase Authentication using GoogleAuthProvider.
3. Firebase validates the Google credential and returns a Firebase UID. Firebase Authentication persists its own session.
4. DailyBrain saves the Google OIDC subject, Firebase UID, and optional display metadata in the currently active local Room profile.

## Firebase Console configuration

1. Create or select a Firebase project.
2. Register an Android app with package name com.jaroapps.dailybrain.
3. Add the SHA-1 fingerprint for each signing certificate used to install the app (debug and release).
4. In Firebase Authentication, enable the Google provider.
5. Download google-services.json and place it at app/google-services.json.
6. In the same Google Cloud / Firebase project, make sure a Web application OAuth client exists. Credential Manager uses this Web client ID as its server client ID.
7. Sync and rebuild the Android project.

The Google Services Gradle plugin is applied automatically only when app/google-services.json exists. The project can still compile and launch without this file, but Firebase sign-in will report that Firebase is not configured.

The generated default_web_client_id resource is preferred when available. The fallback value in app/src/main/res/values/google_sign_in.xml is used only when the Google Services plugin has not generated that resource.

## Local profiles remain local

- Training history, statistics, onboarding progress, avatar configuration, and profile details remain owned by the selected localAccountId in Room.
- The Google subject is the stable OIDC identity key. A unique Room index prevents the same Google account from being linked to two local profiles on this device.
- The Firebase UID is stored on the same local profile and identifies that profile in future Firebase-backed features.
- A Firebase UID is not used as a replacement for localAccountId. Signing in does not merge profiles, switch the active local profile, or move local training data.
- Email is display metadata only; it is not used as the identity key.
- ID tokens are never written to Room, DataStore, logs, or the repository.

Firebase Authentication keeps a single current-user session for the app installation. The local profiles remain independent; future Firebase-backed operations must confirm that FirebaseAuth.currentUser.uid matches the active local profile's firebaseUid before authorizing a request. When switching between local profiles linked to different Google accounts, re-authenticate the selected account before making remote requests.

## Scope

This change adds Firebase Authentication only. Firestore collections, friend search, friend requests, and cloud synchronization are intentionally not created yet. They can be added later while keeping Room as the source of truth for local profile and training data.
