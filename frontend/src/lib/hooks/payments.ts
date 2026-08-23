"use client";

/**
 * Query and mutation hooks for payments and outstanding payables.
 *
 * Recording a payment sets the invoice paid amount and derives the invoice
 * status server-side (Requirements 25.5–25.7), so the invoice caches and the
 * outstanding payables total are invalidated with it.
 */

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  getOutstandingPayables,
  listInvoicePayments,
  listPayments,
  recordPayment,
  type PaymentListParams,
  type PaymentRequest,
} from "@/lib/api/payments";
import type { PageParams, Uuid } from "@/lib/api/types";
import {
  analyticsKeys,
  invoiceKeys,
  notificationKeys,
  paymentKeys,
} from "./keys";

export function usePayments(params: PaymentListParams = {}) {
  return useQuery({
    queryKey: paymentKeys.list(params),
    queryFn: () => listPayments(params),
  });
}

export function useInvoicePayments(
  invoiceId: Uuid | undefined,
  params: PageParams = {},
) {
  return useQuery({
    queryKey: paymentKeys.byInvoice(invoiceId ?? "", params),
    queryFn: () => listInvoicePayments(invoiceId as Uuid, params),
    enabled: Boolean(invoiceId),
  });
}

export function useOutstandingPayables() {
  return useQuery({
    queryKey: paymentKeys.outstanding(),
    queryFn: getOutstandingPayables,
  });
}

export function useRecordPayment(invoiceId: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: PaymentRequest) => recordPayment(invoiceId, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: paymentKeys.all });
      // Paid amount and invoice status are re-derived (Requirements 25.5–25.7).
      void queryClient.invalidateQueries({
        queryKey: invoiceKeys.detail(invoiceId),
      });
      void queryClient.invalidateQueries({ queryKey: invoiceKeys.lists() });
      void queryClient.invalidateQueries({ queryKey: analyticsKeys.all });
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}
