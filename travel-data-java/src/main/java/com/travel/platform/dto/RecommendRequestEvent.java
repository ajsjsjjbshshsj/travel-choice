package com.travel.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendRequestEvent {
    private String requestId;
    private String originCity;
    private LocalDate travelStartDate;
    private LocalDate travelEndDate;
    private BigDecimal userBudget;
    private Integer destinationCount;
    private List<DestinationSnapshot> destinations;
    private LocalDateTime eventTime;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DestinationSnapshot {
        private String destinationCode;
        private String destinationName;
    }
}
