
import type { Uuid } from "@/lib/api/types";

type Params = Record<string, unknown>;

export const vendorKeys = {
  all: ["vendors"] as const,
  lists: () => [...vendorKeys.all, "list"] as const,
  list: (params: Params = {}) => [...vendorKeys.lists(), params] as const,
  detail: (id: Uuid) => [...vendorKeys.all, "detail", id] as const,
  contacts: (id: Uuid) => [...vendorKeys.detail(id), "contacts"] as const,
  documents: (id: Uuid) => [...vendorKeys.detail(id), "documents"] as const,
  performance: (id: Uuid) => [...vendorKeys.detail(id), "performance"] as const,
};

export const vendorCategoryKeys = {
  all: ["vendor-categories"] as const,
  list: () => [...vendorCategoryKeys.all, "list"] as const,
};

export const purchaseRequestKeys = {
  all: ["purchase-requests"] as const,
  lists: () => [...purchaseRequestKeys.all, "list"] as const,
  list: (params: Params = {}) => [...purchaseRequestKeys.lists(), params] as const,
  detail: (id: Uuid) => [...purchaseRequestKeys.all, "detail", id] as const,
};

export const rfqKeys = {
  all: ["rfqs"] as const,
  lists: () => [...rfqKeys.all, "list"] as const,
  list: (params: Params = {}) => [...rfqKeys.lists(), params] as const,
  detail: (id: Uuid) => [...rfqKeys.all, "detail", id] as const,
  vendors: (id: Uuid) => [...rfqKeys.detail(id), "vendors"] as const,
};

export const quotationKeys = {
  all: ["quotations"] as const,
  byRfq: (rfqId: Uuid, params: Params = {}) =>
    [...quotationKeys.all, "by-rfq", rfqId, params] as const,
  detail: (id: Uuid) => [...quotationKeys.all, "detail", id] as const,
  comparison: (rfqId: Uuid) =>
    [...quotationKeys.all, "comparison", rfqId] as const,
};

export const criteriaWeightKeys = {
  all: ["evaluation-criteria-weights"] as const,
};

export const purchaseOrderKeys = {
  all: ["purchase-orders"] as const,
  lists: () => [...purchaseOrderKeys.all, "list"] as const,
  list: (params: Params = {}) => [...purchaseOrderKeys.lists(), params] as const,
  detail: (id: Uuid) => [...purchaseOrderKeys.all, "detail", id] as const,
};

export const deliveryKeys = {
  all: ["deliveries"] as const,
  lists: () => [...deliveryKeys.all, "list"] as const,
  list: (params: Params = {}) => [...deliveryKeys.lists(), params] as const,
  detail: (id: Uuid) => [...deliveryKeys.all, "detail", id] as const,
  byPurchaseOrder: (purchaseOrderId: Uuid, params: Params = {}) =>
    [...deliveryKeys.all, "by-purchase-order", purchaseOrderId, params] as const,
  progress: (purchaseOrderId: Uuid) =>
    [...deliveryKeys.all, "progress", purchaseOrderId] as const,
};

export const invoiceKeys = {
  all: ["invoices"] as const,
  lists: () => [...invoiceKeys.all, "list"] as const,
  list: (params: Params = {}) => [...invoiceKeys.lists(), params] as const,
  detail: (id: Uuid) => [...invoiceKeys.all, "detail", id] as const,
  match: (id: Uuid) => [...invoiceKeys.detail(id), "match"] as const,
};

export const paymentKeys = {
  all: ["payments"] as const,
  lists: () => [...paymentKeys.all, "list"] as const,
  list: (params: Params = {}) => [...paymentKeys.lists(), params] as const,
  byInvoice: (invoiceId: Uuid, params: Params = {}) =>
    [...paymentKeys.all, "by-invoice", invoiceId, params] as const,
  outstanding: () => [...paymentKeys.all, "outstanding"] as const,
};

export const analyticsKeys = {
  all: ["analytics"] as const,
  dashboard: () => [...analyticsKeys.all, "dashboard"] as const,
  monthlySpend: (params: Params = {}) =>
    [...analyticsKeys.all, "spend", "monthly", params] as const,
  spendByDepartment: () =>
    [...analyticsKeys.all, "spend", "by-department"] as const,
  spendByVendor: () => [...analyticsKeys.all, "spend", "by-vendor"] as const,
  categories: () => [...analyticsKeys.all, "categories"] as const,
  vendorPerformance: () =>
    [...analyticsKeys.all, "vendor-performance"] as const,
  cycleTime: () => [...analyticsKeys.all, "cycle-time"] as const,
};

export const notificationKeys = {
  all: ["notifications"] as const,
  lists: () => [...notificationKeys.all, "list"] as const,
  list: (params: Params = {}) => [...notificationKeys.lists(), params] as const,
  unreadCount: () => [...notificationKeys.all, "unread-count"] as const,
};

export const auditKeys = {
  all: ["audit-logs"] as const,
  list: (params: Params = {}) => [...auditKeys.all, "list", params] as const,
};
