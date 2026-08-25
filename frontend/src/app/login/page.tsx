"use client";

/**
 * Sign-in screen: split layout with an animated brand panel (floating orbs,
 * orbiting supply-chain nodes and a live stats ticker) beside the form.
 */

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Boxes, Globe2, PackageCheck, ShieldCheck, Truck } from "lucide-react";
import { login } from "@/lib/api";
import { playSuccess } from "@/lib/sound";

const loginSchema = z.object({
  email: z.string().email("Enter a valid email"),
  password: z.string().min(1, "Password is required"),
});

type LoginForm = z.infer<typeof loginSchema>;

/** Slowly drifting gradient orb used behind the brand panel. */
function Orb({ className }: { className: string }) {
  return (
    <div
      className={`pointer-events-none absolute rounded-full blur-3xl ${className}`}
      aria-hidden
    />
  );
}

const TICKER = [
  { icon: Truck, text: "RFQ-2026-014 awarded to Acme Supplies" },
  { icon: ShieldCheck, text: "Three-way match passed for INV-2026-0088" },
  { icon: PackageCheck, text: "PO-2026-0031 fully delivered" },
  { icon: Globe2, text: "12 vendors onboarded this quarter" },
];

export default function LoginPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "admin@demo-corp.com", password: "Admin@123" },
  });

  async function onSubmit(data: LoginForm) {
    setError(null);
    try {
      await login(data.email, data.password);
      playSuccess();
      router.push("/dashboard");
    } catch {
      setError("Invalid email or password");
    }
  }

  return (
    <div className="grid min-h-screen bg-slate-950 lg:grid-cols-[1.15fr_1fr]">
      {/* Brand panel */}
      <section className="relative hidden overflow-hidden lg:block">
        <Orb className="left-[-10%] top-[-15%] h-[28rem] w-[28rem] bg-emerald-500/20" />
        <Orb className="bottom-[-20%] right-[-10%] h-[32rem] w-[32rem] bg-teal-500/15" />
        <Orb className="left-[35%] top-[45%] h-72 w-72 bg-cyan-400/10" />

        {/* animated grid backdrop */}
        <div
          aria-hidden
          className="absolute inset-0 opacity-[0.06]"
          style={{
            backgroundImage:
              "linear-gradient(#34d399 1px, transparent 1px), linear-gradient(90deg, #34d399 1px, transparent 1px)",
            backgroundSize: "44px 44px",
            maskImage:
              "radial-gradient(ellipse at 40% 40%, black 30%, transparent 75%)",
          }}
        />

        <div className="relative flex h-full flex-col justify-between p-12">
          <div className="flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-emerald-400 to-teal-600 shadow-lg shadow-emerald-500/25">
              <Boxes className="h-5 w-5 text-slate-950" strokeWidth={2.4} />
            </div>
            <p className="text-xl font-semibold tracking-tight text-white">
              Vendor<span className="text-emerald-400">Sphere</span>
            </p>
          </div>

          <div className="max-w-lg">
            <h2 className="text-4xl font-semibold leading-tight tracking-tight text-white">
              Every quotation,{" "}
              <span className="bg-gradient-to-r from-emerald-300 to-teal-200 bg-clip-text text-transparent">
                every delivery
              </span>
              , one command center.
            </h2>
            <p className="mt-4 text-base leading-relaxed text-slate-400">
              From purchase request to payment — source vendors, compare bids on
              equivalent terms and keep three-way matching honest, all in one place.
            </p>

            <dl className="mt-10 grid grid-cols-3 gap-6">
              {[
                ["RFQ → PO", "full lifecycle"],
                ["3-way match", "PO · GRN · invoice"],
                ["Live scoring", "vendor performance"],
              ].map(([title, sub]) => (
                <div key={title} className="rounded-xl border border-slate-800 bg-slate-900/50 p-4 backdrop-blur">
                  <dt className="text-sm font-semibold text-white">{title}</dt>
                  <dd className="mt-1 text-xs text-slate-500">{sub}</dd>
                </div>
              ))}
            </dl>
          </div>

          {/* live activity ticker */}
          <div className="overflow-hidden rounded-xl border border-slate-800 bg-slate-900/60 backdrop-blur">
            <div className="flex items-center gap-3 px-4 py-3">
              <span className="relative flex h-2 w-2 shrink-0">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-60" />
                <span className="relative inline-flex h-2 w-2 rounded-full bg-emerald-400" />
              </span>
              <ul className="flex w-full animate-[ticker_18s_linear_infinite] gap-10 whitespace-nowrap text-xs text-slate-400 [animation-name:ticker]">
                {[...TICKER, ...TICKER].map((item, i) => (
                  <li key={i} className="flex items-center gap-2">
                    <item.icon className="h-3.5 w-3.5 text-emerald-400" />
                    {item.text}
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      </section>

      {/* Form panel */}
      <section className="flex items-center justify-center px-6 py-12">
        <div className="w-full max-w-md">
          <div className="mb-8 flex items-center gap-3 lg:hidden">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-emerald-400 to-teal-600">
              <Boxes className="h-5 w-5 text-slate-950" strokeWidth={2.4} />
            </div>
            <p className="text-lg font-semibold text-white">
              Vendor<span className="text-emerald-400">Sphere</span>
            </p>
          </div>

          <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-8 shadow-2xl shadow-black/40">
            <h1 className="text-xl font-semibold text-white">Sign in to your workspace</h1>
            <p className="mt-2 text-sm text-slate-400">
              Demo credentials are prefilled — just press sign in.
            </p>

            <form onSubmit={handleSubmit(onSubmit)} className="mt-8 space-y-5">
              <div>
                <label htmlFor="email" className="mb-1.5 block text-sm text-slate-300">Email</label>
                <input
                  id="email"
                  type="email"
                  {...register("email")}
                  className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-sm text-white outline-none transition focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20"
                />
                {errors.email && <p className="mt-1 text-xs text-red-400">{errors.email.message}</p>}
              </div>

              <div>
                <label htmlFor="password" className="mb-1.5 block text-sm text-slate-300">Password</label>
                <input
                  id="password"
                  type="password"
                  {...register("password")}
                  className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-sm text-white outline-none transition focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20"
                />
                {errors.password && (
                  <p className="mt-1 text-xs text-red-400">{errors.password.message}</p>
                )}
              </div>

              {error && (
                <p className="rounded-lg border border-rose-900 bg-rose-950/40 px-3 py-2 text-sm text-rose-300" role="alert">
                  {error}
                </p>
              )}

              <button
                type="submit"
                disabled={isSubmitting}
                className="w-full rounded-lg bg-gradient-to-r from-emerald-500 to-teal-500 px-4 py-2.5 text-sm font-medium text-slate-950 transition hover:brightness-110 disabled:opacity-50"
              >
                {isSubmitting ? "Signing in…" : "Sign in"}
              </button>
            </form>

            <p className="mt-6 text-center text-sm text-slate-400">
              New organization?{" "}
              <Link href="/register" className="font-medium text-emerald-400 hover:underline">
                Create an account
              </Link>
            </p>
          </div>
        </div>
      </section>
    </div>
  );
}
