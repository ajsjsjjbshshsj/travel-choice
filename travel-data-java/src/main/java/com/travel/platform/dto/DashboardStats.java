package com.travel.platform.dto;

import com.travel.platform.entity.DataQualityResult;
import com.travel.platform.entity.Destination;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class DashboardStats {
    private int destinationCount;
    private int etlTotalCount;
    private int etlSuccessCount;
    private int etlFailedCount;
    private int etlRunningCount;
    private int qualityTotalCount;
    private int qualityPassCount;
    private int qualityWarningCount;
    private int qualityFailedCount;
    private int recommendTotalCount;
    private BigDecimal avgFinalScore;
    private BigDecimal avgCrowdIndex;
    private List<Destination> topDestinations;
    private List<DataQualityResult> latestQualityResults;
    private String redisStatus;
    private String schedulerStatus;
}
