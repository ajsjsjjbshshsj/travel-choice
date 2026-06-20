
# Travel Decision Platform — 旅行智能推荐决策平台

基于多维度加权评分模型的旅行目的地智能推荐系统。整合交通、天气、酒店、景观、预算、拥挤度六大维度，为用户提供数据驱动的旅行决策支持。

## 目录

- [项目简介](#项目简介)
- [技术栈](#技术栈)
- [系统架构](#系统架构)
- [数据链路](#数据链路)
- [数据库表结构](#数据库表结构)
- [推荐评分模型](#推荐评分模型)
- [后端接口](#后端接口)
- [本地启动方式](#本地启动方式)
- [页面一览](#页面一览)
- [版本演进](#版本演进)

---

## 项目简介

本平台解决一个核心问题：**给定出发城市、旅行日期和预算，去哪个目的地性价比最高？**

系统从全国 14 个主要城市出发，通过高德 API 动态采集路线和天气数据，由 Python 评分引擎计算 6 个维度的加权得分，将结果写入应用数据服务层（ADS），再由 Java 后端提供 API 供前端展示。同时通过 Kafka + Flink + ClickHouse 构建实时计算链路，提供热门目的地 TopN 实时看板。

**项目结构：**

```
travel-choice/
├── infra/                              # 基础设施
│   ├── mysql/
│   │   ├── init.sql                    # 建库建表 + 种子数据（6 个目的地）
│   │   ├── mock_data.sql               # 扩展测试数据
│   │   ├── upgrade_v2.sql              # V2: ETL 日志 + 数据质量表
│   │   ├── upgrade_v3.sql              # V3: 天气/路线字段增强
│   │   ├── upgrade_v3_dynamic_recommend.sql  # V3: 城市坐标表 + 14 个城市
│   │   ├── upgrade_v4.sql              # V4: ETL request_id + duration
│   │   └── upgrade_v5.sql              # V5: 推荐请求事件表
│   ├── clickhouse/
│   │   └── init.sql                    # ClickHouse 实时 ADS 表
│   └── airflow/dags/
│       ├── travel_daily_etl_dag.py     # 每日 ETL DAG
│       └── travel_realtime_monitor_dag.py  # 实时链路监控 DAG
│
├── travel-data-java/                   # 后端服务 (Spring Boot)
│   └── src/main/java/com/travel/platform/
│       ├── TravelDataApplication.java  # 启动入口
│       ├── common/
│       │   ├── result/ApiResponse.java       # 统一响应信封
│       │   ├── exception/GlobalExceptionHandler.java  # 全局异常处理
│       │   └── config/JacksonConfig.java     # JSON 序列化配置
│       ├── client/amap/
│       │   ├── AmapRouteClient.java          # 高德路线规划 API
│       │   └── AmapWeatherClient.java        # 高德天气 API
│       ├── controller/                 # 11 个 Controller
│       │   ├── DashboardController.java      # 数据看板
│       │   ├── RecommendController.java      # 推荐计算 + 查询
│       │   ├── DestinationController.java    # 目的地 CRUD
│       │   ├── CityController.java           # 城市列表
│       │   ├── WeatherController.java        # 天气采集
│       │   ├── RouteController.java          # 路线采集
│       │   ├── HotelPriceController.java     # 酒店价格生成
│       │   ├── EtlJobController.java         # ETL 监控
│       │   ├── DataQualityController.java    # 数据质量
│       │   └── HotDestinationController.java # 热门目的地 TopN
│       ├── service/                    # 10 个 Service
│       │   ├── DashboardService.java         # 看板统计
│       │   ├── RecommendService.java         # 推荐编排（核心）
│       │   ├── DestinationService.java       # 目的地查询
│       │   ├── CityLocationService.java      # 城市查询
│       │   ├── JobLogService.java            # ETL 日志
│       │   ├── DataQualityService.java       # 数据质量（9 条规则）
│       │   ├── RedisCacheService.java        # Redis 缓存
│       │   ├── RecommendRequestProducer.java # Kafka 事件发布
│       │   ├── HotDestinationService.java    # ClickHouse TopN 查询
│       │   └── collect/
│       │       ├── RouteCollectService.java       # 路线采集
│       │       ├── WeatherCollectService.java     # 天气采集
│       │       └── HotelPriceGenerateService.java # 酒店价格生成
│       ├── mapper/                     # 10 个 MyBatis Mapper
│       ├── entity/                     # 9 个实体类
│       ├── dto/                        # 5 个 DTO
│       └── vo/                         # DestinationDetailVO
│
├── travel-analysis-python/             # 评分引擎 (Python)
│   ├── config/db_config.py             # 数据库连接
│   ├── model/
│   │   ├── score_model.py              # 加权总分 + 等级 + 理由
│   │   ├── budget_model.py             # 费用估算 + 预算匹配度
│   │   └── crowd_index_model.py        # 拥挤度评分
│   ├── repository/
│   │   └── recommend_repository.py     # 数据访问（4 张 DWD 表）
│   ├── jobs/
│   │   └── build_recommend_result_job.py  # 编排层
│   ├── utils/helpers.py                # 工具函数
│   └── recommend_score_job.py          # CLI 入口
│
├── travel-realtime-flink/              # 实时计算 (Flink)
│   └── src/main/java/.../realtime/
│       └── HotDestinationTopNJob.java  # Kafka → 窗口聚合 → ClickHouse
│
├── travel-web/                         # 前端 (Vue 3)
│   └── src/
│       ├── layout/AppLayout.vue        # 侧边栏布局（可折叠）
│       ├── views/                      # 8 个页面
│       │   ├── Dashboard.vue           # 数据看板 + 热门 TopN
│       │   ├── RecommendPage.vue       # 智能推荐
│       │   ├── RecommendResult.vue     # 推荐结果
│       │   ├── DestinationList.vue     # 目的地列表
│       │   ├── DestinationDetail.vue   # 目的地详情
│       │   ├── DataCollect.vue         # 数据采集
│       │   ├── EtlJobMonitor.vue       # ETL 监控
│       │   └── DataQuality.vue         # 数据质量
│       ├── components/                 # 4 个组件
│       │   ├── SearchForm.vue          # 推荐搜索表单
│       │   ├── RecommendTable.vue      # 推荐结果表格
│       │   ├── ScoreBarChart.vue       # 评分柱状图
│       │   └── ScoreRadarChart.vue     # 评分雷达图
│       ├── api/                        # 8 个 API 模块
│       └── router/index.ts             # 路由配置
│
├── docker-compose.yml                  # 8 个服务编排
├── .env.example                        # 环境变量模板
└── README.md
```

---

## 技术栈

| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **前端框架** | Vue 3 | 3.5+ | Composition API + `<script setup>` |
| **构建工具** | Vite | 5.4+ | 开发代理 + 生产构建 |
| **UI 组件库** | Element Plus | 2.8+ | 表格/表单/标签/卡片等 |
| **图表** | ECharts | 5.5+ | 柱状图、饼图、雷达图 |
| **HTTP 客户端** | Axios | 1.7+ | 统一请求拦截 + ApiResponse 自动解包 |
| **路由** | Vue Router | 4.4+ | 嵌套路由 + 懒加载 |
| **后端框架** | Spring Boot | 4.0+ | REST API 服务 |
| **ORM** | MyBatis | 3.x | 注解式 SQL |
| **缓存** | Redis | 7+ | 推荐结果/看板/城市缓存，故障自动降级 |
| **消息队列** | Kafka (KRaft) | 3.7 | 推荐请求事件发布 |
| **实时计算** | Flink | 1.19 | 滑动窗口聚合热门目的地 TopN |
| **OLAP** | ClickHouse | 24.8 | 实时 ADS 表存储 |
| **数据库** | MySQL | 8.0+ | 数据仓库分层存储 |
| **评分引擎** | Python | 3.10+ | Pandas + SQLAlchemy |
| **任务编排** | Airflow | 2.9 | 每日 ETL + 实时链路监控 |
| **容器化** | Docker Compose | — | MySQL/Redis/Kafka/ClickHouse/Flink/Airflow |
| **依赖管理** | Maven / npm / pip | — | Java / JS / Python |

---

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                      浏览器 (Vue 3)                         │
│  Dashboard │ 推荐页 │ 结果页 │ 目的地 │ 采集 │ ETL │ 质量   │
└──────────────────────────┬──────────────────────────────────┘
                           │ Axios (/api/*)
                           ▼
┌──────────────────────────────────────────────────────────────┐
│                 Spring Boot (port 8080)                      │
│                                                              │
│  RecommendController → RecommendService                      │
│    → 城市查询 → 路线采集 → 天气采集 → 酒店生成 → Python 评分 │
│    → Kafka 事件发布                                          │
│  DashboardController  → DashboardService (Redis 缓存)       │
│  HotDestinationController → HotDestinationService (ClickHouse)│
│  DestinationController  → DestinationService (Redis 缓存)   │
│  WeatherController / RouteController / HotelPriceController  │
│  EtlJobController      → JobLogService                       │
│  DataQualityController  → DataQualityService (9 条规则)     │
│                                                              │
│  common: ApiResponse / GlobalExceptionHandler / JacksonConfig│
│  client: AmapRouteClient / AmapWeatherClient                │
└───────┬──────────┬──────────┬──────────┬────────────────────┘
        │          │          │          │
        ▼          ▼          ▼          ▼
┌───────────┐ ┌─────────┐ ┌────────┐ ┌──────────────┐
│ MySQL 8   │ │ Redis 7 │ │ Kafka  │ │  ClickHouse  │
│           │ │         │ │ (KRaft)│ │              │
│ DWD 层:   │ │ 缓存:   │ │        │ │ 实时 ADS:    │
│ city_loc  │ │ dash:*  │ │ topic: │ │ ads_hot_dest │
│ dest      │ │ recom:* │ │ recom_ │ │ _topn        │
│ route     │ │ city:*  │ │ req_ev │ │              │
│ weather   │ │ dest:*  │ │        │ │              │
│ hotel     │ │         │ └───┬────┘ └──────▲───────┘
│ req_event │ │         │     │             │
│           │ │         │     ▼             │
│ ADS 层:   │ │         │ ┌────────────┐    │
│ ads_dest  │ │         │ │   Flink    │    │
│ _recommend│ │         │ │  1.19      │    │
│ _result   │ │         │ │ 窗口聚合   │────┘
│           │ │         │ │ TopN 计算  │
│ 运维表:   │ │         │ └────────────┘
│ etl_job   │ │         │
│ _log      │ │         │
│ data_qual │ │         │
│ _ity_check│ │         │
└───────────┘ └─────────┘
        ▲
        │ SQLAlchemy
┌───────┴──────────────────────────────────────────────────────┐
│                 Python 评分引擎                               │
│                                                              │
│  recommend_score_job.py (CLI: originCity startDate endDate   │
│                          budget requestId [destCodes])       │
│    → jobs/build_recommend_result_job.py (编排)               │
│      → repository/recommend_repository.py (查询 4 张 DWD 表) │
│      → model/budget_model.py     (费用估算 + 预算评分)        │
│      → model/crowd_index_model.py (拥挤度评分)               │
│      → model/score_model.py      (加权总分 + 等级 + 理由)    │
│    → save_results(request_id) (写入 ADS 表)                  │
└──────────────────────────────────────────────────────────────┘
```

**批处理链路：**

```
用户点击"生成推荐"
  → 前端 POST /api/recommend/calculate
    → RecommendController 接收参数
      → RecommendService 编排：
        1. 查询出发城市坐标 (CityLocationMapper)
        2. 筛选候选目的地
        3. 发布 Kafka 事件 (RecommendRequestProducer)
        4. 动态采集路线 (RouteCollectService → 高德 API)
        5. 动态采集天气 (WeatherCollectService → 高德 API)
        6. 生成酒店价格 (HotelPriceGenerateService)
        7. 调用 Python 脚本 (ProcessBuilder)
           → 查询 4 张 DWD 表 → 6 维度加权评分 → 写入 ADS 表
        8. 记录 ETL 日志
      → 返回 { requestId, message }
  → 前端跳转结果页 GET /api/recommend/results/by-request?requestId=xxx
    → RecommendController 查询 ADS 表返回结果
```

**实时链路：**

```
Spring Boot 发布推荐请求
  → MySQL dwd_recommend_request_event (持久化)
  → Kafka recommend_request_event (消息)
  → Flink HotDestinationTopNJob (窗口聚合)
  → ClickHouse ads_hot_destination_topn (写入)
  → Spring Boot HotDestinationController (查询)
  → Dashboard 实时热门 TopN 表格 (展示)
```

---

## 数据链路

采用类数据仓库分层设计：

```
数据源 → DWD 明细层 → Python 评分引擎 → ADS 应用层 → API → 前端
```

### DWD 层（Data Warehouse Detail）— 原始明细数据

| 表名 | 数据来源 | 关键字段 | 行数级 |
|------|----------|----------|--------|
| `dwd_destination` | 目的地维表 | 编码、名称、省市、类型、amap_adcode、3 项评分 | 6+ |
| `dwd_city_location` | 城市坐标 | 编码、名称、省份、adcode、经纬度 | 14+ |
| `dwd_route_detail` | 交通路线 (高德 API) | 出发城市、目的地、交通类型、距离、费用、时长、便利度 | 按城市×日期 |
| `dwd_weather_detail` | 天气预报 (高德 API) | 目的地、日期、高低温、天气类型、降水概率、评分 | 按目的地×天 |
| `dwd_hotel_price_detail` | 酒店价格 (模型生成) | 目的地、入住/退房日期、均/最低/最高价、评分 | 按目的地×日期区间 |
| `dwd_recommend_request_event` | 推荐请求事件 | 请求 ID、出发城市、日期、预算、目的地数 | 按请求 |

### ADS 层（Application Data Service）— 应用结果数据

| 表名 | 写入方 | 存储引擎 | 关键字段 |
|------|--------|----------|----------|
| `ads_destination_recommend_result` | Python 评分引擎 | MySQL | 6 维度得分、费用明细、总分、排名、等级、理由 |
| `ads_hot_destination_topn` | Flink 实时计算 | ClickHouse | 窗口起止时间、目的地、请求次数、排名 |

### 运维表

| 表名 | 用途 |
|------|------|
| `etl_job_log` | ETL 任务执行日志（任务名、状态、耗时、行数） |
| `data_quality_check_result` | 数据质量检查结果（表名、规则、通过/警告/失败） |

---

## 数据库表结构

### `dwd_destination` — 目的地维表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 自增主键 |
| `destination_code` | VARCHAR(50) UK | 目的地编码 (DEST001~DEST006) |
| `destination_name` | VARCHAR(200) | 名称 |
| `country` / `province` / `city` | VARCHAR | 地理位置 |
| `latitude` / `longitude` | DECIMAL(10,6) | 经纬度 |
| `destination_type` | VARCHAR(50) | 类型: nature / culture / adventure / leisure |
| `amap_adcode` | VARCHAR(20) | 高德行政区 adcode |
| `scenery_score` | DECIMAL(5,2) | 景观评分 (0~100) |
| `popularity_score` | DECIMAL(5,2) | 人气评分 (0~100) |
| `facility_score` | DECIMAL(5,2) | 配套评分 (0~100) |
| `description` | TEXT | 简介 |
| `is_active` | TINYINT | 是否启用 (1=启用) |

### `dwd_city_location` — 城市坐标表

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 自增主键 |
| `city_code` | VARCHAR(50) UK | 城市编码 (BEIJING, SHANGHAI...) |
| `city_name` | VARCHAR(100) | 城市名称 |
| `province` | VARCHAR(100) | 省份 |
| `amap_adcode` | VARCHAR(20) | 高德行政区 adcode |
| `amap_citycode` | VARCHAR(20) | 高德 citycode |
| `latitude` / `longitude` | DECIMAL(10,6) | 经纬度 |
| `is_active` | TINYINT | 是否启用 (1=启用) |

### `dwd_route_detail` — 交通路线明细

| 字段 | 类型 | 说明 |
|------|------|------|
| `origin_city` | VARCHAR(50) | 出发城市 |
| `destination_code` | VARCHAR(50) | 目的地编码 |
| `destination_name` | VARCHAR(200) | 目的地名称 |
| `travel_date` | DATE | 出行日期 |
| `transport_type` | VARCHAR(20) | 交通类型: train / flight / bus |
| `distance_km` | DECIMAL(10,2) | 行程距离（公里） |
| `duration_minutes` | INT | 行程时长（分钟） |
| `traffic_cost` | DECIMAL(10,2) | 交通费用 |
| `currency` | VARCHAR(10) | 币种 |
| `transfer_count` | INT | 换乘次数 |
| `convenience_score` | DECIMAL(5,2) | 交通便利度评分 |
| `data_source` | VARCHAR(50) | 数据来源 (amap_route) |

### `dwd_weather_detail` — 天气明细

| 字段 | 类型 | 说明 |
|------|------|------|
| `destination_code` | VARCHAR(50) | 目的地编码 |
| `destination_name` | VARCHAR(200) | 目的地名称 |
| `weather_date` | DATE | 天气日期 |
| `weather_type` | VARCHAR(20) | 天气类型: sunny / cloudy / rainy / snowy |
| `temp_max` / `temp_min` | DECIMAL(5,2) | 最高/最低温度 |
| `precipitation_probability` | DECIMAL(5,2) | 降水概率 |
| `wind_speed` | DECIMAL(5,2) | 风速 |
| `weather_score` | DECIMAL(5,2) | 天气评分 (0~100) |
| `weather_risk_level` | VARCHAR(20) | 天气风险等级 |
| `data_source` | VARCHAR(50) | 数据来源 (amap_weather) |

### `dwd_hotel_price_detail` — 酒店价格明细

| 字段 | 类型 | 说明 |
|------|------|------|
| `destination_code` | VARCHAR(50) | 目的地编码 |
| `destination_name` | VARCHAR(200) | 目的地名称 |
| `checkin_date` / `checkout_date` | DATE | 入住/退房日期 |
| `avg_price` | DECIMAL(10,2) | 均价 |
| `min_price` / `max_price` | DECIMAL(10,2) | 最低/最高价 |
| `hotel_count` | INT | 酒店数量 |
| `currency` | VARCHAR(10) | 币种 |
| `price_level` | VARCHAR(20) | 价格等级 |
| `hotel_score` | DECIMAL(5,2) | 酒店评分 |
| `data_source` | VARCHAR(50) | 数据来源 (model_generate) |

### `dwd_recommend_request_event` — 推荐请求事件

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | BIGINT PK | 自增主键 |
| `request_id` | VARCHAR(100) | 请求 ID |
| `origin_city` | VARCHAR(50) | 出发城市 |
| `travel_start_date` / `travel_end_date` | DATE | 旅行日期区间 |
| `user_budget` | DECIMAL(10,2) | 用户预算 |
| `destination_count` | INT | 候选目的地数量 |
| `event_time` | DATETIME | 事件时间 |

### `ads_destination_recommend_result` — 推荐结果 (MySQL)

| 字段 | 类型 | 说明 |
|------|------|------|
| `request_id` | VARCHAR(100) | 请求批次 ID |
| `origin_city` | VARCHAR(50) | 出发城市 |
| `destination_code` / `destination_name` | VARCHAR | 目的地 |
| `travel_start_date` / `travel_end_date` | DATE | 旅行日期区间 |
| `travel_days` | INT | 天数 |
| `user_budget` | DECIMAL(10,2) | 用户预算 |
| `estimated_total_cost` | DECIMAL(10,2) | 预估总费用 |
| `traffic_cost` / `hotel_cost` / `food_cost` / `ticket_cost` | DECIMAL(10,2) | 费用分项 |
| `scenery_score` / `traffic_score` / `hotel_score` / `weather_score` | DECIMAL(5,2) | 维度得分 |
| `cost_score` / `crowd_score` | DECIMAL(5,2) | 预算/拥挤度得分 |
| `crowd_index` | DECIMAL(5,2) | 拥挤指数 |
| `final_score` | DECIMAL(5,2) | 加权总分 (0~100) |
| `recommend_rank` | INT | 排名 |
| `recommend_level` | VARCHAR(20) | 推荐等级: HIGH / MEDIUM / LOW |
| `recommend_reason` | TEXT | 推荐理由（中文） |
| `model_version` | VARCHAR(50) | 模型版本 |

### `ads_hot_destination_topn` — 热门目的地 TopN (ClickHouse)

| 字段 | 类型 | 说明 |
|------|------|------|
| `window_start` / `window_end` | DateTime | 窗口起止时间 |
| `destination_code` | String | 目的地编码 |
| `destination_name` | String | 目的地名称 |
| `request_count` | UInt64 | 窗口内请求次数 |
| `rank_no` | UInt32 | 排名 |
| `updated_at` | DateTime | 更新时间 |

> 引擎: `ReplacingMergeTree(updated_at)`，按 `toYYYYMMDD(window_end)` 分区

---

## 推荐评分模型

### 费用估算

```
预估总费用 = 交通费 + 酒店均价 × 天数 + 150 × 天数(餐饮) + 200(门票)
```

### 六维度加权评分

| 维度 | 权重 | 数据来源 | 评分规则 |
|------|------|----------|----------|
| **景观评分** | 25% | `dwd_destination.scenery_score` | 直接取值 |
| **天气评分** | 20% | `dwd_weather_detail` 旅行期间均值 | 直接取值 |
| **酒店评分** | 15% | `dwd_hotel_price_detail.hotel_score` | 直接取值 |
| **交通评分** | 15% | `dwd_route_detail.convenience_score` | 直接取值 |
| **预算评分** | 15% | 计算 | 费用/预算比率 → 分段打分 |
| **拥挤度评分** | 10% | `dwd_destination.popularity_score` | `100 - 人气 × 0.5` |

### 预算匹配度分段

| 费用/预算比率 | 得分 | 含义 |
|:---:|:---:|---|
| ≤ 70% | 95 | 远低于预算，非常划算 |
| ≤ 100% | 80 | 在预算范围内 |
| ≤ 130% | 60 | 略超预算，可接受 |
| > 130% | `100 - (ratio-1) × 50` | 超支越多分越低，clamp(0,100) |

### 最终公式

```
final_score = 景观×0.25 + 天气×0.20 + 酒店×0.15 + 交通×0.15 + 预算×0.15 + 拥挤度×0.10
```

结果 clamp 到 [0, 100]，保留 2 位小数。

### 推荐等级

| 总分区间 | 等级 |
|:---:|:---:|
| ≥ 85 | HIGH — 强烈推荐 |
| ≥ 70 | MEDIUM — 推荐 |
| < 70 | LOW — 一般 |

### 推荐理由生成

根据各维度阈值自动生成中文推荐语：

| 条件 | 理由 |
|------|------|
| 景观 ≥ 90 | 景观评分极高 |
| 天气 ≥ 80 | 旅行期间天气良好 |
| 预算 ≥ 80 | 费用在预算范围内 |
| 拥挤度 ≥ 75 | 游客相对较少，体验更佳 |
| 交通 ≥ 85 | 交通便利 |
| 全部不满足 | 综合评分一般 |

### Python 模块结构

```
travel-analysis-python/
├── config/db_config.py          # DB 连接 + get_engine()
├── model/
│   ├── budget_model.py          # estimate_cost() + compute_cost_score()
│   ├── crowd_index_model.py     # compute_crowd_score()
│   └── score_model.py           # compute_final_score() + determine_level() + generate_reasons()
├── repository/
│   └── recommend_repository.py  # query_routes/destinations/weather/hotels + save_results()
├── jobs/
│   └── build_recommend_result_job.py  # 编排: 查询→合并→评分→排序
├── utils/helpers.py             # clamp()
└── recommend_score_job.py       # CLI 入口
```

所有 model 层函数为**纯函数**（无 I/O），可独立单元测试。

---

## 后端接口

基础路径: `http://localhost:8080`

所有接口统一返回 `ApiResponse` 信封格式：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 推荐模块 `/api/recommend`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| `POST` | `/api/recommend/calculate` | Body: `{ originCity, travelStartDate, travelEndDate, userBudget, destinationCodes? }` | 触发推荐计算（编排采集+评分） |
| `GET` | `/api/recommend/calculate` | `originCity`, `travelStartDate`, `travelEndDate`, `userBudget` | 同上（GET 方式） |
| `GET` | `/api/recommend/results` | `originCity`, `startDate`, `endDate` | 查询推荐结果（Redis 缓存 30 分钟） |
| `GET` | `/api/recommend/results/by-request` | `requestId` | 按请求 ID 查询推荐结果（Redis 缓存 30 分钟） |

**触发推荐计算：**

```bash
curl -X POST http://localhost:8080/api/recommend/calculate \
  -H "Content-Type: application/json" \
  -d '{
    "originCity": "北京",
    "travelStartDate": "2026-07-01",
    "travelEndDate": "2026-07-05",
    "userBudget": 5000,
    "destinationCodes": ["DEST001", "DEST002"]
  }'
```

> `destinationCodes` 为空或不传时，系统对所有启用目的地推荐；传入时仅对指定目的地排序。

**按请求 ID 查询结果：**

```bash
curl "http://localhost:8080/api/recommend/results/by-request?requestId=REQ_20260701_a3b2c1d4"
```

### 城市模块 `/api/cities`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| `GET` | `/api/cities` | — | 获取所有活跃城市（Redis 缓存 30 分钟） |

### 目的地模块 `/api/destinations`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| `GET` | `/api/destinations` | — | 获取所有活跃目的地（Redis 缓存 30 分钟） |
| `GET` | `/api/destinations/{code}` | `destinationCode` (path) | 获取单个目的地详情 |

### 数据采集 `/api/weather` `/api/route` `/api/hotel`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| `POST` | `/api/weather/collect` | — | 触发天气采集（高德 API） |
| `POST` | `/api/route/collect` | Body: `{ originCity, originLongitude, originLatitude, travelDate }` | 触发路线采集（高德 API） |
| `POST` | `/api/hotel/generate` | `checkinDate`, `checkoutDate` | 触发酒店价格生成（模型计算） |

### 数据看板 `/api/dashboard`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| `GET` | `/api/dashboard/stats` | — | 获取看板统计数据（Redis 缓存 5 分钟） |

返回字段包括: `destinationCount`, `etlTotalCount`, `etlSuccessCount`, `etlFailedCount`, `etlRunningCount`, `qualityTotalCount`, `qualityPassCount`, `qualityWarningCount`, `qualityFailedCount`, `recommendTotalCount`, `avgFinalScore`, `avgCrowdIndex`, `topDestinations`, `latestQualityResults`, `redisStatus`, `schedulerStatus`

### ETL 监控 `/api/etl/jobs`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| `GET` | `/api/etl/jobs` | — | 获取最近 50 条 ETL 任务日志 |

### 数据质量 `/api/quality`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| `POST` | `/api/quality/run` | — | 执行全部 9 条数据质量规则 |
| `GET` | `/api/quality/results` | — | 获取最近 100 条检查结果 |

### 热门目的地 `/api/hot-destinations`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| `GET` | `/api/hot-destinations/topn` | `limit` (默认 10) | 查询 ClickHouse 实时热门 TopN |

---

## 本地启动方式

### 环境要求

| 依赖 | 最低版本 | 安装检查 |
|------|---------|---------|
| Java | 17+ | `java -version` |
| Node.js | 18+ | `node -v` |
| Python | 3.10+ | `python --version` |
| Docker | 24+ | `docker --version` |
| Docker Compose | v2+ | `docker compose version` |

### 1. 配置环境变量

```bash
cp .env.example .env
```

编辑 `.env` 填入数据库密码和高德 API Key：

```properties
MYSQL_PASSWORD=your_password
AMAP_API_KEY=your_amap_key_here
```

### 2. 启动基础设施（Docker Compose）

```bash
# 启动核心服务: MySQL + Redis + Kafka + ClickHouse
docker compose up -d mysql redis kafka kafka-topic-init clickhouse
```

MySQL 启动时会自动执行 `infra/mysql/` 下所有 SQL 脚本完成建库建表和种子数据插入。

如需启动实时计算:

```bash
# 先打包 Flink 作业
cd travel-realtime-flink && mvn -q -DskipTests package && cd ..

# 启动 Flink 集群 (需要 --profile realtime)
docker compose --profile realtime up -d flink-jobmanager flink-taskmanager

# 提交 Flink 作业
docker exec -it travel-flink-jobmanager flink run /opt/flink/usrlib/travel-realtime-flink-0.1.0.jar
```

如需启动 Airflow 调度:

```bash
docker compose --profile airflow up -d airflow
# Airflow UI: http://localhost:8088 (admin/admin)
```

### 3. 启动后端

```bash
cd travel-data-java
mvn spring-boot:run
```

后端启动在 `http://localhost:8080`。

### 4. 启动前端

```bash
cd travel-web
npm install
npm run dev
```

前端启动在 `http://localhost:5173`，`/api/*` 请求自动代理到后端。

### 5. 配置高德地图 API Key

路线规划和天气查询依赖高德开放平台 API，需要申请 Web 服务类型的 Key。

1. 前往 [高德开放平台控制台](https://console.amap.com/dev/key/app) 注册并创建应用
2. 在应用下添加 Key，服务平台选择 **Web服务**
3. 将 Key 填入 `.env` 文件的 `AMAP_API_KEY` 字段

> 高德个人开发者账号有每日调用配额（5000 次/天），生产环境建议使用企业认证账号。

### 启动顺序

```
① Docker Compose (MySQL/Redis/Kafka/ClickHouse) → ② Spring Boot (8080) → ③ Vue Dev Server (5173)
```

### 服务端口一览

| 服务 | 端口 | 说明 |
|------|------|------|
| Spring Boot | 8080 | 后端 API |
| Vue Dev Server | 5173 | 前端 |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| Kafka | 9092 | 消息队列 |
| ClickHouse HTTP | 8123 | OLAP 查询 |
| ClickHouse TCP | 9000 | OLAP 写入 |
| Flink Web UI | 8081 | Flink 作业管理 |
| Airflow Web UI | 8088 | 任务调度 |

---

## 页面一览

> 启动后访问 `http://localhost:5173` 查看。

| 页面 | 路由 | 说明 |
|------|------|------|
| 数据看板 | `/` | 8 项统计指标 + 目的地评分柱状图 + 质量分布饼图 + 实时热门 TopN |
| 智能推荐 | `/recommend` | 出发城市下拉 + 日期 + 预算 + 目的地多选，触发推荐计算 |
| 推荐结果 | `/result?requestId=xxx` | 结果表格 + Top5 六维评分对比柱状图 |
| 目的地列表 | `/destinations` | 全部目的地表格，点击行进入详情 |
| 目的地详情 | `/destinations/:code` | 基本信息 + 雷达图 + 进度条评分 |
| 数据采集 | `/data-collect` | 天气/路线/酒店/推荐 4 项采集操作面板 + 操作日志 |
| ETL 监控 | `/etl-monitor` | 任务状态统计卡 + 状态筛选 + 30s 自动刷新表格 |
| 数据质量 | `/data-quality` | 执行检查按钮 + 通过/警告/失败统计 + 结果表 |

---

## 版本演进

| 版本 | 核心能力 | 关键变更 |
|------|----------|----------|
| **V1** | 基础推荐 | 3 页面 + 单文件 Python 评分 + 固定出发地 |
| **V2** | 管理后台 | 侧边栏布局、Dashboard、ETL 监控、数据质量、目的地详情、Python 模块化 |
| **V3** | 动态推荐 | 任意出发城市、候选目的地多选、高德 API 动态采集路线/天气、requestId 隔离、城市坐标表 |
| **V4** | 工程化 | 统一 ApiResponse、全局异常处理、Redis 缓存、Docker Compose、Airflow DAG、ETL 耗时/requestId 追踪、9 条数据质量规则 |
| **V5** | 实时计算 | Kafka 事件发布、Flink 窗口聚合 TopN、ClickHouse OLAP、热门目的地实时看板、推荐请求事件持久化 |

### V3: 动态出发地与候选目的地推荐

- 支持从全国 14 个主要城市出发（`dwd_city_location` 表）
- 用户可指定候选目的地集合，或让系统自动从全部目的地推荐
- 推荐前自动通过高德 API 动态采集路线和天气数据
- 每次推荐生成独立 `requestId`，结果互不覆盖
- 新增接口: `GET /api/cities`、`GET /api/recommend/results/by-request`
- 新增页面: 数据采集面板 (`/data-collect`)

### V4: 工程化升级

**统一响应格式:** 所有 Controller 返回 `ApiResponse` 信封，`GlobalExceptionHandler` 处理异常。

**Redis 缓存:**

| 缓存键 | TTL | 说明 |
|--------|-----|------|
| `recommend:result:{requestId}` | 30 min | 按请求 ID 查询推荐结果 |
| `recommend:results:{originCity}:{startDate}:{endDate}` | 30 min | 按条件查询推荐结果 |
| `dashboard:stats` | 5 min | 看板统计数据 |
| `city:list:active` | 30 min | 活跃城市列表 |
| `destination:list:active` | 30 min | 活跃目的地列表 |

缓存失效策略: ETL 数据写入完成后自动清除 `recommend:*` 和 `dashboard:*` 前缀缓存。Redis 故障时自动降级到 MySQL 查询。

**数据质量规则 (9 条):** 城市坐标非空、目的地坐标非空、amap_adcode 非空、天气评分范围、路线距离>0、路线时长>0、酒店价格有效、requestId 非空、排名不重复。

**Airflow DAG:**
- `travel_daily_etl_dag` (每日): 天气采集 → 酒店价格生成 → 数据质量检查
- `travel_realtime_monitor_dag` (每 10 分钟): Kafka 连通性 → ClickHouse 表检查

### V5: 实时计算链路

**数据仓库分层:**

```
ODS: 外部 API 原始事件（高德路线/天气）
DWD: dwd_destination, dwd_city_location, dwd_weather_detail, dwd_route_detail,
     dwd_hotel_price_detail, dwd_recommend_request_event
DWS: Flink 实时窗口聚合
ADS: ads_destination_recommend_result (MySQL), ads_hot_destination_topn (ClickHouse)
```

**实时链路:** Spring Boot 推荐请求 → MySQL 持久化 → Kafka 发布 → Flink 窗口聚合 → ClickHouse 写入 → HotDestinationController 查询 → Dashboard 展示 TopN

**Docker Compose 服务编排 (8 个服务):**

| 服务 | 镜像 | Profile |
|------|------|---------|
| mysql | mysql:8.4 | default |
| redis | redis:7-alpine | default |
| kafka | bitnami/kafka:3.7 (KRaft) | default |
| kafka-topic-init | bitnami/kafka:3.7 (一次性) | default |
| clickhouse | clickhouse/clickhouse-server:24.8 | default |
| flink-jobmanager | flink:1.19-java17 | realtime |
| flink-taskmanager | flink:1.19-java17 | realtime |
| airflow | apache/airflow:2.9.3 | airflow |

---

## License

MIT
