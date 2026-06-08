package com.travel.platform.mapper;

import com.travel.platform.entity.CityLocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CityLocationMapper {

    @Select("""
            SELECT
                id,
                city_code,
                city_name,
                province,
                amap_adcode,
                amap_citycode,
                latitude,
                longitude,
                is_active,
                created_at,
                updated_at
            FROM dwd_city_location
            WHERE city_name = #{cityName}
              AND is_active = 1
            LIMIT 1
            """)
    CityLocation selectByCityName(String cityName);

    @Select("""
            SELECT
                id,
                city_code,
                city_name,
                province,
                amap_adcode,
                amap_citycode,
                latitude,
                longitude,
                is_active,
                created_at,
                updated_at
            FROM dwd_city_location
            WHERE is_active = 1
            ORDER BY city_name
            """)
    List<CityLocation> selectAllActive();
}
