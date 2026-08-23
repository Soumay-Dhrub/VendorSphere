"use client";

/**
 * Query hooks for the dashboard summary and the analytics reports.
 *
 * Every figure is aggregated server-side; these hooks are read-only, and the
 * lifecycle mutations that change spend, performance or cycle time invalidate
 * `analyticsKeys.all` from their own modules.
 */

import { useQuery } from "@tanstack/react-query";
import {
  getCategoryDistribution,
  getCycleTime,
  getDashboardSummary,
  getMonthlySpend,
  getSpendByDepartment,
  getSpendByVendor,
  getVendorPerformanceReport,
  type MonthlySpendParams,
} from "@/lib/api/analytics";
import { analyticsKeys } from "./keys";

export function useDashboardSummary() {
  return useQuery({
    queryKey: analyticsKeys.dashboard(),
    queryFn: getDashboardSummary,
  });
}

export function useMonthlySpend(params: MonthlySpendParams = {}) {
  return useQuery({
    queryKey: analyticsKeys.monthlySpend(params),
    queryFn: () => getMonthlySpend(params),
  });
}

export function useSpendByDepartment() {
  return useQuery({
    queryKey: analyticsKeys.spendByDepartment(),
    queryFn: getSpendByDepartment,
  });
}

export function useSpendByVendor() {
  return useQuery({
    queryKey: analyticsKeys.spendByVendor(),
    queryFn: getSpendByVendor,
  });
}

export function useCategoryDistribution() {
  return useQuery({
    queryKey: analyticsKeys.categories(),
    queryFn: getCategoryDistribution,
  });
}

export function useVendorPerformanceReport() {
  return useQuery({
    queryKey: analyticsKeys.vendorPerformance(),
    queryFn: getVendorPerformanceReport,
  });
}

export function useCycleTime() {
  return useQuery({
    queryKey: analyticsKeys.cycleTime(),
    queryFn: getCycleTime,
  });
}
