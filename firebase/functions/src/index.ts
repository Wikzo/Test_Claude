// Cloud Functions entry point for the Todo app's device pairing flow.
// See /docs/data-model.md for the Firestore schema these functions read
// and write, and /firebase/firestore.rules for why pairing must go through
// server code (the Admin SDK) rather than direct client writes.

export { createPairingCode } from "./createPairingCode";
export { claimPairingCode } from "./claimPairingCode";
