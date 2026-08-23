"use client";

/** Query and mutation hooks for RFQs, RFQ items and vendor invitations. */

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  addRfqItem,
  cancelRfq,
  closeRfq,
  createRfq,
  deleteRfqItem,
  getRfq,
  inviteRfqVendors,
  listRfqs,
  listRfqVendors,
  openRfq,
  updateRfq,
  updateRfqItem,
  uploadRfqDocument,
  type RfqCancellationRequest,
  type RfqItemRequest,
  type RfqListParams,
  type RfqRequest,
  type RfqVendorInviteRequest,
} from "@/lib/api/rfqs";
import type { Uuid } from "@/lib/api/types";
import {
  notificationKeys,
  purchaseRequestKeys,
  quotationKeys,
  rfqKeys,
} from "./keys";

export function useRfqs(params: RfqListParams = {}) {
  return useQuery({
    queryKey: rfqKeys.list(params),
    queryFn: () => listRfqs(params),
  });
}

export function useRfq(id: Uuid | undefined) {
  return useQuery({
    queryKey: rfqKeys.detail(id ?? ""),
    queryFn: () => getRfq(id as Uuid),
    enabled: Boolean(id),
  });
}

export function useCreateRfq() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: RfqRequest) => createRfq(input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: rfqKeys.lists() });
      // Creating the first RFQ moves the source purchase request to
      // PROCUREMENT_STARTED (Requirement 9.4).
      void queryClient.invalidateQueries({ queryKey: purchaseRequestKeys.all });
    },
  });
}

export function useUpdateRfq(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: RfqRequest) => updateRfq(id, input),
    onSuccess: () => invalidateOne(queryClient, id),
  });
}

export function useAddRfqItem(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: RfqItemRequest) => addRfqItem(id, input),
    onSuccess: () => invalidateOne(queryClient, id),
  });
}

export function useUpdateRfqItem(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (variables: { itemId: Uuid; input: RfqItemRequest }) =>
      updateRfqItem(id, variables.itemId, variables.input),
    onSuccess: () => invalidateOne(queryClient, id),
  });
}

export function useDeleteRfqItem(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (itemId: Uuid) => deleteRfqItem(id, itemId),
    onSuccess: () => invalidateOne(queryClient, id),
  });
}

export function useRfqVendors(id: Uuid | undefined) {
  return useQuery({
    queryKey: rfqKeys.vendors(id ?? ""),
    queryFn: () => listRfqVendors(id as Uuid),
    enabled: Boolean(id),
  });
}

export function useInviteRfqVendors(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: RfqVendorInviteRequest) => inviteRfqVendors(id, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: rfqKeys.vendors(id) });
      invalidateOne(queryClient, id);
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

export function useUploadRfqDocument(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (file: File) => uploadRfqDocument(id, file),
    onSuccess: () => invalidateOne(queryClient, id),
  });
}

export function useOpenRfq(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => openRfq(id),
    onSuccess: () => {
      invalidateOne(queryClient, id);
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

export function useCloseRfq(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: () => closeRfq(id),
    onSuccess: () => {
      invalidateOne(queryClient, id);
      void queryClient.invalidateQueries({ queryKey: quotationKeys.all });
    },
  });
}

export function useCancelRfq(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: RfqCancellationRequest) => cancelRfq(id, input),
    onSuccess: () => {
      invalidateOne(queryClient, id);
      // Cancellation rejects the open quotations of the RFQ (Requirement 11.7).
      void queryClient.invalidateQueries({ queryKey: quotationKeys.all });
      void queryClient.invalidateQueries({ queryKey: notificationKeys.all });
    },
  });
}

function invalidateOne(
  queryClient: ReturnType<typeof useQueryClient>,
  id: Uuid,
): void {
  void queryClient.invalidateQueries({ queryKey: rfqKeys.detail(id) });
  void queryClient.invalidateQueries({ queryKey: rfqKeys.lists() });
}
