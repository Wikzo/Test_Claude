import { onCall, HttpsError } from "firebase-functions/v2/https";
import { FieldValue } from "firebase-admin/firestore";
import { db } from "./firebaseAdmin";
import { PairingCodeDoc } from "./pairing";

type Platform = "android" | "web";

interface ClaimPairingCodeRequest {
  code: string;
  platform: Platform;
  deviceName: string;
}

interface ClaimPairingCodeResponse {
  groupId: string;
}

/**
 * Claims a pairing code minted by `createPairingCode`, adding the calling
 * device as a member of the code's sync group and marking the code used.
 *
 * Validity (existence, `used`, `expiresAt`) is checked once up front for a
 * fast, friendly error, and then re-checked inside a transaction so two
 * devices racing to claim the same code can't both succeed -- the loser
 * gets the same "already used" error the second check would have produced
 * anyway.
 */
export const claimPairingCode = onCall<ClaimPairingCodeRequest, Promise<ClaimPairingCodeResponse>>(
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "You must be signed in to claim a pairing code.");
    }
    const uid = request.auth.uid;

    const { code, platform, deviceName } = request.data ?? ({} as ClaimPairingCodeRequest);
    if (typeof code !== "string" || code.length === 0) {
      throw new HttpsError("invalid-argument", "code is required.");
    }
    if (platform !== "android" && platform !== "web") {
      throw new HttpsError("invalid-argument", 'platform must be "android" or "web".');
    }
    if (typeof deviceName !== "string" || deviceName.length === 0) {
      throw new HttpsError("invalid-argument", "deviceName is required.");
    }

    const codeRef = db.doc(`pairingCodes/${code}`);

    // Up-front check: cheap, and gives a clean error before we even open a
    // transaction for the common (non-racing) case.
    const initialSnap = await codeRef.get();
    if (!initialSnap.exists) {
      throw new HttpsError("not-found", "Invalid pairing code");
    }
    const initialData = initialSnap.data() as PairingCodeDoc;
    if (initialData.used) {
      throw new HttpsError("failed-precondition", "This pairing code has already been used");
    }
    if (initialData.expiresAt.toMillis() <= Date.now()) {
      throw new HttpsError("deadline-exceeded", "This pairing code has expired");
    }

    const groupId = await db.runTransaction(async (tx) => {
      const snap = await tx.get(codeRef);
      if (!snap.exists) {
        throw new HttpsError("not-found", "Invalid pairing code");
      }
      const data = snap.data() as PairingCodeDoc;

      // Re-check inside the transaction to close the race between two
      // simultaneous claims of the same code -- the loser lands here.
      if (data.used) {
        throw new HttpsError("failed-precondition", "This pairing code has already been used");
      }
      if (data.expiresAt.toMillis() <= Date.now()) {
        throw new HttpsError("deadline-exceeded", "This pairing code has expired");
      }

      const memberRef = db.doc(`syncGroups/${data.groupId}/members/${uid}`);
      tx.set(memberRef, {
        uid,
        platform,
        deviceName,
        joinedAt: FieldValue.serverTimestamp(),
        lastSeenAt: FieldValue.serverTimestamp(),
      });
      tx.update(codeRef, {
        used: true,
        usedByUid: uid,
      });

      return data.groupId;
    });

    return { groupId };
  }
);
