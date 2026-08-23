/**
 * Notification list, unread count and read-state endpoints (`/notifications`).
 *
 * The list is scoped to the calling user server-side and ordered by creation
 * instant descending (Requirement 28.3).
 */

import { apiGet, apiGetPage, apiPatchEmpty } from "./client";
import type { IsoInstant, PageParams, PageResponse, Uuid } from "./types";

export type NotificationResponse = {
  id: Uuid;
  userId: Uuid;
  eventType: string | null;
  title: string;
  message: string;
  entityType: string | null;
  entityId: Uuid | null;
  read: boolean;
  createdAt: IsoInstant;
};

export type NotificationListParams = PageParams & {
  /** Restricts the list to unread notifications (Requirement 28.4). */
  unreadOnly?: boolean;
};

export type UnreadCountResponse = {
  unreadCount: number;
};

export function listNotifications(
  params: NotificationListParams = {},
): Promise<PageResponse<NotificationResponse>> {
  return apiGetPage<NotificationResponse>("/notifications", params);
}

export function getUnreadNotificationCount(): Promise<UnreadCountResponse> {
  return apiGet<UnreadCountResponse>("/notifications/unread-count");
}

export function markNotificationRead(id: Uuid): Promise<void> {
  return apiPatchEmpty(`/notifications/${id}/read`);
}

export function markAllNotificationsRead(): Promise<void> {
  return apiPatchEmpty("/notifications/read-all");
}
