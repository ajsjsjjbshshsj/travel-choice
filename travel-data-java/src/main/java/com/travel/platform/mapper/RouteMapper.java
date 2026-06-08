package com.travel.platform.mapper;

import com.travel.platform.entity.RouteDetail;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RouteMapper {

    @Delete("DELETE FROM dwd_route_detail WHERE origin_city = #{originCity} AND travel_date = #{travelDate}")
    int deleteByOriginAndDate(@Param("originCity") String originCity, @Param("travelDate") String travelDate);

    @Insert({
        "<script>",
        "INSERT INTO dwd_route_detail",
        "(origin_city, destination_code, destination_name, travel_date,",
        " transport_type, distance_km, duration_minutes, traffic_cost,",
        " transfer_count, convenience_score, data_source)",
        "VALUES",
        "<foreach collection='list' item='r' separator=','>",
        "(#{r.originCity}, #{r.destinationCode}, #{r.destinationName}, #{r.travelDate},",
        " #{r.transportType}, #{r.distanceKm}, #{r.durationMinutes}, #{r.trafficCost},",
        " #{r.transferCount}, #{r.convenienceScore}, #{r.dataSource})",
        "</foreach>",
        "</script>"
    })
    int batchInsert(@Param("list") List<RouteDetail> list);
}