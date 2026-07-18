package com.orphanage.oms.dashboard.controller;

import com.orphanage.oms.dashboard.dto.DashboardGenderCountResponse;
import com.orphanage.oms.dashboard.dto.DashboardMonthlyAdmissionResponse;
import com.orphanage.oms.dashboard.dto.DashboardStatusCountResponse;
import com.orphanage.oms.dashboard.dto.DashboardSummaryResponse;
import com.orphanage.oms.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Executive dashboard statistics endpoints.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Dashboard summary counts and recent students")
    public DashboardSummaryResponse summary() {
        return dashboardService.getSummary();
    }

    @GetMapping("/admissions")
    @Operation(summary = "Monthly admission trend for the last 12 months")
    public List<DashboardMonthlyAdmissionResponse> admissions() {
        return dashboardService.getAdmissionsTrend();
    }

    @GetMapping("/gender")
    @Operation(summary = "Gender distribution of active students")
    public List<DashboardGenderCountResponse> gender() {
        return dashboardService.getGenderDistribution();
    }

    @GetMapping("/status")
    @Operation(summary = "Status distribution of all retained students")
    public List<DashboardStatusCountResponse> status() {
        return dashboardService.getStatusDistribution();
    }
}
