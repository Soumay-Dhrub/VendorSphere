"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  acknowledgePurchaseOrder,
  cancelPurchaseOrder,
  closePurchaseOrder,
  generatePurchaseOrderFromRfq,
  getPurchaseOrder,
  issuePurchaseOrder,
  listPurchaseOrders,
  updatePurchaseOrder,
  type PurchaseOrderCancellationRequest,
  type PurchaseOrderListParams,
  type PurchaseOrderUpdateRequest,
} from "@/lib/api/purchaseOrders";
import type { Uuid } from "@/lib/api/types";
import {
  analyticsKeys,
  deliveryKeys,
  notificationKeys,
  purchaseOrderKeys,
  quotationKeys,
  rfqKeys,
  vendorKeys,
} from "./keys";

export function usePurchaseOrders(params: PurchaseOrderListParams = {}) {
  return useQuery({
    queryKey: purchaseOrderKeys.list(params),
    queryFn: () => listPurchaseOrders(params),
  });
}

export function usePurchaseOrder(id: Uuid | undefined) {
  return useQuery({
    queryKey: purchaseOrderKeys.detail(id ?? ""),
    queryFn: () => getPurchaseOrder(id as Uuid),
    enabled: Boolean(id),
  });
}

export function useGeneratePurchaseOrderFromRfq(rfqId: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => generatePurchaseOrderFromRfq(rfqId),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: purchaseOrderKeys.lists() });
      void queryClient.invalidateQueries({ queryKey: rfqKeys.detail(rfqId) });
      void queryClient.invalidateQueries({ queryKey: quotationKeys.all });
    },
  });
}

export function useUpdatePurchaseOrder(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: PurchaseOrderUpdateRequest) =>
      updatePurchaseOrder(id, input),
    onSuccess: () => invalidateOne(queryClient, id),
  });
}

export function useIssuePurchaseOrder(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => issuePurchaseOrder(id),
    onSuccess: () => {
      invalidateOne(queryClient, id);
      void queryClient.invalidateQueries({ queryKey: vendorKeys.all });
      void queryClient.invalidateQueries({ queryKey: analyticsKeys.all });
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

export function useAcknowledgePurchaseOrder(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => acknowledgePurchaseOrder(id),
    onSuccess: () => {
      invalidateOne(queryClient, id);
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

export function useClosePurchaseOrder(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => closePurchaseOrder(id),
    onSuccess: () => {
      invalidateOne(queryClient, id);
      void queryClient.invalidateQueries({ queryKey: vendorKeys.all });
      void queryClient.invalidateQueries({ queryKey: analyticsKeys.all });
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

export function useCancelPurchaseOrder(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: PurchaseOrderCancellationRequest) =>
      cancelPurchaseOrder(id, input),
    onSuccess: () => {
      invalidateOne(queryClient, id);
      void queryClient.invalidateQueries({ queryKey: analyticsKeys.all });
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

function invalidateOne(
  queryClient: ReturnType<typeof useQueryClient>,
  id: Uuid,
): void {
  void queryClient.invalidateQueries({ queryKey: purchaseOrderKeys.detail(id) });
  void queryClient.invalidateQueries({ queryKey: purchaseOrderKeys.lists() });
  // The order status drives the delivery progress view (Requirement 21.1).
  void queryClient.invalidateQueries({ queryKey: deliveryKeys.progress(id) });
}
