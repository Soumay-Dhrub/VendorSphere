/**
 * Shared wire types for the VendorSphere API.
 *
 * The backend wraps every response in `ApiResponse` (see
 * `common/dto/ApiResponse.java`) and every paged list in `PageResponse`
 * (`common/dto/PageResponse.java`). The types below mirror those records field
 * for field so the client never guesses at the envelope shape.
 *
 * Money and quantity values arrive already scaled by the server (Requirement
 * 32.7) either as JSON numbers or plain strings, so they are typed as
 * `string | number` and rendered through `formatMoney` from `src/lib/format.ts`.
 * The client never recomputes a monetary figure.
 */

export type { ApiResponse } from "@/lib/api";

/** Paged envelope carried inside `ApiResponse.data` on every list endpoint. */
export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

/** Query parameters accepted by every list endpoint (Requirement 31.2). */
export type PageParams = {
  page?: number;
  size?: number;
  sort?: string;
  direction?: SortDirection;
};

export type SortDirection = "asc" | "desc";

/** Pre-scaled monetary value: two decimals, produced server-side. */
export type Money = string | number;

/** Pre-scaled quantity value: three decimals, produced server-side. */
export type Quantity = string | number;

/** Score, rate, weight and rating values, all pre-scaled server-side. */
export type Decimal = string | number;

/** ISO-8601 instant, e.g. `2026-02-14T09:31:07.412Z`. */
export type IsoInstant = string;

/** ISO-8601 local date, e.g. `2026-02-14`. */
export type IsoDate = string;

export type Uuid = string;

// --- Enumerations -----------------------------------------------------------
// Values match the backend enums exactly; the API serializes them as names.

export type VendorStatus =
  | "PROSPECTIVE"
  | "ACTIVE"
  | "SUSPENDED"
  | "BLACKLISTED"
  | "INACTIVE";

export type VendorDocumentType =
  | "GST_CERTIFICATE"
  | "REGISTRATION_CERTIFICATE"
  | "TAX_DOCUMENT"
  | "COMPLIANCE_CERTIFICATE"
  | "BANK_DETAILS"
  | "AGREEMENT";

export type DocumentExpiryState = "VALID" | "EXPIRING_SOON" | "EXPIRED";

export type PurchaseRequestStatus =
  | "DRAFT"
  | "SUBMITTED"
  | "UNDER_REVIEW"
  | "APPROVED"
  | "REJECTED"
  | "PROCUREMENT_STARTED"
  | "COMPLETED";

export type Priority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";

export type RfqStatus =
  | "DRAFT"
  | "OPEN"
  | "CLOSED"
  | "EVALUATION"
  | "AWARDED"
  | "CANCELLED";

export type RfqVendorStatus =
  | "INVITED"
  | "VIEWED"
  | "RESPONDED"
  | "DECLINED"
  | "AWARDED";

export type QuotationStatus =
  | "DRAFT"
  | "SUBMITTED"
  | "UNDER_REVIEW"
  | "SELECTED"
  | "REJECTED"
  | "WITHDRAWN";

export type PurchaseOrderStatus =
  | "DRAFT"
  | "ISSUED"
  | "ACKNOWLEDGED"
  | "PARTIALLY_DELIVERED"
  | "DELIVERED"
  | "CLOSED"
  | "CANCELLED";

export type InvoiceStatus =
  | "SUBMITTED"
  | "UNDER_REVIEW"
  | "APPROVED"
  | "REJECTED"
  | "PARTIALLY_PAID"
  | "PAID"
  | "OVERDUE";

export type MatchStatus =
  | "PENDING"
  | "MATCHED"
  | "QUANTITY_MISMATCH"
  | "PRICE_MISMATCH"
  | "MISSING_DELIVERY"
  | "DUPLICATE_INVOICE";

/** Declaration order is the precedence order of Requirement 23.7. */
export type MatchFindingType =
  | "DUPLICATE_INVOICE"
  | "MISSING_DELIVERY"
  | "QUANTITY_MISMATCH"
  | "PRICE_MISMATCH";

export type MatchResolutionState = "UNRESOLVED" | "OVERRIDDEN";

export type PaymentStatus = "PENDING" | "PARTIALLY_PAID" | "PAID" | "FAILED";

export type AttachmentOwnerType =
  | "VENDOR_DOCUMENT"
  | "PURCHASE_REQUEST"
  | "RFQ"
  | "QUOTATION"
  | "DELIVERY_PROOF"
  | "INVOICE";

/** Metadata of a stored attachment; the file itself is fetched by identifier. */
export type AttachmentResponse = {
  id: Uuid;
  ownerType: AttachmentOwnerType;
  ownerId: Uuid;
  originalFilename: string;
  contentType: string;
  byteSize: number;
  uploadedBy: Uuid;
  createdAt: IsoInstant;
};
