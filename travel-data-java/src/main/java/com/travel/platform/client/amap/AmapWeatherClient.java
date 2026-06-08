//调用高德天气 API

package com.travel.platform.client.amap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class AmapWeatherClient {

    private static final String WEATHER_URL = "https://restapi.amap.com/v3/weather/weatherInfo";

    @Value("${api.amap.key}")
    private String amapKey;

    private final RestClient restClient;

    public AmapWeatherClient() {
        this.restClient = RestClient.create();
    }

    /**
     * 查询预报天气（extensions=all 返回未来 4 天预报）
     *
     * @param adcode 城市 adcode，例如 110101
     * @return 高德天气 API 完整响应 Map，包含 forecasts 字段
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getWeatherForecast(String adcode) {
        log.info("请求高德天气 API, adcode={}", adcode);

        Map<String, Object> response = restClient.get()
                .uri(WEATHER_URL + "?key={key}&city={city}&extensions=all&output=JSON", amapKey, adcode)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            log.error("高德天气 API 返回为空, adcode={}", adcode);
            return Map.of();
        }

        String status = String.valueOf(response.get("status"));
        if (!"1".equals(status)) {
            log.error("高德天气 API 请求失败, adcode={}, info={}, infocode={}",
                    adcode, response.get("info"), response.get("infocode"));
            return Map.of();
        }

        log.info("高德天气 API 请求成功, adcode={}, city={}",
                adcode, extractCity(response));

        return response;
    }

    /**
     * 从 forecasts 中提取城市名称（辅助日志）
     */
    @SuppressWarnings("unchecked")
    private String extractCity(Map<String, Object> response) {
        try {
            var forecasts = (java.util.List<Map<String, Object>>) response.get("forecasts");
            if (forecasts != null && !forecasts.isEmpty()) {
                return String.valueOf(forecasts.get(0).get("city"));
            }
        } catch (Exception ignored) {
            // 解析失败不影响主流程
        }
        return "unknown";
    }
}
