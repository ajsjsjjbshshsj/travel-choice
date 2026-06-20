package com.travel.platform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.travel.platform.entity.Destination;
import com.travel.platform.mapper.DestinationMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class DestinationService {

    private final DestinationMapper destinationMapper;
    private final RedisCacheService redisCacheService;

    public DestinationService(DestinationMapper destinationMapper, RedisCacheService redisCacheService) {
        this.destinationMapper = destinationMapper;
        this.redisCacheService = redisCacheService;
    }

    public List<Destination> listDestinations() {
        String cacheKey = "destination:list:active";
        return redisCacheService.get(cacheKey, new TypeReference<List<Destination>>() {})
                .orElseGet(() -> {
                    List<Destination> destinations = destinationMapper.selectAllActive();
                    redisCacheService.set(cacheKey, destinations, Duration.ofMinutes(30));
                    return destinations;
                });
    }

    public Destination getByCode(String destinationCode) {
        return destinationMapper.selectByCode(destinationCode);
    }
}
