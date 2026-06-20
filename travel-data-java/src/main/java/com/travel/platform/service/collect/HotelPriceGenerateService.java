//生成酒店价格数据

package com.travel.platform.service.collect;

import com.travel.platform.entity.Destination;
import com.travel.platform.entity.EtlJobLog;
import com.travel.platform.entity.HotelPriceDetail;
import com.travel.platform.mapper.DestinationMapper;
import com.travel.platform.mapper.HotelPriceMapper;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelPriceGenerateService {

    private final DestinationMapper destinationMapper;
    private final HotelPriceMapper hotelPriceMapper;
    private final JobLogService jobLogService;
    private final RedisCacheService redisCacheService;

    /**
     * 为所有活跃目的地生成酒店参考价格
     *
     * @param checkinDate  入住日期
     * @param checkoutDate 退房日期
     * @return 生成的酒店价格条数
     */
    public int generateAll(LocalDate checkinDate, LocalDate checkoutDate) {
        EtlJobLog jobLog = jobLogService.startJob(
                "generate_hotel_price_model",
                "GENERATE",
                "酒店参考价格生成",
                checkinDate
        );

        try {
            List<Destination> destinations = destinationMapper.selectAllActive();
            log.info("酒店价格生成: 目的地数量={}, 日期={}~{}", destinations.size(), checkinDate, checkoutDate);

            List<HotelPriceDetail> priceList = new ArrayList<>();

            for (Destination dest : destinations) {
                HotelPriceDetail price = generateForDestination(dest, checkinDate, checkoutDate);
                priceList.add(price);
            }

            if (!priceList.isEmpty()) {
                hotelPriceMapper.deleteByDateRange(checkinDate.toString(), checkoutDate.toString());
                hotelPriceMapper.batchInsert(priceList);
                redisCacheService.evictByPrefix("recommend:");
                redisCacheService.evictByPrefix("dashboard:");
            }

            log.info("酒店价格生成完成: 写入 {} 条数据", priceList.size());
            jobLogService.finishSuccess(jobLog.getId(), priceList.size());
            return priceList.size();
        } catch (Exception e) {
            jobLogService.finishFailed(jobLog.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * 为指定目的地集合生成酒店参考价格（用于推荐请求动态生成）
     */
    public int generateForDestinations(LocalDate checkinDate, LocalDate checkoutDate,
                                        List<Destination> destinations) {
        EtlJobLog jobLog = jobLogService.startJob(
                "generate_hotel_price_model",
                "GENERATE",
                "推荐请求-酒店价格生成",
                checkinDate
        );

        try {
            log.info("推荐酒店价格生成: 目的地数量={}, 日期={}~{}", destinations.size(), checkinDate, checkoutDate);

            List<HotelPriceDetail> priceList = new ArrayList<>();
            for (Destination dest : destinations) {
                HotelPriceDetail price = generateForDestination(dest, checkinDate, checkoutDate);
                priceList.add(price);
            }

            if (!priceList.isEmpty()) {
                hotelPriceMapper.deleteByDateRange(checkinDate.toString(), checkoutDate.toString());
                hotelPriceMapper.batchInsert(priceList);
                redisCacheService.evictByPrefix("recommend:");
                redisCacheService.evictByPrefix("dashboard:");
            }

            log.info("推荐酒店价格生成完成: 写入 {} 条数据", priceList.size());
            jobLogService.finishSuccess(jobLog.getId(), priceList.size());
            return priceList.size();
        } catch (Exception e) {
            jobLogService.finishFailed(jobLog.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * 根据目的地类型和热度生成价格模型
     *
     * 基础价 = 根据目的地类型确定
     *   nature:    280
     *   culture:   350
     *   leisure:   500
     *   adventure: 220
     *   默认:      300
     *
     * 热度加成 = popularity_score / 100 * 200
     * 设施加成 = facility_score / 100 * 100
     */
    private HotelPriceDetail generateForDestination(Destination dest,
                                                     LocalDate checkinDate,
                                                     LocalDate checkoutDate) {
        BigDecimal base = getBasePrice(dest.getDestinationType());

        double popularity = dest.getPopularityScore() != null ? dest.getPopularityScore().doubleValue() : 60;
        double facility = dest.getFacilityScore() != null ? dest.getFacilityScore().doubleValue() : 60;

        double avgPriceVal = base.doubleValue()
                + (popularity / 100.0) * 200
                + (facility / 100.0) * 100;

        BigDecimal avgPrice = BigDecimal.valueOf(avgPriceVal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal minPrice = avgPrice.multiply(BigDecimal.valueOf(0.7)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal maxPrice = avgPrice.multiply(BigDecimal.valueOf(1.5)).setScale(2, RoundingMode.HALF_UP);

        // 酒店评分: 基于设施分 + 随机因子
        BigDecimal hotelScore = BigDecimal.valueOf(
                Math.min(100, Math.max(0, 50 + facility * 0.4 + Math.random() * 15))
        ).setScale(2, RoundingMode.HALF_UP);

        // 价格等级
        String priceLevel;
        if (avgPriceVal >= 600) priceLevel = "luxury";
        else if (avgPriceVal >= 400) priceLevel = "high";
        else if (avgPriceVal >= 250) priceLevel = "medium";
        else priceLevel = "budget";

        HotelPriceDetail detail = new HotelPriceDetail();
        detail.setDestinationCode(dest.getDestinationCode());
        detail.setDestinationName(dest.getDestinationName());
        detail.setCheckinDate(checkinDate);
        detail.setCheckoutDate(checkoutDate);
        detail.setAvgPrice(avgPrice);
        detail.setMinPrice(minPrice);
        detail.setMaxPrice(maxPrice);
        detail.setHotelCount(10 + (int) (popularity / 10));
        detail.setCurrency("CNY");
        detail.setPriceLevel(priceLevel);
        detail.setHotelScore(hotelScore);
        detail.setDataSource("model_v3");

        log.info("目的地 {}: 均价={}, 等级={}, 评分={}",
                dest.getDestinationName(), avgPrice, priceLevel, hotelScore);

        return detail;
    }

    private BigDecimal getBasePrice(String type) {
        if (type == null) return BigDecimal.valueOf(300);
        return switch (type) {
            case "nature" -> BigDecimal.valueOf(280);
            case "culture" -> BigDecimal.valueOf(350);
            case "leisure" -> BigDecimal.valueOf(500);
            case "adventure" -> BigDecimal.valueOf(220);
            default -> BigDecimal.valueOf(300);
        };
    }
}