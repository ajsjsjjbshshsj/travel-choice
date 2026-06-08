package com.travel.platform.mapper;

import com.travel.platform.entity.WeatherDetail;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WeatherMapper {

    @Delete("DELETE FROM dwd_weather_detail WHERE destination_code = #{destinationCode}")
    int deleteByDestinationCode(@Param("destinationCode") String destinationCode);

    @Insert({
        "<script>",
        "INSERT INTO dwd_weather_detail",
        "(destination_code, destination_name, weather_date, weather_type,",
        " temp_max, temp_min, precipitation_probability, wind_speed,",
        " weather_score, weather_risk_level, data_source)",
        "VALUES",
        "<foreach collection='list' item='w' separator=','>",
        "(#{w.destinationCode}, #{w.destinationName}, #{w.weatherDate}, #{w.weatherType},",
        " #{w.tempMax}, #{w.tempMin}, #{w.precipitationProbability}, #{w.windSpeed},",
        " #{w.weatherScore}, #{w.weatherRiskLevel}, #{w.dataSource})",
        "</foreach>",
        "</script>"
    })
    int batchInsert(@Param("list") List<WeatherDetail> list);
}