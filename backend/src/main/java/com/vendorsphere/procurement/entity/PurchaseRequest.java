package com.vendorsphere.procurement.entity;

import com.vendorsphere.common.entity.BaseEntity;
import com.vendorsphere.organization.entity.Department;
import com.vendorsphere.organization.entity.Organization;
import com.vendorsphere.procurement.Priority;
import com.vendorsphere.procurement.PurchaseRequestStatus;
import com.vendorsphere.user.entity.User;
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
@Table(name = "purchase_requests")
public class PurchaseRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "request_number", nullable = false, length = 50)
    private String requestNumber;

    @Column(nullable = false)
    private String title;

    @Column(name = "business_justification", columnDefinition = "TEXT")
    private String businessJustification;

    @Column(name = "required_date")
    private LocalDate requiredDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Priority priority = Priority.MEDIUM;

    @Column(name = "estimated_budget", precision = 15, scale = 2)
    private BigDecimal estimatedBudget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PurchaseRequestStatus status = PurchaseRequestStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Version
    @Column(nullable = false)
    private long version;

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public User getRequester() {
        return requester;
    }

    public void setRequester(User requester) {
        this.requester = requester;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getRequestNumber() {
        return requestNumber;
    }

    public void setRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBusinessJustification() {
        return businessJustification;
    }

    public void setBusinessJustification(String businessJustification) {
        this.businessJustification = businessJustification;
    }

    public LocalDate getRequiredDate() {
        return requiredDate;
    }

    public void setRequiredDate(LocalDate requiredDate) {
        this.requiredDate = requiredDate;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public BigDecimal getEstimatedBudget() {
        return estimatedBudget;
    }

    public void setEstimatedBudget(BigDecimal estimatedBudget) {
        this.estimatedBudget = estimatedBudget;
    }

    public PurchaseRequestStatus getStatus() {
        return status;
    }

    public void setStatus(PurchaseRequestStatus status) {
        this.status = status;
    }

    public User getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewNotes() {
        return reviewNotes;
    }

    public void setReviewNotes(String reviewNotes) {
        this.reviewNotes = reviewNotes;
    }

    public long getVersion() {
        return version;
    }
}
