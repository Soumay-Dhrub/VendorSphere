package com.vendorsphere.delivery;

import com.vendorsphere.delivery.repository.DeliveryRepository;
import com.vendorsphere.notification.NotificationEvent;
import com.vendorsphere.notification.service.NotificationService;
import com.vendorsphere.purchaseorder.PurchaseOrderStatus;
import com.vendorsphere.purchaseorder.entity.PurchaseOrder;
import com.vendorsphere.purchaseorder.repository.PurchaseOrderItemRepository;
import com.vendorsphere.purchaseorder.repository.PurchaseOrderRepository;
import com.vendorsphere.user.RoleName;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Component
public class OverdueDeliveryJob {

    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderItemRepository poItemRepository;
    private final com.vendorsphere.delivery.repository.DeliveryRepository deliveryRepository;
    private final NotificationService notificationService;
    private final Clock clock;

    public OverdueDeliveryJob(
            PurchaseOrderRepository poRepository,
            PurchaseOrderItemRepository poItemRepository,
            com.vendorsphere.delivery.repository.DeliveryRepository deliveryRepository,
            NotificationService notificationService,
            Clock clock
    ) {
        this.poRepository = poRepository;
        this.poItemRepository = poItemRepository;
        this.deliveryRepository = deliveryRepository;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 1 * * *", zone = "UTC")
    @Transactional
    public void run() {
        LocalDate today = LocalDate.now(clock);
        List<PurchaseOrder> candidates = poRepository
                .findByStatusInAndExpectedDeliveryBeforeAndDeliveryOverdueFalse(
                        List.of(PurchaseOrderStatus.ISSUED, PurchaseOrderStatus.ACKNOWLEDGED),
                        today);
        for (PurchaseOrder po : candidates) {
            if (deliveryRepository.existsByPurchaseOrderId(po.getId())) {
                continue; // goods already received: not overdue (Requirement 21.4)
            }
            po.setDeliveryOverdue(true);
            poRepository.save(po);
            notificationService.createForRole(po.getOrganization().getId(),
                    RoleName.PROCUREMENT_OFFICER, NotificationEvent.OVERDUE_DELIVERY_DETECTED,
                    "PurchaseOrder", po.getId(), "Delivery overdue",
                    po.getPoNumber() + " passed its expected delivery date of "
                            + po.getExpectedDelivery() + " with nothing received.");
        }
    }

}
