package com.travel.platform.controller;

import com.travel.platform.common.result.ApiResponse;
import com.travel.platform.service.collect.HotelPriceGenerateService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/hotel")
@RequiredArgsConstructor
public class HotelPriceController {

    private final HotelPriceGenerateService hotelPriceGenerateService;

    @PostMapping("/generate")
    public ApiResponse<Map<String, Object>> generate(
            @RequestParam(defaultValue = "2026-07-01")
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate checkinDate,
            @RequestParam(defaultValue = "2026-07-05")
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate checkoutDate
    ) {
        int count = hotelPriceGenerateService.generateAll(checkinDate, checkoutDate);
        return ApiResponse.success(Map.of(
                "status", "success",
                "message", "hotel price data generated",
                "generatedCount", count
        ));
    }
}
