"use client";

import { Bell } from "lucide-react";
import Link from "next/link";
import { useUnreadNotificationCount } from "@/lib/hooks/notifications";

export function notificationBellLabel(unreadCount: number): string {
  if (unreadCount <= 0) {
    return "Notifications, no unread notifications";
  }
  if (unreadCount === 1) {
    return "Notifications, 1 unread notification";
  }
  return `Notifications, ${unreadCount} unread notifications`;
}

export function NotificationBell() {
  const { data } = useUnreadNotificationCount();
  const unreadCount = Math.max(0, data?.unreadCount ?? 0);

  return (
    <Link
      href="/notifications"
      aria-label={notificationBellLabel(unreadCount)}
      className="relative inline-flex size-9 items-center justify-center rounded-md text-slate-300 transition-colors hover:bg-slate-800 hover:text-white focus-visible:ring-2 focus-visible:ring-ring focus-visible:outline-none"
    >
      <Bell className="size-5" aria-hidden="true" />
      {unreadCount > 0 && (
        <span
          aria-hidden="true"
          className="absolute -top-0.5 -right-0.5 min-w-5 rounded-full bg-emerald-500 px-1 text-center text-[11px] font-semibold leading-5 text-slate-950"
        >
          {unreadCount > 99 ? "99+" : unreadCount}
        </span>
      )}
    </Link>
  );
}
