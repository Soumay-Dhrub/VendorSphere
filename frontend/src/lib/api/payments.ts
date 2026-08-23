/**
 * Payment recording and outstanding payables endpoints.
 *
 * The invoice paid amount and derived status come back from the invoice
 * endpoints; this module only records payments and reads them (Requirements
 * 25.5–25.7), so payment hooks invalidate the invoice caches too.
 */

import { apiGet, apiGetPage, apiPost } from "./client";
import type {
  IsoDate,
  IsoInstant,
  Money,
  PageParams,
  PageResponse,
  PaymentStatus,
  Uuid,
} from "./types";

export type PaymentRequest = {
  /** Must be greater than zero (Requirement 25.3). */
  amount: Money;
  paymentDate: IsoDate;
  paymentReference?: string | null;
  paymentMethod?: string | null;
  notes?: string | null;
};

export type PaymentResponse = {
  id: Uuid;
  invoiceId: Uuid;
  invoiceNumber: string | null;
  vendorId: Uuid | null;
  vendorCompanyName: string | null;
  amount: Money;
  paymentDate: IsoDate;
  paymentReference: string | null;
  paymentMethod: string | null;
  notes: string | null;
  status: PaymentStatus;
  recordedBy: Uuid;
  recordedByName: string | null;
  createdAt: IsoInstant;
};

export type PaymentListParams = PageParams & {
  invoiceId?: Uuid;
  vendorId?: Uuid;
  status?: PaymentStatus;
  from?: IsoDate;
  to?: IsoDate;
};

export type OutstandingPayableVendor = {
  vendorId: Uuid;
  vendorCompanyName: string;
  outstandingAmount: Money;
  invoiceCount: number;
};

/** Organization total plus the per-vendor breakdown (Requirement 25.10). */
export type OutstandingPayablesResponse = {
  totalOutstanding: Money;
  vendors: OutstandingPayableVendor[];
};

export function listInvoicePayments(
  invoiceId: Uuid,
  params: PageParams = {},
): Promise<PageResponse<PaymentResponse>> {
  return apiGetPage<PaymentResponse>(`/invoices/${invoiceId}/payments`, params);
}

export function recordPayment(
  invoiceId: Uuid,
  input: PaymentRequest,
): Promise<PaymentResponse> {
  return apiPost<PaymentResponse>(`/invoices/${invoiceId}/payments`, input);
}

export function listPayments(
  params: PaymentListParams = {},
): Promise<PageResponse<PaymentResponse>> {
  return apiGetPage<PaymentResponse>("/payments", params);
}

export function getOutstandingPayables(): Promise<OutstandingPayablesResponse> {
  return apiGet<OutstandingPayablesResponse>("/payments/outstanding");
}
