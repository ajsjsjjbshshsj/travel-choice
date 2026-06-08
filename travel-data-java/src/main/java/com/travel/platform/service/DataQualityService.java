package com.travel.platform.service;

import com.travel.platform.entity.DataQualityResult;
import com.travel.platform.entity.EtlJobLog;
import com.travel.platform.mapper.DataQualityResultMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DataQualityService {

    private final JdbcTemplate jdbcTemplate;
    private final DataQualityResultMapper dataQualityResultMapper;
    private final JobLogService jobLogService;

    public DataQualityService(
            JdbcTemplate jdbcTemplate,
            DataQualityResultMapper dataQualityResultMapper,
            JobLogService jobLogService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataQualityResultMapper = dataQualityResultMapper;
        this.jobLogService = jobLogService;
    }

    public void runChecks() {
        EtlJobLog jobLog = jobLogService.startJob(
                "run_data_quality_check",
                "QUALITY",
                "数据质量规则检查",
                LocalDate.now()
        );

        try {
            // 原有规则
            checkDestinationCodeNotNull();
            checkHotelPriceValid();
            checkWeatherScoreRange();
            checkRecommendScoreRange();

            // 第三版新增规则
            checkDestinationLocationNotNull();
            checkDestinationAdcodeNotNull();
            checkWeatherDataSource();
            checkRouteDataSource();
            checkRouteDistanceAndDuration();

            jobLogService.finishSuccess(jobLog.getId(), 9);
        } catch (Exception e) {
            jobLogService.finishFailed(jobLog.getId(), e.getMessage());
            throw e;
        }
    }

    public List<DataQualityResult> listLatest() {
        return dataQualityResultMapper.selectLatest();
    }

    private void saveResult(
            String tableName,
            String ruleName,
            String ruleDesc,
            int errorCount,
            String errorMessage
    ) {
        DataQualityResult result = new DataQualityResult();
        result.setTableName(tableName);
        result.setRuleName(ruleName);
        result.setRuleDesc(ruleDesc);
        result.setErrorCount(errorCount);
        result.setCheckTime(LocalDateTime.now());
        result.setErrorMessage(errorMessage);

        if (errorCount == 0) {
            result.setCheckResult("PASS");
        } else if (errorCount <= 3) {
            result.setCheckResult("WARNING");
        } else {
            result.setCheckResult("FAILED");
        }

        dataQualityResultMapper.insert(result);
    }

    // ===== 原有规则 =====

    private void checkDestinationCodeNotNull() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dwd_destination WHERE destination_code IS NULL OR destination_code = ''",
                Integer.class
        );

        saveResult(
                "dwd_destination",
                "destination_code_not_null",
                "目的地编码不能为空",
                count == null ? 0 : count,
                count != null && count > 0 ? "存在目的地编码为空的数据" : null
        );
    }

    private void checkHotelPriceValid() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dwd_hotel_price_detail WHERE avg_price < 0",
                Integer.class
        );

        saveResult(
                "dwd_hotel_price_detail",
                "hotel_price_valid",
                "酒店价格不能小于0",
                count == null ? 0 : count,
                count != null && count > 0 ? "存在酒店价格小于0的数据" : null
        );
    }

    private void checkWeatherScoreRange() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dwd_weather_detail WHERE weather_score < 0 OR weather_score > 100",
                Integer.class
        );

        saveResult(
                "dwd_weather_detail",
                "weather_score_range",
                "天气评分必须在0到100之间",
                count == null ? 0 : count,
                count != null && count > 0 ? "存在天气评分越界数据" : null
        );
    }

    private void checkRecommendScoreRange() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ads_destination_recommend_result WHERE final_score < 0 OR final_score > 100",
                Integer.class
        );

        saveResult(
                "ads_destination_recommend_result",
                "final_score_range",
                "推荐综合评分必须在0到100之间",
                count == null ? 0 : count,
                count != null && count > 0 ? "存在推荐分越界数据" : null
        );
    }

    // ===== 第三版新增规则 =====

    private void checkDestinationLocationNotNull() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dwd_destination WHERE latitude IS NULL OR longitude IS NULL",
                Integer.class
        );

        saveResult(
                "dwd_destination",
                "destination_location_not_null",
                "目的地经纬度不能为空",
                count == null ? 0 : count,
                count != null && count > 0 ? "存在目的地经纬度为空的数据" : null
        );
    }

    private void checkDestinationAdcodeNotNull() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dwd_destination WHERE amap_adcode IS NULL OR amap_adcode = ''",
                Integer.class
        );

        saveResult(
                "dwd_destination",
                "destination_adcode_not_null",
                "目的地 amap_adcode 不能为空",
                count == null ? 0 : count,
                count != null && count > 0 ? "存在目的地 amap_adcode 为空的数据" : null
        );
    }

    private void checkWeatherDataSource() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dwd_weather_detail WHERE data_source IS NULL OR data_source = ''",
                Integer.class
        );

        saveResult(
                "dwd_weather_detail",
                "weather_data_source_valid",
                "天气数据 data_source 必须是 amap_weather",
                count == null ? 0 : count,
                count != null && count > 0 ? "存在天气数据 data_source 为空或异常的数据" : null
        );
    }

    private void checkRouteDataSource() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dwd_route_detail WHERE data_source IS NULL OR data_source = ''",
                Integer.class
        );

        saveResult(
                "dwd_route_detail",
                "route_data_source_valid",
                "路线数据 data_source 必须是 amap_route",
                count == null ? 0 : count,
                count != null && count > 0 ? "存在路线数据 data_source 为空或异常的数据" : null
        );
    }

    private void checkRouteDistanceAndDuration() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM dwd_route_detail WHERE distance_km <= 0 OR duration_minutes <= 0",
                Integer.class
        );

        saveResult(
                "dwd_route_detail",
                "route_distance_duration_valid",
                "路线距离和耗时必须大于0",
                count == null ? 0 : count,
                count != null && count > 0 ? "存在路线距离或耗时异常的数据" : null
        );
    }
}