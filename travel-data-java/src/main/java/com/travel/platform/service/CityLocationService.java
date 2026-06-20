package com.travel.platform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.travel.platform.entity.CityLocation;
import com.travel.platform.mapper.CityLocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CityLocationService {

    private final CityLocationMapper cityLocationMapper;
    private final RedisCacheService redisCacheService;

    public List<CityLocation> listAllActiveCities() {
        String cacheKey = "city:list:active";
        return redisCacheService.get(cacheKey, new TypeReference<List<CityLocation>>() {})
                .orElseGet(() -> {
                    List<CityLocation> cities = cityLocationMapper.selectAllActive();
                    redisCacheService.set(cacheKey, cities, Duration.ofMinutes(30));
                    return cities;
                });
    }

    public CityLocation getByCityName(String cityName) {
        return cityLocationMapper.selectByCityName(cityName);
    }
}
