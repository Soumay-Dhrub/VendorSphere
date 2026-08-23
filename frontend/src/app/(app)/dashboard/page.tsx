"use client";

/**
 * Placeholder dashboard carried over from the Phase 2 scaffold. It now renders inside the
 * `(app)` shell, which owns the header, the sidebar, the notification bell, sign-out and
 * the redirect to /login, so this screen only renders content. Task 20.1 replaces it with
 * the dashboard of Requirement 35.1.
 */

import { useQuery } from "@tanstack/react-query";
import { getCurrentUser, getDepartments, getHealth } from "@/lib/api";

const stats = [
  { label: "Active RFQs", value: "—" },
  { label: "Open POs", value: "—" },
  { label: "Pending Invoices", value: "—" },
  { label: "Active Vendors", value: "—" },
];

export default function DashboardPage() {
  const { data: health, isLoading: healthLoading, isError: healthError } = useQuery({
    queryKey: ["health"],
    queryFn: getHealth,
  });

  const { data: user } = useQuery({
    queryKey: ["currentUser"],
    queryFn: getCurrentUser,
  });

  const { data: departments } = useQuery({
    queryKey: ["departments"],
    queryFn: getDepartments,
  });

  return (
    <div className="mx-auto max-w-6xl">
      <h1 className="text-lg font-semibold text-white">Procurement Dashboard</h1>
      {user && (
        <p className="mt-1 text-sm text-slate-400">
          Welcome, {user.firstName} {user.lastName}
        </p>
      )}

      <section className="mt-8 mb-8 rounded-2xl border border-slate-800 bg-slate-900/50 p-6">
        <h2 className="text-sm font-medium uppercase tracking-wider text-slate-400">
          API Status
        </h2>
        {healthLoading && <p className="mt-3 text-slate-300">Checking backend...</p>}
        {healthError && (
          <p className="mt-3 text-amber-300">
            Backend unavailable. Start PostgreSQL and the API with Docker Compose.
          </p>
        )}
        {health?.data && (
          <div className="mt-3 flex flex-wrap gap-4 text-sm">
            <span className="rounded-full bg-emerald-500/10 px-3 py-1 text-emerald-300">
              {health.data.status}
            </span>
            <span className="text-slate-400">{health.data.service}</span>
            <span className="text-slate-400">v{health.data.version}</span>
          </div>
        )}
      </section>

      <section className="mb-8 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
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

      <section className="rounded-2xl border border-slate-800 bg-slate-900/50 p-6">
        <h2 className="text-sm font-medium uppercase tracking-wider text-slate-400">
          Departments
        </h2>
        {departments && departments.length > 0 ? (
          <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {departments.map((dept) => (
              <div
                key={dept.id}
                className="rounded-xl border border-slate-800 bg-slate-950/50 px-4 py-3"
              >
                <p className="font-medium text-white">{dept.name}</p>
                <p className="text-xs text-slate-500">{dept.code ?? "No code"}</p>
              </div>
            ))}
          </div>
        ) : (
          <p className="mt-3 text-sm text-slate-400">No departments found.</p>
        )}
      </section>
    </div>
  );
}
