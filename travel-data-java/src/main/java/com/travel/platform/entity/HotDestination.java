package com.travel.platform.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class HotDestination {
    private String windowStart;
    private String windowEnd;
    private String destinationCode;
    private String destinationName;
    private Long requestCount;
    private Integer rankNo;
    private LocalDateTime updatedAt;
}
