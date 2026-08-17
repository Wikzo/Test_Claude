import { onCall, HttpsError } from "firebase-functions/v2/https";
import { FieldValue, Timestamp } from "firebase-admin/firestore";
import { db } from "./firebaseAdmin";
import {
  generatePairingCode,
  isPairingCodeLive,
  MAX_CODE_GENERATION_ATTEMPTS,
  PAIRING_CODE_TTL_MILLIS,
  PairingCodeDoc,
} from "./pairing";

interface CreatePairingCodeRequest {
  groupId: string;
}

interface CreatePairingCodeResponse {
  code: string;
  expiresAtMillis: number;
}

/**
 * Mints a short-lived, single-use 6-digit pairing code for `groupId`, so a
 * second device can join this sync group via `claimPairingCode`.
 *
 * The caller must be signed in and already be a member of `groupId` -- this
 * is enforced server-side (via the Admin SDK) even though the SDK itself
 * bypasses Firestore security rules, because otherwise any authenticated
 * caller could mint a join code for a group they don't belong to.
 */
export const createPairingCode = onCall<CreatePairingCodeRequest, Promise<CreatePairingCodeResponse>>(
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "You must be signed in to create a pairing code.");
    }
    const uid = request.auth.uid;

    const groupId = request.data?.groupId;
    if (typeof groupId !== "string" || groupId.length === 0) {
      throw new HttpsError("invalid-argument", "groupId is required.");
    }

    const memberRef = db.doc(`syncGroups/${groupId}/members/${uid}`);
    const memberSnap = await memberRef.get();
    if (!memberSnap.exists) {
      throw new HttpsError("permission-denied", "Not a member of this group");
    }

    const now = Timestamp.now();
    const expiresAt = Timestamp.fromMillis(now.toMillis() + PAIRING_CODE_TTL_MILLIS);

    let code: string | undefined;
    for (let attempt = 0; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
      const candidate = generatePairingCode();
      const candidateRef = db.doc(`pairingCodes/${candidate}`);
      const candidateSnap = await candidateRef.get();

      if (!candidateSnap.exists) {
        code = candidate;
        break;
      }

      const existing = candidateSnap.data() as PairingCodeDoc;
      if (!isPairingCodeLive(existing, now)) {
        // Expired or already-used code occupying this ID -- safe to reuse.
        code = candidate;
        break;
      }
      // Collided with a still-live code; try again.
    }

    if (!code) {
      throw new HttpsError(
        "resource-exhausted",
        "Could not generate a unique pairing code, please try again."
      );
    }

    await db.doc(`pairingCodes/${code}`).set({
      groupId,
      createdByUid: uid,
      createdAt: FieldValue.serverTimestamp(),
      expiresAt,
      used: false,
      usedByUid: null,
    });

    return {
      code,
      expiresAtMillis: expiresAt.toMillis(),
    };
  }
);
