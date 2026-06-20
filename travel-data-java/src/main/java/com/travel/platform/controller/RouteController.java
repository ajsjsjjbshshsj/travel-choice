package com.travel.platform.controller;

import com.travel.platform.common.result.ApiResponse;
import com.travel.platform.dto.RouteCollectRequest;
import com.travel.platform.service.collect.RouteCollectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/route")
@RequiredArgsConstructor
public class RouteController {

    private final RouteCollectService routeCollectService;

    @PostMapping("/collect")
    public ApiResponse<Map<String, Object>> collect(@RequestBody RouteCollectRequest request) {
        int count = routeCollectService.collectAll(
                request.getOriginCity(),
                request.getOriginLongitude(),
                request.getOriginLatitude(),
                request.getTravelDate()
        );
        return ApiResponse.success(Map.of(
                "status", "success",
                "message", "route data collected",
                "collectedCount", count
        ));
    }
}
