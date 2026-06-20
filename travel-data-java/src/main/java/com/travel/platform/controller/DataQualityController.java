package com.travel.platform.controller;

import com.travel.platform.common.result.ApiResponse;
import com.travel.platform.entity.DataQualityResult;
import com.travel.platform.service.DataQualityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quality")
public class DataQualityController {

    private final DataQualityService dataQualityService;

    public DataQualityController(DataQualityService dataQualityService) {
        this.dataQualityService = dataQualityService;
    }

    @PostMapping("/run")
    public ApiResponse<Map<String, Object>> runQualityChecks() {
        String requestId = dataQualityService.runChecks();
        return ApiResponse.success(Map.of(
                "status", "success",
                "message", "data quality checks completed",
                "requestId", requestId
        ));
    }

    @GetMapping("/run")
    public ApiResponse<Map<String, Object>> runQualityChecksGet() {
        return runQualityChecks();
    }

    @GetMapping("/results")
    public ApiResponse<List<DataQualityResult>> listResults() {
        return ApiResponse.success(dataQualityService.listLatest());
    }
}
