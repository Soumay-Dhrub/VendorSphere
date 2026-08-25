"use client";

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { register as registerUser } from "@/lib/api";
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
      router.push("/dashboard");
    } catch {
      setError("Registration failed. Email may already be in use.");
    }
  }

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-gradient-to-br from-slate-950 via-slate-900 to-teal-950/60 px-6 py-10">
      <div aria-hidden className="pointer-events-none absolute left-[-12%] top-[-18%] h-[30rem] w-[30rem] animate-pulse rounded-full bg-emerald-500/10 blur-3xl" />
      <div aria-hidden className="pointer-events-none absolute bottom-[-22%] right-[-12%] h-[32rem] w-[32rem] rounded-full bg-teal-500/10 blur-3xl" />
      <div className="relative w-full max-w-lg rounded-2xl border border-slate-800 bg-slate-900/60 p-8 shadow-2xl shadow-black/40">
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
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-emerald-500"
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
                className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-emerald-500"
              />
              {errors.firstName && (
                <p className="mt-1 text-xs text-red-400">{errors.firstName.message}</p>
              )}
            </div>
            <div>
              <label className="mb-1.5 block text-sm text-slate-300">Last name</label>
              <input
                {...register("lastName")}
                className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-emerald-500"
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
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-emerald-500"
            />
            {errors.email && (
              <p className="mt-1 text-xs text-red-400">{errors.email.message}</p>
            )}
          </div>

          <div>
            <label className="mb-1.5 block text-sm text-slate-300">Phone (optional)</label>
            <input
              {...register("phone")}
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-emerald-500"
            />
          </div>

          <div>
            <label className="mb-1.5 block text-sm text-slate-300">Password</label>
            <input
              type="password"
              {...register("password")}
              className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-white outline-none focus:border-emerald-500"
            />
            {errors.password && (
              <p className="mt-1 text-xs text-red-400">{errors.password.message}</p>
            )}
          </div>

          {error && <p className="text-sm text-red-400">{error}</p>}

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full rounded-lg bg-emerald-500 py-2.5 text-sm font-medium text-slate-950 transition hover:bg-emerald-400 disabled:opacity-60"
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
    </div>
  );
}
