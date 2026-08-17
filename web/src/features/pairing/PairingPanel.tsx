import { useState } from "react";
import { PairDevice } from "./PairDevice";
import { PairingCodeDisplay } from "./PairingCodeDisplay";

interface PairingPanelProps {
  groupId: string;
  onPaired: () => void;
  onClose: () => void;
}

type Tab = "show" | "enter";

/**
 * "Link a device" panel: lets the user either show a code for another
 * device to scan/enter, or enter a code shown on another device.
 */
export function PairingPanel({ groupId, onPaired, onClose }: PairingPanelProps) {
  const [tab, setTab] = useState<Tab>("show");

  return (
    <div className="flex flex-col gap-4 rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-semibold text-slate-800">Link a device</h2>
        <button
          type="button"
          onClick={onClose}
          aria-label="Close"
          className="rounded-lg p-1 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
        >
          <svg
            viewBox="0 0 20 20"
            className="h-4 w-4"
            fill="none"
            stroke="currentColor"
            strokeWidth={2}
          >
            <path d="M5 5l10 10M15 5L5 15" strokeLinecap="round" />
          </svg>
        </button>
      </div>

      <div className="flex overflow-hidden rounded-lg border border-slate-300">
        <button
          type="button"
          onClick={() => setTab("show")}
          className={`flex-1 border-r border-slate-300 px-3 py-2 text-sm font-medium transition ${
            tab === "show"
              ? "bg-accent-500 text-white"
              : "bg-white text-slate-600 hover:bg-slate-50"
          }`}
        >
          Show my code
        </button>
        <button
          type="button"
          onClick={() => setTab("enter")}
          className={`flex-1 px-3 py-2 text-sm font-medium transition ${
            tab === "enter"
              ? "bg-accent-500 text-white"
              : "bg-white text-slate-600 hover:bg-slate-50"
          }`}
        >
          Enter a code
        </button>
      </div>

      {tab === "show" ? (
        <PairingCodeDisplay groupId={groupId} />
      ) : (
        <PairDevice onPaired={onPaired} />
      )}
    </div>
  );
}
