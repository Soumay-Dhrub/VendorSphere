"use client";

import { usePurchaseOrders } from "@/lib/hooks/purchaseOrders";
import { formatMoney } from "@/lib/format";

const STATUS_STYLES: Record<string, string> = {
  DRAFT: "bg-slate-500/10 text-slate-300",
  ISSUED: "bg-sky-500/10 text-sky-300",
  ACKNOWLEDGED: "bg-violet-500/10 text-violet-300",
  PARTIALLY_DELIVERED: "bg-amber-500/10 text-amber-300",
  DELIVERED: "bg-emerald-500/10 text-emerald-300",
  CLOSED: "bg-emerald-500/20 text-emerald-200",
  CANCELLED: "bg-rose-500/10 text-rose-300",
};

export default function PurchaseOrdersPage() {
  const { data, isLoading, isError } = usePurchaseOrders({ page: 0, size: 50 });

  return (
    <div className="mx-auto max-w-6xl">
      <h1 className="text-lg font-semibold text-white">Purchase Orders</h1>

      {isLoading && <p className="mt-6 text-sm text-slate-400" role="status">Loading orders…</p>}
      {isError && <p className="mt-6 text-sm text-amber-300" role="alert">Could not load purchase orders.</p>}

      {data && (
        <div className="mt-6 overflow-x-auto rounded-2xl border border-slate-800">
          <table className="w-full min-w-[680px] text-left text-sm">
            <thead className="border-b border-slate-800 bg-slate-900/70 text-xs uppercase tracking-wider text-slate-400">
              <tr>
                <th scope="col" className="px-4 py-3">Number</th>
                <th scope="col" className="px-4 py-3">Vendor</th>
                <th scope="col" className="px-4 py-3">Expected</th>
                <th scope="col" className="px-4 py-3 text-right">Total</th>
                <th scope="col" className="px-4 py-3">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800 bg-slate-950/40">
              {data.content.map((po) => (
                <tr key={po.id} className="hover:bg-slate-900/50">
                  <td className="px-4 py-3 font-mono text-xs text-slate-400">{po.purchaseOrderNumber}</td>
                  <td className="px-4 py-3 font-medium text-white">{po.vendorCompanyName}</td>
                  <td className="px-4 py-3 text-slate-400">{po.expectedDelivery ?? "—"}</td>
                  <td className="px-4 py-3 text-right font-mono text-white">{formatMoney(po.totalAmount)}</td>
                  <td className="px-4 py-3">
                    <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ${STATUS_STYLES[po.status]}`}>
                      {po.status}
                    </span>
                    {po.deliveryOverdue && (
                      <span className="ml-2 rounded-full bg-rose-500/20 px-2 py-0.5 text-xs font-medium text-rose-200">overdue</span>
                    )}
                  </td>
                </tr>
              ))}
              {data.content.length === 0 && (
                <tr><td colSpan={5} className="px-4 py-8 text-center text-slate-400">No purchase orders yet.</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
