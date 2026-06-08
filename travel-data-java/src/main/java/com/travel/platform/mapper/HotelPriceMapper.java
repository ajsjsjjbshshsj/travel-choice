package com.travel.platform.mapper;

import com.travel.platform.entity.HotelPriceDetail;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface HotelPriceMapper {

    @Delete("DELETE FROM dwd_hotel_price_detail WHERE checkin_date = #{checkinDate} AND checkout_date = #{checkoutDate}")
    int deleteByDateRange(@Param("checkinDate") String checkinDate, @Param("checkoutDate") String checkoutDate);

    @Insert({
        "<script>",
        "INSERT INTO dwd_hotel_price_detail",
        "(destination_code, destination_name, checkin_date, checkout_date,",
        " avg_price, min_price, max_price, hotel_count, currency,",
        " price_level, hotel_score, data_source)",
        "VALUES",
        "<foreach collection='list' item='h' separator=','>",
        "(#{h.destinationCode}, #{h.destinationName}, #{h.checkinDate}, #{h.checkoutDate},",
        " #{h.avgPrice}, #{h.minPrice}, #{h.maxPrice}, #{h.hotelCount}, #{h.currency},",
        " #{h.priceLevel}, #{h.hotelScore}, #{h.dataSource})",
        "</foreach>",
        "</script>"
    })
    int batchInsert(@Param("list") List<HotelPriceDetail> list);
}