"use client";

import { useAuditLogs } from "@/lib/hooks/audit";

export default function AuditLogsPage() {
  const logs = useAuditLogs({ page: 0, size: 50 });

  return (
    <div className="mx-auto max-w-6xl">
      <h1 className="text-lg font-semibold text-white">Audit Logs</h1>

      {logs.isLoading && <p className="mt-6 text-sm text-slate-400" role="status">Loading audit trail…</p>}
      {logs.isError && (
        <p className="mt-6 text-sm text-amber-300" role="alert">
          Could not load the audit trail. ADMIN access is required.
        </p>
      )}

      {logs.data && (
        <div className="mt-6 overflow-x-auto rounded-2xl border border-slate-800">
          <table className="w-full min-w-[760px] text-left text-sm">
            <thead className="border-b border-slate-800 bg-slate-900/70 text-xs uppercase tracking-wider text-slate-400">
              <tr>
                <th scope="col" className="px-4 py-3">When</th>
                <th scope="col" className="px-4 py-3">Actor</th>
                <th scope="col" className="px-4 py-3">Action</th>
                <th scope="col" className="px-4 py-3">Entity</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800 bg-slate-950/40">
              {logs.data.content.map((entry) => (
                <tr key={entry.id} className="hover:bg-slate-900/50">
                  <td className="px-4 py-3 text-xs text-slate-400">
                    {new Date(entry.createdAt).toLocaleString()}
                  </td>
                  <td className="px-4 py-3 text-slate-300">{entry.actorName ?? "system"}</td>
                  <td className="px-4 py-3 font-mono text-xs text-emerald-300">{entry.action}</td>
                  <td className="px-4 py-3 text-slate-400">
                    {entry.entityType}
                    <span className="ml-2 font-mono text-[10px] text-slate-600">
                      {entry.entityId?.slice(0, 8)}
                    </span>
                  </td>
                </tr>
              ))}
              {logs.data.content.length === 0 && (
                <tr><td colSpan={4} className="px-4 py-8 text-center text-slate-400">No entries yet.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
