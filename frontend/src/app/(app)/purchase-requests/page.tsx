"use client";

/**
 * The purchase request list screen (Requirement 35.3): paged, filterable by status,
 * newest first. Rows link to the request detail screen once task 20.x builds it.
 */

import { useState } from "react";
import { usePurchaseRequests } from "@/lib/hooks/purchaseRequests";
import type { PurchaseRequestStatus } from "@/lib/api/types";

const STATUSES: (PurchaseRequestStatus | "ALL")[] = [
  "ALL",
  "DRAFT",
  "SUBMITTED",
  "UNDER_REVIEW",
  "APPROVED",
  "REJECTED",
  "PROCUREMENT_STARTED",
  "COMPLETED",
];

const STATUS_STYLES: Record<string, string> = {
  DRAFT: "bg-slate-500/10 text-slate-300",
  SUBMITTED: "bg-sky-500/10 text-sky-300",
  UNDER_REVIEW: "bg-amber-500/10 text-amber-300",
  APPROVED: "bg-emerald-500/10 text-emerald-300",
  REJECTED: "bg-rose-500/10 text-rose-300",
  PROCUREMENT_STARTED: "bg-violet-500/10 text-violet-300",
  COMPLETED: "bg-emerald-500/20 text-emerald-200",
};

export default function PurchaseRequestsPage() {
  const [status, setStatus] = useState<PurchaseRequestStatus | "ALL">("ALL");
  const [page, setPage] = useState(0);

  const { data, isLoading, isError } = usePurchaseRequests({
    page,
    size: 20,
    status: status === "ALL" ? undefined : status,
  });

  return (
    <div className="mx-auto max-w-6xl">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-lg font-semibold text-white">Purchase Requests</h1>
        <select
          aria-label="Filter by status"
          value={status}
          onChange={(event) => {
            setStatus(event.target.value as PurchaseRequestStatus | "ALL");
            setPage(0);
          }}
          className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-1.5 text-sm text-white focus:border-emerald-500 focus:outline-none"
        >
          {STATUSES.map((value) => (
            <option key={value} value={value}>
              {value}
            </option>
          ))}
        </select>
      </div>

      {isLoading && (
        <p className="mt-6 text-sm text-slate-400" role="status">Loading requests…</p>
      )}
      {isError && (
        <p className="mt-6 text-sm text-amber-300" role="alert">
          Could not load purchase requests.
        </p>
      )}

      {data && (
        <>
          <div className="mt-6 overflow-x-auto rounded-2xl border border-slate-800">
            <table className="w-full min-w-[680px] text-left text-sm">
              <thead className="border-b border-slate-800 bg-slate-900/70 text-xs uppercase tracking-wider text-slate-400">
                <tr>
                  <th scope="col" className="px-4 py-3">Number</th>
                  <th scope="col" className="px-4 py-3">Title</th>
                  <th scope="col" className="px-4 py-3">Department</th>
                  <th scope="col" className="px-4 py-3">Priority</th>
                  <th scope="col" className="px-4 py-3">Required</th>
                  <th scope="col" className="px-4 py-3">Items</th>
                  <th scope="col" className="px-4 py-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800 bg-slate-950/40">
                {data.content.map((request) => (
                  <tr key={request.id} className="hover:bg-slate-900/50">
                    <td className="px-4 py-3 font-mono text-xs text-slate-400">
                      {request.requestNumber}
                    </td>
                    <td className="px-4 py-3 font-medium text-white">{request.title}</td>
                    <td className="px-4 py-3 text-slate-300">{request.departmentName}</td>
                    <td className="px-4 py-3 text-slate-300">{request.priority}</td>
                    <td className="px-4 py-3 text-slate-400">{request.requiredDate ?? "—"}</td>
                    <td className="px-4 py-3 text-slate-400">{request.items?.length ?? 0}</td>
                    <td className="px-4 py-3">
                      <span
                        className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_STYLES[request.status]}`}
                      >
                        {request.status}
                      </span>
                    </td>
                  </tr>
                ))}
                {data.content.length === 0 && (
                  <tr>
                    <td colSpan={7} className="px-4 py-8 text-center text-slate-400">
                      No purchase requests match the current filter.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <nav aria-label="Request pages" className="mt-4 flex items-center justify-between text-sm text-slate-400">
            <span>Page {data.page + 1} of {Math.max(data.totalPages, 1)}</span>
            <div className="flex gap-2">
              <button type="button" disabled={data.first}
                onClick={() => setPage((c) => Math.max(c - 1, 0))}
                className="rounded-lg border border-slate-700 px-3 py-1.5 disabled:opacity-40 enabled:hover:bg-slate-800">
                Previous
              </button>
              <button type="button" disabled={data.last}
                onClick={() => setPage((c) => c + 1)}
                className="rounded-lg border border-slate-700 px-3 py-1.5 disabled:opacity-40 enabled:hover:bg-slate-800">
                Next
              </button>
            </div>
          </nav>
        </>
      )}
    </div>
  );
}
