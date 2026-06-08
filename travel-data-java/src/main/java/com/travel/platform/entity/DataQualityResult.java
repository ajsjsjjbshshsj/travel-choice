package com.travel.platform.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DataQualityResult {
    private Long id;
    private String tableName;
    private String ruleName;
    private String ruleDesc;
    private String checkResult;
    private Integer errorCount;
    private LocalDateTime checkTime;
    private String errorMessage;
    private LocalDateTime createdAt;
}