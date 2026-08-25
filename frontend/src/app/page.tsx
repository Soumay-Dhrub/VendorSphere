"use client";

/**
 * VendorSphere landing page: animated hero with a live lifecycle pipeline
 * (a pulse travelling the flow), drifting orbs, masked grid and feature cards.
 * Pure CSS/SVG animation, no dependencies.
 */

import Link from "next/link";
import { Boxes, FileText, Gauge, PackageCheck, ReceiptText, Scale3d, ShieldCheck, Truck } from "lucide-react";

const STAGES = [
  { label: "Requirement", icon: FileText },
  { label: "RFQ", icon: Gauge },
  { label: "Quotes", icon: Scale3d },
  { label: "Award", icon: ShieldCheck },
  { label: "PO", icon: PackageCheck },
  { label: "Delivery", icon: Truck },
  { label: "Invoice", icon: ReceiptText },
  { label: "Score", icon: Boxes },
];

const MODULES = [
  {
    icon: Gauge,
    title: "Weighted quotation scoring",
    body: "Price, delivery, warranty and history blended into one comparable score with a recommended bid — the manager still decides.",
    accent: "from-sky-500/20",
  },
  {
    icon: ShieldCheck,
    title: "Three-way matching",
    body: "Every invoice is checked against the purchase order and goods received before finance pays. Exceptions are explicit.",
    accent: "from-emerald-500/20",
  },
  {
    icon: Truck,
    title: "Partial deliveries, tracked",
    body: "Receive in instalments; ordered vs received vs outstanding stays honest per line, and late suppliers are flagged daily.",
    accent: "from-amber-500/20",
  },
  {
    icon: Boxes,
    title: "Vendor performance engine",
    body: "Delivery, quality, pricing, responsiveness and fulfilment roll into a monthly score that follows every vendor into the next RFQ.",
    accent: "from-violet-500/20",
  },
];

function Pipeline() {
  return (
    <ol className="flex flex-wrap items-center justify-center gap-y-4" aria-label="Procurement lifecycle">
      {STAGES.map((stage, i) => (
        <li key={stage.label} className="flex items-center">
          <div className="group relative">
            <div className="flex h-14 w-14 flex-col items-center justify-center rounded-2xl border border-slate-700 bg-slate-900/80 transition group-hover:border-emerald-500/60 group-hover:bg-slate-800/80">
              <stage.icon className="h-5 w-5 text-emerald-400" />
              <span className="mt-0.5 text-[10px] text-slate-400">{stage.label}</span>
            </div>
            <span
              aria-hidden
              className="pointer-events-none absolute inset-0 rounded-2xl border border-emerald-400/60 opacity-0"
              style={{ animation: `pipeline-glow 4s ease-in-out ${i * 0.5}s infinite` }}
            />
          </div>
          {i < STAGES.length - 1 && (
            <span className="relative mx-1 hidden h-px w-8 bg-gradient-to-r from-emerald-500/50 to-slate-700 sm:block">
              <span
                aria-hidden
                className="absolute top-1/2 h-1.5 w-1.5 -translate-y-1/2 rounded-full bg-emerald-300 shadow-[0_0_8px_2px_rgba(52,211,153,0.6)]"
                style={{ animation: `pipeline-pulse 4s linear ${i * 0.5}s infinite` }}
              />
            </span>
          )}
        </li>
      ))}
      <style>{`
        @keyframes pipeline-pulse {
          0% { left: 0; opacity: 0; }
          15% { opacity: 1; }
          85% { opacity: 1; }
          100% { left: calc(100% - 6px); opacity: 0; }
        }
        @keyframes pipeline-glow {
          0%, 100% { opacity: 0; transform: scale(1); }
          12% { opacity: 1; transform: scale(1.05); }
          30% { opacity: 0; transform: scale(1); }
        }
      `}</style>
    </ol>
  );
}

