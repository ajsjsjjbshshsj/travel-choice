package com.travel.platform.controller;

import com.travel.platform.common.result.ApiResponse;
import com.travel.platform.dto.DashboardStats;
import com.travel.platform.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public ApiResponse<DashboardStats> getStats() {
        return ApiResponse.success(dashboardService.getStats());
    }
}
