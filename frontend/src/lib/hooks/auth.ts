"use client";

import { useMemo, useSyncExternalStore } from "react";
import { getStoredUser, type User } from "@/lib/api";

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

function getStoredUserSnapshot(): string {
  return window.localStorage.getItem("user") ?? "";
}

function getServerSnapshot(): null {
  return null;
}
