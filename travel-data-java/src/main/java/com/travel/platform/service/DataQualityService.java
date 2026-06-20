package com.travel.platform.service;

import com.travel.platform.entity.DataQualityResult;
import com.travel.platform.entity.EtlJobLog;
import com.travel.platform.mapper.DataQualityResultMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    public String runChecks() {
        String requestId = buildRequestId();
        EtlJobLog jobLog = jobLogService.startJob(
                "run_data_quality_check",
                "QUALITY",
                "data quality rule checks",
                LocalDate.now(),
                requestId
        );

        try {
            checkCityLocationNotNull();
            checkDestinationLocationNotNull();
            checkDestinationAdcodeNotNull();
            checkWeatherScoreRange();
            checkRouteDistanceValid();
            checkRouteDurationValid();
            checkHotelPriceValid();
            checkRecommendRequestIdNotNull();
            checkRecommendRankNotDuplicated();

            jobLogService.finishSuccess(jobLog.getId(), 9);
            return requestId;
        } catch (Exception e) {
            jobLogService.finishFailed(jobLog.getId(), e.getMessage());
            throw e;
        }
    }

    public List<DataQualityResult> listLatest() {
        return dataQualityResultMapper.selectLatest();
    }

    private void checkCityLocationNotNull() {
        saveCountRule(
                "dwd_city_location",
                "city_location_not_null",
                "city latitude and longitude cannot be null",
                "SELECT COUNT(*) FROM dwd_city_location WHERE latitude IS NULL OR longitude IS NULL",
                "city coordinate missing"
        );
    }

    private void checkDestinationLocationNotNull() {
        saveCountRule(
                "dwd_destination",
                "destination_location_not_null",
                "destination latitude and longitude cannot be null",
                "SELECT COUNT(*) FROM dwd_destination WHERE latitude IS NULL OR longitude IS NULL",
                "destination coordinate missing"
        );
    }

    private void checkDestinationAdcodeNotNull() {
        saveCountRule(
                "dwd_destination",
                "destination_adcode_not_null",
                "destination amap_adcode cannot be null",
                "SELECT COUNT(*) FROM dwd_destination WHERE amap_adcode IS NULL OR amap_adcode = ''",
                "destination amap_adcode missing"
        );
    }

    private void checkWeatherScoreRange() {
        saveCountRule(
                "dwd_weather_detail",
                "weather_score_range",
                "weather score must be between 0 and 100",
                "SELECT COUNT(*) FROM dwd_weather_detail WHERE weather_score < 0 OR weather_score > 100",
                "weather score out of range"
        );
    }

    private void checkRouteDistanceValid() {
        saveCountRule(
                "dwd_route_detail",
                "route_distance_valid",
                "route distance must be greater than 0",
                "SELECT COUNT(*) FROM dwd_route_detail WHERE distance_km IS NULL OR distance_km <= 0",
                "route distance invalid"
        );
    }

    private void checkRouteDurationValid() {
        saveCountRule(
                "dwd_route_detail",
                "route_duration_valid",
                "route duration must be greater than 0",
                "SELECT COUNT(*) FROM dwd_route_detail WHERE duration_minutes IS NULL OR duration_minutes <= 0",
                "route duration invalid"
        );
    }

    private void checkHotelPriceValid() {
        saveCountRule(
                "dwd_hotel_price_detail",
                "hotel_price_valid",
                "hotel price must be greater than or equal to 0",
                "SELECT COUNT(*) FROM dwd_hotel_price_detail WHERE avg_price IS NULL OR avg_price < 0",
                "hotel price invalid"
        );
    }

    private void checkRecommendRequestIdNotNull() {
        saveCountRule(
                "ads_destination_recommend_result",
                "recommend_request_id_not_null",
                "recommend requestId cannot be null",
                "SELECT COUNT(*) FROM ads_destination_recommend_result WHERE request_id IS NULL OR request_id = ''",
                "recommend result requestId missing"
        );
    }

    private void checkRecommendRankNotDuplicated() {
        saveCountRule(
                "ads_destination_recommend_result",
                "recommend_rank_not_duplicated",
                "recommend rank cannot duplicate within one requestId",
                """
                SELECT COUNT(*) FROM (
                    SELECT request_id, recommend_rank
                    FROM ads_destination_recommend_result
                    WHERE request_id IS NOT NULL AND request_id <> ''
                    GROUP BY request_id, recommend_rank
                    HAVING COUNT(*) > 1
                ) t
                """,
                "duplicated recommend rank"
        );
    }

    private void saveCountRule(
            String tableName,
            String ruleName,
            String ruleDesc,
            String sql,
            String errorMessage
    ) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        saveResult(tableName, ruleName, ruleDesc, count == null ? 0 : count, errorMessage);
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
        result.setErrorMessage(errorCount > 0 ? errorMessage : null);

        if (errorCount == 0) {
            result.setCheckResult("PASS");
        } else if (errorCount <= 3) {
            result.setCheckResult("WARNING");
        } else {
            result.setCheckResult("FAILED");
        }

        dataQualityResultMapper.insert(result);
    }

    private String buildRequestId() {
        String shortUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "QUALITY_" + LocalDate.now().toString().replace("-", "") + "_" + shortUuid;
    }
}
