"use client";

/** Invoice list with status filter and match-state badges (Requirement 35.7). */
import { useState } from "react";
import { useInvoices } from "@/lib/hooks/invoices";
import { formatMoney } from "@/lib/format";
import type { InvoiceStatus, MatchStatus } from "@/lib/api/types";

const STATUSES: (InvoiceStatus | "ALL")[] = [
  "ALL", "SUBMITTED", "UNDER_REVIEW", "APPROVED", "REJECTED",
  "PARTIALLY_PAID", "PAID", "OVERDUE",
];

const STATUS_STYLES: Record<string, string> = {
  SUBMITTED: "bg-sky-500/10 text-sky-300",
  UNDER_REVIEW: "bg-amber-500/10 text-amber-300",
  APPROVED: "bg-emerald-500/10 text-emerald-300",
  REJECTED: "bg-rose-500/10 text-rose-300",
  PARTIALLY_PAID: "bg-violet-500/10 text-violet-300",
  PAID: "bg-emerald-500/20 text-emerald-200",
  OVERDUE: "bg-rose-500/20 text-rose-200",
};

const MATCH_STYLES: Record<string, string> = {
  MATCHED: "bg-emerald-500/10 text-emerald-300",
  PENDING: "bg-slate-500/10 text-slate-300",
};

export default function InvoicesPage() {
  const [status, setStatus] = useState<InvoiceStatus | "ALL">("ALL");
  const [page, setPage] = useState(0);
  const { data, isLoading, isError } = useInvoices({
    page,
    size: 20,
    status: status === "ALL" ? undefined : status,
  });

  return (
    <div className="mx-auto max-w-6xl">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-lg font-semibold text-white">Invoices</h1>
        <select
          aria-label="Filter by status"
          value={status}
          onChange={(event) => {
            setStatus(event.target.value as InvoiceStatus | "ALL");
            setPage(0);
          }}
          className="rounded-lg border border-slate-700 bg-slate-900 px-3 py-1.5 text-sm text-white focus:border-emerald-500 focus:outline-none"
        >
          {STATUSES.map((value) => (<option key={value} value={value}>{value}</option>))}
        </select>
      </div>

      {isLoading && <p className="mt-6 text-sm text-slate-400" role="status">Loading invoices…</p>}
      {isError && <p className="mt-6 text-sm text-amber-300" role="alert">Could not load invoices.</p>}

      {data && (
        <>
          <div className="mt-6 overflow-x-auto rounded-2xl border border-slate-800">
            <table className="w-full min-w-[720px] text-left text-sm">
              <thead className="border-b border-slate-800 bg-slate-900/70 text-xs uppercase tracking-wider text-slate-400">
                <tr>
                  <th scope="col" className="px-4 py-3">Number</th>
                  <th scope="col" className="px-4 py-3">Due date</th>
                  <th scope="col" className="px-4 py-3 text-right">Total</th>
                  <th scope="col" className="px-4 py-3 text-right">Paid</th>
                  <th scope="col" className="px-4 py-3">Status</th>
                  <th scope="col" className="px-4 py-3">Match</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800 bg-slate-950/40">
                {data.content.map((invoice) => (
                  <tr key={invoice.id} className="hover:bg-slate-900/50">
                    <td className="px-4 py-3 font-mono text-xs text-slate-300">{invoice.invoiceNumber}</td>
                    <td className="px-4 py-3 text-slate-400">{invoice.dueDate ?? "—"}</td>
                    <td className="px-4 py-3 text-right font-mono text-white">{formatMoney(invoice.totalAmount)}</td>
                    <td className="px-4 py-3 text-right font-mono text-slate-300">{formatMoney(invoice.paidAmount)}</td>
                    <td className="px-4 py-3">
                      <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_STYLES[invoice.status]}`}>
                        {invoice.status}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      {invoice.matchStatus && (
                        <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${MATCH_STYLES[invoice.matchStatus] ?? "bg-amber-500/10 text-amber-300"}`}>
                          {invoice.matchStatus}
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
                {data.content.length === 0 && (
                  <tr><td colSpan={6} className="px-4 py-8 text-center text-slate-400">No invoices match the current filter.</td></tr>
                )}
              </tbody>
            </table>
          </div>
          <nav aria-label="Invoice pages" className="mt-4 flex items-center justify-between text-sm text-slate-400">
            <span>Page {data.page + 1} of {Math.max(data.totalPages, 1)}</span>
            <div className="flex gap-2">
              <button type="button" disabled={data.first} onClick={() => setPage((c) => Math.max(c - 1, 0))}
                className="rounded-lg border border-slate-700 px-3 py-1.5 disabled:opacity-40 enabled:hover:bg-slate-800">Previous</button>
              <button type="button" disabled={data.last} onClick={() => setPage((c) => c + 1)}
                className="rounded-lg border border-slate-700 px-3 py-1.5 disabled:opacity-40 enabled:hover:bg-slate-800">Next</button>
            </div>
          </nav>
        </>
      )}
    </div>
  );
}
