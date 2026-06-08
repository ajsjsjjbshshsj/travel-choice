package com.travel.platform.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Destination {

    private Long id;

    private String destinationCode;
    private String destinationName;

    private String country;
    private String province;
    private String city;

    private BigDecimal latitude;
    private BigDecimal longitude;

    private String destinationType;

    private String amapAdcode;

    private BigDecimal sceneryScore;
    private BigDecimal popularityScore;
    private BigDecimal facilityScore;

    private String description;

    private Integer isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}