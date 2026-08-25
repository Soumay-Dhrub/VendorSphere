package com.vendorsphere.payment.service;

import com.vendorsphere.audit.AuditAction;
import com.vendorsphere.audit.service.AuditService;
import com.vendorsphere.common.exception.BusinessException;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.common.util.Money;
import com.vendorsphere.invoice.InvoiceStatus;
import com.vendorsphere.invoice.InvoiceStatusTransitions;
import com.vendorsphere.invoice.entity.Invoice;
import com.vendorsphere.invoice.repository.InvoiceRepository;
import com.vendorsphere.notification.NotificationEvent;
import com.vendorsphere.notification.service.NotificationService;
import com.vendorsphere.payment.entity.Payment;
import com.vendorsphere.payment.repository.PaymentRepository;
import com.vendorsphere.user.RoleName;
import com.vendorsphere.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    static final String AMOUNT_MESSAGE = "Payment amount must be greater than zero";

    static final String NOT_FOUND_MESSAGE = "Invoice not found";

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final Clock clock;

    public PaymentService(
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            NotificationService notificationService,
            AuditService auditService,
            Clock clock
    ) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public void record(UUID invoiceId, com.vendorsphere.payment.dto.PaymentRecordRequest request) {
        Invoice invoice = invoiceRepository.findByIdAndOrganizationId(
                        invoiceId, SecurityUtils.getCurrentOrganizationId())
                .orElseThrow(() -> new BusinessException(NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));

        if (invoice.getStatus() != InvoiceStatus.APPROVED
                && invoice.getStatus() != InvoiceStatus.PARTIALLY_PAID
                && invoice.getStatus() != InvoiceStatus.OVERDUE) {
            throw new BusinessException(
                    "Cannot record a payment against a " + invoice.getStatus() + " invoice",
                    HttpStatus.CONFLICT);
        }
        if (request.amount() == null || request.amount().signum() <= 0) {
            throw new BusinessException(AMOUNT_MESSAGE, HttpStatus.BAD_REQUEST);
        }
        BigDecimal newPaid = invoice.getPaidAmount().add(request.amount());
        if (newPaid.compareTo(invoice.getTotalAmount()) > 0) {
            throw new BusinessException(overPaymentMessage(invoice, newPaid),
                    HttpStatus.CONFLICT);
        }

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(Money.money(request.amount()));
        payment.setPaymentDate(request.paymentDate());
        payment.setPaymentReference(request.paymentReference());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setNotes(request.notes());
        payment.setRecordedBy(userRepository.getReferenceById(SecurityUtils.getCurrentUserId()));
        paymentRepository.save(payment);

        // Requirement 25.5: paid amount is the PAID-payment sum; derive the status next to it.
        BigDecimal paidSum = Money.ZERO_MONEY;
        for (Payment recorded : paymentRepository
                .findByInvoiceIdOrderByCreatedAtAscIdAsc(invoice.getId())) {
            paidSum = paidSum.add(recorded.getAmount());
        }
        invoice.setPaidAmount(Money.money(paidSum));
        InvoiceStatus target = paidSum.compareTo(invoice.getTotalAmount()) == 0
                ? InvoiceStatus.PAID : InvoiceStatus.PARTIALLY_PAID;
        InvoiceStatusTransitions.MACHINE.assertTransition(invoice.getStatus(), target);
        invoice.setStatus(target);
        Invoice saved = invoiceRepository.save(invoice);

        notificationService.createForVendorUsers(saved.getVendor().getId(),
                NotificationEvent.PAYMENT_RECORDED, "Invoice", saved.getId(),
                "Payment recorded",
                saved.getInvoiceNumber() + ": " + saved.getPaidAmount()
                        + " of " + saved.getTotalAmount() + " paid.");
        auditService.record(AuditAction.PAYMENT_RECORDED, "Invoice", saved.getId(),
                null, request.amount().toPlainString());
    }

    static String overPaymentMessage(Invoice invoice, BigDecimal cumulative) {
        return "Payment would exceed the invoice total of " + invoice.getTotalAmount()
                + "; cumulative paid would be " + cumulative;
    }

    @Transactional(readOnly = true)
    public com.vendorsphere.payment.dto.OutstandingResponse outstanding() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        List<Object[]> totalRows = paymentRepository.outstandingByOrganization(organizationId);
        BigDecimal total = Money.ZERO_MONEY;
        for (Object[] row : totalRows) {
            total = total.add((BigDecimal) row[1]);
        }
        Map<UUID, BigDecimal> byVendor = new HashMap<>();
        for (Object[] row : paymentRepository.outstandingByVendor(organizationId)) {
            byVendor.put((UUID) row[0], Money.money((BigDecimal) row[1]));
        }
        return new com.vendorsphere.payment.dto.OutstandingResponse(
                Money.money(total), byVendor);
    }
}
