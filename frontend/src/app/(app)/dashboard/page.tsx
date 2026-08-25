"use client";

/**
 * The procurement dashboard (Requirement 35.1): the seven headline figures of
 * Requirement 27.1 rendered from /analytics/dashboard, with the API status panel
 * kept as a start-up aid while Docker Compose comes up.
 */

import { useQuery } from "@tanstack/react-query";
import { getCurrentUser, getHealth } from "@/lib/api";
import { useDashboardSummary } from "@/lib/hooks/analytics";
import { formatMoney } from "@/lib/format";

export default function DashboardPage() {
  const { data: health, isLoading: healthLoading, isError: healthError } = useQuery({
    queryKey: ["health"],
    queryFn: getHealth,
  });

  const { data: user } = useQuery({
    queryKey: ["currentUser"],
    queryFn: getCurrentUser,
  });

  const summary = useDashboardSummary();

  const stats = [
    { label: "Total Spend", value: summary.data ? formatMoney(summary.data.totalSpend) : "—" },
    { label: "Active RFQs", value: summary.data ? String(summary.data.activeRfqCount) : "—" },
    { label: "Open POs", value: summary.data ? String(summary.data.openPurchaseOrderCount) : "—" },
    { label: "Pending Deliveries", value: summary.data ? String(summary.data.pendingDeliveryCount) : "—" },
    { label: "Outstanding Invoices", value: summary.data ? String(summary.data.outstandingInvoiceCount) : "—" },
    { label: "Overdue Invoices", value: summary.data ? String(summary.data.overdueInvoiceCount) : "—" },
    { label: "Active Vendors", value: summary.data ? String(summary.data.activeVendorCount) : "—" },
  ];

  return (
    <div className="mx-auto max-w-6xl">
      <h1 className="text-lg font-semibold text-white">Procurement Dashboard</h1>
      {user && (
        <p className="mt-1 text-sm text-slate-400">
          Welcome, {user.firstName} {user.lastName}
        </p>
      )}

      {healthLoading && <p className="mt-6 text-sm text-slate-400" role="status">Checking backend…</p>}
      {healthError && (
        <p className="mt-6 text-sm text-amber-300" role="alert">
          Backend unavailable. Start PostgreSQL and the API with Docker Compose.
        </p>
      )}

      <section className="mb-8 mt-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {stats.map((stat) => (
          <article
            key={stat.label}
            className="rounded-2xl border border-slate-800 bg-slate-900/50 p-6"
          >
            <p className="text-sm text-slate-400">{stat.label}</p>
            <p className="mt-2 text-3xl font-semibold text-white">{stat.value}</p>
          </article>
        ))}
      </section>

      {summary.isError && (
        <p className="text-sm text-amber-300" role="alert">
          Dashboard figures are unavailable right now.
        </p>
      )}
    </div>
  );
}
