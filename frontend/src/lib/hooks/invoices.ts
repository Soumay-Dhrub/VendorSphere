"use client";

/**
 * Query and mutation hooks for invoices, three-way match findings and review.
 *
 * Submission runs the matcher server-side and stores its findings, so the match
 * cache is invalidated alongside the invoice itself (Requirements 23.1, 23.6).
 */

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  getInvoice,
  getInvoiceMatch,
  listInvoices,
  overrideMatchFinding,
  reviewInvoice,
  submitInvoice,
  type InvoiceListParams,
  type InvoiceRequest,
  type InvoiceReviewRequest,
  type MatchFindingOverrideRequest,
} from "@/lib/api/invoices";
import type { Uuid } from "@/lib/api/types";
import {
  analyticsKeys,
  invoiceKeys,
  notificationKeys,
  paymentKeys,
  purchaseOrderKeys,
} from "./keys";

export function useInvoices(params: InvoiceListParams = {}) {
  return useQuery({
    queryKey: invoiceKeys.list(params),
    queryFn: () => listInvoices(params),
  });
}

export function useInvoice(id: Uuid | undefined) {
  return useQuery({
    queryKey: invoiceKeys.detail(id ?? ""),
    queryFn: () => getInvoice(id as Uuid),
    enabled: Boolean(id),
  });
}

export function useInvoiceMatch(id: Uuid | undefined) {
  return useQuery({
    queryKey: invoiceKeys.match(id ?? ""),
    queryFn: () => getInvoiceMatch(id as Uuid),
    enabled: Boolean(id),
  });
}

export function useSubmitInvoice(purchaseOrderId: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: InvoiceRequest) =>
      submitInvoice(purchaseOrderId, input),
    onSuccess: () => {
      // Submission produces the match status and findings (Requirement 23.6).
      void queryClient.invalidateQueries({ queryKey: invoiceKeys.all });
      void queryClient.invalidateQueries({
        queryKey: purchaseOrderKeys.detail(purchaseOrderId),
      });
      void queryClient.invalidateQueries({ queryKey: paymentKeys.outstanding() });
      void queryClient.invalidateQueries({ queryKey: analyticsKeys.all });
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

/** Overriding clears the approval gate on that finding (Requirement 24.5). */
export function useOverrideMatchFinding(invoiceId: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (variables: {
      findingId: Uuid;
      input: MatchFindingOverrideRequest;
    }) => overrideMatchFinding(invoiceId, variables.findingId, variables.input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: invoiceKeys.match(invoiceId) });
      void queryClient.invalidateQueries({
        queryKey: invoiceKeys.detail(invoiceId),
      });
    },
  });
}

export function useReviewInvoice(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: InvoiceReviewRequest) => reviewInvoice(id, input),
    onSuccess: () => {
      invalidateOne(queryClient, id);
      // Approval makes the invoice payable (Requirement 25.4).
      void queryClient.invalidateQueries({ queryKey: paymentKeys.outstanding() });
      void queryClient.invalidateQueries({ queryKey: analyticsKeys.all });
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

function invalidateOne(
  queryClient: ReturnType<typeof useQueryClient>,
  id: Uuid,
): void {
  void queryClient.invalidateQueries({ queryKey: invoiceKeys.detail(id) });
  void queryClient.invalidateQueries({ queryKey: invoiceKeys.lists() });
}
