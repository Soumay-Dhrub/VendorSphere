"use client";

/**
 * Synthesized UI sounds via the Web Audio API — no audio assets needed.
 * Preferences persist in localStorage; sounds only play after a user gesture
 * (browser autoplay policy), which is always the case here.
 */

let muted = false;

export function setSoundsEnabled(enabled: boolean) {
  muted = !enabled;
  try {
    localStorage.setItem("vs-sounds", enabled ? "on" : "off");
  } catch {}
}

export function soundsEnabled(): boolean {
  if (muted) return false;
  try {
    return localStorage.getItem("vs-sounds") !== "off";
  } catch {
    return true;
  }
}

function tone(
  freq: number,
  duration: number,
  type: OscillatorType = "sine",
  gainValue = 0.08,
  delay = 0,
) {
  const ctx = new (window.AudioContext ||
    (window as unknown as { webkitAudioContext: typeof AudioContext })
      .webkitAudioContext)();
  const oscillator = ctx.createOscillator();
  const gain = ctx.createGain();
  const start = ctx.currentTime + delay;
  oscillator.type = type;
  oscillator.frequency.value = freq;
  gain.gain.setValueAtTime(0, start);
  gain.gain.linearRampToValueAtTime(gainValue, start + 0.01);
  gain.gain.exponentialRampToValueAtTime(0.0001, start + duration);
  oscillator.connect(gain).connect(ctx.destination);
  oscillator.start(start);
  oscillator.stop(start + duration + 0.05);
  setTimeout(() => void ctx.close(), (delay + duration + 0.2) * 1000);
}

/** Short neutral click for buttons/toggles. */
export function playClick() {
  if (!soundsEnabled()) return;
  tone(660, 0.06, "triangle", 0.05);
}

/** Pleasant two-note chime for successful actions (sign-in, register, saved). */
export function playSuccess() {
  if (!soundsEnabled()) return;
  tone(523.25, 0.14, "sine", 0.07);
  tone(783.99, 0.2, "sine", 0.06, 0.09);
}

/** Soft pop for incoming notifications. */
export function playNotificationPop() {
  if (!soundsEnabled()) return;
  tone(880, 0.09, "sine", 0.05);
  tone(1174.66, 0.12, "sine", 0.04, 0.06);
}
