/**
 * Quotation submission and revision, comparison, evaluation, selection and
 * criteria weight endpoints.
 *
 * Request types deliberately omit every computed figure — tax amount, line
 * total, subtotal, total amount — because the server derives them from the
 * primitives and discards anything else (Requirement 13.6). Response types
 * expose those figures as read-only pre-scaled values.
 */

import { apiGet, apiGetPage, apiPost, apiPostForm, apiPut, fileForm } from "./client";
import type {
  AttachmentResponse,
  Decimal,
  IsoDate,
  IsoInstant,
  Money,
  PageParams,
  PageResponse,
  Quantity,
  QuotationStatus,
  RfqStatus,
  Uuid,
} from "./types";

export type QuotationItemRequest = {
  rfqItemId: Uuid;
  quantity: Quantity;
  unitPrice: Money;
  /** 0.00 to 100.00 (Requirement 12.5). */
  taxRate: Decimal;
  discountAmount?: Money;
};

export type QuotationRequest = {
  items: QuotationItemRequest[];
  shippingAmount?: Money;
  deliveryPeriodDays?: number | null;
  paymentTerms?: string | null;
  warranty?: string | null;
  warrantyMonths?: number | null;
  /** On or after the RFQ closing date (Requirement 12.6). */
  validityDate: IsoDate;
  notes?: string | null;
};

export type QuotationItemResponse = {
  id: Uuid;
  quotationId: Uuid;
  rfqItemId: Uuid;
  itemName: string;
  quantity: Quantity;
  unitPrice: Money;
  taxRate: Decimal;
  discountAmount: Money;
  taxAmount: Money;
  lineTotal: Money;
};

export type QuotationResponse = {
  id: Uuid;
  rfqId: Uuid;
  rfqNumber: string | null;
  vendorId: Uuid;
  vendorCompanyName: string | null;
  status: QuotationStatus;
  items: QuotationItemResponse[];
  subtotal: Money;
  taxAmount: Money;
  discountAmount: Money;
  shippingAmount: Money;
  totalAmount: Money;
  deliveryPeriodDays: number | null;
  paymentTerms: string | null;
  warranty: string | null;
  warrantyMonths: number | null;
  validityDate: IsoDate;
  notes: string | null;
  documents: AttachmentResponse[];
  submittedAt: IsoInstant | null;
  createdAt: IsoInstant;
  updatedAt: IsoInstant;
};

export type QuotationListParams = PageParams & {
  status?: QuotationStatus;
  vendorId?: Uuid;
};

export type ComparisonItemResponse = {
  rfqItemId: Uuid;
  itemName: string;
  quantity: Quantity;
  unitPrice: Money;
  lineTotal: Money;
};

/** One column of the comparison screen (Requirements 15.2, 15.3). */
export type ComparisonRowResponse = {
  quotationId: Uuid;
  vendorId: Uuid;
  vendorCompanyName: string;
  vendorPerformanceScore: Decimal;
  totalAmount: Money;
  subtotal: Money;
  taxAmount: Money;
  discountAmount: Money;
  shippingAmount: Money;
  deliveryPeriodDays: number | null;
  warrantyMonths: number | null;
  paymentTerms: string | null;
  validityDate: IsoDate;
  priceScore: Decimal;
  deliveryScore: Decimal;
  warrantyScore: Decimal;
  performanceScore: Decimal;
  totalScore: Decimal;
  recommended: boolean;
  items: ComparisonItemResponse[];
};

export type ComparisonRfqSummary = {
  rfqId: Uuid;
  rfqNumber: string;
  title: string;
  status: RfqStatus;
  closingDate: IsoInstant;
  currency: string | null;
  itemNames: string[];
};

/** Rows are already ordered by score descending, total ascending (Req 15.6). */
export type ComparisonResponse = {
  rfq: ComparisonRfqSummary;
  rows: ComparisonRowResponse[];
};

export type EvaluationResultResponse = {
  quotationId: Uuid;
  vendorId: Uuid;
  priceScore: Decimal;
  deliveryScore: Decimal;
  warrantyScore: Decimal;
  performanceScore: Decimal;
  totalScore: Decimal;
  recommended: boolean;
  comments: string | null;
};

export type QuotationCommentRequest = {
  comment: string;
};

/** Justification is mandatory (Requirement 17.3). */
export type VendorSelectionRequest = {
  quotationId: Uuid;
  justification: string;
};

export type VendorSelectionResponse = {
  id: Uuid;
  rfqId: Uuid;
  quotationId: Uuid;
  vendorId: Uuid;
  vendorCompanyName: string | null;
  selectedBy: Uuid;
  selectedByName: string | null;
  justification: string;
  selectedAt: IsoInstant;
};

/** The four weights must sum to 1.00 (Requirement 16.11). */
export type CriteriaWeightsRequest = {
  price: Decimal;
  delivery: Decimal;
  performance: Decimal;
  warranty: Decimal;
};

export type CriteriaWeightsResponse = CriteriaWeightsRequest & {
  updatedBy: Uuid | null;
  updatedAt: IsoInstant | null;
};

export function listRfqQuotations(
  rfqId: Uuid,
  params: QuotationListParams = {},
): Promise<PageResponse<QuotationResponse>> {
  return apiGetPage<QuotationResponse>(`/rfqs/${rfqId}/quotations`, params);
}

export function submitQuotation(
  rfqId: Uuid,
  input: QuotationRequest,
): Promise<QuotationResponse> {
  return apiPost<QuotationResponse>(`/rfqs/${rfqId}/quotations`, input);
}

export function getQuotation(id: Uuid): Promise<QuotationResponse> {
  return apiGet<QuotationResponse>(`/quotations/${id}`);
}

export function reviseQuotation(
  id: Uuid,
  input: QuotationRequest,
): Promise<QuotationResponse> {
  return apiPut<QuotationResponse>(`/quotations/${id}`, input);
}

export function uploadQuotationDocument(
  id: Uuid,
  file: File,
): Promise<AttachmentResponse> {
  return apiPostForm<AttachmentResponse>(
    `/quotations/${id}/documents`,
    fileForm(file),
  );
}

export function getRfqComparison(rfqId: Uuid): Promise<ComparisonResponse> {
  return apiGet<ComparisonResponse>(`/rfqs/${rfqId}/comparison`);
}

export function evaluateRfq(rfqId: Uuid): Promise<EvaluationResultResponse[]> {
  return apiPost<EvaluationResultResponse[]>(`/rfqs/${rfqId}/evaluate`);
}

export function addQuotationComment(
  id: Uuid,
  input: QuotationCommentRequest,
): Promise<EvaluationResultResponse> {
  return apiPost<EvaluationResultResponse>(`/quotations/${id}/comments`, input);
}

export function selectQuotation(
  rfqId: Uuid,
  input: VendorSelectionRequest,
): Promise<VendorSelectionResponse> {
  return apiPost<VendorSelectionResponse>(`/rfqs/${rfqId}/select`, input);
}

export function getCriteriaWeights(): Promise<CriteriaWeightsResponse> {
  return apiGet<CriteriaWeightsResponse>("/evaluation-criteria-weights");
}

export function updateCriteriaWeights(
  input: CriteriaWeightsRequest,
): Promise<CriteriaWeightsResponse> {
  return apiPut<CriteriaWeightsResponse>("/evaluation-criteria-weights", input);
}
