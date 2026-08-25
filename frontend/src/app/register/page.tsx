"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { register as registerUser } from "@/lib/api";
import { playSuccess } from "@/lib/sound";
import { Boxes } from "lucide-react";

const registerSchema = z.object({
  organizationName: z.string().min(2, "Organization name is required"),
  firstName: z.string().min(1, "First name is required"),
  lastName: z.string().min(1, "Last name is required"),
  email: z.string().email("Enter a valid email"),
  phone: z.string().optional(),
  password: z.string().min(8, "Password must be at least 8 characters"),
});

type RegisterForm = z.infer<typeof registerSchema>;

export default function RegisterPage() {
  const router = useRouter();
  const [error, setError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<RegisterForm>({
    resolver: zodResolver(registerSchema),
  });

  async function onSubmit(data: RegisterForm) {
    setError(null);
    try {
      await registerUser(data);
      playSuccess();
      router.push("/dashboard");
    } catch {
      setError("Registration failed. Email may already be in use.");
    }
  }

  return (
    <div className="grid min-h-screen overflow-hidden bg-slate-950 lg:grid-cols-[1.05fr_1fr]">
      {/* Animated brand panel */}
      <aside className="relative hidden flex-col justify-between overflow-hidden p-12 lg:flex">
        <div aria-hidden className="pointer-events-none absolute left-[-12%] top-[-18%] h-[28rem] w-[28rem] animate-pulse rounded-full bg-emerald-500/20 blur-3xl" />
        <div aria-hidden className="pointer-events-none absolute bottom-[-22%] right-[-18%] h-[30rem] w-[30rem] rounded-full bg-teal-500/15 blur-3xl" />
        <div
          aria-hidden
          className="absolute inset-0 opacity-[0.06]"
          style={{
            backgroundImage:
              "linear-gradient(#34d399 1px, transparent 1px), linear-gradient(90deg, #34d399 1px, transparent 1px)",
            backgroundSize: "44px 44px",
            maskImage: "radial-gradient(ellipse at 45% 40%, black 30%, transparent 75%)",
          }}
        />
        <div className="relative">
          <p className="inline-flex items-center gap-2 rounded-full border border-emerald-500/20 bg-emerald-500/10 px-3 py-1 text-xs font-medium uppercase tracking-wider text-emerald-300">
            <span className="relative flex h-1.5 w-1.5">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-70" />
              <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-emerald-400" />
            </span>
            Start in minutes
          </p>
          <h2 className="mt-6 max-w-md text-4xl font-semibold leading-tight tracking-tight text-white">
            Your procurement pipeline,{" "}
            <span className="bg-gradient-to-r from-emerald-300 to-teal-200 bg-clip-text text-transparent">
              live from day one.
            </span>
          </h2>
          <ul className="mt-8 space-y-3 text-sm text-slate-400">
            {[
              "Invite vendors and collect scored quotations",
              "Award with justification — fully audited",
              "Match every invoice before it is paid",
            ].map((item) => (
              <li key={item} className="flex items-center gap-3">
                <span className="flex h-5 w-5 items-center justify-center rounded-full bg-emerald-500/15 text-emerald-400">
                  <svg viewBox="0 0 12 12" className="h-2.5 w-2.5 fill-current"><path d="M4.6 8.4 2 5.8l1.1-1.1 1.5 1.5 4.3-4.3L10 3z" /></svg>
                </span>
                {item}
              </li>
            ))}
          </ul>
        </div>
        <div className="relative mt-10 overflow-hidden rounded-xl border border-slate-800 bg-slate-900/60 backdrop-blur">
          <div className="flex items-center gap-3 px-4 py-3">
            <span className="relative flex h-2 w-2 shrink-0">
              <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-emerald-400 opacity-60" />
              <span className="relative inline-flex h-2 w-2 rounded-full bg-emerald-400" />
            </span>
            <p className="whitespace-nowrap text-xs text-slate-400 [animation:ticker_16s_linear_infinite]">
              PO-2026-0031 delivered · INV-2026-0088 matched · Vendor score updated · RFQ-2026-014 awarded · Payment recorded ·&nbsp;
              PO-2026-0031 delivered · INV-2026-0088 matched · Vendor score updated · RFQ-2026-014 awarded · Payment recorded
            </p>
          </div>
        </div>
      </aside>

      {/* Form panel */}
      <section className="flex items-center justify-center px-6 py-12">
      <div className="w-full max-w-lg rounded-2xl border border-slate-800 bg-slate-900/60 p-8 shadow-2xl shadow-black/40">
        <div className="mb-7 flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-emerald-400 to-teal-600 shadow-lg shadow-emerald-500/20">
            <Boxes className="h-5 w-5 text-slate-950" strokeWidth={2.4} />
          </div>
          <div>
            <p className="text-lg font-semibold tracking-tight text-white">
              Vendor<span className="text-emerald-400">Sphere</span>
            </p>
            <p className="text-xs text-slate-500">Create your procurement workspace</p>
          </div>
        </div>
        <h1 className="text-xl font-semibold text-white">Register organization</h1>
        <p className="mt-2 text-sm text-slate-400">
          Sets up your organization and its admin account in one step.
        </p>

        <form onSubmit={handleSubmit(onSubmit)} className="mt-8 space-y-5">
          <div>
            <label className="mb-1.5 block text-sm text-slate-300">Organization name</label>
            <input
              {...register("organizationName")}
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-sm text-white outline-none transition focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20"
            />
            {errors.organizationName && (
              <p className="mt-1 text-xs text-red-400">{errors.organizationName.message}</p>
            )}
          </div>

          <div className="grid gap-5 sm:grid-cols-2">
            <div>
              <label className="mb-1.5 block text-sm text-slate-300">First name</label>
              <input
                {...register("firstName")}
                className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-sm text-white outline-none transition focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20"
              />
              {errors.firstName && (
                <p className="mt-1 text-xs text-red-400">{errors.firstName.message}</p>
              )}
            </div>
            <div>
              <label className="mb-1.5 block text-sm text-slate-300">Last name</label>
              <input
                {...register("lastName")}
                className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-sm text-white outline-none transition focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20"
              />
              {errors.lastName && (
                <p className="mt-1 text-xs text-red-400">{errors.lastName.message}</p>
              )}
            </div>
          </div>

          <div>
            <label className="mb-1.5 block text-sm text-slate-300">Work email</label>
            <input
              type="email"
              {...register("email")}
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-sm text-white outline-none transition focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20"
            />
            {errors.email && (
              <p className="mt-1 text-xs text-red-400">{errors.email.message}</p>
            )}
          </div>

          <div>
            <label className="mb-1.5 block text-sm text-slate-300">Phone (optional)</label>
            <input
              {...register("phone")}
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-sm text-white outline-none transition focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20"
            />
          </div>

          <div>
            <label className="mb-1.5 block text-sm text-slate-300">Password</label>
            <input
              type="password"
              {...register("password")}
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2.5 text-sm text-white outline-none transition focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20"
            />
            {errors.password && (
              <p className="mt-1 text-xs text-red-400">{errors.password.message}</p>
            )}
          </div>

          {error && <p className="text-sm text-red-400">{error}</p>}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-lg bg-gradient-to-r from-emerald-500 to-teal-500 py-2.5 text-sm font-medium text-slate-950 shadow-lg shadow-emerald-500/25 transition hover:brightness-110 disabled:opacity-60"
          >
            {isSubmitting ? "Creating account..." : "Create account"}
          </button>
        </form>

        <p className="mt-6 text-center text-sm text-slate-400">
          Already registered?{" "}
          <Link href="/login" className="text-emerald-400 hover:text-emerald-300">
            Sign in
          </Link>
        </p>
      </div>
      </section>
    </div>
  );
}
