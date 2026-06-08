//调用高德路线规划 API

package com.travel.platform.client.amap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
public class AmapRouteClient {

    private static final String TRANSIT_URL = "https://restapi.amap.com/v3/direction/transit/integrated";

    @Value("${api.amap.key}")
    private String amapKey;

    private final RestClient restClient;

    public AmapRouteClient() {
        this.restClient = RestClient.create();
    }

    /**
     * 公交路径规划
     *
     * @param originLng  出发地经度
     * @param originLat  出发地纬度
     * @param destLng    目的地经度
     * @param destLat    目的地纬度
     * @param originCity 出发地城市名称
     * @param destCity   目的地城市名称
     * @return 高德路径规划 API 完整响应 Map，包含 route.transits 字段
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getTransitRoute(BigDecimal originLng, BigDecimal originLat,
                                               BigDecimal destLng, BigDecimal destLat,
                                               String originCity, String destCity) {
        String origin = originLng + "," + originLat;
        String destination = destLng + "," + destLat;

        log.info("请求高德公交路径规划 API, origin={}, destination={}, city={}, cityd={}",
                origin, destination, originCity, destCity);

        Map<String, Object> response = restClient.get()
                .uri(TRANSIT_URL + "?key={key}&origin={origin}&destination={destination}&city={city}&cityd={cityd}&output=JSON",
                        amapKey, origin, destination, originCity, destCity)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            log.error("高德路径规划 API 返回为空, origin={}, destination={}", origin, destination);
            return Map.of();
        }

        String status = String.valueOf(response.get("status"));
        if (!"1".equals(status)) {
            log.error("高德路径规划 API 请求失败, origin={}, destination={}, info={}, infocode={}",
                    origin, destination, response.get("info"), response.get("infocode"));
            return Map.of();
        }

        log.info("高德路径规划 API 请求成功, origin={}, destination={}", origin, destination);

        return response;
    }
}
