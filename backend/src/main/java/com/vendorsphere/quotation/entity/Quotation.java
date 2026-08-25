package com.vendorsphere.quotation.entity;

import com.vendorsphere.common.entity.BaseEntity;
import com.vendorsphere.quotation.QuotationStatus;
import com.vendorsphere.rfq.entity.Rfq;
import com.vendorsphere.vendor.entity.Vendor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "quotations")
public class Quotation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rfq_id", nullable = false)
    private Rfq rfq;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendor vendor;

    @Column(name = "quotation_number", length = 50)
    private String quotationNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO.setScale(2);

    @Column(name = "tax_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO.setScale(2);

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO.setScale(2);

    @Column(name = "shipping_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal shippingAmount = BigDecimal.ZERO.setScale(2);

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO.setScale(2);

    @Column(name = "delivery_period_days")
    private Integer deliveryPeriodDays;

    @Column(name = "payment_terms", columnDefinition = "TEXT")
    private String paymentTerms;

    @Column(columnDefinition = "TEXT")
    private String warranty;

    @Column(name = "warranty_months")
    private Integer warrantyMonths;

    @Column(name = "validity_date")
    private LocalDate validityDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private QuotationStatus status = QuotationStatus.DRAFT;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Version
    @Column(nullable = false)
    private long version;

    public Rfq getRfq() {
        return rfq;
    }

    public void setRfq(Rfq rfq) {
        this.rfq = rfq;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public void setVendor(Vendor vendor) {
        this.vendor = vendor;
    }

    public String getQuotationNumber() {
        return quotationNumber;
    }

    public void setQuotationNumber(String quotationNumber) {
        this.quotationNumber = quotationNumber;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getShippingAmount() {
        return shippingAmount;
    }

    public void setShippingAmount(BigDecimal shippingAmount) {
        this.shippingAmount = shippingAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getDeliveryPeriodDays() {
        return deliveryPeriodDays;
    }

    public void setDeliveryPeriodDays(Integer deliveryPeriodDays) {
        this.deliveryPeriodDays = deliveryPeriodDays;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public void setPaymentTerms(String paymentTerms) {
        this.paymentTerms = paymentTerms;
    }

    public String getWarranty() {
        return warranty;
    }

    public void setWarranty(String warranty) {
        this.warranty = warranty;
    }

    public Integer getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(Integer warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }

    public LocalDate getValidityDate() {
        return validityDate;
    }

    public void setValidityDate(LocalDate validityDate) {
        this.validityDate = validityDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public QuotationStatus getStatus() {
        return status;
    }

    public void setStatus(QuotationStatus status) {
        this.status = status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    public long getVersion() {
        return version;
    }
}
