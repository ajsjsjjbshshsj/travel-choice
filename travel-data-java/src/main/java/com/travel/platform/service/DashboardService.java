package com.travel.platform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.travel.platform.dto.DashboardStats;
import com.travel.platform.mapper.DashboardMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class DashboardService {

    private final DashboardMapper dashboardMapper;
    private final DestinationService destinationService;
    private final DataQualityService dataQualityService;
    private final RedisCacheService redisCacheService;

    public DashboardService(
            DashboardMapper dashboardMapper,
            DestinationService destinationService,
            DataQualityService dataQualityService,
            RedisCacheService redisCacheService
    ) {
        this.dashboardMapper = dashboardMapper;
        this.destinationService = destinationService;
        this.dataQualityService = dataQualityService;
        this.redisCacheService = redisCacheService;
    }

    public DashboardStats getStats() {
        String cacheKey = "dashboard:stats";
        return redisCacheService.get(cacheKey, new TypeReference<DashboardStats>() {})
                .orElseGet(() -> {
                    DashboardStats stats = buildStats();
                    redisCacheService.set(cacheKey, stats, Duration.ofMinutes(5));
                    return stats;
                });
    }

    private DashboardStats buildStats() {
        DashboardStats stats = new DashboardStats();

        stats.setDestinationCount(dashboardMapper.countDestinations());
        stats.setEtlTotalCount(dashboardMapper.countEtlJobs());
        stats.setEtlSuccessCount(dashboardMapper.countEtlJobsByStatus("SUCCESS"));
        stats.setEtlFailedCount(dashboardMapper.countEtlJobsByStatus("FAILED"));
        stats.setEtlRunningCount(dashboardMapper.countEtlJobsByStatus("RUNNING"));

        stats.setQualityTotalCount(dashboardMapper.countQualityResults());
        stats.setQualityPassCount(dashboardMapper.countQualityByResult("PASS"));
        stats.setQualityWarningCount(dashboardMapper.countQualityByResult("WARNING"));
        stats.setQualityFailedCount(dashboardMapper.countQualityByResult("FAILED"));

        stats.setRecommendTotalCount(dashboardMapper.countRecommendResults());
        stats.setAvgFinalScore(dashboardMapper.avgFinalScore());
        stats.setAvgCrowdIndex(dashboardMapper.avgCrowdIndex());

        stats.setTopDestinations(destinationService.listDestinations().stream().limit(5).toList());
        stats.setLatestQualityResults(dataQualityService.listLatest().stream().limit(4).toList());
        stats.setRedisStatus(redisCacheService.isAvailable() ? "UP" : "DOWN");
        stats.setSchedulerStatus("AIRFLOW_DAG_CONFIGURED");

        return stats;
    }
}
