import { useEffect, useState } from "react";
import { QRCodeSVG } from "qrcode.react";
import { createPairingCode } from "../../lib/syncGroup";

interface PairingCodeDisplayProps {
  groupId: string;
}

interface ActiveCode {
  code: string;
  expiresAtMillis: number;
}

function formatRemaining(ms: number): string {
  const totalSeconds = Math.max(0, Math.ceil(ms / 1000));
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${String(seconds).padStart(2, "0")}`;
}

/**
 * "Show my code" panel: lets this device generate a short-lived pairing
 * code (plus QR code) that another device can use, via `claimPairingCode`,
 * to join this device's sync group.
 */
export function PairingCodeDisplay({ groupId }: PairingCodeDisplayProps) {
  const [activeCode, setActiveCode] = useState<ActiveCode | null>(null);
  const [generating, setGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    if (!activeCode) return;
    const interval = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(interval);
  }, [activeCode]);

  const remainingMs = activeCode ? activeCode.expiresAtMillis - now : 0;
  const expired = Boolean(activeCode) && remainingMs <= 0;

  const handleGenerate = async () => {
    setGenerating(true);
    setError(null);
    try {
      const result = await createPairingCode(groupId);
      setNow(Date.now());
      setActiveCode(result);
    } catch (err) {
      console.error("Failed to create pairing code", err);
      setError("Couldn't generate a code. Try again.");
    } finally {
      setGenerating(false);
    }
  };

  return (
    <div className="flex flex-col items-center gap-4 text-center">
      <p className="text-sm text-slate-500">
        Generate a code and scan it (or type it in) on your other device to
        link it to this account.
      </p>

      {activeCode && !expired && (
        <div className="flex flex-col items-center gap-3">
          <div className="rounded-xl border border-slate-200 bg-white p-3 shadow-sm">
            <QRCodeSVG value={activeCode.code} size={160} />
          </div>
          <p className="font-mono text-3xl font-semibold tracking-[0.3em] text-slate-900">
            {activeCode.code}
          </p>
          <p className="text-xs text-slate-400">
            Expires in {formatRemaining(remainingMs)}
          </p>
        </div>
      )}

      {activeCode && expired && (
        <p className="text-sm text-slate-500">This code has expired.</p>
      )}

      {error && <p className="text-sm text-rose-600">{error}</p>}

      <button
        type="button"
        onClick={handleGenerate}
        disabled={generating}
        className="inline-flex items-center gap-1.5 rounded-lg bg-accent-600 px-3.5 py-2 text-sm font-medium text-white shadow-sm transition hover:bg-accent-700 disabled:opacity-60"
      >
        {generating
          ? "Generating…"
          : activeCode
            ? "Generate a new code"
            : "Generate a code"}
      </button>
    </div>
  );
}
