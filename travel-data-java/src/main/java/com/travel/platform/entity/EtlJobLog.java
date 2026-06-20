package com.travel.platform.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class EtlJobLog {
    private Long id;
    private String jobName;
    private String jobType;
    private String jobDesc;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Integer rowCount;
    private LocalDate dataDate;
    private String requestId;
    private Long durationSeconds;
    private String errorMessage;
    private String triggerType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
