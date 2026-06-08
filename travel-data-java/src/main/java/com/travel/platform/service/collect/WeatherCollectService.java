//把高德天气数据写入 dwd_weather_detail

package com.travel.platform.service.collect;

import com.travel.platform.client.amap.AmapWeatherClient;
import com.travel.platform.entity.Destination;
import com.travel.platform.entity.EtlJobLog;
import com.travel.platform.entity.WeatherDetail;
import com.travel.platform.mapper.DestinationMapper;
import com.travel.platform.mapper.WeatherMapper;
import com.travel.platform.service.JobLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherCollectService {

    private final DestinationMapper destinationMapper;
    private final WeatherMapper weatherMapper;
    private final AmapWeatherClient amapWeatherClient;
    private final JobLogService jobLogService;

    /**
     * 采集所有活跃目的地的天气预报数据
     *
     * @return 成功采集的目的地数量
     */
    @Transactional
    public int collectAll() {
        EtlJobLog jobLog = jobLogService.startJob(
                "collect_weather_amap",
                "COLLECT",
                "高德天气预报数据采集",
                LocalDate.now()
        );

        try {
            List<Destination> destinations = destinationMapper.selectAllActive();
            log.info("查询到 {} 个活跃目的地", destinations.size());

            int successCount = 0;
            for (Destination dest : destinations) {
                if (dest.getAmapAdcode() == null || dest.getAmapAdcode().isBlank()) {
                    log.warn("目的地 {}({}) 缺少 amap_adcode，跳过天气采集",
                            dest.getDestinationName(), dest.getDestinationCode());
                    continue;
                }
                try {
                    collectForDestination(dest);
                    successCount++;
                } catch (Exception e) {
                    log.error("采集目的地 {} 天气失败", dest.getDestinationName(), e);
                }
            }

            log.info("天气采集完成: 总计={}, 成功={}, 失败={}",
                    destinations.size(), successCount, destinations.size() - successCount);

            jobLogService.finishSuccess(jobLog.getId(), successCount);
            return successCount;
        } catch (Exception e) {
            jobLogService.finishFailed(jobLog.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * 采集指定目的地集合的天气数据（用于推荐请求动态采集）
     */
    @Transactional
    public int collectForDestinations(List<Destination> destinations) {
        EtlJobLog jobLog = jobLogService.startJob(
                "collect_weather_amap",
                "COLLECT",
                "推荐请求-天气数据采集",
                LocalDate.now()
        );

        try {
            log.info("推荐天气采集: 目的地数量={}", destinations.size());
            int successCount = 0;

            for (Destination dest : destinations) {
                if (dest.getAmapAdcode() == null || dest.getAmapAdcode().isBlank()) {
                    log.warn("目的地 {} 缺少 amap_adcode，跳过天气采集", dest.getDestinationName());
                    continue;
                }
                try {
                    collectForDestination(dest);
                    successCount++;
                } catch (Exception e) {
                    log.error("采集目的地 {} 天气失败", dest.getDestinationName(), e);
                }
            }

            log.info("推荐天气采集完成: 成功={}", successCount);
            jobLogService.finishSuccess(jobLog.getId(), successCount);
            return successCount;
        } catch (Exception e) {
            jobLogService.finishFailed(jobLog.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * 采集单个目的地的天气数据
     */
    public void collectForDestination(Destination dest) {
        String adcode = dest.getAmapAdcode();
        Map<String, Object> response = amapWeatherClient.getWeatherForecast(adcode);

        if (response.isEmpty()) {
            log.warn("目的地 {} 天气 API 返回为空，跳过", dest.getDestinationName());
            return;
        }

        List<WeatherDetail> weatherList = parseForecasts(response, dest);
        if (weatherList.isEmpty()) {
            log.warn("目的地 {} 天气预报数据为空，跳过", dest.getDestinationName());
            return;
        }

        // 先删除旧数据，再批量写入
        weatherMapper.deleteByDestinationCode(dest.getDestinationCode());
        weatherMapper.batchInsert(weatherList);

        log.info("目的地 {}({}) 写入 {} 条天气预报数据",
                dest.getDestinationName(), dest.getDestinationCode(), weatherList.size());
    }

    /**
     * 解析高德天气预报 casts 数据
     */
    @SuppressWarnings("unchecked")
    private List<WeatherDetail> parseForecasts(Map<String, Object> response, Destination dest) {
        List<WeatherDetail> result = new ArrayList<>();

        List<Map<String, Object>> forecasts = (List<Map<String, Object>>) response.get("forecasts");
        if (forecasts == null || forecasts.isEmpty()) {
            return result;
        }

        List<Map<String, Object>> casts = (List<Map<String, Object>>) forecasts.get(0).get("casts");
        if (casts == null) {
            return result;
        }

        for (Map<String, Object> cast : casts) {
            WeatherDetail detail = buildWeatherDetail(cast, dest);
            if (detail != null) {
                result.add(detail);
            }
        }

        return result;
    }

    /**
     * 构建单条天气详情
     */
    private WeatherDetail buildWeatherDetail(Map<String, Object> cast, Destination dest) {
        try {
            WeatherDetail detail = new WeatherDetail();
            detail.setDestinationCode(dest.getDestinationCode());
            detail.setDestinationName(dest.getDestinationName());

            // 日期
            String dateStr = String.valueOf(cast.get("date"));
            detail.setWeatherDate(LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd")));

            // 天气类型（取白天天气）
            String dayWeather = String.valueOf(cast.get("dayweather"));
            detail.setWeatherType(dayWeather);

            // 温度：白天温度 → max，夜间温度 → min
            detail.setTempMax(new BigDecimal(String.valueOf(cast.get("daytemp"))));
            detail.setTempMin(new BigDecimal(String.valueOf(cast.get("nighttemp"))));

            // 风力等级（级） → 估算风速 km/h
            BigDecimal windSpeed = estimateWindSpeed(String.valueOf(cast.get("daypower")));
            detail.setWindSpeed(windSpeed);

            // 根据天气类型估算降水概率
            detail.setPrecipitationProbability(estimatePrecipitation(dayWeather));

            // 天气评分 & 风险等级
            detail.setWeatherScore(calcWeatherScore(dayWeather, windSpeed));
            detail.setWeatherRiskLevel(calcRiskLevel(dayWeather, windSpeed));

            detail.setDataSource("amap_weather");

            return detail;
        } catch (Exception e) {
            log.warn("解析天气 cast 数据异常: {}", cast, e);
            return null;
        }
    }

    /**
     * 根据天气类型估算降水概率
     */
    private BigDecimal estimatePrecipitation(String weatherType) {
        if (weatherType.contains("大雨") || weatherType.contains("暴雨")) return new BigDecimal("95");
        if (weatherType.contains("中雨")) return new BigDecimal("90");
        if (weatherType.contains("小雨") || weatherType.contains("阵雨")) return new BigDecimal("70");
        if (weatherType.contains("雨夹雪") || weatherType.contains("雷阵雨")) return new BigDecimal("75");
        if (weatherType.contains("雪")) return new BigDecimal("50");
        if (weatherType.contains("阴")) return new BigDecimal("40");
        if (weatherType.contains("多云")) return new BigDecimal("25");
        if (weatherType.contains("晴")) return new BigDecimal("5");
        return new BigDecimal("30"); // 默认
    }

    /**
     * 风力等级（级）→ 估算风速 km/h
     * 0级=0, 1级=5, 2级=12, 3级=20, 4级=28, 5级=38, 6级=50, 7+=65
     */
    private BigDecimal estimateWindSpeed(String windPower) {
        try {
            int level = Integer.parseInt(windPower);
            double speed = switch (level) {
                case 0 -> 0;
                case 1 -> 5;
                case 2 -> 12;
                case 3 -> 20;
                case 4 -> 28;
                case 5 -> 38;
                case 6 -> 50;
                default -> 65;
            };
            return BigDecimal.valueOf(speed);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 计算天气评分（0-100），越高越好
     * 基础分由天气类型决定，大风扣分
     */
    private BigDecimal calcWeatherScore(String weatherType, BigDecimal windSpeed) {
        double base = 80;

        // 天气类型修正
        if (weatherType.contains("晴")) base = 95;
        else if (weatherType.contains("多云")) base = 85;
        else if (weatherType.contains("阴")) base = 70;
        else if (weatherType.contains("小雨") || weatherType.contains("阵雨")) base = 55;
        else if (weatherType.contains("中雨")) base = 40;
        else if (weatherType.contains("大雨") || weatherType.contains("暴雨")) base = 25;
        else if (weatherType.contains("雪")) base = 45;
        else if (weatherType.contains("雷")) base = 35;

        // 大风扣分：>30km/h 每 10km/h 扣 5 分
        if (windSpeed.compareTo(BigDecimal.valueOf(30)) > 0) {
            double penalty = windSpeed.subtract(BigDecimal.valueOf(30)).doubleValue() / 10 * 5;
            base -= penalty;
        }

        return BigDecimal.valueOf(Math.max(0, Math.min(100, base)));
    }

    /**
     * 计算天气风险等级
     */
    private String calcRiskLevel(String weatherType, BigDecimal windSpeed) {
        // 极端天气直接判定
        if (weatherType.contains("暴雨") || weatherType.contains("大雪")) return "high";
        if (weatherType.contains("大雨") || weatherType.contains("中雪")) return "medium-high";
        if (weatherType.contains("雷")) return "medium-high";

        // 大风判定
        if (windSpeed.compareTo(BigDecimal.valueOf(50)) >= 0) return "medium-high";
        if (windSpeed.compareTo(BigDecimal.valueOf(30)) >= 0) return "medium";

        // 普通天气
        if (weatherType.contains("小雨") || weatherType.contains("阵雨") || weatherType.contains("雨夹雪")) {
            return "medium";
        }
        if (weatherType.contains("阴") || weatherType.contains("多云")) return "low";
        if (weatherType.contains("晴")) return "low";

        return "medium"; // 默认
    }
}