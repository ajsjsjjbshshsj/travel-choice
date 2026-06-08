package com.travel.platform.controller;

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
    public List<CityLocation> listCities() {
        return cityLocationService.listAllActiveCities();
    }
}
