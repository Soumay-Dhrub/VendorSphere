"use client";

import { useQuery } from "@tanstack/react-query";
import { listAuditLogs, type AuditLogListParams } from "@/lib/api/audit";
import { auditKeys } from "./keys";

export function useAuditLogs(params: AuditLogListParams = {}) {
  return useQuery({
    queryKey: auditKeys.list(params),
    queryFn: () => listAuditLogs(params),
  });
}
