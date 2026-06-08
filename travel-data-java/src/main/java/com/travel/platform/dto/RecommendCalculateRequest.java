package com.travel.platform.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class RecommendCalculateRequest {

    private String originCity;

    private LocalDate travelStartDate;

    private LocalDate travelEndDate;

    private BigDecimal userBudget;

    /**
     * 候选目的地编码。
     * 为空：系统对所有启用目的地排序。
     * 不为空：只对用户选择的目的地排序。
     */
    private List<String> destinationCodes;
}