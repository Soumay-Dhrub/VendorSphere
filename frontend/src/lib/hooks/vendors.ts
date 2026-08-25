"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  changeVendorStatus,
  createVendor,
  createVendorCategory,
  createVendorContact,
  deleteVendorCategory,
  deleteVendorContact,
  getVendor,
  getVendorPerformance,
  listVendorCategories,
  listVendorContacts,
  listVendorDocuments,
  listVendors,
  updateVendor,
  updateVendorCategory,
  updateVendorContact,
  uploadVendorDocument,
  type VendorCategoryRequest,
  type VendorContactRequest,
  type VendorDocumentUpload,
  type VendorListParams,
  type VendorRequest,
  type VendorStatusChangeRequest,
} from "@/lib/api/vendors";
import type { Uuid } from "@/lib/api/types";
import { analyticsKeys, vendorCategoryKeys, vendorKeys } from "./keys";

export function useVendors(params: VendorListParams = {}) {
  return useQuery({
    queryKey: vendorKeys.list(params),
    queryFn: () => listVendors(params),
  });
}

export function useVendor(id: Uuid | undefined) {
  return useQuery({
    queryKey: vendorKeys.detail(id ?? ""),
    queryFn: () => getVendor(id as Uuid),
    enabled: Boolean(id),
  });
}

export function useCreateVendor() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: VendorRequest) => createVendor(input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: vendorKeys.all });
      void queryClient.invalidateQueries({ queryKey: analyticsKeys.all });
    },
  });
}

export function useUpdateVendor(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: VendorRequest) => updateVendor(id, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: vendorKeys.detail(id) });
      void queryClient.invalidateQueries({ queryKey: vendorKeys.lists() });
    },
  });
}

export function useChangeVendorStatus(id: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: VendorStatusChangeRequest) =>
      changeVendorStatus(id, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: vendorKeys.detail(id) });
      void queryClient.invalidateQueries({ queryKey: vendorKeys.lists() });
      void queryClient.invalidateQueries({ queryKey: analyticsKeys.all });
    },
  });
}

export function useVendorContacts(vendorId: Uuid | undefined) {
  return useQuery({
    queryKey: vendorKeys.contacts(vendorId ?? ""),
    queryFn: () => listVendorContacts(vendorId as Uuid),
    enabled: Boolean(vendorId),
  });
}

export function useCreateVendorContact(vendorId: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: VendorContactRequest) =>
      createVendorContact(vendorId, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: vendorKeys.contacts(vendorId),
      });
    },
  });
}

export function useUpdateVendorContact(vendorId: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (variables: { contactId: Uuid; input: VendorContactRequest }) =>
      updateVendorContact(vendorId, variables.contactId, variables.input),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: vendorKeys.contacts(vendorId),
      });
    },
  });
}

export function useDeleteVendorContact(vendorId: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (contactId: Uuid) => deleteVendorContact(vendorId, contactId),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: vendorKeys.contacts(vendorId),
      });
    },
  });
}

export function useVendorDocuments(vendorId: Uuid | undefined) {
  return useQuery({
    queryKey: vendorKeys.documents(vendorId ?? ""),
    queryFn: () => listVendorDocuments(vendorId as Uuid),
    enabled: Boolean(vendorId),
  });
}

export function useUploadVendorDocument(vendorId: Uuid) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: VendorDocumentUpload) =>
      uploadVendorDocument(vendorId, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: vendorKeys.documents(vendorId),
      });
      void queryClient.invalidateQueries({
        queryKey: vendorKeys.detail(vendorId),
      });
    },
  });
}

export function useVendorPerformance(vendorId: Uuid | undefined) {
  return useQuery({
    queryKey: vendorKeys.performance(vendorId ?? ""),
    queryFn: () => getVendorPerformance(vendorId as Uuid),
    enabled: Boolean(vendorId),
  });
}

export function useVendorCategories() {
  return useQuery({
    queryKey: vendorCategoryKeys.list(),
    queryFn: listVendorCategories,
  });
}

export function useCreateVendorCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (input: VendorCategoryRequest) => createVendorCategory(input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: vendorCategoryKeys.all });
    },
  });
}

export function useUpdateVendorCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (variables: { id: Uuid; input: VendorCategoryRequest }) =>
      updateVendorCategory(variables.id, variables.input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: vendorCategoryKeys.all });
      void queryClient.invalidateQueries({ queryKey: vendorKeys.lists() });
    },
  });
}

export function useDeleteVendorCategory() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: Uuid) => deleteVendorCategory(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: vendorCategoryKeys.all });
    },
  });
}
