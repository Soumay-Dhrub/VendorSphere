"use client";

/**
 * VendorSphere B2B SaaS landing page.
 * Sections: sticky nav, hero with product mock, problem, lifecycle pipeline,
 * features bento, comparison showcase, three-way matching, vendor scorecard,
 * roles, analytics preview, security, CTA, footer.
 * Reuses the existing design system (slate/emerald Tailwind tokens, lucide icons)
 * and keeps the animated pipeline from the previous iteration.
 */

import Link from "next/link";
import {
  ArrowRight, BarChart3, Boxes, Building2, CheckCircle2, ClipboardList,
  FileText, Gauge, GitCompareArrows, Landmark, LineChart, PackageCheck,
  ReceiptText, Scale3d, ShieldCheck, Truck, UserCog, Users, XCircle,
} from "lucide-react";

const NAV = [
  ["Product", "#product"],
  ["Solutions", "#solutions"],
  ["How It Works", "#how-it-works"],
  ["Features", "#features"],
  ["Security", "#security"],
] as const;

const STAGES = [
  { label: "Purchase Request", icon: FileText },
  { label: "RFQ", icon: Gauge },
  { label: "Quotation", icon: Scale3d },
  { label: "Compare", icon: GitCompareArrows },
  { label: "Purchase Order", icon: PackageCheck },
  { label: "Delivery", icon: Truck },
  { label: "Invoice", icon: ReceiptText },
  { label: "Payment", icon: Landmark },
];

const PAINS = [
  { title: "Scattered vendor information", body: "Profiles, contacts, documents and supplier history end up fragmented across inboxes and drives." },
  { title: "Manual quotation comparison", body: "Teams hand-compare pricing, delivery terms, warranties and history — slowly and inconsistently." },
  { title: "Poor purchase visibility", body: "Nobody can say where a request, PO, delivery or invoice currently stands without asking around." },
  { title: "Limited accountability", body: "Decisions lack a centralized history; who approved what, when, and why is hard to answer later." },
];

const FEATURES = [
  { icon: Building2, title: "Vendor Management", body: "Profiles, contacts, compliance documents, categories and lifecycle status — with expiry tracking.", wide: true },
  { icon: FileText, title: "RFQ Management", body: "Create RFQs from approved requests and invite multiple vendors to bid on the same terms." },
  { icon: GitCompareArrows, title: "Quotation Comparison", body: "Price, delivery, warranty, terms and supplier performance, normalized side by side." },
  { icon: PackageCheck, title: "Purchase Orders", body: "Generate orders straight from the winning quotation and track their lifecycle to closure." },
  { icon: Truck, title: "Delivery Tracking", body: "Record complete and partial deliveries with damaged and rejected quantities per line." },
  { icon: ReceiptText, title: "Invoice Management", body: "The full invoice lifecycle from submission through review to payment status." },
  { icon: ShieldCheck, title: "Three-Way Matching", body: "Purchase order vs goods received vs invoice — discrepancies surface before payment.", wide: true },
  { icon: LineChart, title: "Vendor Performance", body: "Delivery, quality, pricing, responsiveness and fulfilment scored into one comparable number." },
  { icon: BarChart3, title: "Procurement Analytics", body: "Spend, vendors, outstanding invoices, open RFQs and cycle performance at a glance." },
];

const ROLES = [
  { icon: Gauge, role: "Procurement", body: "Run RFQs, compare quotations, select winners and issue purchase orders." },
  { icon: Users, role: "Department Teams", body: "Raise purchase requirements and follow their status without chasing email." },
  { icon: Landmark, role: "Finance", body: "Review invoices, see match results, track outstanding amounts and payments." },
  { icon: Truck, role: "Vendors", body: "Respond to invitations, quote against line items and submit invoices." },
  { icon: UserCog, role: "Administrators", body: "Manage users, roles, departments, vendors and the audit trail." },
];

const COMPARISON_ROWS = [
  ["Total Price", "₹12,00,000", "₹11,50,000", "₹12,20,000"],
  ["Delivery", "7 days", "15 days", "5 days"],
  ["Warranty", "3 years", "2 years", "3 years"],
  ["Vendor Rating", "4.7", "4.2", "4.8"],
  ["Overall Score", "89", "76", "91"],
];

