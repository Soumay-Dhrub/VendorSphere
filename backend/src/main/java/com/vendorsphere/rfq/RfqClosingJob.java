package com.vendorsphere.rfq;

import com.vendorsphere.notification.NotificationEvent;
import com.vendorsphere.notification.service.NotificationService;
import com.vendorsphere.rfq.repository.RfqRepository;
import com.vendorsphere.rfq.service.RfqVendorService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The scheduled RFQ closing evaluation (Requirements 11.3 through 11.5).
 *
 * <p>Runs every five minutes in UTC, as pinned. Two passes over OPEN RFQs:
 *
 * <ul>
 *   <li>any RFQ past its closing date transitions to CLOSED - the machine step an officer could also
 *       have pressed by hand;</li>
 *   <li>an RFQ closing between 24 and 25 hours out nudges every invited vendor that has not yet
 *       submitted a quotation. The five-minute cadence bounds the window, so each RFQ is nudged on
 *       roughly four consecutive runs; {@code createOnce} dedupe keeps those repeats from stacking
 *       up.</li>
 * </ul>
 */
@Component
public class RfqClosingJob {

    static final Duration NUDGE_WINDOW_START = Duration.ofHours(24);
    static final Duration NUDGE_WINDOW_END = Duration.ofHours(25);

    private final RfqRepository rfqRepository;
    private final RfqVendorService rfqVendorService;
    private final NotificationService notificationService;
    private final Clock clock;

    public RfqClosingJob(
            RfqRepository rfqRepository,
            RfqVendorService rfqVendorService,
            NotificationService notificationService,
            Clock clock
    ) {
        this.rfqRepository = rfqRepository;
        this.rfqVendorService = rfqVendorService;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    @Scheduled(cron = "0 */5 * * * *", zone = "UTC")
    @Transactional
    public void run() {
        Instant now = clock.instant();

        // Requirement 11.3: overdue OPEN RFQs close.
        for (var rfq : rfqRepository.findByStatusAndClosingDateBefore(RfqStatus.OPEN, now)) {
            RfqStatusTransitions.MACHINE.assertTransition(rfq.getStatus(), RfqStatus.CLOSED);
            rfq.setStatus(RfqStatus.CLOSED);
            rfqRepository.save(rfq);
        }

        // Requirement 11.5: non-responding invited vendors get one nudge as the deadline nears.
        List<Object[]> unresponsive = rfqRepository.findUnresponsiveInvitations(
                now.plus(NUDGE_WINDOW_START), now.plus(NUDGE_WINDOW_END));
        for (Object[] row : unresponsive) {
            UUID rfqId = (UUID) row[0];
            UUID vendorId = (UUID) row[1];
            notificationService.createForVendorUsers(vendorId,
                    NotificationEvent.RFQ_CLOSING_WITHIN_24_HOURS, "Rfq", rfqId,
                    "RFQ closing soon",
                    "Your quotation for " + rfqLabel(rfqId)
                            + " is still pending; the window closes within 24 hours.");
        }
    }

    private String rfqLabel(UUID rfqId) {
        return rfqRepository.findById(rfqId)
                .map(rfq -> rfq.getRfqNumber() + " - " + rfq.getTitle())
                .orElse(rfqId.toString());
    }
}
