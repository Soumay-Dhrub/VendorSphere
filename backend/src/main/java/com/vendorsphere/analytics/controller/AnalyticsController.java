package com.vendorsphere.analytics.controller;

import com.vendorsphere.analytics.service.DashboardService;
import com.vendorsphere.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics")
public class AnalyticsController {

    private final DashboardService dashboardService;

    public AnalyticsController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'PROCUREMENT_MANAGER', 'FINANCE')")
    @Operation(summary = "Procurement dashboard figures for the current organization")
    public ApiResponse<DashboardService.Dashboard> dashboard() {
        return ApiResponse.ok(dashboardService.dashboard());
    }
}
