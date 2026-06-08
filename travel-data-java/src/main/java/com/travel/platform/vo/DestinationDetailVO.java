package com.travel.platform.vo;

import com.travel.platform.entity.Destination;
import com.travel.platform.entity.HotelPriceDetail;
import com.travel.platform.entity.RecommendResult;
import com.travel.platform.entity.RouteDetail;
import com.travel.platform.entity.WeatherDetail;
import lombok.Data;

import java.util.List;

@Data
public class DestinationDetailVO {
    private Destination destination;
    private List<RouteDetail> routes;
    private List<WeatherDetail> weatherList;
    private List<HotelPriceDetail> hotelPrices;
    private List<RecommendResult> recommendHistory;
}