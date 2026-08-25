"use client";

/**
 * The vendor list screen (Requirement 35.2): paged, searchable by company name and
 * filterable by status through the list API of Requirement 6. Rows link to the vendor
 * detail screen; statuses render as badges so lifecycle state is scannable.
 */

import { useState } from "react";
import Link from "next/link";
import { useVendors } from "@/lib/hooks/vendors";
import { formatMoney } from "@/lib/format";
import type { VendorStatus } from "@/lib/api/types";

const STATUSES: (VendorStatus | "ALL")[] = [
  "ALL",
  "PROSPECTIVE",
  "ACTIVE",
  "SUSPENDED",
  "BLACKLISTED",
  "INACTIVE",
];

const STATUS_STYLES: Record<VendorStatus, string> = {
  PROSPECTIVE: "bg-slate-500/10 text-slate-300",
  ACTIVE: "bg-emerald-500/10 text-emerald-300",
  SUSPENDED: "bg-amber-500/10 text-amber-300",
  BLACKLISTED: "bg-rose-500/10 text-rose-300",
  INACTIVE: "bg-slate-500/10 text-slate-400",
};

export default function VendorsPage() {
  const [companyName, setCompanyName] = useState("");
  const [status, setStatus] = useState<VendorStatus | "ALL">("ALL");
  const [page, setPage] = useState(0);

  const { data, isLoading, isError } = useVendors({
    page,
    size: 20,
    companyName: companyName.trim() === "" ? undefined : companyName.trim(),
    status: status === "ALL" ? undefined : status,
  });

  return (
    <div className="mx-auto max-w-6xl">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <h1 className="text-lg font-semibold text-white">Vendors</h1>
        <div className="flex flex-wrap items-center gap-2">
          <input
            aria-label="Search vendors by company name"
            value={companyName}
            onChange={(event) => {
              setCompanyName(event.target.value);
              setPage(0);
            }}
            placeholder="Search company name"
            className="w-56 rounded-lg border border-slate-700 bg-slate-900 px-3 py-1.5 text-sm text-white placeholder:text-slate-500 focus:border-emerald-500 focus:outline-none"
          />
          <select
            aria-label="Filter by status"
            value={status}
            onChange={(event) => {
              setStatus(event.target.value as VendorStatus | "ALL");
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
      </div>

      {isLoading && (
        <p className="mt-6 text-sm text-slate-400" role="status">
          Loading vendors…
        </p>
      )}
      {isError && (
        <p className="mt-6 text-sm text-amber-300" role="alert">
          Could not load vendors. Is the backend running?
        </p>
      )}

      {data && (
        <>
          <div className="mt-6 overflow-x-auto rounded-2xl border border-slate-800">
            <table className="w-full min-w-[720px] text-left text-sm">
              <thead className="border-b border-slate-800 bg-slate-900/70 text-xs uppercase tracking-wider text-slate-400">
                <tr>
                  <th scope="col" className="px-4 py-3">Code</th>
                  <th scope="col" className="px-4 py-3">Company</th>
                  <th scope="col" className="px-4 py-3">Category</th>
                  <th scope="col" className="px-4 py-3">Status</th>
                  <th scope="col" className="px-4 py-3 text-right">Rating</th>
                  <th scope="col" className="px-4 py-3 text-right">Score</th>
                  <th scope="col" className="px-4 py-3 text-right">Expiring docs</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800 bg-slate-950/40">
                {data.content.map((vendor) => (
                  <tr key={vendor.id} className="hover:bg-slate-900/50">
                    <td className="px-4 py-3 font-mono text-xs text-slate-400">
                      {vendor.vendorCode}
                    </td>
                    <td className="px-4 py-3">
                      <Link
                        href={`/vendors/${vendor.id}`}
                        className="font-medium text-emerald-300 hover:underline"
                      >
                        {vendor.companyName}
                      </Link>
                      <p className="text-xs text-slate-500">{vendor.email}</p>
                    </td>
                    <td className="px-4 py-3 text-slate-300">
                      {vendor.categoryName ?? "—"}
                    </td>
                    <td className="px-4 py-3">
                      <span
                        className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_STYLES[vendor.status]}`}
                      >
                        {vendor.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right font-mono text-slate-200">
                      {formatMoney(vendor.rating)}
                    </td>
                    <td className="px-4 py-3 text-right font-mono text-slate-200">
                      {formatMoney(vendor.performanceScore)}
                    </td>
                    <td className="px-4 py-3 text-right">
                      {vendor.expiringDocumentCount > 0 ? (
                        <span className="rounded-full bg-amber-500/10 px-2 py-0.5 text-xs font-medium text-amber-300">
                          {vendor.expiringDocumentCount}
                        </span>
                      ) : (
                        <span className="text-slate-500">0</span>
                      )}
                    </td>
                  </tr>
                ))}
                {data.content.length === 0 && (
                  <tr>
                    <td colSpan={7} className="px-4 py-8 text-center text-slate-400">
                      No vendors match the current filters.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          <nav
            aria-label="Vendor pages"
            className="mt-4 flex items-center justify-between text-sm text-slate-400"
          >
            <span>
              Page {data.page + 1} of {Math.max(data.totalPages, 1)} ·{" "}
              {data.totalElements} vendors
            </span>
            <div className="flex gap-2">
              <button
                type="button"
                disabled={data.first}
                onClick={() => setPage((current) => Math.max(current - 1, 0))}
                className="rounded-lg border border-slate-700 px-3 py-1.5 disabled:opacity-40 enabled:hover:bg-slate-800"
              >
                Previous
              </button>
              <button
                type="button"
                disabled={data.last}
                onClick={() => setPage((current) => current + 1)}
                className="rounded-lg border border-slate-700 px-3 py-1.5 disabled:opacity-40 enabled:hover:bg-slate-800"
              >
                Next
              </button>
            </div>
          </nav>
        </>
      )}
    </div>
  );
}
