package com.vendorsphere.rfq.dto;

import com.vendorsphere.rfq.RfqStatus;
import com.vendorsphere.rfq.RfqVendorStatus;
import com.vendorsphere.rfq.entity.Rfq;
import com.vendorsphere.rfq.entity.RfqItem;
import com.vendorsphere.rfq.entity.RfqVendor;
import com.vendorsphere.vendor.entity.Vendor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RfqResponse(
        UUID id,
        String rfqNumber,
        UUID purchaseRequestId,
        String title,
        String description,
        Instant openingDate,
        Instant closingDate,
        String currency,
        String deliveryLocation,
        String terms,
        RfqStatus status,
        String cancellationReason,
        List<RfqItemResponse> items,
        List<RfqVendorResponse> vendors,
        Instant createdAt
) {

    public record RfqItemResponse(
            UUID id,
            UUID sourceItemId,
            String itemName,
            BigDecimal quantity,
            String unit,
            String specification,
            int sortOrder
    ) {

        public static RfqItemResponse from(RfqItem item) {
            return new RfqItemResponse(
                    item.getId(),
                    item.getSourceItemId(),
                    item.getItemName(),
                    item.getQuantity(),
                    item.getUnit(),
                    item.getSpecification(),
                    item.getSortOrder());
        }
    }

    public record RfqVendorResponse(
            UUID vendorId,
            String companyName,
            RfqVendorStatus status,
            Instant invitedAt
    ) {

        public static RfqVendorResponse from(RfqVendor invitation) {
            Vendor vendor = invitation.getVendor();
            return new RfqVendorResponse(
                    vendor.getId(),
                    vendor.getCompanyName(),
                    invitation.getStatus(),
                    invitation.getInvitedAt());
        }
    }

    public static RfqResponse from(
            Rfq rfq, List<RfqItemResponse> items, List<RfqVendorResponse> vendors) {
        return new RfqResponse(
                rfq.getId(),
                rfq.getRfqNumber(),
                rfq.getPurchaseRequest() != null ? rfq.getPurchaseRequest().getId() : null,
                rfq.getTitle(),
                rfq.getDescription(),
                rfq.getOpeningDate(),
                rfq.getClosingDate(),
                rfq.getCurrency(),
                rfq.getDeliveryLocation(),
                rfq.getTerms(),
                rfq.getStatus(),
                rfq.getCancellationReason(),
                items,
                vendors,
                rfq.getCreatedAt());
    }
}
