package com.vendorsphere.delivery.service;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.audit.service.AuditService;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.common.util.Money;
import com.vendorsphere.common.util.ReferenceNumberGenerator;
import com.vendorsphere.common.util.ReferencePrefix;
import com.vendorsphere.delivery.entity.Delivery;
import com.vendorsphere.delivery.entity.DeliveryItem;
import com.vendorsphere.delivery.repository.DeliveryItemRepository;
import com.vendorsphere.delivery.repository.DeliveryRepository;
import com.vendorsphere.notification.NotificationEvent;
import com.vendorsphere.notification.service.NotificationService;
import com.vendorsphere.purchaseorder.PurchaseOrderStatus;
import com.vendorsphere.purchaseorder.entity.PurchaseOrder;
import com.vendorsphere.purchaseorder.entity.PurchaseOrderItem;
import com.vendorsphere.purchaseorder.repository.PurchaseOrderItemRepository;
import com.vendorsphere.purchaseorder.repository.PurchaseOrderRepository;
import com.vendorsphere.user.RoleName;
import com.vendorsphere.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DeliveryService {

    static final String RECEIVED_QUANTITY_MESSAGE = "Received quantity must be greater than zero";

    static final String DAMAGED_EXCEEDS_MESSAGE =
            "Damaged and rejected quantities cannot exceed the received quantity";

    static final String FOREIGN_ITEM_MESSAGE = "Delivery item does not belong to the purchase order";

    static final String NOT_FOUND_MESSAGE = "Purchase order not found";

    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderItemRepository poItemRepository;
    private final DeliveryRepository deliveryRepository;
    private final DeliveryItemRepository deliveryItemRepository;
    private final UserRepository userRepository;
    private final ReferenceNumberGenerator referenceNumberGenerator;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final com.vendorsphere.analytics.service.PerformanceEngine performanceEngine;
    private final Clock clock;

    public DeliveryService(
            PurchaseOrderRepository poRepository,
            PurchaseOrderItemRepository poItemRepository,
            DeliveryRepository deliveryRepository,
            DeliveryItemRepository deliveryItemRepository,
            UserRepository userRepository,
            ReferenceNumberGenerator referenceNumberGenerator,
            NotificationService notificationService,
            AuditService auditService,
            com.vendorsphere.analytics.service.PerformanceEngine performanceEngine,
            Clock clock
    ) {
        this.poRepository = poRepository;
        this.poItemRepository = poItemRepository;
        this.deliveryRepository = deliveryRepository;
        this.deliveryItemRepository = deliveryItemRepository;
        this.userRepository = userRepository;
        this.referenceNumberGenerator = referenceNumberGenerator;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.performanceEngine = performanceEngine;
        this.clock = clock;
    }

    @Transactional
    public void record(
            UUID poId, com.vendorsphere.delivery.dto.DeliveryRecordRequest request) {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        PurchaseOrder po = poRepository.findByIdAndOrganizationId(poId, organizationId)
                .orElseThrow(() -> new BusinessException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
        if (po.getStatus() != PurchaseOrderStatus.ISSUED
                && po.getStatus() != PurchaseOrderStatus.ACKNOWLEDGED
                && po.getStatus() != PurchaseOrderStatus.PARTIALLY_DELIVERED) {
            throw new BusinessException(
                    "Cannot record a delivery against a " + po.getStatus() + " purchase order",
                    HttpStatus.CONFLICT);
        }

        List<PurchaseOrderItem> orderItems =
                poItemRepository.findByPurchaseOrderIdOrderByCreatedAtAscIdAsc(po.getId());

        // Validation pass first - all-or-nothing like invitations.
        Map<UUID, BigDecimal> receivedByItemId = new HashMap<>();
        for (var line : request.items()) {
            PurchaseOrderItem orderItem = orderItems.stream()
                    .filter(item -> item.getId().equals(line.purchaseOrderItemId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(
                            FOREIGN_ITEM_MESSAGE, HttpStatus.BAD_REQUEST));
            if (line.receivedQuantity() == null || line.receivedQuantity().signum() <= 0) {
                throw new BusinessException(RECEIVED_QUANTITY_MESSAGE, HttpStatus.BAD_REQUEST);
            }
            BigDecimal damaged = line.damagedQuantity() == null
                    ? Money.ZERO_QUANTITY : line.damagedQuantity();
            BigDecimal rejected = line.rejectedQuantity() == null
                    ? Money.ZERO_QUANTITY : line.rejectedQuantity();
            if (damaged.compareTo(line.receivedQuantity()) > 0
                    || rejected.compareTo(line.receivedQuantity()) > 0) {
                throw new BusinessException(DAMAGED_EXCEEDS_MESSAGE, HttpStatus.BAD_REQUEST);
            }
            BigDecimal cumulative = receivedByItemId.merge(
                    orderItem.getId(), line.receivedQuantity(), BigDecimal::add);
            if (cumulative.compareTo(orderItem.getQuantity()) > 0) {
                throw new BusinessException(overReceiveMessage(orderItem, cumulative),
                        HttpStatus.CONFLICT);
            }
        }

        Delivery delivery = new Delivery();
        delivery.setPurchaseOrder(po);
        delivery.setDeliveryNumber(
                referenceNumberGenerator.allocate(organizationId, ReferencePrefix.DEL));
        delivery.setDeliveryDate(request.deliveryDate());
        delivery.setReceivedBy(userRepository.getReferenceById(SecurityUtils.getCurrentUserId()));
        delivery.setNotes(request.notes());
        delivery.setProofDocumentUrl(request.proofDocumentUrl());
        Delivery savedDelivery = deliveryRepository.save(delivery);

        for (var line : request.items()) {
            PurchaseOrderItem orderItem = orderItems.stream()
                    .filter(item -> item.getId().equals(line.purchaseOrderItemId()))
                    .findFirst()
                    .orElseThrow();
            DeliveryItem item = new DeliveryItem();
            item.setDelivery(savedDelivery);
            item.setPurchaseOrderItem(orderItem);
            item.setReceivedQuantity(Money.quantity(line.receivedQuantity()));
            item.setDamagedQuantity(Money.quantity(
                    line.damagedQuantity() == null ? Money.ZERO_QUANTITY : line.damagedQuantity()));
            item.setRejectedQuantity(Money.quantity(
                    line.rejectedQuantity() == null ? Money.ZERO_QUANTITY : line.rejectedQuantity()));
            item.setNotes(line.notes());
            deliveryItemRepository.save(item);
        }

        deriveProgress(po, orderItems);

        notificationService.createForRole(organizationId, RoleName.PROCUREMENT_OFFICER,
                NotificationEvent.DELIVERY_RECORDED, "Delivery", savedDelivery.getId(),
                "Delivery recorded",
                savedDelivery.getDeliveryNumber() + " received against " + po.getPoNumber() + ".");
        notificationService.createForRole(organizationId, RoleName.FINANCE,
                NotificationEvent.DELIVERY_RECORDED, "Delivery", savedDelivery.getId(),
                "Delivery recorded",
                savedDelivery.getDeliveryNumber() + " received against " + po.getPoNumber() + ".");
        auditService.record(AuditAction.DELIVERY_RECORDED, "Delivery", savedDelivery.getId(),
                null, savedDelivery.getDeliveryNumber());

        // Requirement 26.9: a recorded receipt recalculates the vendor's performance.
        performanceEngine.recalculate(po.getVendor().getId());
    }

    @Transactional(readOnly = true)
    public List<com.vendorsphere.delivery.dto.DeliveryProgressResponse.ItemProgress> progress(
            UUID poId) {
        var po = poRepository.findByIdAndOrganizationId(poId, SecurityUtils.getCurrentOrganizationId())
                .orElseThrow(() -> new BusinessException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
        List<PurchaseOrderItem> items =
                poItemRepository.findByPurchaseOrderIdOrderByCreatedAtAscIdAsc(po.getId());
        return progressOf(po.getId(), items);
    }

    List<com.vendorsphere.delivery.dto.DeliveryProgressResponse.ItemProgress> progressOf(
            UUID poId, List<PurchaseOrderItem> items) {
        List<com.vendorsphere.delivery.dto.DeliveryProgressResponse.ItemProgress> rows =
                new ArrayList<>();
        for (PurchaseOrderItem item : items) {
            BigDecimal received = Money.ZERO_QUANTITY;
            BigDecimal damaged = Money.ZERO_QUANTITY;
            BigDecimal rejected = Money.ZERO_QUANTITY;
            for (Delivery delivery : deliveryRepository
                    .findByPurchaseOrderIdOrderByDeliveryDateAscIdAsc(poId)) {
                for (DeliveryItem line : deliveryItemRepository.findByDeliveryId(delivery.getId())) {
                    if (!line.getPurchaseOrderItem().getId().equals(item.getId())) {
                        continue;
                    }
                    received = received.add(line.getReceivedQuantity());
                    damaged = damaged.add(line.getDamagedQuantity());
                    rejected = rejected.add(line.getRejectedQuantity());
                }
            }
            rows.add(new com.vendorsphere.delivery.dto.DeliveryProgressResponse.ItemProgress(
                    item.getId(), item.getItemName(), item.getQuantity(), received, damaged,
                    rejected, item.getQuantity().subtract(received)));
        }
        return rows;
    }

    private void deriveProgress(PurchaseOrder po, List<PurchaseOrderItem> items) {
        boolean allDelivered = !items.isEmpty();
        boolean anyDelivered = false;
        for (PurchaseOrderItem item : items) {
            BigDecimal received = Money.ZERO_QUANTITY;
            for (Delivery delivery : deliveryRepository
                    .findByPurchaseOrderIdOrderByDeliveryDateAscIdAsc(po.getId())) {
                for (DeliveryItem line : deliveryItemRepository.findByDeliveryId(delivery.getId())) {
                    if (line.getPurchaseOrderItem().getId().equals(item.getId())) {
                        received = received.add(line.getReceivedQuantity());
                    }
                }
            }
            item.setDeliveredQuantity(Money.quantity(received));
            poItemRepository.save(item);
            if (received.signum() > 0) {
                anyDelivered = true;
            }
            if (received.compareTo(item.getQuantity()) < 0) {
                allDelivered = false;
            }
        }

        PurchaseOrderStatus target = allDelivered ? PurchaseOrderStatus.DELIVERED
                : anyDelivered ? PurchaseOrderStatus.PARTIALLY_DELIVERED : null;
        if (target != null && target != po.getStatus()) {
            PurchaseOrderStatusTransitionsHelper.transition(po.getStatus(), target);
            po.setStatus(target);
        }
        if (po.isDeliveryOverdue() && anyDelivered) {
            po.setDeliveryOverdue(false);
        }
        poRepository.save(po);
    }

    static final class PurchaseOrderStatusTransitionsHelper {

        private PurchaseOrderStatusTransitionsHelper() {
        }

        static void transition(PurchaseOrderStatus from, PurchaseOrderStatus to) {
            com.vendorsphere.purchaseorder.PurchaseOrderStatusTransitions.MACHINE
                    .assertTransition(from, to);
        }
    }

    static String overReceiveMessage(PurchaseOrderItem item, BigDecimal cumulative) {
        return "Delivered quantity for " + item.getItemName()
                + " would exceed the ordered quantity of " + item.getQuantity()
                + "; cumulative received is " + cumulative;
    }
}
