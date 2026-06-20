package com.travel.platform.controller;

import com.travel.platform.common.result.ApiResponse;
import com.travel.platform.service.collect.WeatherCollectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherCollectService weatherCollectService;

    @PostMapping("/collect")
    public ApiResponse<Map<String, Object>> collect() {
        int count = weatherCollectService.collectAll();
        return ApiResponse.success(Map.of(
                "status", "success",
                "message", "weather data collected",
                "collectedCount", count
        ));
    }
}
