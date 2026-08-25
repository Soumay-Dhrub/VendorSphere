"use client";

import { useState } from "react";
import { useMarkAllNotificationsRead, useMarkNotificationRead, useNotifications } from "@/lib/hooks/notifications";
import type { NotificationResponse } from "@/lib/api/notifications";

export default function NotificationsPage() {
  const [unreadOnly, setUnreadOnly] = useState(false);
  const notifications = useNotifications({ page: 0, size: 50, unreadOnly });
  const markRead = useMarkNotificationRead();
  const markAllRead = useMarkAllNotificationsRead();

  const rows = notifications.data?.content ?? [];

  return (
    <div className="mx-auto max-w-4xl">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-lg font-semibold text-white">Notifications</h1>
        <div className="flex items-center gap-3">
          <label className="flex items-center gap-2 text-sm text-slate-400">
            <input
              type="checkbox"
              checked={unreadOnly}
              onChange={(event) => setUnreadOnly(event.target.checked)}
              className="h-4 w-4 rounded border-slate-600 bg-slate-900"
            />
            Unread only
          </label>
          <button
            type="button"
            onClick={() => markAllRead.mutate()}
            disabled={markAllRead.isPending}
            className="rounded-lg border border-slate-700 px-3 py-1.5 text-sm text-slate-200 hover:bg-slate-800 disabled:opacity-40"
          >
            Mark all read
          </button>
        </div>
      </div>

      {notifications.isLoading && (
        <p className="mt-6 text-sm text-slate-400" role="status">Loading notifications…</p>
      )}
      {notifications.isError && (
        <p className="mt-6 text-sm text-amber-300" role="alert">Could not load notifications.</p>
      )}

      {!notifications.isLoading && rows.length === 0 && (
        <p className="mt-8 text-sm text-slate-400">Nothing here yet.</p>
      )}

      <ul className="mt-6 space-y-3">
        {rows.map((notification: NotificationResponse) => (
          <li
            key={notification.id}
            className={`rounded-xl border p-4 ${
              notification.read
                ? "border-slate-800 bg-slate-950/40"
                : "border-emerald-900 bg-emerald-950/20"
            }`}
          >
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="text-sm font-medium text-white">{notification.title}</p>
                <p className="mt-1 text-sm text-slate-400">{notification.message}</p>
                <p className="mt-1 text-xs text-slate-500">
                  {new Date(notification.createdAt).toLocaleString()}
                </p>
              </div>
              {!notification.read && (
                <button
                  type="button"
                  onClick={() => markRead.mutate(notification.id)}
                  disabled={markRead.isPending}
                  className="rounded-lg border border-slate-700 px-3 py-1.5 text-xs text-slate-200 hover:bg-slate-800 disabled:opacity-40"
                >
                  Mark read
                </button>
              )}
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
}
