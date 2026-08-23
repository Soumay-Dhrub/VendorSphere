/**
 * Purchase request authoring, item management, submission and review endpoints
 * (`/purchase-requests`).
 *
 * Item and attachment mutations are only accepted while the request is DRAFT
 * (Requirements 7.3, 7.6, 8.3); the client surfaces the server rejection rather
 * than duplicating the rule.
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
  IsoDate,
  IsoInstant,
  Money,
  PageParams,
  PageResponse,
  Priority,
  PurchaseRequestStatus,
  Quantity,
  Uuid,
} from "./types";

export type PurchaseRequestRequest = {
  title: string;
  departmentId: Uuid;
  justification?: string | null;
  requiredDate: IsoDate;
  /** Absent priority is stored as MEDIUM (Requirement 7.2). */
  priority?: Priority;
  estimatedBudget?: Money | null;
};

export type PurchaseRequestItemRequest = {
  itemName: string;
  description?: string | null;
  quantity: Quantity;
  unitOfMeasure?: string | null;
  estimatedUnitPrice?: Money | null;
  specifications?: string | null;
};

export type PurchaseRequestItemResponse = {
  id: Uuid;
  purchaseRequestId: Uuid;
  itemName: string;
  description: string | null;
  quantity: Quantity;
  unitOfMeasure: string | null;
  estimatedUnitPrice: Money | null;
  specifications: string | null;
  sortOrder: number;
  createdAt: IsoInstant;
};

/** RFQ derived from a purchase request, surfaced on the detail screen (Req 8.9). */
export type PurchaseRequestRfqRef = {
  rfqId: Uuid;
  rfqNumber: string;
};

export type PurchaseRequestResponse = {
  id: Uuid;
  requestNumber: string;
  title: string;
  departmentId: Uuid;
  departmentName: string | null;
  requesterId: Uuid;
  requesterName: string | null;
  justification: string | null;
  requiredDate: IsoDate;
  priority: Priority;
  status: PurchaseRequestStatus;
  estimatedBudget: Money | null;
  reviewerId: Uuid | null;
  reviewerName: string | null;
  reviewedAt: IsoInstant | null;
  reviewNotes: string | null;
  items: PurchaseRequestItemResponse[];
  attachments: AttachmentResponse[];
  rfqs: PurchaseRequestRfqRef[];
  createdAt: IsoInstant;
  updatedAt: IsoInstant;
};

export type PurchaseRequestListParams = PageParams & {
  status?: PurchaseRequestStatus;
  departmentId?: Uuid;
  requesterId?: Uuid;
  priority?: Priority;
};

export type PurchaseRequestApprovalRequest = {
  comments?: string;
};

/** A reason is required (Requirement 8.6). */
export type PurchaseRequestRejectionRequest = {
  reason: string;
};

export function listPurchaseRequests(
  params: PurchaseRequestListParams = {},
): Promise<PageResponse<PurchaseRequestResponse>> {
  return apiGetPage<PurchaseRequestResponse>("/purchase-requests", params);
}

export function createPurchaseRequest(
  input: PurchaseRequestRequest,
): Promise<PurchaseRequestResponse> {
  return apiPost<PurchaseRequestResponse>("/purchase-requests", input);
}

export function getPurchaseRequest(id: Uuid): Promise<PurchaseRequestResponse> {
  return apiGet<PurchaseRequestResponse>(`/purchase-requests/${id}`);
}

export function updatePurchaseRequest(
  id: Uuid,
  input: PurchaseRequestRequest,
): Promise<PurchaseRequestResponse> {
  return apiPut<PurchaseRequestResponse>(`/purchase-requests/${id}`, input);
}

export function addPurchaseRequestItem(
  id: Uuid,
  input: PurchaseRequestItemRequest,
): Promise<PurchaseRequestItemResponse> {
  return apiPost<PurchaseRequestItemResponse>(
    `/purchase-requests/${id}/items`,
    input,
  );
}

export function updatePurchaseRequestItem(
  id: Uuid,
  itemId: Uuid,
  input: PurchaseRequestItemRequest,
): Promise<PurchaseRequestItemResponse> {
  return apiPut<PurchaseRequestItemResponse>(
    `/purchase-requests/${id}/items/${itemId}`,
    input,
  );
}

export function deletePurchaseRequestItem(
  id: Uuid,
  itemId: Uuid,
): Promise<void> {
  return apiDelete(`/purchase-requests/${id}/items/${itemId}`);
}

export function uploadPurchaseRequestAttachment(
  id: Uuid,
  file: File,
): Promise<AttachmentResponse> {
  return apiPostForm<AttachmentResponse>(
    `/purchase-requests/${id}/attachments`,
    fileForm(file),
  );
}

export function submitPurchaseRequest(
  id: Uuid,
): Promise<PurchaseRequestResponse> {
  return apiPost<PurchaseRequestResponse>(`/purchase-requests/${id}/submit`);
}

export function approvePurchaseRequest(
  id: Uuid,
  input: PurchaseRequestApprovalRequest = {},
): Promise<PurchaseRequestResponse> {
  return apiPost<PurchaseRequestResponse>(
    `/purchase-requests/${id}/approve`,
    input,
  );
}

export function rejectPurchaseRequest(
  id: Uuid,
  input: PurchaseRequestRejectionRequest,
): Promise<PurchaseRequestResponse> {
  return apiPost<PurchaseRequestResponse>(
    `/purchase-requests/${id}/reject`,
    input,
  );
}
