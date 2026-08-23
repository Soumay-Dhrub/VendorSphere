/**
 * RFQ creation, item management, vendor invitation and lifecycle endpoints
 * (`/rfqs`).
 *
 * Vendor users see only RFQs their linked vendor is invited to, filtered
 * server-side (Requirement 10.7), so the same list call serves both audiences.
 */

import {
  apiDelete,
  apiGet,
  apiGetPage,
  apiPost,
  apiPostForm,
  apiPut,
  fileForm,
} from "./client";
import type {
  AttachmentResponse,
  IsoInstant,
  PageParams,
  PageResponse,
  Quantity,
  RfqStatus,
  RfqVendorStatus,
  Uuid,
} from "./types";

export type RfqRequest = {
  purchaseRequestId: Uuid;
  title: string;
  description?: string | null;
  openingDate: IsoInstant;
  /** Must be after the opening date (Requirement 9.6). */
  closingDate: IsoInstant;
  currency?: string | null;
  deliveryLocation?: string | null;
  termsAndConditions?: string | null;
};

export type RfqItemRequest = {
  itemName: string;
  description?: string | null;
  quantity: Quantity;
  unitOfMeasure?: string | null;
  specifications?: string | null;
};

export type RfqItemResponse = {
  id: Uuid;
  rfqId: Uuid;
  purchaseRequestItemId: Uuid | null;
  itemName: string;
  description: string | null;
  quantity: Quantity;
  unitOfMeasure: string | null;
  specifications: string | null;
  sortOrder: number;
  createdAt: IsoInstant;
};

export type RfqVendorResponse = {
  id: Uuid;
  rfqId: Uuid;
  vendorId: Uuid;
  vendorCompanyName: string;
  status: RfqVendorStatus;
  invitedAt: IsoInstant;
  viewedAt: IsoInstant | null;
  respondedAt: IsoInstant | null;
};

export type RfqResponse = {
  id: Uuid;
  rfqNumber: string;
  purchaseRequestId: Uuid;
  purchaseRequestNumber: string | null;
  title: string;
  description: string | null;
  status: RfqStatus;
  openingDate: IsoInstant;
  closingDate: IsoInstant;
  currency: string | null;
  deliveryLocation: string | null;
  termsAndConditions: string | null;
  cancellationReason: string | null;
  createdBy: Uuid;
  createdByName: string | null;
  items: RfqItemResponse[];
  invitedVendors: RfqVendorResponse[];
  documents: AttachmentResponse[];
  /** Visible while the RFQ is OPEN, without prices (Requirement 14.5). */
  submittedQuotationCount: number;
  createdAt: IsoInstant;
  updatedAt: IsoInstant;
};

export type RfqListParams = PageParams & {
  status?: RfqStatus;
  purchaseRequestId?: Uuid;
  vendorId?: Uuid;
};

/** All-or-nothing invitation of active vendors (Requirements 10.1–10.3). */
export type RfqVendorInviteRequest = {
  vendorIds: Uuid[];
};

/** A reason is required (Requirement 11.6). */
export type RfqCancellationRequest = {
  reason: string;
};

export function listRfqs(
  params: RfqListParams = {},
): Promise<PageResponse<RfqResponse>> {
  return apiGetPage<RfqResponse>("/rfqs", params);
}

export function createRfq(input: RfqRequest): Promise<RfqResponse> {
  return apiPost<RfqResponse>("/rfqs", input);
}

export function getRfq(id: Uuid): Promise<RfqResponse> {
  return apiGet<RfqResponse>(`/rfqs/${id}`);
}

export function updateRfq(id: Uuid, input: RfqRequest): Promise<RfqResponse> {
  return apiPut<RfqResponse>(`/rfqs/${id}`, input);
}

export function addRfqItem(
  id: Uuid,
  input: RfqItemRequest,
): Promise<RfqItemResponse> {
  return apiPost<RfqItemResponse>(`/rfqs/${id}/items`, input);
}

export function updateRfqItem(
  id: Uuid,
  itemId: Uuid,
  input: RfqItemRequest,
): Promise<RfqItemResponse> {
  return apiPut<RfqItemResponse>(`/rfqs/${id}/items/${itemId}`, input);
}

export function deleteRfqItem(id: Uuid, itemId: Uuid): Promise<void> {
  return apiDelete(`/rfqs/${id}/items/${itemId}`);
}

export function listRfqVendors(id: Uuid): Promise<RfqVendorResponse[]> {
  return apiGet<RfqVendorResponse[]>(`/rfqs/${id}/vendors`);
}

export function inviteRfqVendors(
  id: Uuid,
  input: RfqVendorInviteRequest,
): Promise<RfqVendorResponse[]> {
  return apiPost<RfqVendorResponse[]>(`/rfqs/${id}/vendors`, input);
}

export function uploadRfqDocument(
  id: Uuid,
  file: File,
): Promise<AttachmentResponse> {
  return apiPostForm<AttachmentResponse>(
    `/rfqs/${id}/documents`,
    fileForm(file),
  );
}

export function openRfq(id: Uuid): Promise<RfqResponse> {
  return apiPost<RfqResponse>(`/rfqs/${id}/open`);
}

export function closeRfq(id: Uuid): Promise<RfqResponse> {
  return apiPost<RfqResponse>(`/rfqs/${id}/close`);
}

export function cancelRfq(
  id: Uuid,
  input: RfqCancellationRequest,
): Promise<RfqResponse> {
  return apiPost<RfqResponse>(`/rfqs/${id}/cancel`, input);
}
