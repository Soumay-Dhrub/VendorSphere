"use client";

/**
 * Query and mutation hooks for quotations, comparison, evaluation, selection and
 * criteria weights.
 */

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  addQuotationComment,
  evaluateRfq,
  getCriteriaWeights,
  getQuotation,
  getRfqComparison,
  listRfqQuotations,
  reviseQuotation,
  selectQuotation,
  submitQuotation,
  updateCriteriaWeights,
  uploadQuotationDocument,
  type CriteriaWeightsRequest,
  type QuotationCommentRequest,
  type QuotationListParams,
  type QuotationRequest,
  type VendorSelectionRequest,
} from "@/lib/api/quotations";
import type { Uuid } from "@/lib/api/types";
import {
  criteriaWeightKeys,
  notificationKeys,
  purchaseOrderKeys,
  quotationKeys,
  rfqKeys,
  vendorKeys,
} from "./keys";

export function useRfqQuotations(
  rfqId: Uuid | undefined,
  params: QuotationListParams = {},
) {
  return useQuery({
    queryKey: quotationKeys.byRfq(rfqId ?? "", params),
    queryFn: () => listRfqQuotations(rfqId as Uuid, params),
    enabled: Boolean(rfqId),
  });
}

export function useQuotation(id: Uuid | undefined) {
  return useQuery({
    queryKey: quotationKeys.detail(id ?? ""),
    queryFn: () => getQuotation(id as Uuid),
    enabled: Boolean(id),
  });
}

export function useSubmitQuotation(rfqId: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: QuotationRequest) => submitQuotation(rfqId, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: quotationKeys.all });
      void queryClient.invalidateQueries({ queryKey: rfqKeys.detail(rfqId) });
      void queryClient.invalidateQueries({ queryKey: rfqKeys.vendors(rfqId) });
      // Submission recalculates vendor performance (Requirement 26.9).
      void queryClient.invalidateQueries({ queryKey: vendorKeys.all });
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

export function useReviseQuotation(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: QuotationRequest) => reviseQuotation(id, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: quotationKeys.all });
    },
  });
}

export function useUploadQuotationDocument(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => uploadQuotationDocument(id, file),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: quotationKeys.detail(id) });
    },
  });
}

export function useRfqComparison(rfqId: Uuid | undefined) {
  return useQuery({
    queryKey: quotationKeys.comparison(rfqId ?? ""),
    queryFn: () => getRfqComparison(rfqId as Uuid),
    enabled: Boolean(rfqId),
  });
}

/** Scoring leaves the quotation and RFQ statuses untouched (Requirement 16.14). */
export function useEvaluateRfq(rfqId: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => evaluateRfq(rfqId),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: quotationKeys.comparison(rfqId),
      });
    },
  });
}

export function useAddQuotationComment(rfqId: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (variables: {
      quotationId: Uuid;
      input: QuotationCommentRequest;
    }) => addQuotationComment(variables.quotationId, variables.input),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: quotationKeys.comparison(rfqId),
      });
    },
  });
}

/** Award: one quotation SELECTED, the rest REJECTED, RFQ AWARDED (Req 17.1). */
export function useSelectQuotation(rfqId: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: VendorSelectionRequest) =>
      selectQuotation(rfqId, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: quotationKeys.all });
      void queryClient.invalidateQueries({ queryKey: rfqKeys.all });
      void queryClient.invalidateQueries({ queryKey: purchaseOrderKeys.all });
      void queryClient.invalidateQueries({ queryKey: vendorKeys.all });
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

export function useCriteriaWeights() {
  return useQuery({
    queryKey: criteriaWeightKeys.all,
    queryFn: getCriteriaWeights,
  });
}

export function useUpdateCriteriaWeights() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: CriteriaWeightsRequest) => updateCriteriaWeights(input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: criteriaWeightKeys.all });
    },
  });
}
