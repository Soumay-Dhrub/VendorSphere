
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

  from?: IsoInstant;
  to?: IsoInstant;
};

export function listAuditLogs(
  params: AuditLogListParams = {},
): Promise<PageResponse<AuditLogResponse>> {
  return apiGetPage<AuditLogResponse>("/audit-logs", params);
}
