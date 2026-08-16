import Link from "next/link";
import {
  ArrowRight,
  BarChart3,
  FileText,
  Package,
  Shield,
  Truck,
} from "lucide-react";

const lifecycle = [
  "Purchase Requirement",
  "RFQ",
  "Quotations",
  "Comparison",
  "Approval",
  "Purchase Order",
  "Delivery",
  "Invoice",
  "Payment",
  "Vendor Performance",
];

const modules = [
  {
    icon: FileText,
    title: "RFQ & Quotation Comparison",
    description:
      "Normalize vendor bids side-by-side with weighted evaluation scores.",
  },
  {
    icon: Package,
    title: "Purchase Orders",
    description:
      "Issue POs from awarded quotations with full audit trail.",
  },
  {
    icon: Truck,
    title: "Delivery Tracking",
    description:
      "Support partial deliveries and proof-of-delivery records.",
  },
  {
    icon: Shield,
    title: "Three-Way Matching",
    description:
      "Match PO, goods received, and invoice before payment approval.",
  },
  {
    icon: BarChart3,
    title: "Vendor Performance",
    description:
      "Score delivery, quality, pricing, responsiveness, and fulfilment.",
  },
];

export default function Home() {
  return (
    <div className="flex flex-1 flex-col">
      <header className="border-b border-slate-800/80 bg-slate-950/80 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-5">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-500/15 text-emerald-400 ring-1 ring-emerald-500/30">
              VS
            </div>
            <div>
              <p className="text-lg font-semibold tracking-tight">VendorSphere</p>
              <p className="text-xs text-slate-400">B2B Procurement Platform</p>
            </div>
          </div>
          <nav className="flex items-center gap-3">
            <Link
              href="/login"
              className="rounded-lg px-4 py-2 text-sm text-slate-300 transition hover:text-white"
            >
              Sign in
            </Link>
            <Link
              href="/register"
              className="rounded-lg bg-emerald-500 px-4 py-2 text-sm font-medium text-slate-950 transition hover:bg-emerald-400"
            >
              Get started
            </Link>
          </nav>
        </div>
      </header>

      <main className="flex-1">
        <section className="mx-auto max-w-6xl px-6 py-20">
          <div className="max-w-3xl">
            <p className="mb-4 inline-flex rounded-full border border-emerald-500/20 bg-emerald-500/10 px-3 py-1 text-xs font-medium uppercase tracking-wider text-emerald-300">
              Version 1.0 · Placement-ready SaaS
            </p>
            <h1 className="text-4xl font-semibold tracking-tight text-white sm:text-5xl">
              Procurement lifecycle management, from requirement to vendor score.
            </h1>
            <p className="mt-6 text-lg leading-8 text-slate-400">
              VendorSphere replaces spreadsheets, email threads, and PDF
              quotations with a structured digital workflow for RFQs,
              quotation comparison, purchase orders, deliveries, invoices, and
              vendor performance.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              <Link
                href="/dashboard"
                className="inline-flex items-center gap-2 rounded-xl bg-emerald-500 px-5 py-3 text-sm font-medium text-slate-950 transition hover:bg-emerald-400"
              >
                Open dashboard
                <ArrowRight className="h-4 w-4" />
              </Link>
              <a
                href="http://localhost:8080/swagger-ui.html"
                className="inline-flex items-center gap-2 rounded-xl border border-slate-700 px-5 py-3 text-sm font-medium text-slate-200 transition hover:border-slate-500 hover:bg-slate-900"
                target="_blank"
                rel="noreferrer"
              >
                API documentation
              </a>
            </div>
          </div>
        </section>

        <section className="border-y border-slate-800 bg-slate-900/40">
          <div className="mx-auto max-w-6xl px-6 py-14">
            <h2 className="text-sm font-medium uppercase tracking-wider text-slate-400">
              Procurement lifecycle
            </h2>
            <div className="mt-6 flex flex-wrap gap-2">
              {lifecycle.map((step, index) => (
                <div key={step} className="flex items-center gap-2">
                  <span className="rounded-full border border-slate-700 bg-slate-900 px-3 py-1.5 text-sm text-slate-200">
                    {step}
                  </span>
                  {index < lifecycle.length - 1 && (
                    <ArrowRight className="hidden h-4 w-4 text-slate-600 sm:block" />
                  )}
                </div>
              ))}
            </div>
          </div>
        </section>

        <section className="mx-auto max-w-6xl px-6 py-16">
          <div className="mb-10 max-w-2xl">
            <h2 className="text-2xl font-semibold text-white">
              Built for real procurement workflows
            </h2>
            <p className="mt-3 text-slate-400">
              Complex relational modeling, transactional business logic, RBAC,
              state management, and three-way invoice matching.
            </p>
          </div>
          <div className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
            {modules.map(({ icon: Icon, title, description }) => (
              <article
                key={title}
                className="rounded-2xl border border-slate-800 bg-slate-900/50 p-6"
              >
                <div className="mb-4 flex h-11 w-11 items-center justify-center rounded-xl bg-emerald-500/10 text-emerald-400">
                  <Icon className="h-5 w-5" />
                </div>
                <h3 className="text-lg font-medium text-white">{title}</h3>
                <p className="mt-2 text-sm leading-6 text-slate-400">
                  {description}
                </p>
              </article>
            ))}
          </div>
        </section>
      </main>

      <footer className="border-t border-slate-800">
        <div className="mx-auto flex max-w-6xl flex-col gap-2 px-6 py-8 text-sm text-slate-500 sm:flex-row sm:items-center sm:justify-between">
          <p>VendorSphere · Spring Boot · Next.js · PostgreSQL</p>
          <p>Phase 1 foundation scaffold</p>
        </div>
      </footer>
    </div>
  );
}
