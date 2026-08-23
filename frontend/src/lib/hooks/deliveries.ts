"use client";

/**
 * Query and mutation hooks for delivery recording and delivery progress.
 *
 * Recording a delivery is the widest-reaching mutation of the lifecycle: it
 * recomputes delivered quantities and purchase order status, re-evaluates the
 * three-way match of any open invoice on the order, and recalculates vendor
 * performance (Requirements 20.9, 23.10, 26.9). The invalidations mirror that.
 */

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  getDelivery,
  getDeliveryProgress,
  listDeliveries,
  listPurchaseOrderDeliveries,
  recordDelivery,
  type DeliveryListParams,
  type DeliveryRequest,
} from "@/lib/api/deliveries";
import type { PageParams, Uuid } from "@/lib/api/types";
import {
  analyticsKeys,
  deliveryKeys,
  invoiceKeys,
  notificationKeys,
  purchaseOrderKeys,
  vendorKeys,
} from "./keys";

export function useDeliveries(params: DeliveryListParams = {}) {
  return useQuery({
    queryKey: deliveryKeys.list(params),
    queryFn: () => listDeliveries(params),
  });
}

export function useDelivery(id: Uuid | undefined) {
  return useQuery({
    queryKey: deliveryKeys.detail(id ?? ""),
    queryFn: () => getDelivery(id as Uuid),
    enabled: Boolean(id),
  });
}

export function usePurchaseOrderDeliveries(
  purchaseOrderId: Uuid | undefined,
  params: PageParams = {},
) {
  return useQuery({
    queryKey: deliveryKeys.byPurchaseOrder(purchaseOrderId ?? "", params),
    queryFn: () => listPurchaseOrderDeliveries(purchaseOrderId as Uuid, params),
    enabled: Boolean(purchaseOrderId),
  });
}

export function useDeliveryProgress(purchaseOrderId: Uuid | undefined) {
  return useQuery({
    queryKey: deliveryKeys.progress(purchaseOrderId ?? ""),
    queryFn: () => getDeliveryProgress(purchaseOrderId as Uuid),
    enabled: Boolean(purchaseOrderId),
  });
}

export function useRecordDelivery(purchaseOrderId: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: DeliveryRequest) =>
      recordDelivery(purchaseOrderId, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: deliveryKeys.all });
      // Delivered quantities and the derived order status change (Req 20.9).
      void queryClient.invalidateQueries({
        queryKey: purchaseOrderKeys.detail(purchaseOrderId),
      });
      void queryClient.invalidateQueries({ queryKey: purchaseOrderKeys.lists() });
      // Open invoices on the order are re-matched (Requirement 23.10).
      void queryClient.invalidateQueries({ queryKey: invoiceKeys.all });
      void queryClient.invalidateQueries({ queryKey: vendorKeys.all });
      void queryClient.invalidateQueries({ queryKey: analyticsKeys.all });
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}
