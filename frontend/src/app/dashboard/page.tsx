"use client";

import { useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
import {
  getCurrentUser,
  getDepartments,
  getHealth,
  getStoredUser,
  logout,
} from "@/lib/api";

const stats = [
  { label: "Active RFQs", value: "—" },
  { label: "Open POs", value: "—" },
  { label: "Pending Invoices", value: "—" },
  { label: "Active Vendors", value: "—" },
];

export default function DashboardPage() {
  const router = useRouter();
  const storedUser = getStoredUser();

  useEffect(() => {
    if (!storedUser) {
      router.replace("/login");
    }
  }, [storedUser, router]);

  const { data: health, isLoading: healthLoading, isError: healthError } = useQuery({
    queryKey: ["health"],
    queryFn: getHealth,
  });

  const { data: user } = useQuery({
    queryKey: ["currentUser"],
    queryFn: getCurrentUser,
    enabled: !!storedUser,
  });

  const { data: departments } = useQuery({
    queryKey: ["departments"],
    queryFn: getDepartments,
    enabled: !!storedUser,
  });

  async function handleLogout() {
    await logout();
    router.push("/login");
  }

  if (!storedUser) {
    return null;
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <header className="border-b border-slate-800 px-6 py-5">
        <div className="mx-auto flex max-w-6xl items-center justify-between">
          <div>
            <p className="text-lg font-semibold">Procurement Dashboard</p>
            <p className="text-sm text-slate-400">
              Welcome, {user?.firstName ?? storedUser.firstName}{" "}
              {user?.lastName ?? storedUser.lastName}
              {user?.roles?.length ? ` · ${user.roles.join(", ")}` : ""}
            </p>
          </div>
          <div className="flex items-center gap-4">
            <Link href="/" className="text-sm text-emerald-400 hover:text-emerald-300">
              Home
            </Link>
            <button
              onClick={handleLogout}
              className="rounded-lg border border-slate-700 px-3 py-1.5 text-sm text-slate-300 hover:border-slate-500"
            >
              Sign out
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto max-w-6xl px-6 py-10">
        <section className="mb-8 rounded-2xl border border-slate-800 bg-slate-900/50 p-6">
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
      </main>
    </div>
  );
}
