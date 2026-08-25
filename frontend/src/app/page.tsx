"use client";

/**
 * VendorSphere landing page with purposeful product animations:
 * live hero dashboard simulation, sequential lifecycle reveal, interactive
 * quotation comparison, three-way-match demo with mismatch toggle, role
 * workspace switcher, count-up scorecards and IntersectionObserver reveals.
 * All motion respects prefers-reduced-motion and runs once.
 */

import { useCallback, useEffect, useRef, useState } from "react";
import Link from "next/link";
import {
  ArrowRight, BarChart3, Boxes, Building2, CheckCircle2, ClipboardList,
  FileText, Gauge, GitCompareArrows, Landmark, LineChart, PackageCheck,
  ReceiptText, Scale3d, ShieldCheck, Truck, UserCog, Users, XCircle,
} from "lucide-react";

/* ---------- shared motion primitives ---------- */

/**
 * Client-side reduced-motion flag. Read inside an effect so the first
 * (server) render always matches the client, avoiding hydration mismatches.
 */
function usePrefersReducedMotion() {
  const [reduced, setReduced] = useState(false);
  useEffect(() => {
    const media = window.matchMedia("(prefers-reduced-motion: reduce)");
    const update = () => setReduced(media.matches);
    update();
    media.addEventListener("change", update);
    return () => media.removeEventListener("change", update);
  }, []);
  return reduced;
}

/** Fires once when the element enters the viewport. */
function useInView<T extends HTMLElement>(threshold = 0.25) {
  const reduced = usePrefersReducedMotion();
  const ref = useRef<T | null>(null);
  const [inView, setInView] = useState(false);
  useEffect(() => {
    const node = ref.current;
    if (!node) return;
    if (reduced) { setInView(true); return; }
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setInView(true);
          observer.disconnect();
        }
      },
      { threshold },
    );
    observer.observe(node);
    return () => observer.disconnect();
  }, [threshold]);
  return [ref, inView] as const;
}

/** Fade/slide reveal wrapper (Requirement 10). */
function Reveal({
  children,
  delay = 0,
  className = "",
}: {
  children: React.ReactNode;
  delay?: number;
  className?: string;
}) {
  const [ref, inView] = useInView<HTMLDivElement>(0.15);
  const reduced = usePrefersReducedMotion();
  return (
    <div
      ref={ref}
      className={className}
      style={{
        opacity: inView ? 1 : 0,
        transform: inView ? "translateY(0)" : "translateY(16px)",
        transition: reduced ? "none" : `opacity .55s ease ${delay}ms, transform .55s ease ${delay}ms`,
      }}
    >
      {children}
    </div>
  );
}

