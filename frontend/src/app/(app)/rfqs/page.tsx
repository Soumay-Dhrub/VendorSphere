"use client";

import { useState } from "react";
import { useRfqs } from "@/lib/hooks/rfqs";
import type { RfqStatus } from "@/lib/api/types";

const STATUSES: (RfqStatus | "ALL")[] = [
  "ALL", "DRAFT", "OPEN", "CLOSED", "EVALUATION", "AWARDED", "CANCELLED",
];

const STATUS_STYLES: Record<string, string> = {
  DRAFT: "bg-slate-500/10 text-slate-300",
  OPEN: "bg-emerald-500/10 text-emerald-300",
  CLOSED: "bg-sky-500/10 text-sky-300",
  EVALUATION: "bg-amber-500/10 text-amber-300",
  AWARDED: "bg-violet-500/10 text-violet-300",
  CANCELLED: "bg-rose-500/10 text-rose-300",
};

function formatInstant(value: string): string {
  return new Date(value).toLocaleString(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  });
}

export default function RfqsPage() {
  const [status, setStatus] = useState<RfqStatus | "ALL">("ALL");
  const [page, setPage] = useState(0);

  const { data, isLoading, isError } = useRfqs({
    page,
    size: 20,
    status: status === "ALL" ? undefined : status,
  });

  return (
    <div className="mx-auto max-w-6xl">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-lg font-semibold text-white">RFQs</h1>
        <select
          aria-label="Filter by status"
          value={status}
          onChange={(event) => {
            setStatus(event.target.value as RfqStatus | "ALL");
            setPage(0);
          }}
          className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-1.5 text-sm text-white focus:border-emerald-500 focus:outline-none"
        >
          {STATUSES.map((value) => (
            <option key={value} value={value}>{value}</option>
          ))}
        </select>
      </div>

      {isLoading && <p className="mt-6 text-sm text-slate-400" role="status">Loading RFQs…</p>}
      {isError && <p className="mt-6 text-sm text-amber-300" role="alert">Could not load RFQs.</p>}

      {data && (
        <>
          <div className="mt-6 overflow-x-auto rounded-2xl border border-slate-800">
            <table className="w-full min-w-[720px] text-left text-sm">
              <thead className="border-b border-slate-800 bg-slate-900/70 text-xs uppercase tracking-wider text-slate-400">
                <tr>
                  <th scope="col" className="px-4 py-3">Number</th>
                  <th scope="col" className="px-4 py-3">Title</th>
                  <th scope="col" className="px-4 py-3">Opens</th>
                  <th scope="col" className="px-4 py-3">Closes</th>
                  <th scope="col" className="px-4 py-3">Currency</th>
                  <th scope="col" className="px-4 py-3">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800 bg-slate-950/40">
                {data.content.map((rfq) => (
                  <tr key={rfq.id} className="hover:bg-slate-900/50">
                    <td className="px-4 py-3 font-mono text-xs text-slate-400">{rfq.rfqNumber}</td>
                    <td className="px-4 py-3 font-medium text-white">{rfq.title}</td>
                    <td className="px-4 py-3 text-slate-400">{formatInstant(rfq.openingDate)}</td>
                    <td className="px-4 py-3 text-slate-400">{formatInstant(rfq.closingDate)}</td>
                    <td className="px-4 py-3 text-slate-300">{rfq.currency}</td>
                    <td className="px-4 py-3">
                      <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_STYLES[rfq.status]}`}>
                        {rfq.status}
                      </span>
                    </td>
                  </tr>
                ))}
                {data.content.length === 0 && (
                  <tr>
                    <td colSpan={6} className="px-4 py-8 text-center text-slate-400">
                      No RFQs match the current filter.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <nav aria-label="RFQ pages" className="mt-4 flex items-center justify-between text-sm text-slate-400">
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
