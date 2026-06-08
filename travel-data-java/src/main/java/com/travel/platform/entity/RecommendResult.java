package com.travel.platform.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class RecommendResult {

    private Long id;

    private String requestId;

    private String originCity;
    private String destinationCode;
    private String destinationName;

    private LocalDate travelStartDate;
    private LocalDate travelEndDate;
    private Integer travelDays;

    private BigDecimal userBudget;
    private BigDecimal estimatedTotalCost;

    private BigDecimal trafficCost;
    private BigDecimal hotelCost;
    private BigDecimal foodCost;
    private BigDecimal ticketCost;

    private BigDecimal sceneryScore;
    private BigDecimal trafficScore;
    private BigDecimal hotelScore;
    private BigDecimal weatherScore;
    private BigDecimal costScore;

    private BigDecimal crowdIndex;
    private BigDecimal crowdScore;

    private BigDecimal finalScore;
    private Integer recommendRank;

    private String recommendLevel;
    private String recommendReason;

    private String modelVersion;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}