"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  addPurchaseRequestItem,
  approvePurchaseRequest,
  createPurchaseRequest,
  deletePurchaseRequestItem,
  getPurchaseRequest,
  listPurchaseRequests,
  rejectPurchaseRequest,
  submitPurchaseRequest,
  updatePurchaseRequest,
  updatePurchaseRequestItem,
  uploadPurchaseRequestAttachment,
  type PurchaseRequestApprovalRequest,
  type PurchaseRequestItemRequest,
  type PurchaseRequestListParams,
  type PurchaseRequestRejectionRequest,
  type PurchaseRequestRequest,
} from "@/lib/api/purchaseRequests";
import type { Uuid } from "@/lib/api/types";
import { notificationKeys, purchaseRequestKeys } from "./keys";

export function usePurchaseRequests(params: PurchaseRequestListParams = {}) {
  return useQuery({
    queryKey: purchaseRequestKeys.list(params),
    queryFn: () => listPurchaseRequests(params),
  });
}

export function usePurchaseRequest(id: Uuid | undefined) {
  return useQuery({
    queryKey: purchaseRequestKeys.detail(id ?? ""),
    queryFn: () => getPurchaseRequest(id as Uuid),
    enabled: Boolean(id),
  });
}

export function useCreatePurchaseRequest() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: PurchaseRequestRequest) => createPurchaseRequest(input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: purchaseRequestKeys.lists() });
    },
  });
}

export function useUpdatePurchaseRequest(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: PurchaseRequestRequest) =>
      updatePurchaseRequest(id, input),
    onSuccess: () => invalidateOne(queryClient, id),
  });
}

export function useAddPurchaseRequestItem(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: PurchaseRequestItemRequest) =>
      addPurchaseRequestItem(id, input),
    onSuccess: () => invalidateOne(queryClient, id),
  });
}

export function useUpdatePurchaseRequestItem(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (variables: {
      itemId: Uuid;
      input: PurchaseRequestItemRequest;
    }) => updatePurchaseRequestItem(id, variables.itemId, variables.input),
    onSuccess: () => invalidateOne(queryClient, id),
  });
}

export function useDeletePurchaseRequestItem(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (itemId: Uuid) => deletePurchaseRequestItem(id, itemId),
    onSuccess: () => invalidateOne(queryClient, id),
  });
}

export function useUploadPurchaseRequestAttachment(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => uploadPurchaseRequestAttachment(id, file),
    onSuccess: () => invalidateOne(queryClient, id),
  });
}

export function useSubmitPurchaseRequest(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => submitPurchaseRequest(id),
    onSuccess: () => {
      invalidateOne(queryClient, id);
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

export function useApprovePurchaseRequest(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: PurchaseRequestApprovalRequest = {}) =>
      approvePurchaseRequest(id, input),
    onSuccess: () => {
      invalidateOne(queryClient, id);
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

export function useRejectPurchaseRequest(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: PurchaseRequestRejectionRequest) =>
      rejectPurchaseRequest(id, input),
    onSuccess: () => {
      invalidateOne(queryClient, id);
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

function invalidateOne(
  queryClient: ReturnType<typeof useQueryClient>,
  id: Uuid,
): void {
  void queryClient.invalidateQueries({ queryKey: purchaseRequestKeys.detail(id) });
  void queryClient.invalidateQueries({ queryKey: purchaseRequestKeys.lists() });
}