/** Counts a number up once, triggered by `active`. */
function useCountUp(target: number, active: boolean, duration = 1200) {
  const reduced = usePrefersReducedMotion();
  const [value, setValue] = useState(reduced ? target : 0);
  useEffect(() => {
    if (!active || reduced) return;
    let frame = 0;
    const start = performance.now();
    const tick = (now: number) => {
      const progress = Math.min((now - start) / duration, 1);
      setValue(Math.round(target * (1 - Math.pow(1 - progress, 3))));
      if (progress < 1) frame = requestAnimationFrame(tick);
    };
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [target, active, duration]);
  return value;
}

/* ---------- data ---------- */

const NAV = [
  ["Product", "#product"], ["Solutions", "#solutions"],
  ["How It Works", "#how-it-works"], ["Features", "#features"],
  ["Security", "#security"],
] as const;

const STAGES = [
  ["Purchase Request", ClipboardList], ["RFQ", Gauge], ["Quotation", FileText],
  ["Compare", GitCompareArrows], ["Purchase Order", PackageCheck],
  ["Delivery", Truck], ["Invoice", ReceiptText], ["Payment", Landmark],
] as const;

const PAINS = [
  ["Scattered vendor information", "Profiles, contacts, documents and supplier history end up fragmented across inboxes and drives."],
  ["Manual quotation comparison", "Teams hand-compare pricing, delivery terms, warranties and history — slowly and inconsistently."],
  ["Poor purchase visibility", "Nobody can say where a request, PO, delivery or invoice stands without asking around."],
  ["Limited accountability", "Decisions lack a centralized history; who approved what, when, and why is hard to answer later."],
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

const COMPARISON_ROWS = [
  { criteria: "Total Price", values: ["₹12,00,000", "₹11,50,000", "₹12,20,000"], tip: "Lowest total earns the full price component of the evaluation score." },
  { criteria: "Delivery", values: ["7 days", "15 days", "5 days"], tip: "Shorter committed delivery improves the delivery component of the vendor score." },
  { criteria: "Warranty", values: ["3 years", "2 years", "3 years"], tip: "Longer warranty raises the warranty component of the evaluation score." },
  { criteria: "Vendor Rating", values: ["4.7", "4.2", "4.8"], tip: "Historical supplier performance feeds the performance component." },
  { criteria: "Overall Score", values: ["89", "76", "91"], tip: "Weighted blend of price, delivery, warranty and performance." },
];

const ROLES = [
  { key: "procurement", label: "Procurement", icon: Gauge, body: "Run RFQs, compare quotations, select winners and issue purchase orders.", preview: ["RFQ-1042 · Open · 3 quotes", "Comparison ready — recommend Vendor C", "PO-2098 issued to Acme Supplies"] },
  { key: "department", label: "Department", icon: Users, body: "Raise purchase requirements and follow their status without chasing email.", preview: ["PR-118 Laptops · Approved", "PR-122 Monitors · Under review", "PR-115 Desks · Delivered"] },
  { key: "finance", label: "Finance", icon: Landmark, body: "Review invoices, see match results, track outstanding amounts and payments.", preview: ["INV-7821 · Matched · ₹11.5L", "INV-7790 · Price mismatch flagged", "Outstanding payables ₹11.6L"] },
  { key: "vendor", label: "Vendor", icon: Truck, body: "Respond to invitations, quote against line items and submit invoices.", preview: ["Invited: RFQ-1042 closes in 3 days", "Your quote for PO-2098 accepted", "Invoice INV-8810 submitted"] },
  { key: "admin", label: "Admin", icon: UserCog, body: "Manage users, roles, departments, vendors and the audit trail.", preview: ["User roles updated", "Vendor document expiring in 30 days", "Audit trail: award justification recorded"] },
];

/* ---------- animated hero dashboard simulation (Priority 4) ---------- */

type Activity = { id: number; text: string };

function HeroDashboard() {
  const reduced = usePrefersReducedMotion();
  const [ref, inView] = useInView<HTMLDivElement>(0.35);
  const [rfqs, setRfqs] = useState(11);
  const [deliveries, setDeliveries] = useState(5);
  const [score, setScore] = useState(84);
  const [invoiceStatus, setInvoiceStatus] = useState("Invoice Review");
  const [activities, setActivities] = useState<Activity[]>([
    { id: 0, text: "PO-2077 · Delivery received" },
  ]);

  useEffect(() => {
    if (!inView || reduced) return;
    const steps: [number, () => void][] = [
      [2500, () => { setRfqs(12); pushActivity("RFQ-1042 opened for bidding"); }],
      [5200, () => pushActivity("Vendor ABC submitted quotation for RFQ-1042")],
      [8000, () => { setDeliveries(4); pushActivity("PO-2098 · Delivery received") }],
      [10800, () => setScore(87)],
      [13600, () => { setInvoiceStatus("Matched"); pushActivity("INV-7821 · Three-way match completed") }],
    ];
    const timers = steps.map(([ms, fn]) => setTimeout(fn, ms));
    function pushActivity(text: string) {
      setActivities((current) => [{ id: Date.now(), text }, ...current].slice(0, 2));
    }
    return () => timers.forEach(clearTimeout);
  }, [inView]);

  return (
    <div
      ref={ref}
      className="rounded-2xl border border-slate-800 bg-slate-900/80 p-4 shadow-[0_24px_70px_-20px_rgba(0,0,0,0.65)] transition-transform duration-700 [transform:perspective(1400px)_rotateX(1.5deg)]"
    >
      <div className="flex items-center gap-1.5 pb-3">
        <span className="h-2.5 w-2.5 rounded-full bg-slate-700" />
        <span className="h-2.5 w-2.5 rounded-full bg-slate-700" />
        <span className="h-2.5 w-2.5 rounded-full bg-slate-700" />
        <span className="ml-3 rounded-md bg-slate-950 px-3 py-1 text-[10px] text-slate-500">vendorsphere / dashboard</span>
      </div>

      <div className="grid grid-cols-2 gap-2.5 sm:grid-cols-3">
        <Kpi label="Active RFQs" value={String(rfqs)} highlight={rfqs === 12} />
        <Kpi label="Open POs" value="18" />
        <Kpi label="Pending Deliveries" value={String(deliveries)} highlight={deliveries === 4} />
        <Kpi label="Outstanding Invoices" value="₹11.6L" />
        <div className="rounded-lg border border-slate-800 bg-slate-950/70 p-3">
          <p className="text-[10px] uppercase tracking-wide text-slate-500">Supplier Performance</p>
          <p className={`mt-1 text-sm font-semibold tabular-nums text-white transition-colors duration-700 ${score === 87 ? "text-emerald-300" : ""}`}>
            {score}
            {score === 87 && <span className="ml-1 text-[10px] text-emerald-400">▲</span>}
          </p>
        </div>
        <div className="rounded-lg border border-slate-800 bg-slate-950/70 p-3">
          <p className="text-[10px] uppercase tracking-wide text-slate-500">Latest Invoice</p>
          <p className={`mt-1 inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium transition-all duration-700 ${
            invoiceStatus === "Matched"
              ? "bg-emerald-500/15 text-emerald-300"
              : "bg-amber-500/10 text-amber-300"
          }`}>
            {invoiceStatus === "Matched" && <CheckCircle2 className="h-3 w-3" />}
            {invoiceStatus}
          </p>
        </div>
      </div>

      {/* activity feed */}
      <ul className="mt-2.5 space-y-1.5 rounded-lg border border-slate-800 bg-slate-950/70 p-2.5">
        {activities.map((activity) => (
          <li
            key={activity.id}
            className="flex items-center gap-2 px-1 py-1 text-xs text-slate-400"
            style={{ animation: reduced ? undefined : "fade-slide .6s ease both" }}
          >
            <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-emerald-400" />
            {activity.text}
          </li>
        ))}
      </ul>

      <style>{`
        @keyframes fade-slide {
          from { opacity: 0; transform: translateY(-6px); }
          to { opacity: 1; transform: translateY(0); }
        }
      `}</style>
    </div>
  );
}

function Kpi({ label, value, highlight }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className={`rounded-lg border p-3 transition-colors duration-700 ${
      highlight ? "border-emerald-500/40 bg-emerald-950/30" : "border-slate-800 bg-slate-950/70"
    }`}>
      <p className="text-[10px] uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-1 text-sm font-semibold tabular-nums text-white">{value}</p>
    </div>
  );
}

/* ---------- lifecycle (Priority 2): sequential stage reveal ---------- */

function Pipeline() {
  const reduced = usePrefersReducedMotion();
  const [ref, inView] = useInView<HTMLOListElement>(0.3);
  return (
    <ol className="grid grid-cols-2 gap-3 sm:grid-cols-4 lg:grid-cols-8" aria-label="Procurement lifecycle">
      {STAGES.map(([label, Icon], i) => {
        const shown = inView || reduced;
        return (
          <li
            key={label}
            className="relative flex flex-col items-center gap-2 rounded-xl border border-slate-800 bg-slate-900/70 px-3 py-4"
            style={{
              opacity: shown ? 1 : 0,
              transform: shown ? "translateY(0)" : "translateY(14px)",
              transition: reduced ? "none" : `opacity .45s ease ${i * 220}ms, transform .45s ease ${i * 220}ms`,
            }}
          >
            <Icon className="h-5 w-5 text-emerald-400" />
            <span className="text-center text-[11px] leading-tight text-slate-300">{label}</span>
            {shown && !reduced && (
              <span
                aria-hidden
                className="absolute inset-x-2 bottom-1 h-0.5 origin-left rounded-full bg-emerald-400/70"
                style={{ animation: `stage-fill .5s ease-out ${i * 220 + 250}ms both` }}
              />
            )}
          </li>
        );
      })}
      <style>{`
        @keyframes stage-fill { from { transform: scaleX(0); } to { transform: scaleX(1); } }
      `}</style>
    </ol>
  );
}

/* ---------- comparison showcase (Priority 3) ---------- */

function ComparisonShowcase() {
  const reduced = usePrefersReducedMotion();
  const [ref, inView] = useInView<HTMLDivElement>(0.3);
  const winnerScore = useCountUp(91, inView, 1400);

  return (
    <div ref={ref}>
      <div className="overflow-x-auto rounded-2xl border border-slate-800">
        <table className="w-full min-w-[640px] text-left text-sm">
          <caption className="sr-only">Example quotation comparison for three vendors</caption>
          <thead className="bg-slate-900/80 text-xs uppercase tracking-wider text-slate-400">
            <tr>
              <th scope="col" className="px-5 py-4">Criteria</th>
              <th scope="col" className="px-5 py-4">Vendor A</th>
              <th scope="col" className="px-5 py-4">Vendor B</th>
              <th scope="col" className="relative px-5 py-4 text-emerald-300">
                <span
                  className="absolute right-3 top-2 rounded-full bg-emerald-500/15 px-2 py-0.5 text-[10px] normal-case tracking-normal transition-opacity duration-1000"
                  style={{ opacity: inView ? 1 : 0 }}
                >
                  Best Overall Value
                </span>
                Vendor C ★
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800 bg-slate-950/60">
            {COMPARISON_ROWS.map((row, i) => (
              <tr
                key={row.criteria}
                className="group"
                style={{
                  opacity: inView ? 1 : 0,
                  transition: reduced ? "none" : `opacity .5s ease ${i * 160}ms`,
                }}
              >
                <th scope="row" className="px-5 py-3.5">
                  <span className="cursor-help font-medium text-slate-300 underline decoration-slate-600 decoration-dotted underline-offset-4">
                    {row.criteria}
                    <span role="tooltip" className="pointer-events-none absolute z-10 ml-2 hidden w-56 rounded-lg border border-slate-700 bg-slate-900 px-3 py-2 text-xs font-normal normal-case leading-5 text-slate-300 shadow-xl group-hover:block">
                      {row.tip}
                    </span>
                  </span>
                </th>
                <td className="px-5 py-3.5 text-slate-400">{row.values[0]}</td>
                <td className="px-5 py-3.5 text-slate-400">{row.values[1]}</td>
                <td className="border-x-2 border-emerald-500/40 bg-emerald-950/20 px-5 py-3.5 font-medium tabular-nums text-emerald-200">
                  {row.criteria === "Overall Score"
                    ? winnerScore
                    : row.values[2]}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="mt-3 text-center text-xs text-slate-600">
        The platform recommends; an authorized procurement user makes the final selection with a recorded justification.
      </p>
    </div>
  );
}

/* ---------- three-way matching demo (Priority 1) ---------- */

const MATCH_CHECKS = ["Quantity match", "Unit price match", "Delivery confirmed", "Duplicate check"];

function MatchDemo() {
  const reduced = usePrefersReducedMotion();
  const [ref, inView] = useInView<HTMLDivElement>(0.35);
  const [checkIndex, setCheckIndex] = useState(0);
  const [matched, setMatched] = useState(false);
  const [mismatch, setMismatch] = useState(false);

  // Sequential checks once in view (or immediately after toggle reset).
  useEffect(() => {
    if (!inView || reduced) {
      if (reduced) { setCheckIndex(MATCH_CHECKS.length); setMatched(true); }
      return;
    }
    const timers = MATCH_CHECKS.map((_, i) =>
      setTimeout(() => setCheckIndex(i + 1), 900 + i * 550));
    timers.push(setTimeout(() => setMatched(true), 900 + MATCH_CHECKS.length * 550));
    return () => timers.forEach(clearTimeout);
  }, [inView]);

  const replay = useCallback(() => {
    setMismatch(false);
    setMatched(false);
    setCheckIndex(0);
    if (reduced) { setCheckIndex(MATCH_CHECKS.length); setMatched(true); return; }
    MATCH_CHECKS.forEach((_, i) =>
      setTimeout(() => setCheckIndex(i + 1), 700 + i * 500));
    setTimeout(() => setMatched(true), 700 + MATCH_CHECKS.length * 500);
  }, []);

  const sources = [
    { label: "Purchase Order", detail: "100 units × ₹1,000" },
    { label: "Goods Received", detail: "100 units received" },
    { label: "Vendor Invoice", detail: mismatch ? "100 units × ₹1,200" : "100 units × ₹1,000" },
  ];

  return (
    <div ref={ref} className="grid items-center gap-8 lg:grid-cols-2">
      <div className="space-y-3">
        {sources.map((source, i) => (
          <div
            key={source.label}
            className="relative flex items-center justify-between rounded-xl border border-slate-800 bg-slate-950/60 px-4 py-3.5"
            style={{
              opacity: inView ? 1 : 0,
              transform: inView ? "translateX(0)" : "translateX(-14px)",
              transition: reduced ? "none" : `all .5s ease ${i * 180}ms`,
            }}
          >
            <span className="font-medium text-white">{source.label}</span>
            <span className={`text-sm tabular-nums ${source.label === "Vendor Invoice" && mismatch ? "text-amber-300" : "text-slate-400"}`}>
              {source.detail}
            </span>
            <span
              aria-hidden
              className={`absolute inset-y-0 right-full my-auto h-px w-16 bg-gradient-to-l ${
                matched ? "from-emerald-500/60" : "from-slate-700"
              }`}
            />
          </div>
        ))}
        <div className="flex items-center gap-2 pl-1 pt-1 text-xs text-slate-500">
          <ArrowRight className="h-3.5 w-3.5 text-emerald-400" />
          All three streams feed the matching engine
        </div>
      </div>

      <div className="space-y-3">
        <ol className="space-y-2">
          {MATCH_CHECKS.map((check, i) => {
            const done = checkIndex > i;
            return (
              <li
                key={check}
                className="flex items-center gap-3 rounded-lg border border-slate-800 bg-slate-950/60 px-4 py-2.5 text-sm transition-all duration-500"
                style={{ opacity: done ? 1 : 0.35, borderColor: done ? "rgb(16 185 129 / .35)" : undefined }}
                aria-live="polite"
              >
                {done ? (
                  <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-400" />
                ) : (
                  <span className="h-4 w-4 shrink-0 rounded-full border border-slate-700" />
                )}
                {check}
              </li>
            );
          })}
        </ol>

        <div
          className={`flex items-center justify-between rounded-xl border px-4 py-3.5 transition-all duration-700 ${
            mismatch
              ? "border-amber-700/60 bg-amber-950/30"
              : "border-emerald-500/30 bg-emerald-500/10"
          }`}
          style={{ opacity: matched ? 1 : 0.4 }}
          aria-live="polite"
        >
          {mismatch ? (
            <>
              <p className="flex items-center gap-2 text-sm font-medium text-amber-300">
                <XCircle className="h-4 w-4" /> Price mismatch detected
              </p>
              <span className="text-xs text-amber-200/70">₹1,000 → ₹1,200 · blocked for review</span>
            </>
          ) : (
            <>
              <p className="flex items-center gap-2 text-sm font-medium text-emerald-200">
                <CheckCircle2 className="h-4 w-4" /> MATCHED
              </p>
              <span className="text-xs text-slate-500">cleared for payment approval</span>
            </>
          )}
        </div>

        <button
          type="button"
          onClick={() => (mismatch ? replay() : (setMismatch(true), setMatched(true)))}
          className="rounded-lg border border-slate-700 px-4 py-2 text-xs text-slate-300 transition hover:border-slate-500 hover:bg-slate-900"
        >
          {mismatch ? "Replay matched scenario" : "Show mismatch example"}
        </button>
      </div>
    </div>
  );
}

/* ---------- vendor scorecard (Priority 6) ---------- */

const SCORE_METRICS = [["Delivery", 92], ["Quality", 88], ["Pricing", 80], ["Responsiveness", 91], ["Fulfilment", 85]] as const;

function Scorecard() {
  const reduced = usePrefersReducedMotion();
  const [ref, inView] = useInView<HTMLDivElement>(0.4);
  const overall = useCountUp(87, inView, 1300);
  return (
    <div ref={ref} className="rounded-2xl border border-slate-800 bg-slate-950/60 p-7">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-800 text-sm font-semibold text-white">AB</span>
          <div>
            <p className="font-medium text-white">ABC Technologies</p>
            <p className="text-xs text-slate-500">Supplier since 2024</p>
          </div>
        </div>
        <div className="text-right">
          <p className="text-3xl font-semibold tabular-nums text-emerald-300">
            {overall}<span className="text-base text-slate-500">/100</span>
          </p>
          <p className="text-[10px] uppercase tracking-wide text-slate-500">Overall</p>
        </div>
      </div>
      <div className="mt-6 space-y-3.5">
        {SCORE_METRICS.map(([metric, score]) => {
          const value = useCountUp(score, inView, 1100);
          return (
            <div key={metric}>
              <div className="flex justify-between text-xs text-slate-400">
                <span>{metric}</span><span className="tabular-nums">{value}</span>
              </div>
              <div className="mt-1.5 h-2 overflow-hidden rounded-full bg-slate-800">
                <div
                  className="h-full rounded-full bg-gradient-to-r from-emerald-400 to-teal-500"
                  style={{
                    width: `${inView ? value : 0}%`,
                    transition: reduced ? "none" : "width 1.1s cubic-bezier(.22,.61,.36,1)",
                  }}
                />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

/* ---------- role switcher (Priority 5) ---------- */

function RoleSwitcher() {
  const reduced = usePrefersReducedMotion();
  const [active, setActive] = useState(ROLES[0]);
  return (
    <div>
      <div role="tablist" aria-label="Workspace roles" className="flex flex-wrap justify-center gap-2">
        {ROLES.map((role) => (
          <button
            key={role.key}
            role="tab"
            aria-selected={active.key === role.key}
            onClick={() => setActive(role)}
            className={`flex items-center gap-2 rounded-lg px-4 py-2 text-sm transition ${
              active.key === role.key
                ? "bg-emerald-500/15 text-emerald-300 ring-1 ring-emerald-500/40"
                : "text-slate-400 ring-1 ring-slate-800 hover:bg-slate-900 hover:text-white"
            }`}
          >
            <role.icon className="h-4 w-4" />
            {role.label}
          </button>
        ))}
      </div>
      <div className="mx-auto mt-8 max-w-xl rounded-2xl border border-slate-800 bg-slate-950/60 p-5">
        <p key={active.key + "-body"} className="text-sm leading-6 text-slate-300" style={{ animation: reduced ? undefined : "fade-in .4s ease" }}>
          {active.body}
        </p>
        <ul key={active.key} className="mt-4 space-y-2">
          {active.preview.map((line) => (
            <li
              key={line}
              className="rounded-lg border border-slate-800 bg-slate-900/60 px-3.5 py-2.5 text-xs text-slate-300"
              style={{ animation: reduced ? undefined : "fade-slide-up .45s ease both" }}
            >
              {line}
            </li>
          ))}
        </ul>
      </div>
      <style>{`
        @keyframes fade-in { from { opacity: 0; } to { opacity: 1; } }
        @keyframes fade-slide-up { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
      `}</style>
    </div>
  );
}

/* ---------- analytics preview (Priority 7) ---------- */

function AnalyticsPreview() {
  const reduced = usePrefersReducedMotion();
  const [ref, inView] = useInView<HTMLDivElement>(0.35);
  const spend = useCountUp(48, inView, 1400);
  const vendors = useCountUp(28, inView, 1200);
  const rfqs = useCountUp(7, inView, 1000);
  const kpis = [
    [`₹${spend}.6L`, "Procurement Spend"], [String(vendors), "Active Vendors"], [String(rfqs), "Open RFQs"],
  ];
  const bars = [42, 66, 51, 78, 62];
  return (
    <div ref={ref} className="rounded-2xl border border-slate-800 bg-slate-900/70 p-5 backdrop-blur">
      <div className="grid grid-cols-3 gap-2.5">
        {kpis.map(([value, label]) => (
          <div key={label} className="rounded-lg border border-slate-800 bg-slate-950/70 p-3">
            <p className="text-[10px] uppercase tracking-wide text-slate-500">{label}</p>
            <p className="mt-1 text-sm font-semibold tabular-nums text-white">{inView ? value : "—"}</p>
          </div>
        ))}
      </div>
      <div aria-hidden className="mt-2.5 flex h-24 items-end gap-2 rounded-lg border border-slate-800 bg-slate-950/70 p-3">
        {bars.map((height, i) => (
          <div
            key={i}
            className="flex-1 rounded-sm bg-gradient-to-t from-teal-600/60 to-emerald-400/80"
            style={{
              height: inView ? `${height}%` : "4%",
              transition: reduced ? "none" : `height .8s cubic-bezier(.22,.61,.36,1) ${i * 90}ms`,
            }}
          />
        ))}
      </div>
      <p className="mt-2 text-right text-[10px] text-slate-600">Demo data</p>
    </div>
  );
}

/* ---------- page ---------- */

export default function Home() {
  const [scrolled, setScrolled] = useState(false);
  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 12);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <div className="min-h-screen bg-slate-950 text-slate-200">
      {/* Navbar with scroll behaviour */}
      <header
        className={`sticky top-0 z-40 transition-all duration-300 ${
          scrolled
            ? "border-b border-slate-800/80 bg-slate-950/90 shadow-[0_8px_24px_-16px_rgba(0,0,0,0.8)] backdrop-blur"
            : "border-b border-transparent bg-transparent"
        }`}
      >
        <div className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
          <Link href="/" className="flex items-center gap-2.5">
            <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-emerald-400 to-teal-600 shadow-lg shadow-emerald-500/20">
              <Boxes className="h-4 w-4 text-slate-950" strokeWidth={2.4} />
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
            <Link href="/register" className="rounded-lg bg-gradient-to-r from-emerald-500 to-teal-500 px-4 py-2 text-sm font-medium text-slate-950 shadow-lg shadow-emerald-500/20 transition-all duration-200 hover:-translate-y-0.5 hover:brightness-110 hover:shadow-xl hover:shadow-emerald-500/25">Get Started</Link>
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
          </div>
          <div className="relative mx-auto max-w-6xl px-6 pb-16 pt-20 text-center">
            <Reveal>
              <p className="mx-auto mb-5 inline-flex items-center gap-2 rounded-full border border-emerald-500/20 bg-emerald-500/10 px-3 py-1 text-xs font-medium uppercase tracking-wider text-emerald-300">
                Vendor &amp; procurement management platform
              </p>
            </Reveal>
            <Reveal delay={80}>
              <h1 className="mx-auto max-w-3xl text-4xl font-semibold leading-tight tracking-tight text-white sm:text-5xl">
                Procurement, from request to payment —{" "}
                <span className="bg-gradient-to-r from-emerald-300 to-teal-200 bg-clip-text text-transparent">in one place.</span>
              </h1>
            </Reveal>
            <Reveal delay={160}>
              <p className="mx-auto mt-5 max-w-2xl text-base leading-7 text-slate-400 sm:text-lg">
                Manage vendors, RFQs, quotations, purchase orders, deliveries,
                invoices, and supplier performance through one centralized
                procurement workspace.
              </p>
            </Reveal>
            <Reveal delay={240}>
              <div className="mt-8 flex flex-wrap items-center justify-center gap-3">
                <Link
                  href="/register"
                  className="inline-flex items-center gap-2 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 px-6 py-3 text-sm font-medium text-slate-950 shadow-lg shadow-emerald-500/25 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-xl hover:shadow-emerald-500/30"
                >
                  Get Started <ArrowRight className="h-4 w-4" />
                </Link>
                <a
                  href="#how-it-works"
                  className="rounded-xl border border-slate-700 px-6 py-3 text-sm font-medium text-slate-200 transition-colors hover:border-slate-500 hover:bg-slate-900"
                >
                  See How It Works
                </a>
              </div>
            </Reveal>
            <Reveal delay={350} className="mx-auto mt-14 max-w-4xl">
              <HeroDashboard />
              <p className="mt-3 text-xs text-slate-600">Illustrative workspace preview — simulated activity</p>
            </Reveal>
          </div>
        </section>

        {/* Problem */}
        <section id="solutions" className="border-y border-slate-800/70 bg-slate-900/40">
          <div className="mx-auto max-w-6xl px-6 py-20">
            <Reveal>
              <SectionHeading
                eyebrow="The problem"
                title="Procurement shouldn't live across spreadsheets, emails, and PDFs."
                sub="Fragmented tools make sourcing slow, opaque and hard to audit. VendorSphere consolidates the workflow without changing how your team works."
              />
            </Reveal>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              {PAINS.map(([title, body], i) => (
                <Reveal key={title} delay={i * 90}>
                  <article className="h-full rounded-2xl border border-slate-800 bg-slate-950/60 p-5">
                    <XCircle aria-hidden className="h-5 w-5 text-rose-400/80" />
                    <h3 className="mt-3 font-medium text-white">{title}</h3>
                    <p className="mt-2 text-sm leading-6 text-slate-400">{body}</p>
                  </article>
                </Reveal>
              ))}
            </div>
            <Reveal delay={200}>
              <div className="mx-auto mt-10 flex max-w-3xl flex-wrap items-center justify-center gap-3 text-sm">
                {["Email threads", "Excel sheets", "PDF quotes", "Phone calls"].map((tool) => (
                  <span key={tool} className="rounded-full border border-slate-700 px-3.5 py-1.5 text-slate-400 line-through decoration-rose-400/60">{tool}</span>
                ))}
                <ArrowRight className="h-4 w-4 text-emerald-400" />
                <span className="rounded-full border border-emerald-500/30 bg-emerald-500/10 px-4 py-1.5 font-medium text-emerald-300">VendorSphere</span>
              </div>
            </Reveal>
          </div>
        </section>

        {/* Lifecycle */}
        <section id="how-it-works" className="mx-auto max-w-6xl px-6 py-20">
          <Reveal>
            <SectionHeading
              eyebrow="How it works"
              title="One connected procurement pipeline."
              sub="Every stage hands off cleanly to the next — with status, ownership and history preserved."
            />
          </Reveal>
          <Pipeline />
        </section>

        {/* Features bento */}
        <section id="features" className="border-y border-slate-800/70 bg-slate-900/40">
          <div className="mx-auto max-w-6xl px-6 py-20">
            <Reveal>
              <SectionHeading eyebrow="Features" title="Everything procurement needs, nothing it doesn't." />
            </Reveal>
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {FEATURES.map(({ icon: Icon, title, body, wide }, i) => (
                <Reveal key={title} delay={(i % 3) * 90} className={wide ? "sm:col-span-2" : ""}>
                  <article className="h-full rounded-2xl border border-slate-800 bg-slate-950/60 p-6 transition hover:border-emerald-500/30">
                    <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-emerald-500/10 text-emerald-400 ring-1 ring-emerald-500/20">
                      <Icon className="h-4 w-4" />
                    </div>
                    <h3 className="mt-4 font-medium text-white">{title}</h3>
                    <p className="mt-2 text-sm leading-6 text-slate-400">{body}</p>
                  </article>
                </Reveal>
              ))}
            </div>
          </div>
        </section>

        {/* Comparison */}
        <section className="mx-auto max-w-6xl px-6 py-20">
          <Reveal>
            <SectionHeading
              eyebrow="Quotation comparison"
              title="Compare bids on equivalent terms."
              sub="The same request, normalized across every responding vendor — scores computed by the platform, decision made by you."
            />
          </Reveal>
          <ComparisonShowcase />
        </section>

        {/* Three-way matching */}
        <section className="border-y border-slate-800/70 bg-slate-900/40">
          <div className="mx-auto max-w-6xl px-6 py-20">
            <Reveal>
              <SectionHeading eyebrow="Three-way matching" title="Catch invoice discrepancies before payment." />
            </Reveal>
            <MatchDemo />
          </div>
        </section>

        {/* Vendor performance */}
        <section className="mx-auto max-w-6xl px-6 py-20">
          <div className="grid items-center gap-10 lg:grid-cols-2">
            <Reveal>
              <p className="text-xs font-medium uppercase tracking-widest text-emerald-400">Vendor performance</p>
              <h2 className="mt-3 text-3xl font-semibold tracking-tight text-white sm:text-4xl">
                Source on history, not intuition.
              </h2>
              <p className="mt-4 leading-7 text-slate-400">
                Make sourcing decisions using historical supplier performance,
                not intuition alone. Every award, receipt and invoice feeds the
                next month&apos;s scorecard automatically.
              </p>
            </Reveal>
            <Reveal delay={150}><Scorecard /></Reveal>
          </div>
        </section>

        {/* Roles */}
        <section className="border-y border-slate-800/70 bg-slate-900/40">
          <div className="mx-auto max-w-6xl px-6 py-20">
            <Reveal>
              <SectionHeading eyebrow="Role-based workspace" title="A different view for every responsibility." />
            </Reveal>
            <RoleSwitcher />
          </div>
        </section>

        {/* Analytics */}
        <section className="mx-auto max-w-4xl px-6 py-20 text-center">
          <Reveal>
            <SectionHeading eyebrow="Analytics" title="Spend, suppliers and status at a glance." />
          </Reveal>
          <AnalyticsPreview />
        </section>

        {/* Security */}
        <section id="security" className="border-y border-slate-800/70 bg-slate-900/40">
          <div className="mx-auto max-w-6xl px-6 py-20">
            <Reveal>
              <SectionHeading eyebrow="Security" title="Built with control and accountability in mind." />
            </Reveal>
            <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
              {[
                ["Role-Based Access", "Users only access permitted functionality, enforced server-side."],
                ["Secure Authentication", "JWT-based protected APIs with refresh-token rotation."],
                ["Audit Trail", "Critical procurement actions remain traceable to actor and time."],
                ["Backend Validation", "Business rules are enforced in the API, never only in the client."],
              ].map(([title, body], i) => (
                <Reveal key={title} delay={i * 80}>
                  <article className="h-full rounded-2xl border border-slate-800 bg-slate-950/60 p-6">
                    <ShieldCheck className="h-5 w-5 text-emerald-400" />
                    <h3 className="mt-3 font-medium text-white">{title}</h3>
                    <p className="mt-2 text-sm leading-6 text-slate-400">{body}</p>
                  </article>
                </Reveal>
              ))}
            </div>
          </div>
        </section>

        {/* Final CTA */}
        <section className="mx-auto max-w-4xl px-6 pb-24">
          <Reveal>
            <div className="rounded-3xl border border-emerald-900/50 bg-gradient-to-r from-emerald-950/60 via-slate-900 to-slate-900 p-10 text-center">
              <h2 className="text-2xl font-semibold text-white sm:text-3xl">
                Bring your procurement workflow into one workspace.
              </h2>
              <p className="mx-auto mt-3 max-w-xl text-sm leading-6 text-slate-400">
                From vendor sourcing to invoice verification, VendorSphere gives
                teams a structured way to manage the complete procurement lifecycle.
              </p>
              <div className="mt-7 flex flex-wrap items-center justify-center gap-3">
                <Link href="/register" className="rounded-xl bg-gradient-to-r from-emerald-500 to-teal-500 px-7 py-3 text-sm font-medium text-slate-950 shadow-lg shadow-emerald-500/25 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-xl">
                  Get Started
                </Link>
                <Link href="/login" className="rounded-xl border border-slate-700 px-7 py-3 text-sm font-medium text-slate-200 transition-colors hover:border-slate-500 hover:bg-slate-900">
                  Sign In
                </Link>
              </div>
            </div>
          </Reveal>
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

function SectionHeading({ eyebrow, title, sub }: { eyebrow?: string; title: string; sub?: string }) {
  return (
    <div className="mx-auto mb-12 max-w-2xl text-center">
      {eyebrow && <p className="mb-3 text-xs font-medium uppercase tracking-widest text-emerald-400">{eyebrow}</p>}
      <h2 className="text-3xl font-semibold tracking-tight text-white sm:text-4xl">{title}</h2>
      {sub && <p className="mt-4 leading-7 text-slate-400">{sub}</p>}
    </div>
  );
}
