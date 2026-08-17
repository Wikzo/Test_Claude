import { useState, type FormEvent } from "react";
import { claimPairingCode } from "../../lib/syncGroup";

interface PairDeviceProps {
  /** Called once this device has successfully joined a new group. */
  onPaired: () => void;
}

const CODE_LENGTH = 6;

function describeError(error: unknown): string {
  const code = (error as { code?: string } | null)?.code;
  switch (code) {
    case "functions/not-found":
      return "That code doesn't match any device. Double-check it and try again.";
    case "functions/failed-precondition":
      return "That code has already been used. Generate a new one on the other device.";
    case "functions/deadline-exceeded":
      return "That code has expired. Generate a new one on the other device.";
    default:
      return "Couldn't join that device. Try again.";
  }
}

/**
 * "Enter a code" panel: lets this device join another device's sync group
 * by claiming a pairing code generated there.
 */
export function PairDevice({ onPaired }: PairDeviceProps) {
  const [code, setCode] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [paired, setPaired] = useState(false);

  const handleCodeChange = (value: string) => {
    const digitsOnly = value.replace(/\D/g, "").slice(0, CODE_LENGTH);
    setCode(digitsOnly);
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (code.length !== CODE_LENGTH) {
      setError(`Enter the ${CODE_LENGTH}-digit code shown on your other device.`);
      return;
    }

    setSubmitting(true);
    setError(null);

    try {
      await claimPairingCode(code);
      setPaired(true);
      onPaired();
    } catch (err) {
      console.error("Failed to claim pairing code", err);
      setError(describeError(err));
    } finally {
      setSubmitting(false);
    }
  };

  if (paired) {
    return (
      <div className="flex flex-col items-center gap-2 py-4 text-center">
        <p className="text-sm font-medium text-slate-800">Device linked</p>
        <p className="text-xs text-slate-500">
          This device is now synced with the other one.
        </p>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col items-center gap-4">
      <p className="text-center text-sm text-slate-500">
        Enter the {CODE_LENGTH}-digit code shown on the device you want to
        link.
      </p>

      <input
        type="text"
        inputMode="numeric"
        autoComplete="one-time-code"
        pattern="[0-9]*"
        value={code}
        onChange={(e) => handleCodeChange(e.target.value)}
        placeholder="••••••"
        autoFocus
        className="w-full max-w-[220px] rounded-lg border border-slate-300 px-3 py-2 text-center font-mono text-2xl tracking-[0.3em] text-slate-800 outline-none focus:border-accent-500 focus:ring-2 focus:ring-accent-100"
      />

      {error && <p className="text-center text-sm text-rose-600">{error}</p>}

      <button
        type="submit"
        disabled={submitting || code.length !== CODE_LENGTH}
        className="inline-flex items-center gap-1.5 rounded-lg bg-accent-600 px-3.5 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-accent-700 disabled:opacity-60"
      >
        {submitting ? "Joining…" : "Join"}
      </button>
    </form>
  );
}
