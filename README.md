# DailyBrain

DailyBrain is an Android application built with Kotlin and Jetpack Compose.

## Profile deletion

Profile deletion is a local-first, retryable operation. The same deletion flow is used from **Edit profile** and **Profile selection**.

1. A Room transaction stores a pending deletion record containing the former Firebase UID, Google subject (when available), and cleanup status. The same transaction removes the profile's local training history, statistics, rewards, inventory, received gifts, feed cache, and profile row.
2. When the old Firebase session is available, the client removes cloud data and then deletes that Firebase Authentication user. Firebase may reject the Auth deletion when the login is no longer recent; the pending record is retained in that case.
3. If the device was offline, or Auth deletion required a fresh login, the next explicit Google sign-in checks the pending record **before** creating any cloud profile. The client cleans the old UID, deletes that Firebase Auth user, signs in again with the same in-memory Google credential to obtain a new Firebase UID, and only then creates/binds the new profile.
4. The profile selector is shown after local deletion. Deleting the final local profile is supported; the selector offers profile creation.

The pending deletion state is stored in Room table `pending_profile_deletions` (database version 22). Status `CLOUD_CLEANUP_PENDING` means cloud cleanup still needs to run; `CLOUD_DATA_DELETED` means cloud data has been erased and only Auth retirement/recovery remains. The ID token is never persisted.

### Cloud data policy

The client removes the deleted UID's private/public profile documents, public profile card and collectible subcollection, username and invite-code reservations, authored feed events, received gift inbox items, outgoing block records, and both directions of canonical follow relationships plus their public projections.

**Sent gifts are preserved.** Gift delivery documents under other recipients' inboxes and their received-gift feed events are not deleted, so existing recipients keep the endorphins and history they received. Gifts received by the deleted profile are removed with its inbox.

Firestore document deletion is paginated and batched. A `deletedProfiles/{uid}` tombstone is written last. It allows the app to render **"Профиль удалён"** when someone opens an old profile link and blocks writes from any still-valid token belonging to the deleted UID. The tombstone contains only the UID and deletion timestamp; it is not a replacement public profile.

Firestore has no automatic cascading delete for subcollections, so the client explicitly cleans the collections it owns. Block records created by other users are owned by those users and are not deleted by the deleted account; profile links still resolve to the deleted-profile state through the tombstone.

### Firebase setup required for profile deletion

The updated rules are in `firestore.rules`. They:
- allow either participant to remove a canonical follow and its projection;
- allow a profile owner to delete items from their own received-gift inbox;
- let a relationship participant read their projection document during cleanup;
- allow creation/read of the deleted-profile tombstone; and
- prevent a tombstoned UID from creating or mutating profile, relationship, feed, reservation, block, or gift data.

**Deploy the Firestore rules before releasing the app containing this deletion flow.** For a project using the Firebase CLI, deploy with:

```bash
firebase deploy --only firestore:rules
```

Cloud Functions are not used. Deleting the Firebase Authentication user is performed by the Android client after cloud cleanup and requires a recent sign-in. The user's Google account itself is not deleted.
