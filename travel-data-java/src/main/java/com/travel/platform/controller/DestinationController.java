package com.travel.platform.controller;

import com.travel.platform.common.result.ApiResponse;
import com.travel.platform.entity.Destination;
import com.travel.platform.service.DestinationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@RestController
@RequestMapping("/api/destinations")
public class DestinationController {

    private final DestinationService destinationService;

    public DestinationController(DestinationService destinationService) {
        this.destinationService = destinationService;
    }

    @GetMapping("/{destinationCode}")
    public ApiResponse<Destination> getDestinationByCode(@PathVariable String destinationCode) {
        return ApiResponse.success(destinationService.getByCode(destinationCode));
    }

    @GetMapping
    public ApiResponse<List<Destination>> listDestinations() {
        return ApiResponse.success(destinationService.listDestinations());
    }
}