export default function Home() {
  return (
    <div className="min-h-screen bg-slate-950 text-slate-200">
      <div aria-hidden className="pointer-events-none fixed inset-0">
        <div
          className="absolute inset-0 opacity-[0.05]"
          style={{
            backgroundImage:
              "linear-gradient(#34d399 1px, transparent 1px), linear-gradient(90deg, #34d399 1px, transparent 1px)",
            backgroundSize: "48px 48px",
            maskImage: "radial-gradient(ellipse at 30% 20%, black 25%, transparent 70%)",
          }}
        />
        <div className="absolute left-[-15%] top-[-20%] h-[36rem] w-[36rem] animate-pulse rounded-full bg-emerald-500/10 blur-3xl" />
        <div
          className="absolute bottom-[-25%] right-[-10%] h-[40rem] w-[40rem] rounded-full bg-teal-500/10 blur-3xl"
          style={{ animation: "drift 14s ease-in-out infinite alternate" }}
        />
        <style>{`
          @keyframes drift {
            from { transform: translate(0, 0) scale(1); }
            to { transform: translate(-60px, 40px) scale(1.08); }
          }
        `}</style>
      </div>

      <header className="relative z-10 border-b border-slate-800/70 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-5">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-emerald-400 to-teal-600 shadow-lg shadow-emerald-500/20">
              <Boxes className="h-5 w-5 text-slate-950" strokeWidth={2.4} />
            </div>
            <p className="text-lg font-semibold tracking-tight text-white">
              Vendor<span className="text-emerald-400">Sphere</span>
            </p>
          </div>
          <nav className="flex items-center gap-3">
            <Link href="/login" className="rounded-lg px-4 py-2 text-sm text-slate-300 transition hover:text-white">
              Sign in
            </Link>
            <Link
              href="/register"
              className="rounded-lg bg-gradient-to-r from-emerald-500 to-teal-500 px-4 py-2 text-sm font-medium text-slate-950 transition hover:brightness-110"
            >
              Get started
            </Link>
          </nav>
        </div>
      </header>

      <main className="relative z-10">
        <section className="mx-auto max-w-6xl px-6 pb-16 pt-20 text-center">
          <p className="mx-auto mb-5 inline-flex items-center gap-2 rounded-full border border-emerald-500/20 bg-emerald-500/10 px-3 py-1 text-xs font-medium uppercase tracking-wider text-emerald-300">
            <span className="relative flex h-1.5 w-1.5">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-70" />
              <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-emerald-400" />
            </span>
            End-to-end procurement platform
          </p>
          <h1 className="mx-auto max-w-3xl text-4xl font-semibold leading-tight tracking-tight text-white sm:text-6xl">
            Source smarter.{" "}
            <span className="bg-gradient-to-r from-emerald-300 via-teal-200 to-emerald-300 bg-clip-text text-transparent">
              Pay with confidence.
            </span>
          </h1>
          <p className="mx-auto mt-6 max-w-2xl text-lg leading-8 text-slate-400">
            VendorSphere turns scattered spreadsheets and PDF quotes into one
            structured pipeline — RFQs, weighted comparison, purchase orders,
            deliveries, invoices and a vendor score that remembers everything.
          </p>
          <div className="mt-9 flex flex-wrap items-center justify-center gap-3">
            <Link
              href="/login"
              className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 px-6 py-3 text-sm font-medium text-slate-950 shadow-lg shadow-emerald-500/25 transition hover:brightness-110"
            >
              Launch the demo
            </Link>
            <a
              href="http://localhost:8080/swagger-ui.html"
              target="_blank"
              rel="noreferrer"
              className="rounded-xl border border-slate-700 px-6 py-3 text-sm font-medium text-slate-200 transition hover:border-slate-500 hover:bg-slate-900"
            >
              Explore the API
            </a>
          </div>
        </section>

        <section className="border-y border-slate-800/70 bg-slate-900/40 backdrop-blur">
          <div className="mx-auto max-w-5xl px-6 py-12">
            <p className="mb-8 text-center text-xs font-medium uppercase tracking-widest text-slate-500">
              The complete procurement pipeline, wired together
            </p>
            <Pipeline />
          </div>
        </section>

        <section className="mx-auto max-w-6xl px-6 py-20">
          <div className="grid gap-5 md:grid-cols-2">
            {MODULES.map(({ icon: Icon, title, body, accent }) => (
              <article
                key={title}
                className="group relative overflow-hidden rounded-2xl border border-slate-800 bg-slate-900/50 p-7 transition hover:border-slate-600"
              >
                <div className={`pointer-events-none absolute -right-16 -top-16 h-44 w-44 rounded-full bg-gradient-to-br ${accent} to-transparent blur-2xl transition group-hover:scale-125`} />
                <div className="relative">
                  <div className="mb-4 flex h-11 w-11 items-center justify-center rounded-xl bg-emerald-500/10 text-emerald-400 ring-1 ring-emerald-500/20">
                    <Icon className="h-5 w-5" />
                  </div>
                  <h3 className="text-lg font-medium text-white">{title}</h3>
                  <p className="mt-2 text-sm leading-6 text-slate-400">{body}</p>
                </div>
              </article>
            ))}
          </div>
        </section>

        <section className="mx-auto max-w-4xl px-6 pb-24">
          <div className="rounded-3xl border border-emerald-900/50 bg-gradient-to-r from-emerald-950/60 via-slate-900 to-slate-900 p-10 text-center">
            <h2 className="text-2xl font-semibold text-white">See the whole flow in one demo run.</h2>
            <p className="mx-auto mt-3 max-w-xl text-sm text-slate-400">
              Raise a request, invite vendors, compare scored quotations, award,
              receive, match and pay — then watch the vendor score move.
            </p>
            <Link
              href="/dashboard"
              className="mt-7 inline-block rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 px-7 py-3 text-sm font-medium text-slate-950 shadow-lg shadow-emerald-500/25 transition hover:brightness-110"
            >
              Open the dashboard
            </Link>
          </div>
        </section>
      </main>

      <footer className="relative z-10 border-t border-slate-800/70">
        <div className="mx-auto flex max-w-6xl flex-col gap-2 px-6 py-8 text-sm text-slate-500 sm:flex-row sm:items-center sm:justify-between">
          <p>© {new Date().getFullYear()} VendorSphere · Spring Boot · Next.js · PostgreSQL</p>
          <p>Source · Compare · Award · Receive · Match · Pay</p>
        </div>
      </footer>
    </div>
  );
}
