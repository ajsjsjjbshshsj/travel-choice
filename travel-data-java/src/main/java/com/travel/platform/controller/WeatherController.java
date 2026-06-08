//天气采集控制器

package com.travel.platform.controller;

import com.travel.platform.service.collect.WeatherCollectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherCollectService weatherCollectService;

    /**
     * 采集所有活跃目的地的天气预报数据
     * POST /api/weather/collect
     */
    @PostMapping("/collect")
    public ResponseEntity<Map<String, Object>> collect() {
        int count = weatherCollectService.collectAll();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "天气数据采集完成",
                "collectedCount", count
        ));
    }
}