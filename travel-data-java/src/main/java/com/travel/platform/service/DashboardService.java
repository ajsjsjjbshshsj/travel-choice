package com.travel.platform.service;

import com.travel.platform.dto.DashboardStats;
import com.travel.platform.mapper.DashboardMapper;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final DashboardMapper dashboardMapper;
    private final DestinationService destinationService;
    private final DataQualityService dataQualityService;

    public DashboardService(
            DashboardMapper dashboardMapper,
            DestinationService destinationService,
            DataQualityService dataQualityService
    ) {
        this.dashboardMapper = dashboardMapper;
        this.destinationService = destinationService;
        this.dataQualityService = dataQualityService;
    }

    public DashboardStats getStats() {
        DashboardStats stats = new DashboardStats();

        // 目的地统计
        stats.setDestinationCount(dashboardMapper.countDestinations());

        // ETL 任务统计
        stats.setEtlTotalCount(dashboardMapper.countEtlJobs());
        stats.setEtlSuccessCount(dashboardMapper.countEtlJobsByStatus("SUCCESS"));
        stats.setEtlFailedCount(dashboardMapper.countEtlJobsByStatus("FAILED"));
        stats.setEtlRunningCount(dashboardMapper.countEtlJobsByStatus("RUNNING"));

        // 数据质量统计
        stats.setQualityTotalCount(dashboardMapper.countQualityResults());
        stats.setQualityPassCount(dashboardMapper.countQualityByResult("PASS"));
        stats.setQualityWarningCount(dashboardMapper.countQualityByResult("WARNING"));
        stats.setQualityFailedCount(dashboardMapper.countQualityByResult("FAILED"));

        // 推荐统计
        stats.setRecommendTotalCount(dashboardMapper.countRecommendResults());
        stats.setAvgFinalScore(dashboardMapper.avgFinalScore());
        stats.setAvgCrowdIndex(dashboardMapper.avgCrowdIndex());

        // Top 目的地 (按景观评分排序，取前 5)
        var allDestinations = destinationService.listDestinations();
        stats.setTopDestinations(
                allDestinations.stream().limit(5).toList()
        );

        // 最新质量检查结果 (取最新 4 条规则)
        var allQuality = dataQualityService.listLatest();
        stats.setLatestQualityResults(
                allQuality.stream().limit(4).toList()
        );

        return stats;
    }
}
