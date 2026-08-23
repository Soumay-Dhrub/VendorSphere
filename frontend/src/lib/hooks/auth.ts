"use client";

/**
 * Client-side view of the signed-in user.
 *
 * The session lives in `localStorage` (see `src/lib/api.ts`), which is unreadable while
 * rendering on the server and during hydration. `useSyncExternalStore` with a server
 * snapshot models that third state explicitly, so the shell and `RoleGuard` never render
 * a signed-out decision against markup produced on the server.
 */

import { useMemo, useSyncExternalStore } from "react";
import { getStoredUser, type User } from "@/lib/api";

/**
 * The signed-in user, `null` when nobody is signed in, and `undefined` while the client
 * auth state is still unresolved (server render and the hydration pass).
 */
export function useStoredUser(): User | null | undefined {
  const rawUser = useSyncExternalStore(
    subscribeToStoredUser,
    getStoredUserSnapshot,
    getServerSnapshot,
  );

  // Parsing is memoized on the raw string so the returned user keeps a stable identity
  // between renders, which in turn keeps `roles` stable for consumers.
  return useMemo(() => (rawUser === null ? undefined : getStoredUser()), [rawUser]);
}

function subscribeToStoredUser(onStoreChange: () => void) {
  window.addEventListener("storage", onStoreChange);
  return () => window.removeEventListener("storage", onStoreChange);
}

/** Returns the raw stored value so the snapshot identity stays stable between renders. */
function getStoredUserSnapshot(): string {
  return window.localStorage.getItem("user") ?? "";
}

/** `null` is unreachable on the client, so it marks the unresolved state. */
function getServerSnapshot(): null {
  return null;
}
