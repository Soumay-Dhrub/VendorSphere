/**
 * Dashboard summary and analytics report endpoints (`/analytics/...`).
 *
 * Every figure is aggregated server-side at money scale (Requirement 27); the
 * reports return ordered arrays rather than pages.
 */

import { apiGet } from "./client";
import type { Decimal, IsoDate, Money, Uuid } from "./types";

/** The seven dashboard figures of Requirement 27.1. */
export type DashboardSummaryResponse = {
  totalSpend: Money;
  activeRfqCount: number;
  openPurchaseOrderCount: number;
  pendingDeliveryCount: number;
  outstandingInvoiceCount: number;
  overdueInvoiceCount: number;
  activeVendorCount: number;
};

export type MonthlySpendParams = {
  from?: IsoDate;
  to?: IsoDate;
};

export type MonthlySpendEntry = {
  /** Calendar month as `YYYY-MM`. */
  month: string;
  amount: Money;
};

export type SpendByDepartmentEntry = {
  departmentId: Uuid | null;
  departmentName: string;
  amount: Money;
};

/** Ordered by amount descending (Requirement 27.9). */
export type SpendByVendorEntry = {
  vendorId: Uuid;
  vendorCompanyName: string;
  amount: Money;
};

export type CategoryDistributionEntry = {
  categoryId: Uuid | null;
  categoryName: string;
  purchaseOrderCount: number;
  amount: Money;
};

export type VendorPerformanceReportEntry = {
  vendorId: Uuid;
  vendorCompanyName: string;
  deliveryScore: Decimal;
  responsivenessScore: Decimal;
  performanceScore: Decimal;
};

/** Mean cycle time in days over closed purchase orders, one decimal place. */
export type CycleTimeResponse = {
  averageCycleTimeDays: Decimal;
  closedPurchaseOrderCount: number;
};

export function getDashboardSummary(): Promise<DashboardSummaryResponse> {
  return apiGet<DashboardSummaryResponse>("/analytics/dashboard");
}

export function getMonthlySpend(
  params: MonthlySpendParams = {},
): Promise<MonthlySpendEntry[]> {
  return apiGet<MonthlySpendEntry[]>("/analytics/spend/monthly", params);
}

export function getSpendByDepartment(): Promise<SpendByDepartmentEntry[]> {
  return apiGet<SpendByDepartmentEntry[]>("/analytics/spend/by-department");
}

export function getSpendByVendor(): Promise<SpendByVendorEntry[]> {
  return apiGet<SpendByVendorEntry[]>("/analytics/spend/by-vendor");
}

export function getCategoryDistribution(): Promise<CategoryDistributionEntry[]> {
  return apiGet<CategoryDistributionEntry[]>("/analytics/categories");
}

export function getVendorPerformanceReport(): Promise<
  VendorPerformanceReportEntry[]
> {
  return apiGet<VendorPerformanceReportEntry[]>("/analytics/vendor-performance");
}

export function getCycleTime(): Promise<CycleTimeResponse> {
  return apiGet<CycleTimeResponse>("/analytics/cycle-time");
}
