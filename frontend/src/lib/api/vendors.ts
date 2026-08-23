/**
 * Vendor, vendor contact, vendor document and vendor category endpoints.
 *
 * Paths follow the API surface of the design: `/vendors`, `/vendors/{id}/...`
 * and `/vendor-categories`, all relative to the `/api/v1` base URL of
 * `apiClient`. The vendor list is paged (Requirement 31.1); the child
 * collections are returned as ordered arrays.
 */

import {
  apiDelete,
  apiGet,
  apiGetPage,
  apiPatch,
  apiPost,
  apiPostForm,
  apiPut,
  fileForm,
} from "./client";
import type {
  AttachmentResponse,
  Decimal,
  DocumentExpiryState,
  IsoDate,
  IsoInstant,
  Money,
  PageParams,
  PageResponse,
  Uuid,
  VendorDocumentType,
  VendorStatus,
} from "./types";

export type VendorRequest = {
  companyName: string;
  contactPerson?: string | null;
  email: string;
  phone?: string | null;
  address?: string | null;
  taxIdentifier?: string | null;
  categoryId?: Uuid | null;
};

export type VendorResponse = {
  id: Uuid;
  vendorCode: string;
  companyName: string;
  contactPerson: string | null;
  email: string;
  phone: string | null;
  address: string | null;
  taxIdentifier: string | null;
  status: VendorStatus;
  rating: Decimal;
  categoryId: Uuid | null;
  categoryName: string | null;
  performanceScore: Decimal;
  expiringDocumentCount: number;
  registeredAt: IsoInstant;
  createdAt: IsoInstant;
};

/** Sortable fields of the vendor list (Requirement 6.7). */
export type VendorSortField = "companyName" | "registeredAt" | "rating" | "status";

export type VendorListParams = PageParams & {
  companyName?: string;
  categoryId?: Uuid;
  status?: VendorStatus;
  minRating?: number;
};

/** A reason is required for SUSPENDED, BLACKLISTED and INACTIVE (Req 3.4). */
export type VendorStatusChangeRequest = {
  status: VendorStatus;
  reason?: string;
};

export type VendorContactRequest = {
  name: string;
  email?: string | null;
  phone?: string | null;
  designation?: string | null;
  primaryContact: boolean;
};

export type VendorContactResponse = {
  id: Uuid;
  vendorId: Uuid;
  name: string;
  email: string | null;
  phone: string | null;
  designation: string | null;
  primaryContact: boolean;
  createdAt: IsoInstant;
};

export type VendorDocumentUpload = {
  file: File;
  documentType: VendorDocumentType;
  documentNumber?: string;
  expiryDate?: IsoDate;
};

export type VendorDocumentResponse = {
  id: Uuid;
  vendorId: Uuid;
  documentType: VendorDocumentType;
  documentNumber: string | null;
  expiryDate: IsoDate | null;
  expiryState: DocumentExpiryState;
  attachment: AttachmentResponse;
  uploadedAt: IsoInstant;
};

/** The five metrics plus the derived score (Requirements 26.1–26.7, 17.9). */
export type VendorPerformanceResponse = {
  vendorId: Uuid;
  deliveryScore: Decimal;
  qualityScore: Decimal;
  pricingScore: Decimal;
  responsivenessScore: Decimal;
  fulfilmentScore: Decimal;
  performanceScore: Decimal;
  rating: Decimal;
  awardedPurchaseOrderCount: number;
  completedDeliveryCount: number;
  totalSpend: Money;
  calculatedAt: IsoInstant;
};

export type VendorCategoryRequest = {
  name: string;
  description?: string | null;
};

export type VendorCategoryResponse = {
  id: Uuid;
  name: string;
  description: string | null;
  vendorCount: number;
  createdAt: IsoInstant;
};

export function listVendors(
  params: VendorListParams = {},
): Promise<PageResponse<VendorResponse>> {
  return apiGetPage<VendorResponse>("/vendors", params);
}

export function createVendor(input: VendorRequest): Promise<VendorResponse> {
  return apiPost<VendorResponse>("/vendors", input);
}

export function getVendor(id: Uuid): Promise<VendorResponse> {
  return apiGet<VendorResponse>(`/vendors/${id}`);
}

export function updateVendor(
  id: Uuid,
  input: VendorRequest,
): Promise<VendorResponse> {
  return apiPut<VendorResponse>(`/vendors/${id}`, input);
}

export function changeVendorStatus(
  id: Uuid,
  input: VendorStatusChangeRequest,
): Promise<VendorResponse> {
  return apiPatch<VendorResponse>(`/vendors/${id}/status`, input);
}

export function listVendorContacts(
  vendorId: Uuid,
): Promise<VendorContactResponse[]> {
  return apiGet<VendorContactResponse[]>(`/vendors/${vendorId}/contacts`);
}

export function createVendorContact(
  vendorId: Uuid,
  input: VendorContactRequest,
): Promise<VendorContactResponse> {
  return apiPost<VendorContactResponse>(`/vendors/${vendorId}/contacts`, input);
}

export function updateVendorContact(
  vendorId: Uuid,
  contactId: Uuid,
  input: VendorContactRequest,
): Promise<VendorContactResponse> {
  return apiPut<VendorContactResponse>(
    `/vendors/${vendorId}/contacts/${contactId}`,
    input,
  );
}

export function deleteVendorContact(
  vendorId: Uuid,
  contactId: Uuid,
): Promise<void> {
  return apiDelete(`/vendors/${vendorId}/contacts/${contactId}`);
}

export function listVendorDocuments(
  vendorId: Uuid,
): Promise<VendorDocumentResponse[]> {
  return apiGet<VendorDocumentResponse[]>(`/vendors/${vendorId}/documents`);
}

export function uploadVendorDocument(
  vendorId: Uuid,
  input: VendorDocumentUpload,
): Promise<VendorDocumentResponse> {
  const form = fileForm(input.file, {
    documentType: input.documentType,
    documentNumber: input.documentNumber,
    expiryDate: input.expiryDate,
  });
  return apiPostForm<VendorDocumentResponse>(
    `/vendors/${vendorId}/documents`,
    form,
  );
}

export function getVendorPerformance(
  vendorId: Uuid,
): Promise<VendorPerformanceResponse> {
  return apiGet<VendorPerformanceResponse>(`/vendors/${vendorId}/performance`);
}

export function listVendorCategories(): Promise<VendorCategoryResponse[]> {
  return apiGet<VendorCategoryResponse[]>("/vendor-categories");
}

export function createVendorCategory(
  input: VendorCategoryRequest,
): Promise<VendorCategoryResponse> {
  return apiPost<VendorCategoryResponse>("/vendor-categories", input);
}

export function updateVendorCategory(
  id: Uuid,
  input: VendorCategoryRequest,
): Promise<VendorCategoryResponse> {
  return apiPut<VendorCategoryResponse>(`/vendor-categories/${id}`, input);
}

export function deleteVendorCategory(id: Uuid): Promise<void> {
  return apiDelete(`/vendor-categories/${id}`);
}
