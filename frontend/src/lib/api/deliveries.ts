/**
 * Delivery recording and delivery progress endpoints.
 *
 * Quantities are pre-scaled to three decimals by the server; the per-item
 * progress figures (received, damaged, rejected, outstanding) are derived
 * server-side from the receipts (Requirements 20.9, 21.1) and are never summed
 * on the client.
 */

import { apiGet, apiGetPage, apiPost } from "./client";
import type {
  AttachmentResponse,
  IsoDate,
  IsoInstant,
  PageParams,
  PageResponse,
  PurchaseOrderStatus,
  Quantity,
  Uuid,
} from "./types";

export type DeliveryItemRequest = {
  purchaseOrderItemId: Uuid;
  /** Must be greater than zero (Requirement 20.4). */
  receivedQuantity: Quantity;
  damagedQuantity?: Quantity;
  rejectedQuantity?: Quantity;
  notes?: string | null;
};

export type DeliveryRequest = {
  deliveryDate: IsoDate;
  deliveryNote?: string | null;
  notes?: string | null;
  proofOfDeliveryAttachmentId?: Uuid | null;
  items: DeliveryItemRequest[];
};

export type DeliveryItemResponse = {
  id: Uuid;
  deliveryId: Uuid;
  purchaseOrderItemId: Uuid;
  itemName: string;
  receivedQuantity: Quantity;
  damagedQuantity: Quantity;
  rejectedQuantity: Quantity;
  notes: string | null;
};

export type DeliveryResponse = {
  id: Uuid;
  deliveryNumber: string;
  purchaseOrderId: Uuid;
  purchaseOrderNumber: string | null;
  vendorId: Uuid | null;
  vendorCompanyName: string | null;
  deliveryDate: IsoDate;
  deliveryNote: string | null;
  notes: string | null;
  receivedBy: Uuid;
  receivedByName: string | null;
  items: DeliveryItemResponse[];
  proofOfDelivery: AttachmentResponse | null;
  createdAt: IsoInstant;
};

export type DeliveryListParams = PageParams & {
  purchaseOrderId?: Uuid;
  vendorId?: Uuid;
  from?: IsoDate;
  to?: IsoDate;
};

/** Per purchase order item progress (Requirement 20.9). */
export type ItemProgressResponse = {
  purchaseOrderItemId: Uuid;
  itemName: string;
  orderedQuantity: Quantity;
  receivedQuantity: Quantity;
  damagedQuantity: Quantity;
  rejectedQuantity: Quantity;
  outstandingQuantity: Quantity;
};

export type DeliveryProgressResponse = {
  purchaseOrderId: Uuid;
  status: PurchaseOrderStatus;
  deliveryOverdue: boolean;
  items: ItemProgressResponse[];
};

export function listPurchaseOrderDeliveries(
  purchaseOrderId: Uuid,
  params: PageParams = {},
): Promise<PageResponse<DeliveryResponse>> {
  return apiGetPage<DeliveryResponse>(
    `/purchase-orders/${purchaseOrderId}/deliveries`,
    params,
  );
}

export function recordDelivery(
  purchaseOrderId: Uuid,
  input: DeliveryRequest,
): Promise<DeliveryResponse> {
  return apiPost<DeliveryResponse>(
    `/purchase-orders/${purchaseOrderId}/deliveries`,
    input,
  );
}

export function getDeliveryProgress(
  purchaseOrderId: Uuid,
): Promise<DeliveryProgressResponse> {
  return apiGet<DeliveryProgressResponse>(
    `/purchase-orders/${purchaseOrderId}/delivery-progress`,
  );
}

export function listDeliveries(
  params: DeliveryListParams = {},
): Promise<PageResponse<DeliveryResponse>> {
  return apiGetPage<DeliveryResponse>("/deliveries", params);
}

export function getDelivery(id: Uuid): Promise<DeliveryResponse> {
  return apiGet<DeliveryResponse>(`/deliveries/${id}`);
}
