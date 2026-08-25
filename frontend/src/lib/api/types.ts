
export type { ApiResponse } from "@/lib/api";

export type PageResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
};

export type PageParams = {
  page?: number;
  size?: number;
  sort?: string;
  direction?: SortDirection;
};

export type SortDirection = "asc" | "desc";

export type Money = string | number;

export type Quantity = string | number;

export type Decimal = string | number;

export type IsoInstant = string;

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
