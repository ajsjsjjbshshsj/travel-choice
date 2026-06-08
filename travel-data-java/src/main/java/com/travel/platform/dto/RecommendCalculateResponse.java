package com.travel.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecommendCalculateResponse {
    private String requestId;
    private String message;
}
