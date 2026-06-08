package com.travel.platform.mapper;

import com.travel.platform.entity.RecommendResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface RecommendResultMapper {

    @Select("SELECT " +
            "id, " +
            "request_id, " +
            "origin_city, " +
            "destination_code, " +
            "destination_name, " +
            "travel_start_date, " +
            "travel_end_date, " +
            "travel_days, " +
            "user_budget, " +
            "estimated_total_cost, " +
            "traffic_cost, " +
            "hotel_cost, " +
            "food_cost, " +
            "ticket_cost, " +
            "scenery_score, " +
            "traffic_score, " +
            "hotel_score, " +
            "weather_score, " +
            "cost_score, " +
            "crowd_index, " +
            "crowd_score, " +
            "final_score, " +
            "recommend_rank, " +
            "recommend_level, " +
            "recommend_reason, " +
            "model_version, " +
            "created_at, " +
            "updated_at " +
            "FROM ads_destination_recommend_result " +
            "WHERE origin_city = #{originCity} " +
            "AND travel_start_date = #{startDate} " +
            "AND travel_end_date = #{endDate} " +
            "ORDER BY recommend_rank ASC")
    List<RecommendResult> selectRecommendResults(
            String originCity,
            LocalDate startDate,
            LocalDate endDate
    );

    @Select("""
        SELECT
            id, request_id, origin_city, destination_code, destination_name,
            travel_start_date, travel_end_date, travel_days, user_budget,
            estimated_total_cost, traffic_cost, hotel_cost, food_cost, ticket_cost,
            scenery_score, traffic_score, hotel_score, weather_score, cost_score,
            crowd_index, crowd_score, final_score, recommend_rank,
            recommend_level, recommend_reason, model_version, created_at, updated_at
        FROM ads_destination_recommend_result
        WHERE origin_city = #{originCity}
          AND travel_start_date = #{startDate}
          AND travel_end_date = #{endDate}
          AND destination_code IN (${destinationCodes})
        ORDER BY final_score DESC
        """)
    List<RecommendResult> selectCompareResults(
            String originCity,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            String destinationCodes
    );

    @Select("""
        SELECT
            id, request_id, origin_city, destination_code, destination_name,
            travel_start_date, travel_end_date, travel_days, user_budget,
            estimated_total_cost, traffic_cost, hotel_cost, food_cost, ticket_cost,
            scenery_score, traffic_score, hotel_score, weather_score, cost_score,
            crowd_index, crowd_score, final_score, recommend_rank,
            recommend_level, recommend_reason, model_version, created_at, updated_at
        FROM ads_destination_recommend_result
        WHERE request_id = #{requestId}
        ORDER BY recommend_rank ASC
        """)
    List<RecommendResult> selectByRequestId(String requestId);

    @Select("""
        SELECT COUNT(*)
        FROM ads_destination_recommend_result
        WHERE request_id = #{requestId}
        """)
    Integer countByRequestId(String requestId);
}