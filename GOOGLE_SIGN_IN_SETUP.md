# Google Sign-In setup

DailyBrain uses Android Credential Manager with the Google ID library. The selected Google identity is linked to the currently active local profile in the Room database.

## Configure OAuth

1. Open [Google Cloud Console](https://console.cloud.google.com/) and select or create the project used by DailyBrain.
2. Configure the Google Auth Platform consent screen and app details.
3. Create an OAuth client of type **Android** for package name `com.jaroapps.dailybrain`. Add the SHA-1 fingerprints for every certificate used to install the app (debug and release).
4. Create an OAuth client of type **Web application** in the same Google Cloud project.
5. Copy the Web application's client ID into `app/src/main/res/values/google_sign_in.xml`, replacing `YOUR_WEB_CLIENT_ID.apps.googleusercontent.com`.
6. Rebuild and install the app.

Credential Manager's `GetSignInWithGoogleOption` requires the **Web application client ID** as its server client ID. Do not put the Android client ID in that resource.

## Local profile binding

DailyBrain stores the Google OIDC subject (`sub` / `uniqueId`) plus optional email, display name, and photo URL in the active local profile. A unique Room index on `googleSubject` prevents the same Google account from being linked to two local profiles in this app installation. Deleting a local profile also releases its binding.

The email address is display information only; it is not used as the identity key.

## Security boundary

This branch performs the Google Credential Manager sign-in and enforces uniqueness locally on the device. It does not yet send the Google ID token to a backend or verify the token signature server-side. Before using this identity to authorize remote friend requests, social data, or any privileged server operation, add a backend that verifies the ID token (signature, issuer, audience, expiry, and nonce) and enforces account uniqueness centrally. Local database checks alone cannot stop the same Google account being linked on a different device or in a modified app.
