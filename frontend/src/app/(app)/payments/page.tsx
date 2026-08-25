"use client";

/** Payments list plus the outstanding payables summary (Requirement 35.8). */
import { useOutstandingPayables, usePayments } from "@/lib/hooks/payments";
import { formatMoney } from "@/lib/format";

export default function PaymentsPage() {
  const outstanding = useOutstandingPayables();
  const payments = usePayments({ page: 0, size: 50 });

  return (
    <div className="mx-auto max-w-6xl">
      <h1 className="text-lg font-semibold text-white">Payments</h1>

      <section className="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <article className="rounded-2xl border border-slate-800 bg-slate-900/50 p-6">
          <p className="text-sm text-slate-400">Total Outstanding</p>
          <p className="mt-2 text-3xl font-semibold text-white">
            {outstanding.data ? formatMoney(outstanding.data.totalOutstanding) : "—"}
          </p>
        </article>
      </section>

      {payments.isLoading && <p className="mt-6 text-sm text-slate-400" role="status">Loading payments…</p>}
      {payments.isError && <p className="mt-6 text-sm text-amber-300" role="alert">Could not load payments.</p>}

      {payments.data && (
        <div className="mt-6 overflow-x-auto rounded-2xl border border-slate-800">
          <table className="w-full min-w-[640px] text-left text-sm">
            <thead className="border-b border-slate-800 bg-slate-900/70 text-xs uppercase tracking-wider text-slate-400">
              <tr>
                <th scope="col" className="px-4 py-3">Payment date</th>
                <th scope="col" className="px-4 py-3 text-right">Amount</th>
                <th scope="col" className="px-4 py-3">Reference</th>
                <th scope="col" className="px-4 py-3">Method</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800 bg-slate-950/40">
              {payments.data.content.map((payment) => (
                <tr key={payment.id} className="hover:bg-slate-900/50">
                  <td className="px-4 py-3 text-slate-300">{payment.paymentDate}</td>
                  <td className="px-4 py-3 text-right font-mono text-white">{formatMoney(payment.amount)}</td>
                  <td className="px-4 py-3 font-mono text-xs text-slate-400">{payment.paymentReference ?? "—"}</td>
                  <td className="px-4 py-3 text-slate-300">{payment.paymentMethod ?? "—"}</td>
                </tr>
              ))}
              {payments.data.content.length === 0 && (
                <tr><td colSpan={4} className="px-4 py-8 text-center text-slate-400">No payments recorded yet.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
