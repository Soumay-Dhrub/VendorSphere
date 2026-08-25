package com.vendorsphere.analytics.service;

import com.vendorsphere.analytics.repository.AnalyticsQueryRepository;
import com.vendorsphere.common.security.SecurityUtils;
import com.vendorsphere.common.util.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class DashboardService {

    public record Dashboard(
            BigDecimal totalSpend,
            long activeRfqCount,
            long openPurchaseOrderCount,
            long pendingDeliveryCount,
            long outstandingInvoiceCount,
            long overdueInvoiceCount,
            long activeVendorCount) {
    }

    private final AnalyticsQueryRepository analyticsQueries;

    public DashboardService(AnalyticsQueryRepository analyticsQueries) {
        this.analyticsQueries = analyticsQueries;
    }

    @Transactional(readOnly = true)
    public Dashboard dashboard() {
        UUID organizationId = SecurityUtils.getCurrentOrganizationId();
        return new Dashboard(
                Money.money(analyticsQueries.totalSpend(organizationId)),
                analyticsQueries.activeRfqCount(organizationId),
                analyticsQueries.openPurchaseOrderCount(organizationId),
                analyticsQueries.pendingDeliveryCount(organizationId),
                analyticsQueries.outstandingInvoiceCount(organizationId),
                analyticsQueries.overdueInvoiceCount(organizationId),
                analyticsQueries.activeVendorCount(organizationId));
    }
}
