package com.travel.platform.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class RouteCollectRequest {

    private String originCity;

    private BigDecimal originLongitude;

    private BigDecimal originLatitude;

    private LocalDate travelDate;
}