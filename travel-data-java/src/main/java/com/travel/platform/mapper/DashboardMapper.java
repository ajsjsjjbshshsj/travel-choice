package com.travel.platform.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

@Mapper
public interface DashboardMapper {

    @Select("SELECT COUNT(*) FROM dwd_destination WHERE is_active = 1")
    int countDestinations();

    @Select("SELECT COUNT(*) FROM etl_job_log")
    int countEtlJobs();

    @Select("SELECT COUNT(*) FROM etl_job_log WHERE status = #{status}")
    int countEtlJobsByStatus(@Param("status") String status);

    @Select("SELECT COUNT(*) FROM data_quality_check_result")
    int countQualityResults();

    @Select("SELECT COUNT(*) FROM data_quality_check_result WHERE check_result = #{result}")
    int countQualityByResult(@Param("result") String result);

    @Select("SELECT COUNT(*) FROM ads_destination_recommend_result")
    int countRecommendResults();

    @Select("SELECT AVG(final_score) FROM ads_destination_recommend_result")
    BigDecimal avgFinalScore();

    @Select("SELECT AVG(crowd_index) FROM ads_destination_recommend_result")
    BigDecimal avgCrowdIndex();
}
