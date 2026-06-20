//把高德路线数据写入 dwd_route_detail

package com.travel.platform.service.collect;

import com.travel.platform.client.amap.AmapRouteClient;
import com.travel.platform.entity.Destination;
import com.travel.platform.entity.EtlJobLog;
import com.travel.platform.entity.RouteDetail;
import com.travel.platform.mapper.DestinationMapper;
import com.travel.platform.mapper.RouteMapper;
import com.travel.platform.service.JobLogService;
import com.travel.platform.service.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteCollectService {

    private final DestinationMapper destinationMapper;
    private final RouteMapper routeMapper;
    private final AmapRouteClient amapRouteClient;
    private final JobLogService jobLogService;
    private final RedisCacheService redisCacheService;

    /**
     * 采集从出发地到所有活跃目的地的公交路线
     *
     * @param originCity      出发城市名称，如 "北京"
     * @param originLng       出发地经度
     * @param originLat       出发地纬度
     * @param travelDate      旅行日期
     * @return 成功采集的路线数量
     */
    public int collectAll(String originCity,
                          BigDecimal originLng, BigDecimal originLat,
                          LocalDate travelDate) {
        EtlJobLog jobLog = jobLogService.startJob(
                "collect_route_amap",
                "COLLECT",
                "高德公交路径规划数据采集",
                travelDate
        );

        try {
            List<Destination> destinations = destinationMapper.selectAllActive();
            log.info("路线采集开始: 出发城市={}, 日期={}, 目的地数量={}", originCity, travelDate, destinations.size());

            List<RouteDetail> routes = new ArrayList<>();

            for (Destination dest : destinations) {
                if (dest.getLongitude() == null || dest.getLatitude() == null) {
                    log.warn("目的地 {} 缺少经纬度，跳过路线采集", dest.getDestinationName());
                    continue;
                }
                try {
                    RouteDetail route = collectForDestination(
                            originCity, originLng, originLat, dest, travelDate);
                    if (route != null) {
                        routes.add(route);
                    }
                } catch (Exception e) {
                    log.error("采集到目的地 {} 的路线失败", dest.getDestinationName(), e);
                }
            }

            if (routes.isEmpty()) {
                log.warn("未采集到任何路线数据");
                jobLogService.finishSuccess(jobLog.getId(), 0);
                return 0;
            }

            // 清除旧数据后批量写入
            routeMapper.deleteByOriginAndDate(originCity, travelDate.toString());
            routeMapper.batchInsert(routes);
            redisCacheService.evictByPrefix("recommend:");
            redisCacheService.evictByPrefix("dashboard:");

            log.info("路线采集完成: 出发城市={}, 写入 {} 条路线", originCity, routes.size());
            jobLogService.finishSuccess(jobLog.getId(), routes.size());
            return routes.size();
        } catch (Exception e) {
            jobLogService.finishFailed(jobLog.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * 采集从出发地到指定目的地集合的公交路线（用于推荐请求动态采集）
     */
    public int collectForDestinations(String originCity,
                                      BigDecimal originLng, BigDecimal originLat,
                                      LocalDate travelDate,
                                      List<Destination> destinations) {
        EtlJobLog jobLog = jobLogService.startJob(
                "collect_route_amap",
                "COLLECT",
                "推荐请求-路线数据采集",
                travelDate
        );

        try {
            log.info("推荐路线采集: 出发城市={}, 日期={}, 目的地数量={}", originCity, travelDate, destinations.size());

            List<RouteDetail> routes = new ArrayList<>();

            for (Destination dest : destinations) {
                if (dest.getLongitude() == null || dest.getLatitude() == null) {
                    log.warn("目的地 {} 缺少经纬度，跳过路线采集", dest.getDestinationName());
                    continue;
                }
                try {
                    RouteDetail route = collectForDestination(
                            originCity, originLng, originLat, dest, travelDate);
                    if (route != null) {
                        routes.add(route);
                    }
                } catch (Exception e) {
                    log.error("采集到目的地 {} 的路线失败", dest.getDestinationName(), e);
                }
            }

            if (!routes.isEmpty()) {
                routeMapper.deleteByOriginAndDate(originCity, travelDate.toString());
                routeMapper.batchInsert(routes);
                redisCacheService.evictByPrefix("recommend:");
                redisCacheService.evictByPrefix("dashboard:");
            }

            log.info("推荐路线采集完成: 写入 {} 条路线", routes.size());
            jobLogService.finishSuccess(jobLog.getId(), routes.size());
            return routes.size();
        } catch (Exception e) {
            jobLogService.finishFailed(jobLog.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * 采集到单个目的地的最优公交路线
     */
    @SuppressWarnings("unchecked")
    private RouteDetail collectForDestination(String originCity,
                                              BigDecimal originLng, BigDecimal originLat,
                                              Destination dest,
                                              LocalDate travelDate) {
        String destCity = dest.getCity() != null ? dest.getCity() : originCity;
        Map<String, Object> response = amapRouteClient.getTransitRoute(
                originLng, originLat, dest.getLongitude(), dest.getLatitude(), originCity, destCity);

        if (response.isEmpty()) {
            return null;
        }

        Map<String, Object> route = (Map<String, Object>) response.get("route");
        if (route == null) {
            return null;
        }

        // 总距离（米 → 公里）
        BigDecimal totalDistanceM = new BigDecimal(String.valueOf(route.get("distance")));
        BigDecimal distanceKm = totalDistanceM.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);

        // 从 transits 中选出最优方案（耗时最短）
        List<Map<String, Object>> transits = (List<Map<String, Object>>) route.get("transits");
        if (transits == null || transits.isEmpty()) {
            log.warn("到目的地 {} 无公交路线方案", dest.getDestinationName());
            return null;
        }

        Map<String, Object> best = selectBestTransit(transits);

        // 解析最优方案
        int durationMinutes = parseDurationMinutes(best);
        BigDecimal trafficCost = parseCost(best);
        int transferCount = parseTransferCount(best);
        String transportType = parseTransportType(best);

        // 计算便利度评分
        BigDecimal convenienceScore = calcConvenienceScore(durationMinutes, transferCount);

        RouteDetail detail = new RouteDetail();
        detail.setOriginCity(originCity);
        detail.setDestinationCode(dest.getDestinationCode());
        detail.setDestinationName(dest.getDestinationName());
        detail.setTravelDate(travelDate);
        detail.setTransportType(transportType);
        detail.setDistanceKm(distanceKm);
        detail.setDurationMinutes(durationMinutes);
        detail.setTrafficCost(trafficCost);
        detail.setCurrency("CNY");
        detail.setTransferCount(transferCount);
        detail.setConvenienceScore(convenienceScore);
        detail.setDataSource("amap_route");

        log.info("目的地 {}: {}分钟, ¥{}, {}换乘, {}",
                dest.getDestinationName(), durationMinutes, trafficCost, transferCount, transportType);

        return detail;
    }

    /**
     * 选出耗时最短的方案
     */
    private Map<String, Object> selectBestTransit(List<Map<String, Object>> transits) {
        Map<String, Object> best = transits.get(0);
        int bestDuration = parseDurationMinutes(best);

        for (int i = 1; i < transits.size(); i++) {
            int duration = parseDurationMinutes(transits.get(i));
            if (duration < bestDuration) {
                best = transits.get(i);
                bestDuration = duration;
            }
        }
        return best;
    }

    /**
     * 解析方案耗时（秒 → 分钟）
     */
    private int parseDurationMinutes(Map<String, Object> transit) {
        try {
            long seconds = Long.parseLong(String.valueOf(transit.get("duration")));
            return (int) (seconds / 60);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 解析方案费用（元）
     */
    private BigDecimal parseCost(Map<String, Object> transit) {
        try {
            return new BigDecimal(String.valueOf(transit.get("cost")));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 解析换乘次数
     * segments 数组中每段是一次乘车，换乘次数 = 段数 - 1
     */
    @SuppressWarnings("unchecked")
    private int parseTransferCount(Map<String, Object> transit) {
        try {
            List<Map<String, Object>> segments = (List<Map<String, Object>>) transit.get("segments");
            if (segments == null || segments.isEmpty()) {
                return 0;
            }
            // 只计算包含公交/地铁的段，纯步行段不算
            int transitSegments = 0;
            for (Map<String, Object> seg : segments) {
                Map<String, Object> bus = (Map<String, Object>) seg.get("bus");
                if (bus != null) {
                    List<?> buslines = (List<?>) bus.get("buslines");
                    if (buslines != null && !buslines.isEmpty()) {
                        transitSegments++;
                    }
                }
            }
            return Math.max(0, transitSegments - 1);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 解析主要交通方式
     * 根据 segments 中 bus/subway 出现频次判断
     */
    @SuppressWarnings("unchecked")
    private String parseTransportType(Map<String, Object> transit) {
        try {
            List<Map<String, Object>> segments = (List<Map<String, Object>>) transit.get("segments");
            if (segments == null) return "unknown";

            boolean hasSubway = false;
            boolean hasBus = false;

            for (Map<String, Object> seg : segments) {
                Map<String, Object> bus = (Map<String, Object>) seg.get("bus");
                if (bus == null) continue;

                List<Map<String, Object>> buslines = (List<Map<String, Object>>) bus.get("buslines");
                if (buslines == null) continue;

                for (Map<String, Object> line : buslines) {
                    String type = String.valueOf(line.get("type"));
                    if (type.contains("地铁") || type.contains("subway")) {
                        hasSubway = true;
                    } else if (type.contains("公交") || type.contains("bus")) {
                        hasBus = true;
                    }
                }
            }

            if (hasSubway && hasBus) return "subway+bus";
            if (hasSubway) return "subway";
            if (hasBus) return "bus";
            return "walking";
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * 计算便利度评分（0-100）
     * 基础分 100，换乘扣分 + 长时间扣分
     */
    private BigDecimal calcConvenienceScore(int durationMinutes, int transferCount) {
        double score = 100.0;

        // 每次换乘扣 10 分
        score -= transferCount * 10.0;

        // 超过 60 分钟，每 30 分钟额外扣 5 分
        if (durationMinutes > 60) {
            score -= ((durationMinutes - 60) / 30.0) * 5.0;
        }

        return BigDecimal.valueOf(Math.max(0, Math.min(100, score)))
                .setScale(2, RoundingMode.HALF_UP);
    }
}