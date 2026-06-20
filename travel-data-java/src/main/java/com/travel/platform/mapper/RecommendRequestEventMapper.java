package com.travel.platform.mapper;

import com.travel.platform.dto.RecommendRequestEvent;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RecommendRequestEventMapper {

    @Insert("""
            INSERT INTO dwd_recommend_request_event
            (request_id, origin_city, travel_start_date, travel_end_date, user_budget, destination_count, event_time)
            VALUES
            (#{requestId}, #{originCity}, #{travelStartDate}, #{travelEndDate}, #{userBudget}, #{destinationCount}, #{eventTime})
            """)
    int insert(RecommendRequestEvent event);
}
