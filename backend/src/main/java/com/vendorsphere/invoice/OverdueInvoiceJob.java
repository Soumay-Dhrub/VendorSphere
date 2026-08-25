package com.vendorsphere.invoice;

import com.vendorsphere.invoice.repository.InvoiceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Daily overdue-invoice evaluation at 01:15 UTC (Requirement 24.10): unpaid invoices past their due
 * date move to OVERDUE (Requirement 24.9).
 */
@Component
public class OverdueInvoiceJob {

    private final InvoiceRepository invoiceRepository;
    private final Clock clock;

    public OverdueInvoiceJob(InvoiceRepository invoiceRepository, Clock clock) {
        this.invoiceRepository = invoiceRepository;
        this.clock = clock;
    }

    @Scheduled(cron = "0 15 1 * * *", zone = "UTC")
    @Transactional
    public void run() {
        LocalDate today = LocalDate.now(clock);
        List<com.vendorsphere.invoice.entity.Invoice> candidates = invoiceRepository
                .findByDueDateBeforeAndStatusNotIn(today,
                        List.of(InvoiceStatus.PAID, InvoiceStatus.REJECTED, InvoiceStatus.OVERDUE));
        for (var invoice : candidates) {
            if (invoice.getPaidAmount().compareTo(invoice.getTotalAmount()) >= 0) {
                continue; // fully paid already; the PAID status will catch up on the next write
            }
            InvoiceStatusTransitions.MACHINE.assertTransition(
                    invoice.getStatus(), InvoiceStatus.OVERDUE);
            invoice.setStatus(InvoiceStatus.OVERDUE);
            invoiceRepository.save(invoice);
        }
    }
}
