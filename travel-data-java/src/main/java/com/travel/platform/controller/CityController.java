package com.travel.platform.controller;

import com.travel.platform.common.result.ApiResponse;
import com.travel.platform.entity.CityLocation;
import com.travel.platform.service.CityLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
public class CityController {

    private final CityLocationService cityLocationService;

    @GetMapping
    public ApiResponse<List<CityLocation>> listCities() {
        return ApiResponse.success(cityLocationService.listAllActiveCities());
    }
}
