package com.travel.platform.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CityLocation {
    private Long id;

    private String cityCode;
    private String cityName;
    private String province;

    private String amapAdcode;
    private String amapCitycode;

    private BigDecimal latitude;
    private BigDecimal longitude;

    private Integer isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
