package com.travel.platform.controller;

import com.travel.platform.common.result.ApiResponse;
import com.travel.platform.entity.HotDestination;
import com.travel.platform.service.HotDestinationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hot-destinations")
public class HotDestinationController {

    private final HotDestinationService hotDestinationService;

    public HotDestinationController(HotDestinationService hotDestinationService) {
        this.hotDestinationService = hotDestinationService;
    }

    @GetMapping("/topn")
    public ApiResponse<List<HotDestination>> listTopN(@RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(hotDestinationService.listTopN(limit));
    }
}
