"use client";

import { ShieldAlert } from "lucide-react";
import { useMemo, useSyncExternalStore } from "react";
import { getStoredUser } from "@/lib/api";

/** The six roles issued by the backend. */
export const ROLES = [
  "ADMIN",
  "PROCUREMENT_MANAGER",
  "PROCUREMENT_OFFICER",
  "REQUESTER",
  "FINANCE",
  "VENDOR",
] as const;

export type Role = (typeof ROLES)[number];

/** True when the user holds at least one of the roles granted access to the screen. */
export function hasAnyRole(userRoles: readonly string[], allowed: readonly Role[]): boolean {
  if (allowed.length === 0) {
    return true;
  }
  return userRoles.some((role) => (allowed as readonly string[]).includes(role));
}

export type RoleGuardProps = {
  /** Roles granted access to the guarded content under Requirement 30. */
  allow: readonly Role[];
  children: React.ReactNode;
  /**
   * Overrides the roles of the signed-in user. Left unset in application code, where the
   * roles come from the authenticated user held by `src/lib/api.ts`.
   */
  roles?: readonly string[];
};

/**
 * Renders `children` only for users whose roles include one of `allow`, and an access
 * denied message in place of the content otherwise (Requirement 35.12).
 */
export function RoleGuard({ allow, children, roles }: RoleGuardProps) {
  const storedRoles = useStoredRoles();
  const effectiveRoles = roles ?? storedRoles;

  // The signed-in user is held client-side, so the roles are unknown while rendering on the
  // server and during hydration. Nothing is rendered until they resolve.
  if (effectiveRoles === null) {
    return null;
  }

  if (!hasAnyRole(effectiveRoles, allow)) {
    return <AccessDenied allow={allow} />;
  }

  return <>{children}</>;
}

const NO_ROLES: readonly string[] = [];

/** Roles of the signed-in user, or `null` until the client-side auth state is readable. */
function useStoredRoles(): readonly string[] | null {
  const rawUser = useSyncExternalStore(subscribeToStoredUser, getStoredUserSnapshot, () => null);

  return useMemo(
    () => (rawUser === null ? null : (getStoredUser()?.roles ?? NO_ROLES)),
    [rawUser],
  );
}

function subscribeToStoredUser(onStoreChange: () => void) {
  window.addEventListener("storage", onStoreChange);
  return () => window.removeEventListener("storage", onStoreChange);
}

/** Returns the raw stored value so the snapshot identity stays stable between renders. */
function getStoredUserSnapshot(): string {
  return window.localStorage.getItem("user") ?? "";
}

export function AccessDenied({ allow }: { allow: readonly Role[] }) {
  return (
    <section
      role="alert"
      className="mx-auto max-w-xl rounded-xl border border-border bg-card p-8 text-center text-card-foreground"
    >
      <ShieldAlert className="mx-auto size-8 text-amber-400" aria-hidden="true" />
      <h2 className="mt-4 text-lg font-semibold">Access denied</h2>
      <p className="mt-2 text-sm text-muted-foreground">
        Your account does not hold a role granted access to this screen.
      </p>
      {allow.length > 0 && (
        <p className="mt-1 text-sm text-muted-foreground">
          Required {allow.length === 1 ? "role" : "roles"}: {allow.join(", ")}
        </p>
      )}
    </section>
  );
}
