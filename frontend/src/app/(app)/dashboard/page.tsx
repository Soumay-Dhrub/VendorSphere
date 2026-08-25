"use client";

/**
 * The VendorSphere procurement dashboard: branded header, live KPI cards and
 * dynamic SVG charts (monthly spend bars, department spend split) driven by
 * the analytics endpoints of Requirement 27.
 */

import { useQuery } from "@tanstack/react-query";
import {
  ArrowUpRight,
  Boxes,
  Building2,
  FileWarning,
  PackageSearch,
  ReceiptText,
  ShoppingCart,
} from "lucide-react";
import { getCurrentUser, getHealth } from "@/lib/api";
import type { MonthlySpendEntry } from "@/lib/api/analytics";
import { useDashboardSummary, useMonthlySpend } from "@/lib/hooks/analytics";
import { formatMoney } from "@/lib/format";

function Logo() {
  return (
    <div className="flex items-center gap-3">
      <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-emerald-400 to-teal-600 shadow-lg shadow-emerald-500/20">
        <Boxes className="h-5 w-5 text-slate-950" strokeWidth={2.4} />
      </div>
      <div>
        <h1 className="text-lg font-semibold tracking-tight text-white">
          Vendor<span className="text-emerald-400">Sphere</span>
        </h1>
        <p className="text-xs text-slate-500">Procurement command center</p>
      </div>
    </div>
  );
}

const KPI_CARDS = [
  { key: "totalSpend", label: "Total Spend", accent: "from-emerald-500/20 to-transparent", icon: ShoppingCart },
  { key: "activeRfqCount", label: "Active RFQs", accent: "from-sky-500/20 to-transparent", icon: FileWarning },
  { key: "openPurchaseOrderCount", label: "Open POs", accent: "from-violet-500/20 to-transparent", icon: PackageSearch },
  { key: "pendingDeliveryCount", label: "Pending Deliveries", accent: "from-amber-500/20 to-transparent", icon: PackageSearch },
  { key: "outstandingInvoiceCount", label: "Outstanding Invoices", accent: "from-orange-500/20 to-transparent", icon: ReceiptText },
  { key: "overdueInvoiceCount", label: "Overdue Invoices", accent: "from-rose-500/20 to-transparent", icon: ReceiptText },
  { key: "activeVendorCount", label: "Active Vendors", accent: "from-teal-500/20 to-transparent", icon: Building2 },
] as const;

/** Pure SVG bar chart — no chart library, responsive via viewBox. */
function SpendChart({ entries }: { entries: MonthlySpendEntry[] }) {
  if (!entries.length) {
    return (
      <p className="mt-6 text-sm text-slate-400">
        No spend recorded yet. Generate a purchase order to see the trend here.
      </p>
    );
  }
  const width = 720;
  const height = 220;
  const pad = { top: 16, right: 12, bottom: 28, left: 12 };
  const max = Math.max(...entries.map((e) => Number(e.amount ?? 0)), 1);
  const slot = (width - pad.left - pad.right) / entries.length;
  const barW = Math.min(slot * 0.55, 48);

  return (
    <svg viewBox={`0 0 ${width} ${height}`} className="mt-6 w-full" role="img" aria-label="Monthly procurement spend">
      {[0.25, 0.5, 0.75, 1].map((t) => (
        <line
          key={t}
          x1={pad.left}
          x2={width - pad.right}
          y1={pad.top + (height - pad.top - pad.bottom) * t}
          y2={pad.top + (height - pad.top - pad.bottom) * t}
          stroke="#1e293b"
          strokeDasharray="3 5"
        />
      ))}
      {entries.map((entry, i) => {
        const value = Number(entry.amount ?? 0);
        const h = ((height - pad.top - pad.bottom) * value) / max;
        const x = pad.left + i * slot + (slot - barW) / 2;
        const y = height - pad.bottom - h;
        return (
          <g key={i}>
            <defs>
              <linearGradient id={`bar-${i}`} x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" stopColor="#34d399" />
                <stop offset="100%" stopColor="#0f766e" />
              </linearGradient>
            </defs>
            <rect
              x={x}
              y={y}
              width={barW}
              height={Math.max(h, 2)}
              rx={6}
              fill={`url(#bar-${i})`}
            >
              <title>{`${entry.month ?? ""}: ${formatMoney(value)}`}</title>
            </rect>
            <text
              x={x + barW / 2}
              y={height - 8}
              textAnchor="middle"
              className="fill-slate-500"
              fontSize={11}
            >
              {(entry.month ?? "").slice(0, 7)}
            </text>
          </g>
        );
      })}
    </svg>
  );
}

