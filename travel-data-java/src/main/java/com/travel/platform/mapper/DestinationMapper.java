package com.travel.platform.mapper;

import com.travel.platform.entity.Destination;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DestinationMapper {

    // ... existing code ...
    @Select("SELECT " +
            "id, " +
            "destination_code, " +
            "destination_name, " +
            "country, " +
            "province, " +
            "city, " +
            "latitude, " +
            "longitude, " +
            "destination_type, " +
            "amap_adcode, " +
            "scenery_score, " +
            "popularity_score, " +
            "facility_score, " +
            "description, " +
            "is_active, " +
            "created_at, " +
            "updated_at " +
            "FROM dwd_destination " +
            "WHERE is_active = 1 " +
            "ORDER BY scenery_score DESC")
    List<Destination> selectAllActive();


    @Select("""
        SELECT
            id, destination_code, destination_name, country, province, city,
            latitude, longitude, destination_type, amap_adcode, scenery_score, popularity_score,
            facility_score, description, is_active, created_at, updated_at
        FROM dwd_destination
        WHERE destination_code = #{destinationCode}
        """)
    Destination selectByCode(String destinationCode);
}