# Todo

A personal todo app: an Android client (Kotlin/Compose) and a web client (React),
synced through Firebase Firestore, with no login screen — devices are linked into
a shared sync group via a pairing code instead of accounts.

Canonical data schema: `/docs/data-model.md`.

## Status

All planned build phases are done — the app is feature-complete against the
original spec:

- [x] Phase 0 — repo scaffolding
- [x] Phase 1 — core task CRUD (Android + web)
- [x] Phase 2 — offline-first persistence
- [x] Phase 3 — device pairing
- [x] Phase 4 — reminders/notifications
- [x] Phase 5 — whimsical polish

What's left is entirely on you (none of it can be done from an unattended
session): create a real Firebase project and try running both apps. See
**Firebase setup** and **Running it** below.

Neither client has been run end-to-end against a live backend yet. The web
app's build/lint/tests have all been verified in CI-like conditions; the
Android app has been written and reviewed carefully but never compiled (no
Android SDK was available while building it) — opening it in Android Studio
is the first real compile it will get.

## Structure

```
/android    Kotlin/Compose Android app
/web        React/TypeScript web app
/firebase   Firestore rules/indexes + Cloud Functions
/docs       Canonical data model
```

## Firebase setup (required before either app will actually sync)

This repo ships with **placeholder** Firebase config (`android/app/google-services.json`,
`web/src/lib/firebaseConfig.ts`) so the code compiles/builds out of the box. To make
sync actually work against a real project:

1. `firebase login` (needs your Google account, run this yourself — not possible
   from an unattended session).
2. Create a Firebase project (console.firebase.google.com or `firebase projects:create`).
3. In the console, enable **Firestore** (production mode) and enable the
   **Anonymous** sign-in provider under Authentication.
4. Register an Android app with package name `com.wikzo.todo`, download the real
   `google-services.json`, and replace `android/app/google-services.json`.
5. Register a Web app, copy its config object into `web/src/lib/firebaseConfig.ts`,
   replacing the placeholder values.
6. From `/firebase`: `firebase use --add` (select your project), then
   `firebase deploy --only firestore:rules,firestore:indexes,functions`.
7. In the console, under Firestore, add a TTL policy on the `pairingCodes`
   collection group's `expiresAt` field (optional but recommended — pairing
   codes are already checked as expired in code regardless, this just cleans
   up stale documents automatically).

## Running it

- **Android**: open `/android` in Android Studio. It has a Gradle wrapper
  checked in (`./gradlew`), so it should sync without extra setup once you
  have the Android SDK Studio manages. `./gradlew test` runs the unit tests
  under `app/src/test`.
- **Web**: `cd web && npm install && npm run dev`. `npm run build` does a
  production build, `npm test` runs the unit tests, `npm run lint` runs
  oxlint.
- Both apps are unusable for real sync until you've done the Firebase setup
  above — until then they'll each just spin up their own local, unpaired
  sync group against placeholder config and fail to actually reach Firestore.