function SectionHeading({ eyebrow, title, sub }: { eyebrow?: string; title: string; sub?: string }) {
  return (
    <div className="mx-auto mb-12 max-w-2xl text-center">
      {eyebrow && (
        <p className="mb-3 text-xs font-medium uppercase tracking-widest text-emerald-400">{eyebrow}</p>
      )}
      <h2 className="text-3xl font-semibold tracking-tight text-white sm:text-4xl">{title}</h2>
      {sub && <p className="mt-4 leading-7 text-slate-400">{sub}</p>}
    </div>
  );
}

/** Animated procurement pipeline (kept from the previous iteration). */
function Pipeline() {
  return (
    <ol className="flex flex-col items-stretch justify-center gap-3 md:flex-row md:items-center">
      {STAGES.map((stage, i) => (
        <li key={stage.label} className="flex items-center">
          <div className="group relative flex flex-1 items-center gap-3 rounded-xl border border-slate-800 bg-slate-900/70 px-4 py-3 transition hover:border-emerald-500/50 md:flex-none md:flex-col md:gap-1 md:px-3">
            <div className="relative">
              <stage.icon className="h-5 w-5 text-emerald-400" />
              <span
                aria-hidden
                className="absolute inset-0 rounded-full opacity-0"
                style={{ animation: `pipeline-glow 4s ease-in-out ${i * 0.5}s infinite` }}
              />
            </div>
            <span className="text-xs text-slate-300">{stage.label}</span>
          </div>
          {i < STAGES.length - 1 && (
            <span className="relative mx-1 hidden h-px w-8 shrink-0 bg-gradient-to-r from-emerald-500/50 to-slate-700 md:block">
              <span
                aria-hidden
                className="absolute top-1/2 h-1.5 w-1.5 -translate-y-1/2 rounded-full bg-emerald-300 shadow-[0_0_8px_2px_rgba(52,211,153,0.6)]"
                style={{ animation: `pipeline-pulse 4s linear ${i * 0.5}s infinite` }}
              />
            </span>
          )}
          {i < STAGES.length - 1 && (
            <ArrowRight aria-hidden className="ml-auto h-4 w-4 rotate-90 text-slate-600 md:hidden" />
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
          0%, 100% { box-shadow: 0 0 0 0 rgba(52,211,153,0); }
          12% { box-shadow: 0 0 18px 2px rgba(52,211,153,0.35); }
          30% { box-shadow: 0 0 0 0 rgba(52,211,153,0); }
        }
      `}</style>
    </ol>
  );
}

/** Hero product preview: a static, realistic mini-dashboard. */
function ProductMock() {
  const kpis = [
    ["Procurement Spend", "₹48.2L"], ["Active Vendors", "24"],
    ["Open RFQs", "6"], ["Pending Deliveries", "9"],
    ["Outstanding Invoices", "₹11.6L"], ["Vendor Performance", "87"],
  ];
  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/80 p-4 shadow-2xl shadow-black/50 backdrop-blur">
      <div className="flex items-center gap-1.5 pb-3">
        <span className="h-2.5 w-2.5 rounded-full bg-slate-700" />
        <span className="h-2.5 w-2.5 rounded-full bg-slate-700" />
        <span className="h-2.5 w-2.5 rounded-full bg-slate-700" />
        <span className="ml-3 rounded-md bg-slate-950 px-3 py-1 text-[10px] text-slate-500">vendorsphere / dashboard</span>
      </div>
      <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-3">
        {kpis.map(([label, value]) => (
          <div key={label} className="rounded-lg border border-slate-800 bg-slate-950/70 p-3">
            <p className="text-[10px] uppercase tracking-wide text-slate-500">{label}</p>
            <p className="mt-1 text-sm font-semibold tabular-nums text-white">{value}</p>
          </div>
        ))}
      </div>
      <div className="mt-2.5 grid gap-2.5 sm:grid-cols-5">
        <div className="flex h-28 items-end gap-1.5 rounded-lg border border-slate-800 bg-slate-950/70 p-3 sm:col-span-3" aria-label="Monthly spend chart placeholder">
          {[38, 55, 42, 70, 58, 82, 66, 90].map((h, i) => (
            <div key={i} className="flex-1 rounded-sm bg-gradient-to-t from-teal-600/60 to-emerald-400/80" style={{ height: `${h}%` }} />
          ))}
        </div>
        <div className="space-y-2 rounded-lg border border-slate-800 bg-slate-950/70 p-3 sm:col-span-2">
          {[["Acme Supplies", "87"], ["Nova Traders", "81"], ["Kiran & Co.", "74"]].map(([name, score]) => (
            <div key={name}>
              <div className="flex justify-between text-[10px] text-slate-400"><span>{name}</span><span>{score}</span></div>
              <div className="mt-1 h-1.5 rounded-full bg-slate-800">
                <div className="h-full rounded-full bg-gradient-to-r from-emerald-400 to-teal-500" style={{ width: `${Number(score)}%` }} />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default function Home() {
  return (
    <div className="min-h-screen bg-slate-950 text-slate-200">
      {/* Navbar */}
      <header className="sticky top-0 z-40 border-b border-slate-800/70 bg-slate-950/85 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
          <Link href="/" className="flex items-center gap-2.5">
            <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-emerald-400 to-teal-600 shadow-lg shadow-emerald-500/20">
              <Boxes className="h-4.5 w-4.5 text-slate-950" strokeWidth={2.4} />
            </span>
            <span className="text-base font-semibold tracking-tight text-white">
              Vendor<span className="text-emerald-400">Sphere</span>
            </span>
          </Link>
          <nav aria-label="Main" className="hidden items-center gap-7 lg:flex">
            {NAV.map(([label, href]) => (
              <a key={href} href={href} className="text-sm text-slate-400 transition hover:text-white">{label}</a>
            ))}
          </nav>
          <div className="flex items-center gap-2.5">
            <Link href="/login" className="rounded-lg px-3.5 py-2 text-sm text-slate-300 transition hover:text-white">Sign In</Link>
            <Link href="/register" className="rounded-lg bg-gradient-to-r from-emerald-500 to-teal-500 px-4 py-2 text-sm font-medium text-slate-950 shadow-lg shadow-emerald-500/20 transition hover:brightness-110">Get Started</Link>
          </div>
        </div>
      </header>

      <main>
        {/* Hero */}
        <section id="product" className="relative overflow-hidden">
          <div aria-hidden className="pointer-events-none absolute inset-0">
            <div
              className="absolute inset-0 opacity-[0.05]"
              style={{
                backgroundImage:
                  "linear-gradient(#34d399 1px, transparent 1px), linear-gradient(90deg, #34d399 1px, transparent 1px)",
                backgroundSize: "48px 48px",
                maskImage: "radial-gradient(ellipse at 30% 15%, black 25%, transparent 70%)",
              }}
            />
            <div className="absolute left-[-12%] top-[-18%] h-[34rem] w-[34rem] animate-pulse rounded-full bg-emerald-500/10 blur-3xl" />
          </div>
          <div className="relative mx-auto max-w-6xl px-6 pt-20 text-center">
            <p className="mx-auto mb-5 inline-flex items-center gap-2 rounded-full border border-emerald-500/20 bg-emerald-500/10 px-3 py-1 text-xs font-medium uppercase tracking-wider text-emerald-300">
              Vendor &amp; procurement management platform
            </p>
            <h1 className="mx-auto max-w-3xl text-4xl font-semibold leading-tight tracking-tight text-white sm:text-5xl">
              Procurement, from request to payment —{" "}
              <span className="bg-gradient-to-r from-emerald-300 to-teal-200 bg-clip-text text-transparent">in one place.</span>
            </h1>
            <p className="mx-auto mt-5 max-w-2xl text-base leading-7 text-slate-400 sm:text-lg">
              Manage vendors, RFQs, quotations, purchase orders, deliveries,
              invoices, and supplier performance through one centralized
              procurement workspace.
            </p>
            <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
              <Link
                href="/register"
                className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 px-6 py-3 text-sm font-medium text-slate-950 shadow-lg shadow-emerald-500/25 transition hover:brightness-110"
              >
                Get Started <ArrowRight className="h-4 w-4" />
              </Link>
              <a
                href="#how-it-works"
                className="rounded-xl border border-slate-700 px-6 py-3 text-sm font-medium text-slate-200 transition hover:border-slate-500 hover:bg-slate-900"
              >
                See How It Works
              </a>
            </div>

            {/* Product mock */}
            <div className="mx-auto mt-14 max-w-4xl">
              <ProductMock />
              <p className="mt-3 text-xs text-slate-600">Illustrative workspace preview</p>
            </div>
          </div>
        </section>

        {/* Problem */}
        <section id="solutions" className="border-y border-slate-800/70 bg-slate-900/40">
          <div className="mx-auto max-w-6xl px-6 py-20">
            <SectionHeading
              eyebrow="The problem"
              title="Procurement shouldn't live across spreadsheets, emails, and PDFs."
              sub="Fragmented tools make sourcing slow, opaque and hard to audit. VendorSphere consolidates the workflow without changing how your team works."
            />
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              {PAINS.map((pain) => (
                <article key={pain.title} className="rounded-2xl border border-slate-800 bg-slate-950/60 p-5">
                  <XCircle aria-hidden className="h-5 w-5 text-rose-400/80" />
                  <h3 className="mt-3 font-medium text-white">{pain.title}</h3>
                  <p className="mt-2 text-sm leading-6 text-slate-400">{pain.body}</p>
                </article>
              ))}
            </div>
            <div className="mx-auto mt-10 flex max-w-3xl flex-wrap items-center justify-center gap-3 text-sm">
              {["Email threads", "Excel sheets", "PDF quotes", "Phone calls"].map((tool) => (
                <span key={tool} className="rounded-full border border-slate-700 px-3.5 py-1.5 text-slate-400 line-through decoration-rose-400/60">{tool}</span>
              ))}
              <ArrowRight className="h-4 w-4 text-emerald-400" />
              <span className="rounded-full border border-emerald-500/30 bg-emerald-500/10 px-4 py-1.5 font-medium text-emerald-300">VendorSphere</span>
            </div>
          </div>
        </section>

        {/* Lifecycle */}
        <section id="how-it-works" className="mx-auto max-w-6xl px-6 py-20">
          <SectionHeading
            eyebrow="How it works"
            title="One connected procurement pipeline."
            sub="Every stage hands off cleanly to the next — with status, ownership and history preserved."
          />
          <Pipeline />
        </section>

        {/* Features bento */}
        <section id="features" className="border-y border-slate-800/70 bg-slate-900/40">
          <div className="mx-auto max-w-6xl px-6 py-20">
            <SectionHeading
              eyebrow="Features"
              title="Everything procurement needs, nothing it doesn't."
            />
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {FEATURES.map(({ icon: Icon, title, body, wide }) => (
                <article key={title} className={`group rounded-2xl border border-slate-800 bg-slate-950/60 p-6 transition hover:border-emerald-500/30 ${wide ? "sm:col-span-2" : ""}`}>
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-500/10 text-emerald-400 ring-1 ring-emerald-500/20">
                    <Icon className="h-4.5 w-4.5" />
                  </div>
                  <h3 className="mt-4 font-medium text-white">{title}</h3>
                  <p className="mt-2 text-sm leading-6 text-slate-400">{body}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        {/* Comparison showcase */}
        <section className="mx-auto max-w-6xl px-6 py-20">
          <SectionHeading
            eyebrow="Quotation comparison"
            title="Compare bids on equivalent terms."
            sub="The same request, normalized across every responding vendor — scores computed by the platform, decision made by you."
          />
          <div className="overflow-x-auto rounded-2xl border border-slate-800">
            <table className="w-full min-w-[640px] text-left text-sm">
              <caption className="sr-only">Example quotation comparison for three vendors</caption>
              <thead className="bg-slate-900/80 text-xs uppercase tracking-wider text-slate-400">
                <tr>
                  <th scope="col" className="px-5 py-4">Criteria</th>
                  <th scope="col" className="px-5 py-4">Vendor A</th>
                  <th scope="col" className="px-5 py-4">Vendor B</th>
                  <th scope="col" className="relative px-5 py-4 text-emerald-300">
                    <span className="absolute right-3 top-2 rounded-full bg-emerald-500/15 px-2 py-0.5 text-[10px] normal-case tracking-normal">Recommended</span>
                    Vendor C ★
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800 bg-slate-950/60">
                {COMPARISON_ROWS.map(([criteria, a, b, c]) => (
                  <tr key={criteria}>
                    <th scope="row" className="px-5 py-3.5 font-medium text-slate-300">{criteria}</th>
                    <td className={`px-5 py-3.5 tabular-nums ${criteria === "Total Price" ? "text-slate-200" : "text-slate-400"}`}>{a}</td>
                    <td className={`px-5 py-3.5 tabular-nums ${criteria === "Total Price" ? "text-slate-200" : "text-slate-400"}`}>{b}</td>
                    <td className="border-x-2 border-emerald-500/40 bg-emerald-950/20 px-5 py-3.5 font-medium tabular-nums text-emerald-200">{c}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className="mt-3 text-center text-xs text-slate-600">
            The platform recommends; an authorized procurement user makes the final selection with a recorded justification.
          </p>
        </section>

        {/* Three-way matching */}
        <section className="border-y border-slate-800/70 bg-slate-900/40">
          <div className="mx-auto max-w-6xl px-6 py-20">
            <SectionHeading
              eyebrow="Three-way matching"
              title="Catch invoice discrepancies before payment."
            />
            <div className="grid items-center gap-8 lg:grid-cols-2">
              <div className="space-y-3">
                {[["Purchase Order", ClipboardList], ["Goods Received", PackageCheck], ["Vendor Invoice", ReceiptText]].map(([label, Icon]: any) => (
                  <div key={label} className="flex items-center gap-3 rounded-xl border border-slate-800 bg-slate-950/60 px-4 py-3.5">
                    <Icon className="h-4.5 w-4.5 text-slate-400" />
                    <span className="font-medium text-white">{label}</span>
                  </div>
                ))}
                <div className="flex items-center justify-center py-1">
                  <span className="h-px w-12 bg-gradient-to-r from-transparent to-emerald-500/60" />
                  <ArrowRight className="mx-2 h-4 w-4 text-emerald-400" />
                  <span className="h-px w-12 bg-gradient-to-l from-transparent to-emerald-500/60" />
                </div>
                <div className="flex items-center gap-3 rounded-xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-4">
                  <ShieldCheck className="h-5 w-5 text-emerald-300" />
                  <span className="font-medium text-emerald-200">Three-Way Match</span>
                </div>
              </div>
              <div className="space-y-3">
                {["Quantity match", "Unit price match (± tolerance)", "Delivery confirmed", "Duplicate invoice check"].map((check) => (
                  <div key={check} className="flex items-center gap-3 rounded-lg border border-slate-800 bg-slate-950/60 px-4 py-3 text-sm">
                    <CheckCircle2 className="h-4 w-4 text-emerald-400" /> {check}
                  </div>
                ))}
                <div className="rounded-xl border border-amber-800/60 bg-amber-950/30 px-4 py-3.5">
                  <p className="flex items-center gap-2 text-sm font-medium text-amber-300">
                    <XCircle className="h-4 w-4" /> Price mismatch detected
                  </p>
                  <p className="mt-1 text-xs leading-5 text-amber-200/70">
                    PO ₹1,000/unit · Invoice ₹1,200/unit → blocked pending review or override with justification.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Vendor performance */}
        <section className="mx-auto max-w-6xl px-6 py-20">
          <div className="grid items-center gap-10 lg:grid-cols-2">
            <div>
              <p className="text-xs font-medium uppercase tracking-widest text-emerald-400">Vendor performance</p>
              <h2 className="mt-3 text-3xl font-semibold tracking-tight text-white sm:text-4xl">
                Source on history, not intuition.
              </h2>
              <p className="mt-4 leading-7 text-slate-400">
                Make sourcing decisions using historical supplier performance,
                not intuition alone. Every award, receipt and invoice feeds the
                next month&apos;s scorecard automatically.
              </p>
            </div>
            <div className="rounded-2xl border border-slate-800 bg-slate-950/60 p-7">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-800 text-sm font-semibold text-white">AB</span>
                  <div>
                    <p className="font-medium text-white">ABC Technologies</p>
                    <p className="text-xs text-slate-500">Supplier since 2024</p>
                  </div>
                </div>
                <div className="text-right">
                  <p className="text-3xl font-semibold tabular-nums text-emerald-300">87<span className="text-base text-slate-500">/100</span></p>
                  <p className="text-[10px] uppercase tracking-wide text-slate-500">Overall</p>
                </div>
              </div>
              <div className="mt-6 space-y-3.5">
                {[["Delivery", 92], ["Quality", 88], ["Pricing", 80], ["Responsiveness", 91], ["Fulfilment", 85]].map(([metric, score]) => (
                  <div key={metric as string}>
                    <div className="flex justify-between text-xs text-slate-400">
                      <span>{metric}</span><span className="tabular-nums">{score}</span>
                    </div>
                    <div className="mt-1.5 h-2 rounded-full bg-slate-800">
                      <div
                        className="h-full rounded-full bg-gradient-to-r from-emerald-400 to-teal-500 transition-all duration-700"
                        style={{ width: `${score}%` }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* Roles */}
        <section className="border-y border-slate-800/70 bg-slate-900/40">
          <div className="mx-auto max-w-6xl px-6 py-20">
            <SectionHeading
              eyebrow="Role-based workspace"
              title="A different view for every responsibility."
            />
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
              {ROLES.map(({ icon: Icon, role, body }) => (
                <article key={role} className="rounded-2xl border border-slate-800 bg-slate-950/60 p-5">
                  <Icon className="h-5 w-5 text-emerald-400" />
                  <h3 className="mt-3 font-medium text-white">{role}</h3>
                  <p className="mt-2 text-sm leading-6 text-slate-400">{body}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        {/* Security */}
        <section id="security" className="mx-auto max-w-6xl px-6 py-20">
          <SectionHeading
            eyebrow="Security"
            title="Built with control and accountability in mind."
          />
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            {[
              ["Role-Based Access", "Users only access permitted functionality, enforced server-side."],
              ["Secure Authentication", "JWT-based protected APIs with refresh-token rotation."],
              ["Audit Trail", "Critical procurement actions remain traceable to actor and time."],
              ["Backend Validation", "Business rules are enforced in the API, never only in the client."],
            ].map(([title, body]) => (
              <article key={title} className="rounded-2xl border border-slate-800 bg-slate-950/60 p-6">
                <ShieldCheck className="h-5 w-5 text-emerald-400" />
                <h3 className="mt-3 font-medium text-white">{title}</h3>
                <p className="mt-2 text-sm leading-6 text-slate-400">{body}</p>
              </article>
            ))}
          </div>
        </section>

        {/* Final CTA */}
        <section className="mx-auto max-w-4xl px-6 pb-24">
          <div className="rounded-3xl border border-emerald-900/50 bg-gradient-to-r from-emerald-950/60 via-slate-900 to-slate-900 p-10 text-center">
            <h2 className="text-2xl font-semibold text-white sm:text-3xl">
              Bring your procurement workflow into one workspace.
            </h2>
            <p className="mx-auto mt-3 max-w-xl text-sm leading-6 text-slate-400">
              From vendor sourcing to invoice verification, VendorSphere gives
              teams a structured way to manage the complete procurement lifecycle.
            </p>
            <div className="mt-7 flex flex-wrap items-center justify-center gap-3">
              <Link href="/register" className="rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 px-7 py-3 text-sm font-medium text-slate-950 shadow-lg shadow-emerald-500/25 transition hover:brightness-110">
                Get Started
              </Link>
              <Link href="/login" className="rounded-xl border border-slate-700 px-7 py-3 text-sm font-medium text-slate-200 transition hover:border-slate-500 hover:bg-slate-900">
                Sign In
              </Link>
            </div>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-800/70">
        <div className="mx-auto grid max-w-6xl gap-10 px-6 py-14 sm:grid-cols-2 lg:grid-cols-5">
          <div className="lg:col-span-2">
            <div className="flex items-center gap-2.5">
              <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-emerald-400 to-teal-600">
                <Boxes className="h-4 w-4 text-slate-950" strokeWidth={2.4} />
              </span>
              <span className="font-semibold text-white">VendorSphere</span>
            </div>
            <p className="mt-3 max-w-xs text-sm text-slate-500">
              Vendor and procurement management, simplified.
            </p>
          </div>
          {[
            ["Product", ["Features", "Procurement", "Vendor Management", "Analytics"]],
            ["Resources", ["Documentation", "API Documentation"]],
            ["Legal", ["Privacy", "Terms"]],
          ].map(([heading, links]) => (
            <nav key={heading as string} aria-label={heading as string}>
              <p className="text-sm font-medium text-white">{heading}</p>
              <ul className="mt-3 space-y-2 text-sm text-slate-500">
                {(links as string[]).map((link) => (
                  <li key={link}>
                    {link === "API Documentation" ? (
                      <a href="http://localhost:8080/swagger-ui.html" target="_blank" rel="noreferrer" className="transition hover:text-slate-300">{link}</a>
                    ) : link === "Features" ? (
                      <a href="#features" className="transition hover:text-slate-300">{link}</a>
                    ) : (
                      <span className="cursor-default">{link}</span>
                    )}
                  </li>
                ))}
              </ul>
            </nav>
          ))}
        </div>
        <div className="border-t border-slate-800/70 py-5 text-center text-xs text-slate-600">
          © {new Date().getFullYear()} VendorSphere · Source · Compare · Award · Receive · Match · Pay
        </div>
      </footer>
    </div>
  );
}
