//提供 /api/hotel/generate

package com.travel.platform.controller;

import com.travel.platform.service.collect.HotelPriceGenerateService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/hotel")
@RequiredArgsConstructor
public class HotelPriceController {

    private final HotelPriceGenerateService hotelPriceGenerateService;

    /**
     * 生成酒店参考价格数据
     * POST /api/hotel/generate?checkinDate=2026-07-01&checkoutDate=2026-07-05
     */
    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generate(
            @RequestParam(defaultValue = "2026-07-01")
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate checkinDate,

            @RequestParam(defaultValue = "2026-07-05")
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate checkoutDate
    ) {
        int count = hotelPriceGenerateService.generateAll(checkinDate, checkoutDate);
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "酒店价格数据生成完成",
                "generatedCount", count
        ));
    }
}