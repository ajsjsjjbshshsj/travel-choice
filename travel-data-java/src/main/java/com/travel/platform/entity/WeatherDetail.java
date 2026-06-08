package com.travel.platform.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class WeatherDetail {

    private Long id;

    private String destinationCode;
    private String destinationName;

    private LocalDate weatherDate;

    private String weatherType;
    private BigDecimal tempMax;
    private BigDecimal tempMin;
    private BigDecimal precipitationProbability;
    private BigDecimal windSpeed;

    private BigDecimal weatherScore;
    private String weatherRiskLevel;

    private String dataSource;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}