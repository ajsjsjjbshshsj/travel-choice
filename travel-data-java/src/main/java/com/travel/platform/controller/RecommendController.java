package com.travel.platform.controller;

import com.travel.platform.common.result.ApiResponse;
import com.travel.platform.dto.RecommendCalculateRequest;
import com.travel.platform.dto.RecommendCalculateResponse;
import com.travel.platform.entity.RecommendResult;
import com.travel.platform.service.RecommendService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/recommend")
public class RecommendController {

    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @GetMapping("/results")
    public ApiResponse<List<RecommendResult>> listRecommendResults(
            @RequestParam String originCity,

            @RequestParam
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate endDate
    ) {
        return ApiResponse.success(recommendService.listRecommendResults(originCity, startDate, endDate));
    }

    @PostMapping("/calculate")
    public ApiResponse<RecommendCalculateResponse> calculateRecommend(@RequestBody RecommendCalculateRequest request) {
        return ApiResponse.success(recommendService.calculateRecommend(request));
    }

    @GetMapping("/calculate")
    public ApiResponse<RecommendCalculateResponse> calculateRecommendGet(
            @RequestParam(defaultValue = "北京") String originCity,
            @RequestParam(defaultValue = "2026-07-01") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate travelStartDate,
            @RequestParam(defaultValue = "2026-07-05") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate travelEndDate,
            @RequestParam(defaultValue = "5000") BigDecimal userBudget
    ) {
        RecommendCalculateRequest request = new RecommendCalculateRequest();
        request.setOriginCity(originCity);
        request.setTravelStartDate(travelStartDate);
        request.setTravelEndDate(travelEndDate);
        request.setUserBudget(userBudget);
        return ApiResponse.success(recommendService.calculateRecommend(request));
    }

    @GetMapping("/results/by-request")
    public ApiResponse<List<RecommendResult>> listByRequestId(@RequestParam String requestId) {
        return ApiResponse.success(recommendService.listByRequestId(requestId));
    }
}
