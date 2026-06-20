package com.travel.platform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.travel.platform.dto.RecommendCalculateRequest;
import com.travel.platform.dto.RecommendCalculateResponse;
import com.travel.platform.dto.RecommendRequestEvent;
import com.travel.platform.entity.CityLocation;
import com.travel.platform.entity.Destination;
import com.travel.platform.entity.EtlJobLog;
import com.travel.platform.entity.RecommendResult;
import com.travel.platform.mapper.CityLocationMapper;
import com.travel.platform.mapper.DestinationMapper;
import com.travel.platform.mapper.RecommendResultMapper;
import com.travel.platform.service.collect.HotelPriceGenerateService;
import com.travel.platform.service.collect.RouteCollectService;
import com.travel.platform.service.collect.WeatherCollectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class RecommendService {

    private final RecommendResultMapper recommendResultMapper;
    private final JobLogService jobLogService;
    private final CityLocationMapper cityLocationMapper;
    private final DestinationMapper destinationMapper;
    private final RouteCollectService routeCollectService;
    private final WeatherCollectService weatherCollectService;
    private final HotelPriceGenerateService hotelPriceGenerateService;
    private final RedisCacheService redisCacheService;
    @Autowired(required = false)
    private RecommendRequestProducer recommendRequestProducer;

    public RecommendService(
            RecommendResultMapper recommendResultMapper,
            JobLogService jobLogService,
            CityLocationMapper cityLocationMapper,
            DestinationMapper destinationMapper,
            RouteCollectService routeCollectService,
            WeatherCollectService weatherCollectService,
            HotelPriceGenerateService hotelPriceGenerateService,
            RedisCacheService redisCacheService
    ) {
        this.recommendResultMapper = recommendResultMapper;
        this.jobLogService = jobLogService;
        this.cityLocationMapper = cityLocationMapper;
        this.destinationMapper = destinationMapper;
        this.routeCollectService = routeCollectService;
        this.weatherCollectService = weatherCollectService;
        this.hotelPriceGenerateService = hotelPriceGenerateService;
        this.redisCacheService = redisCacheService;
    }

    public List<RecommendResult> listRecommendResults(
            String originCity,
            LocalDate startDate,
            LocalDate endDate
    ) {
        String cacheKey = String.format("recommend:results:%s:%s:%s",
                originCity, startDate, endDate);
        return redisCacheService.get(cacheKey, new TypeReference<List<RecommendResult>>() {})
                .orElseGet(() -> {
                    List<RecommendResult> results = recommendResultMapper.selectRecommendResults(
                            originCity, startDate, endDate);
                    redisCacheService.set(cacheKey, results, Duration.ofMinutes(30));
                    return results;
                });
    }

    public List<RecommendResult> listByRequestId(String requestId) {
        String cacheKey = "recommend:result:" + requestId;
        return redisCacheService.get(cacheKey, new TypeReference<List<RecommendResult>>() {})
                .orElseGet(() -> {
                    List<RecommendResult> results = recommendResultMapper.selectByRequestId(requestId);
                    redisCacheService.set(cacheKey, results, Duration.ofMinutes(30));
                    return results;
                });
    }

    public RecommendCalculateResponse calculateRecommend(RecommendCalculateRequest request) {
        String requestId = buildRequestId(request);

        EtlJobLog jobLog = jobLogService.startJob(
                "build_destination_recommend",
                "RECOMMEND",
                "动态目的地推荐评分计算",
                request.getTravelStartDate(),
                requestId
        );

        try {
            // 1. 查出发城市坐标
            CityLocation origin = cityLocationMapper.selectByCityName(request.getOriginCity());
            if (origin == null) {
                throw new RuntimeException("未找到出发城市坐标：" + request.getOriginCity());
            }

            // 2. 查候选目的地
            List<Destination> destinations = destinationMapper.selectAllActive();
            if (request.getDestinationCodes() != null && !request.getDestinationCodes().isEmpty()) {
                destinations = destinations.stream()
                        .filter(d -> request.getDestinationCodes().contains(d.getDestinationCode()))
                        .toList();
            }
            if (destinations.isEmpty()) {
                throw new RuntimeException("候选目的地为空");
            }

            // 3. 动态采集路线
            if (recommendRequestProducer != null) {
                recommendRequestProducer.publish(buildRecommendRequestEvent(requestId, request, destinations));
            }

            routeCollectService.collectForDestinations(
                    request.getOriginCity(),
                    origin.getLongitude(),
                    origin.getLatitude(),
                    request.getTravelStartDate(),
                    destinations
            );

            // 4. 采集天气
            weatherCollectService.collectForDestinations(destinations);

            // 5. 生成酒店价格
            hotelPriceGenerateService.generateForDestinations(
                    request.getTravelStartDate(),
                    request.getTravelEndDate(),
                    destinations
            );

            // 6. 调 Python
            runPythonRecommendScript(request, requestId);

            // 7. 成功日志
            Integer rowCount = recommendResultMapper.countByRequestId(requestId);
            jobLogService.finishSuccess(jobLog.getId(), rowCount != null ? rowCount : 0);
            List<RecommendResult> results = recommendResultMapper.selectByRequestId(requestId);
            redisCacheService.set("recommend:result:" + requestId, results, Duration.ofMinutes(30));

            return new RecommendCalculateResponse(requestId, "推荐计算完成");

        } catch (Exception e) {
            jobLogService.finishFailed(jobLog.getId(), e.getMessage());
            throw new RuntimeException("执行推荐计算失败：" + e.getMessage(), e);
        }
    }

    private String buildRequestId(RecommendCalculateRequest request) {
        String date = request.getTravelStartDate() != null ? request.getTravelStartDate().toString().replace("-", "") : "nodate";
        String shortUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return "REQ_" + date + "_" + shortUuid;
    }

    private RecommendRequestEvent buildRecommendRequestEvent(
            String requestId,
            RecommendCalculateRequest request,
            List<Destination> destinations
    ) {
        List<RecommendRequestEvent.DestinationSnapshot> snapshots = destinations.stream()
                .map(destination -> new RecommendRequestEvent.DestinationSnapshot(
                        destination.getDestinationCode(),
                        destination.getDestinationName()
                ))
                .toList();

        RecommendRequestEvent event = new RecommendRequestEvent();
        event.setRequestId(requestId);
        event.setOriginCity(request.getOriginCity());
        event.setTravelStartDate(request.getTravelStartDate());
        event.setTravelEndDate(request.getTravelEndDate());
        event.setUserBudget(request.getUserBudget());
        event.setDestinationCount(snapshots.size());
        event.setDestinations(snapshots);
        event.setEventTime(LocalDateTime.now());
        return event;
    }

    private void runPythonRecommendScript(RecommendCalculateRequest request, String requestId) {
        try {
            String userDir = System.getProperty("user.dir");
            System.out.println("当前 Java 工作目录：" + userDir);

            Path scriptPath = Paths.get(
                    userDir,
                    "..",
                    "travel-analysis-python",
                    "recommend_score_job.py"
            ).normalize();

            System.out.println("Python 脚本路径：" + scriptPath);

            if (!Files.exists(scriptPath)) {
                throw new RuntimeException("Python 脚本不存在：" + scriptPath);
            }

            String destCodesArg = "";
            if (request.getDestinationCodes() != null && !request.getDestinationCodes().isEmpty()) {
                destCodesArg = String.join(",", request.getDestinationCodes());
            }

            ProcessBuilder processBuilder = new ProcessBuilder(
                    "python",
                    scriptPath.toString(),
                    request.getOriginCity(),
                    request.getTravelStartDate().toString(),
                    request.getTravelEndDate().toString(),
                    request.getUserBudget().toString(),
                    requestId,
                    destCodesArg
            );

            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            try (
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(
                                    process.getInputStream(),
                                    StandardCharsets.UTF_8
                            )
                    )
            ) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Python] " + line);
                }
            }

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("Python 推荐计算脚本执行失败，exitCode=" + exitCode);
            }

        } catch (Exception e) {
            throw new RuntimeException("Python 脚本执行异常：" + e.getMessage(), e);
        }
    }
}
