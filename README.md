# Todo

A personal todo app: an Android client (Kotlin/Compose) and a web client (React),
synced through Firebase Firestore, with no login screen — devices are linked into
a shared sync group via a pairing code instead of accounts.

See `/root/.claude/plans/i-d-like-to-make-sequential-knuth.md` (or ask Claude) for
the full implementation plan. Canonical data schema: `/docs/data-model.md`.

## Status

Build is in progress, phase by phase:

- [x] Phase 0 — repo scaffolding
- [x] Phase 1 — core task CRUD (Android + web)
- [x] Phase 2 — offline-first persistence
- [x] Phase 3 — device pairing
- [x] Phase 4 — reminders/notifications
- [x] Phase 5 — whimsical polish

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
   `firebase deploy --only firestore:rules,firestore:indexes`.
7. (Phase 3 onward) `firebase deploy --only functions` once the pairing Cloud
   Functions are in place.
