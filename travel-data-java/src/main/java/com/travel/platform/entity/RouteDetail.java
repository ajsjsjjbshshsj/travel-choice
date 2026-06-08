package com.travel.platform.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class RouteDetail {

    private Long id;

    private String originCity;
    private String destinationCode;
    private String destinationName;

    private LocalDate travelDate;

    private String transportType;
    private BigDecimal distanceKm;
    private Integer durationMinutes;

    private BigDecimal trafficCost;
    private String currency;

    private Integer transferCount;
    private BigDecimal convenienceScore;

    private String dataSource;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}