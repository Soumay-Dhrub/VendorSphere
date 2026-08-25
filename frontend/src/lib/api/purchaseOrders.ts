
import { apiGet, apiGetPage, apiPost, apiPut } from "./client";
import type {
  Decimal,
  IsoDate,
  IsoInstant,
  Money,
  PageParams,
  PageResponse,
  PurchaseOrderStatus,
  Quantity,
  Uuid,
} from "./types";

export type PurchaseOrderItemResponse = {
  id: Uuid;
  purchaseOrderId: Uuid;
  quotationItemId: Uuid | null;
  itemName: string;
  quantity: Quantity;
  unitPrice: Money;
  taxRate: Decimal;
  taxAmount: Money;
  lineTotal: Money;
  deliveredQuantity: Quantity;
};

export type PurchaseOrderResponse = {
  id: Uuid;
  purchaseOrderNumber: string;
  rfqId: Uuid | null;
  rfqNumber: string | null;
  quotationId: Uuid | null;
  vendorId: Uuid;
  vendorCompanyName: string | null;
  status: PurchaseOrderStatus;
  items: PurchaseOrderItemResponse[];
  subtotal: Money;
  taxAmount: Money;
  totalAmount: Money;
  deliveryAddress: string | null;
  paymentTerms: string | null;
  termsAndConditions: string | null;
  expectedDelivery: IsoDate | null;
  deliveryOverdue: boolean;
  cancellationReason: string | null;
  issuedBy: Uuid | null;
  issuedAt: IsoInstant | null;
  acknowledgedAt: IsoInstant | null;
  closedAt: IsoInstant | null;
  createdAt: IsoInstant;
  updatedAt: IsoInstant;
};

export type PurchaseOrderListParams = PageParams & {
  status?: PurchaseOrderStatus;
  vendorId?: Uuid;
  deliveryOverdue?: boolean;
};

export type PurchaseOrderUpdateRequest = {
  deliveryAddress?: string | null;
  expectedDelivery?: IsoDate | null;
  paymentTerms?: string | null;
  termsAndConditions?: string | null;
};

export type PurchaseOrderCancellationRequest = {
  reason: string;
};

export function generatePurchaseOrderFromRfq(
  rfqId: Uuid,
): Promise<PurchaseOrderResponse> {
  return apiPost<PurchaseOrderResponse>(`/rfqs/${rfqId}/purchase-order`);
}

export function listPurchaseOrders(
  params: PurchaseOrderListParams = {},
): Promise<PageResponse<PurchaseOrderResponse>> {
  return apiGetPage<PurchaseOrderResponse>("/purchase-orders", params);
}

export function getPurchaseOrder(id: Uuid): Promise<PurchaseOrderResponse> {
  return apiGet<PurchaseOrderResponse>(`/purchase-orders/${id}`);
}

export function updatePurchaseOrder(
  id: Uuid,
  input: PurchaseOrderUpdateRequest,
): Promise<PurchaseOrderResponse> {
  return apiPut<PurchaseOrderResponse>(`/purchase-orders/${id}`, input);
}

export function issuePurchaseOrder(id: Uuid): Promise<PurchaseOrderResponse> {
  return apiPost<PurchaseOrderResponse>(`/purchase-orders/${id}/issue`);
}

export function acknowledgePurchaseOrder(
  id: Uuid,
): Promise<PurchaseOrderResponse> {
  return apiPost<PurchaseOrderResponse>(`/purchase-orders/${id}/acknowledge`);
}

export function closePurchaseOrder(id: Uuid): Promise<PurchaseOrderResponse> {
  return apiPost<PurchaseOrderResponse>(`/purchase-orders/${id}/close`);
}

export function cancelPurchaseOrder(
  id: Uuid,
  input: PurchaseOrderCancellationRequest,
): Promise<PurchaseOrderResponse> {
  return apiPost<PurchaseOrderResponse>(`/purchase-orders/${id}/cancel`, input);
}
