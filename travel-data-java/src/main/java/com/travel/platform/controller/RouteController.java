//路线采集控制器

package com.travel.platform.controller;

import com.travel.platform.dto.RouteCollectRequest;
import com.travel.platform.service.collect.RouteCollectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    /**
     * 采集从出发地到所有目的地的公交路线
     * POST /api/route/collect
     */
    @PostMapping("/collect")
    public ResponseEntity<Map<String, Object>> collect(@RequestBody RouteCollectRequest request) {
        int count = routeCollectService.collectAll(
                request.getOriginCity(),
                request.getOriginLongitude(),
                request.getOriginLatitude(),
                request.getTravelDate()
        );
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "路线数据采集完成",
                "collectedCount", count
        ));
    }
}