"use client";

/**
 * Query hook for the audit trail.
 *
 * The trail is append-only and the API exposes reads only, so there is no
 * mutation here (Requirements 29.8, 29.9).
 */

import { useQuery } from "@tanstack/react-query";
import { listAuditLogs, type AuditLogListParams } from "@/lib/api/audit";
import { auditKeys } from "./keys";

export function useAuditLogs(params: AuditLogListParams = {}) {
  return useQuery({
    queryKey: auditKeys.list(params),
    queryFn: () => listAuditLogs(params),
  });
}
