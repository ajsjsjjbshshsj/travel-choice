package com.travel.platform.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class HotelPriceDetail {

    private Long id;

    private String destinationCode;
    private String destinationName;

    private LocalDate checkinDate;
    private LocalDate checkoutDate;

    private BigDecimal avgPrice;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    private Integer hotelCount;
    private String currency;

    private String priceLevel;
    private BigDecimal hotelScore;

    private String dataSource;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}