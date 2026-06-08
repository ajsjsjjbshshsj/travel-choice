package com.travel.platform.service;

import com.travel.platform.entity.Destination;
import com.travel.platform.mapper.DestinationMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DestinationService {

    private final DestinationMapper destinationMapper;

    public DestinationService(DestinationMapper destinationMapper) {
        this.destinationMapper = destinationMapper;
    }

    public List<Destination> listDestinations() {
        return destinationMapper.selectAllActive();
    }

    public Destination getByCode(String destinationCode) {
        return destinationMapper.selectByCode(destinationCode);
    }
}