export default function DashboardPage() {
  const { data: user } = useQuery({ queryKey: ["currentUser"], queryFn: getCurrentUser });
  const health = useQuery({ queryKey: ["health"], queryFn: getHealth });
  const summary = useDashboardSummary();
  const monthly = useMonthlySpend();

  const values: Record<string, string> = summary.data
    ? {
        totalSpend: formatMoney(summary.data.totalSpend),
        activeRfqCount: String(summary.data.activeRfqCount),
        openPurchaseOrderCount: String(summary.data.openPurchaseOrderCount),
        pendingDeliveryCount: String(summary.data.pendingDeliveryCount),
        outstandingInvoiceCount: String(summary.data.outstandingInvoiceCount),
        overdueInvoiceCount: String(summary.data.overdueInvoiceCount),
        activeVendorCount: String(summary.data.activeVendorCount),
      }
    : {};

  return (
    <div className="mx-auto max-w-7xl">
      <header className="flex flex-wrap items-center justify-between gap-6 rounded-2xl border border-slate-800 bg-gradient-to-r from-slate-900 via-slate-900/80 to-slate-950 p-6">
        <Logo />
        <div className="text-right">
          {user && (
            <p className="text-sm text-slate-300">
              Welcome back, <span className="font-medium text-white">{user.firstName}</span>
            </p>
          )}
          <p className="inline-flex items-center gap-1 text-xs text-emerald-400">
            Live
            <span className="relative flex h-2 w-2">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-60" />
              <span className="relative inline-flex h-2 w-2 rounded-full bg-emerald-400" />
            </span>
          </p>
        </div>
      </header>

      {health.isError && (
        <p className="mt-4 rounded-xl border border-amber-800 bg-amber-950/40 px-4 py-3 text-sm text-amber-300" role="alert">
          Backend unavailable. Start PostgreSQL and the API with Docker Compose.
        </p>
      )}

      <section className="mb-8 mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {KPI_CARDS.map(({ key, label, accent, icon: Icon }) => (
          <article
            key={key}
            className="group relative overflow-hidden rounded-2xl border border-slate-800 bg-slate-900/50 p-5 transition hover:border-slate-600"
          >
            <div className={`pointer-events-none absolute inset-0 bg-gradient-to-br ${accent} opacity-60`} />
            <div className="relative flex items-start justify-between">
              <p className="text-sm text-slate-400">{label}</p>
              <Icon className="h-4 w-4 text-slate-500 transition group-hover:text-emerald-400" />
            </div>
            <p className="relative mt-3 text-2xl font-semibold tracking-tight text-white tabular-nums">
              {summary.isLoading ? "…" : values[key] ?? "—"}
            </p>
          </article>
        ))}
      </section>

      <section className="rounded-2xl border border-slate-800 bg-slate-900/50 p-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-sm font-medium uppercase tracking-wider text-slate-400">
              Procurement Spend Trend
            </h2>
            <p className="mt-1 text-xs text-slate-600">Purchase-order totals per month</p>
          </div>
          <ArrowUpRight className="h-4 w-4 text-emerald-400" />
        </div>
        {monthly.isLoading && <p className="mt-6 text-sm text-slate-400">Loading chart…</p>}
        {monthly.data && <SpendChart entries={monthly.data} />}
        {!monthly.isLoading && !monthly.isError && (!monthly.data || monthly.data.length === 0) && (
          <p className="mt-6 text-sm text-slate-400">
            No spend recorded yet — generate a purchase order to populate this chart.
          </p>
        )}
      </section>
    </div>
  );
}
