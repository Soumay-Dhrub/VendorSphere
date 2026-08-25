
import {
  Bell,
  ClipboardList,
  FileText,
  LayoutDashboard,
  Package,
  Receipt,
  ScrollText,
  Store,
  Wallet,
} from "lucide-react";
import type { ComponentType } from "react";
import { hasAnyRole, type Role } from "./role-guard";

const INTERNAL: readonly Role[] = [
  "ADMIN",
  "PROCUREMENT_MANAGER",
  "PROCUREMENT_OFFICER",
  "REQUESTER",
  "FINANCE",
];

const EVERYONE: readonly Role[] = [...INTERNAL, "VENDOR"];

export type NavIcon = ComponentType<{ className?: string; "aria-hidden"?: boolean }>;

export type NavItem = {
  href: string;
  label: string;
  icon: NavIcon;

  allow: readonly Role[];
};

export const NAV_ITEMS: readonly NavItem[] = [
  {
    // The landing screen after sign-in. Its analytics sections are role-guarded on the
    // screen itself, so every internal role can reach the route.
    href: "/dashboard",
    label: "Dashboard",
    icon: LayoutDashboard,
    allow: INTERNAL,
  },
  {
    href: "/vendors",
    label: "Vendors",
    icon: Store,
    allow: ["ADMIN", "PROCUREMENT_OFFICER", "PROCUREMENT_MANAGER"],
  },
  {
    href: "/purchase-requests",
    label: "Purchase requests",
    icon: ClipboardList,
    allow: ["ADMIN", "PROCUREMENT_OFFICER", "PROCUREMENT_MANAGER", "REQUESTER"],
  },
  {
    href: "/rfqs",
    label: "RFQs",
    icon: FileText,
    allow: ["ADMIN", "PROCUREMENT_OFFICER", "PROCUREMENT_MANAGER"],
  },
  {
    href: "/purchase-orders",
    label: "Purchase orders",
    icon: Package,
    allow: ["ADMIN", "PROCUREMENT_OFFICER", "PROCUREMENT_MANAGER"],
  },
  {
    href: "/invoices",
    label: "Invoices",
    icon: Receipt,
    allow: ["ADMIN", "FINANCE"],
  },
  {
    href: "/payments",
    label: "Payments",
    icon: Wallet,
    allow: ["ADMIN", "FINANCE"],
  },
  {
    href: "/notifications",
    label: "Notifications",
    icon: Bell,
    allow: EVERYONE,
  },
  {
    href: "/audit-logs",
    label: "Audit logs",
    icon: ScrollText,
    allow: ["ADMIN"],
  },
];

export function navItemsForRoles(roles: readonly string[]): readonly NavItem[] {
  return NAV_ITEMS.filter((item) => hasAnyRole(roles, item.allow));
}

export function isCurrentNavItem(href: string, pathname: string | null): boolean {
  if (!pathname) {
    return false;
  }
  return pathname === href || pathname.startsWith(`${href}/`);
}
