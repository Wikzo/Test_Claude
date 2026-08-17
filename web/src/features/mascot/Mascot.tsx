import { useEffect } from "react";
import { AnimatePresence, motion, useAnimation } from "framer-motion";

export type MascotMood = "idle" | "happy" | "celebrating";

interface MascotProps {
  /** Count of incomplete tasks. */
  remaining: number;
  /** Count of completed tasks. */
  completedCount: number;
  /**
   * True for a few seconds right after the list was just fully cleared --
   * the caller owns the timing (see App.tsx), this component just reacts to
   * it by playing the celebration animation and settling back afterwards.
   */
  celebrating: boolean;
  /** Current daily-clear streak; a badge is shown alongside celebrating for streak >= 2. */
  streak: number;
}

/**
 * Blob path, dot eyes and a mouth that mirrors app state -- a small,
 * hand-drawn-feeling companion, not a mascot-forward redesign. Kept
 * self-contained (inline SVG, no image assets) and themeable via the
 * existing `accent` Tailwind color scale.
 */
function moodFrom(props: Omit<MascotProps, "streak">): MascotMood {
  if (props.celebrating) return "celebrating";
  if (props.completedCount > 0 && props.remaining > 0) return "happy";
  return "idle";
}

const MOUTHS: Record<MascotMood, string> = {
  idle: "M22 40 Q32 40 42 40",
  happy: "M22 38 Q32 47 42 38",
  celebrating: "M19 35 Q32 54 45 35",
};

export function Mascot({ remaining, completedCount, celebrating, streak }: MascotProps) {
  const mood = moodFrom({ remaining, completedCount, celebrating });
  const bounce = useAnimation();

  useEffect(() => {
    if (mood === "celebrating") {
      bounce.start({
        scale: [1, 1.22, 0.94, 1.1, 1],
        rotate: [0, -4, 3, -2, 0],
        transition: { duration: 0.7, ease: "easeOut" },
      });
    }
  }, [mood, bounce]);

  return (
    <div className="flex items-center gap-2">
      <motion.div
        animate={bounce}
        className={mood === "celebrating" ? "" : "animate-mascot-bob"}
        aria-hidden
      >
        <svg viewBox="0 0 64 64" className="h-11 w-11 sm:h-12 sm:w-12">
          <path
            d="M32 6C42 5 54 12 57 24C60 37 54 51 41 57C28 63 12 58 6 45C0 33 4 17 16 10C21 7 27 6 32 6Z"
            className="fill-accent-400 stroke-accent-600"
            strokeWidth={1.5}
            strokeLinejoin="round"
          />
          <circle cx="24" cy="28" r="3" className="fill-accent-700" />
          <circle cx="40" cy="28" r="3" className="fill-accent-700" />
          <AnimatePresence mode="wait" initial={false}>
            <motion.path
              key={mood}
              d={MOUTHS[mood]}
              fill="none"
              className="stroke-accent-700"
              strokeWidth={2.5}
              strokeLinecap="round"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.2 }}
            />
          </AnimatePresence>
        </svg>
      </motion.div>

      <AnimatePresence>
        {mood === "celebrating" && streak >= 2 && (
          <motion.span
            initial={{ opacity: 0, scale: 0.8, y: 4 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.8, y: 4 }}
            transition={{ duration: 0.25 }}
            className="whitespace-nowrap rounded-full bg-accent-50 px-2.5 py-1 text-xs font-semibold text-accent-700 ring-1 ring-inset ring-accent-200"
          >
            {streak} in a row!
          </motion.span>
        )}
      </AnimatePresence>
    </div>
  );
}
