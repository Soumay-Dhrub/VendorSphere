/**
 * Invoice submission, three-way match retrieval, finding override and review
 * endpoints.
 *
 * Submission posts against the purchase order (`POST
 * /purchase-orders/{id}/invoices`); reads and review hang off `/invoices`.
 * Line totals and the invoice total are computed server-side (Requirement 22.3).
 */

import { apiGet, apiGetPage, apiPost } from "./client";
import type {
  AttachmentResponse,
  InvoiceStatus,
  IsoDate,
  IsoInstant,
  MatchFindingType,
  MatchResolutionState,
  MatchStatus,
  Money,
  PageParams,
  PageResponse,
  Quantity,
  Uuid,
} from "./types";

export type InvoiceItemRequest = {
  purchaseOrderItemId: Uuid;
  itemName?: string | null;
  quantity: Quantity;
  unitPrice: Money;
  taxAmount?: Money;
};

export type InvoiceRequest = {
  invoiceNumber: string;
  invoiceDate: IsoDate;
  /** On or after the invoice date (Requirement 22.5). */
  dueDate: IsoDate;
  discountAmount?: Money;
  notes?: string | null;
  invoiceDocumentAttachmentId?: Uuid | null;
  items: InvoiceItemRequest[];
};

export type InvoiceItemResponse = {
  id: Uuid;
  invoiceId: Uuid;
  purchaseOrderItemId: Uuid;
  itemName: string;
  quantity: Quantity;
  unitPrice: Money;
  taxAmount: Money;
  lineTotal: Money;
};

export type InvoiceResponse = {
  id: Uuid;
  invoiceNumber: string;
  purchaseOrderId: Uuid;
  purchaseOrderNumber: string | null;
  vendorId: Uuid;
  vendorCompanyName: string | null;
  status: InvoiceStatus;
  matchStatus: MatchStatus;
  invoiceDate: IsoDate;
  dueDate: IsoDate;
  items: InvoiceItemResponse[];
  subtotal: Money;
  taxAmount: Money;
  discountAmount: Money;
  totalAmount: Money;
  paidAmount: Money;
  notes: string | null;
  reviewerId: Uuid | null;
  reviewerName: string | null;
  reviewedAt: IsoInstant | null;
  reviewComments: string | null;
  invoiceDocument: AttachmentResponse | null;
  createdAt: IsoInstant;
  updatedAt: IsoInstant;
};

export type InvoiceListParams = PageParams & {
  status?: InvoiceStatus;
  matchStatus?: MatchStatus;
  vendorId?: Uuid;
  purchaseOrderId?: Uuid;
  dueFrom?: IsoDate;
  dueTo?: IsoDate;
};

export type MatchFindingResponse = {
  id: Uuid;
  invoiceId: Uuid;
  purchaseOrderItemId: Uuid | null;
  findingType: MatchFindingType;
  itemName: string | null;
  expectedValue: string | null;
  actualValue: string | null;
  detail: string | null;
  resolutionState: MatchResolutionState;
  overriddenBy: Uuid | null;
  overriddenByName: string | null;
  overriddenAt: IsoInstant | null;
  overrideJustification: string | null;
  createdAt: IsoInstant;
};

/** Per-item comparison behind the match outcome (Requirement 23.9). */
export type MatchLineResponse = {
  purchaseOrderItemId: Uuid;
  itemName: string;
  orderedQuantity: Quantity;
  receivedQuantity: Quantity;
  invoicedQuantity: Quantity;
  purchaseOrderUnitPrice: Money;
  invoicedUnitPrice: Money;
};

export type MatchResultResponse = {
  invoiceId: Uuid;
  matchStatus: MatchStatus;
  lines: MatchLineResponse[];
  findings: MatchFindingResponse[];
};

/** Justification is mandatory (Requirement 24.5). */
export type MatchFindingOverrideRequest = {
  justification: string;
};

/**
 * Review decision. APPROVED is blocked while any finding is UNRESOLVED
 * (Requirement 24.3); REJECTED requires a reason (Requirement 24.7).
 */
export type InvoiceReviewRequest = {
  status: Extract<InvoiceStatus, "APPROVED" | "REJECTED">;
  comments?: string;
  reason?: string;
};

export function listInvoices(
  params: InvoiceListParams = {},
): Promise<PageResponse<InvoiceResponse>> {
  return apiGetPage<InvoiceResponse>("/invoices", params);
}

export function submitInvoice(
  purchaseOrderId: Uuid,
  input: InvoiceRequest,
): Promise<InvoiceResponse> {
  return apiPost<InvoiceResponse>(
    `/purchase-orders/${purchaseOrderId}/invoices`,
    input,
  );
}

export function getInvoice(id: Uuid): Promise<InvoiceResponse> {
  return apiGet<InvoiceResponse>(`/invoices/${id}`);
}

export function getInvoiceMatch(id: Uuid): Promise<MatchResultResponse> {
  return apiGet<MatchResultResponse>(`/invoices/${id}/match`);
}

export function overrideMatchFinding(
  invoiceId: Uuid,
  findingId: Uuid,
  input: MatchFindingOverrideRequest,
): Promise<MatchFindingResponse> {
  return apiPost<MatchFindingResponse>(
    `/invoices/${invoiceId}/match-findings/${findingId}/override`,
    input,
  );
}

export function reviewInvoice(
  id: Uuid,
  input: InvoiceReviewRequest,
): Promise<InvoiceResponse> {
  return apiPost<InvoiceResponse>(`/invoices/${id}/review`, input);
}
