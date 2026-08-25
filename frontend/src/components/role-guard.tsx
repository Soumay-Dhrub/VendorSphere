"use client";

import { ShieldAlert } from "lucide-react";
import { useStoredUser } from "@/lib/hooks/auth";

export const ROLES = [
  "ADMIN",
  "PROCUREMENT_MANAGER",
  "PROCUREMENT_OFFICER",
  "REQUESTER",
  "FINANCE",
  "VENDOR",
] as const;

export type Role = (typeof ROLES)[number];

export function hasAnyRole(userRoles: readonly string[], allowed: readonly Role[]): boolean {
  if (allowed.length === 0) {
    return true;
  }
  return userRoles.some((role) => (allowed as readonly string[]).includes(role));
}

export type RoleGuardProps = {

  allow: readonly Role[];
  children: React.ReactNode;

  roles?: readonly string[];
};

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

function useStoredRoles(): readonly string[] | null {
  const user = useStoredUser();
  if (user === undefined) {
    return null;
  }
  return user?.roles ?? NO_ROLES;
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
