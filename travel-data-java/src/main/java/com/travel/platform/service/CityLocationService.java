package com.travel.platform.service;

import com.travel.platform.entity.CityLocation;
import com.travel.platform.mapper.CityLocationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CityLocationService {

    private final CityLocationMapper cityLocationMapper;

    public List<CityLocation> listAllActiveCities() {
        return cityLocationMapper.selectAllActive();
    }

    public CityLocation getByCityName(String cityName) {
        return cityLocationMapper.selectByCityName(cityName);
    }
}
