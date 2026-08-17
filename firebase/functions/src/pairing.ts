import { Timestamp } from "firebase-admin/firestore";

/** How long a freshly-minted pairing code stays claimable. */
export const PAIRING_CODE_TTL_MILLIS = 10 * 60 * 1000; // 10 minutes

/** How many times to retry generating a code if we collide with a live one. */
export const MAX_CODE_GENERATION_ATTEMPTS = 5;

/** Shape of a `pairingCodes/{code}` document, as written by createPairingCode. */
export interface PairingCodeDoc {
  groupId: string;
  createdByUid: string;
  createdAt: Timestamp;
  expiresAt: Timestamp;
  used: boolean;
  usedByUid: string | null;
}

/**
 * Generates a 6-digit numeric pairing code, zero-padded (e.g. "048213").
 * Uses Math.random rather than a CSPRNG deliberately -- these codes are
 * short-lived, single-use, and rate-limited by nothing more than human
 * typing speed, so cryptographic unpredictability isn't the goal here.
 */
export function generatePairingCode(): string {
  const n = Math.floor(Math.random() * 1_000_000);
  return n.toString().padStart(6, "0");
}

/**
 * Returns true if a pairing code document is still usable, i.e. it hasn't
 * been claimed yet and its expiry timestamp hasn't passed.
 */
export function isPairingCodeLive(data: PairingCodeDoc, now: Timestamp): boolean {
  return !data.used && data.expiresAt.toMillis() > now.toMillis();
}
