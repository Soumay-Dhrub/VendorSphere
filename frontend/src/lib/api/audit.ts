/**
 * Audit log read endpoint (`GET /audit-logs`, ADMIN only).
 *
 * The audit trail is append-only: the backend declares no write verbs, so this
 * module exposes reads only (Requirements 29.8, 29.9).
 */

import { apiGetPage } from "./client";
import type { IsoInstant, PageParams, PageResponse, Uuid } from "./types";

export type AuditLogResponse = {
  id: Uuid;
  organizationId: Uuid;
  actorId: Uuid | null;
  actorName: string | null;
  action: string;
  entityType: string;
  entityId: Uuid | null;
  /** Serialized JSON snapshots taken before and after the change. */
  previousValue: string | null;
  newValue: string | null;
  ipAddress: string | null;
  userAgent: string | null;
  createdAt: IsoInstant;
};

export type AuditLogListParams = PageParams & {
  actorId?: Uuid;
  entityType?: string;
  entityId?: Uuid;
  action?: string;
  /** Inclusive range over the creation instant (Requirement 29.6). */
  from?: IsoInstant;
  to?: IsoInstant;
};

export function listAuditLogs(
  params: AuditLogListParams = {},
): Promise<PageResponse<AuditLogResponse>> {
  return apiGetPage<AuditLogResponse>("/audit-logs", params);
}
