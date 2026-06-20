# Travel Choice — 智能旅行推荐平台

## 一、项目概述

这是一个**前后端分离 + Python 数据分析 + 实时计算**的旅行推荐系统。用户输入出发城市、旅行日期和预算后，系统通过多维度评分算法（景色、交通、住宿、天气、性价比、拥挤度）自动计算并排名最佳目的地，同时通过 Kafka + Flink 实时追踪热门目的地。

**核心链路：**

```
浏览器 (Vue 3)
  │
  │  POST /api/recommend/calculate
  ▼
Spring Boot (Java 17)
  │
  ├─→ 查城市坐标 → 高德 API 采集路线/天气 → 生成酒店价格
  ├─→ Kafka 发布推荐请求事件 → Flink 窗口聚合 → ClickHouse TopN
  ├─→ ProcessBuilder 调用 Python 评分脚本
  │     │
  │     ▼
  │   Python 推荐引擎
  │     │  读取 dwd_* 表 → 计算评分 → 写入 ads_* 表
  │     ▼
  │   MySQL 8
  │     │
  │     ▼
  └─→ Java 查询 ads 表返回结果
  ▼
浏览器展示表格 + ECharts 图表 + 实时热门 TopN
```

---

## 二、技术栈

| 层       | 技术                                     | 说明                         |
| -------- | ---------------------------------------- | ---------------------------- |
| 前端     | Vue 3 + Element Plus + ECharts + Axios   | SPA，Composition API，8 个页面 |
| 后端     | Spring Boot 4 + MyBatis + Lombok         | REST API，11 个 Controller     |
| 缓存     | Redis 7                                  | 推荐/看板/城市缓存，故障自动降级 |
| 消息队列 | Kafka 3.7 (KRaft)                        | 推荐请求事件发布               |
| 实时计算 | Flink 1.19                               | 窗口聚合热门目的地 TopN        |
| OLAP     | ClickHouse 24.8                          | 实时 ADS 表存储               |
| 数据分析 | Python 3 + pandas + SQLAlchemy + pymysql | 推荐评分计算脚本               |
| 数据库   | MySQL 8                                  | 数仓 dwd/ads 分层，9 张表      |
| 任务调度 | Airflow 2.9                              | 每日 ETL + 实时链路监控        |
| 容器化   | Docker Compose                           | 8 个服务编排                   |
| 构建     | Vite 5 (前端) + Maven (后端)             |                              |

---

## 三、目录结构

```
travel_choice/
│
├── infra/
│   ├── mysql/
│   │   ├── init.sql                      # 建库建表 + 种子数据（⭐ 第一个看）
│   │   ├── upgrade_v2.sql                # V2: ETL 日志 + 数据质量表
│   │   ├── upgrade_v3.sql                # V3: 天气/路线字段增强
│   │   ├── upgrade_v3_dynamic_recommend.sql  # V3: 城市坐标表
│   │   ├── upgrade_v4.sql                # V4: ETL request_id + duration
│   │   └── upgrade_v5.sql                # V5: 推荐请求事件表
│   ├── clickhouse/
│   │   └── init.sql                      # ClickHouse 实时 ADS 表
│   └── airflow/dags/
│       ├── travel_daily_etl_dag.py        # 每日 ETL DAG
│       └── travel_realtime_monitor_dag.py # 实时链路监控 DAG
│
├── travel-data-java/             # Spring Boot 后端
│   ├── pom.xml
│   ├── src/main/resources/
│   │   └── application.yml       # 数据库连接、MyBatis、Redis、Kafka 配置
│   └── src/main/java/.../platform/
│       ├── common/               # ApiResponse + GlobalExceptionHandler + JacksonConfig
│       ├── client/amap/          # 高德路线/天气 API 客户端
│       ├── entity/               # 9 个数据库实体类
│       ├── mapper/               # 10 个 MyBatis SQL 映射
│       ├── service/              # 10 个 Service（含 collect/ 采集子包）
│       ├── controller/           # 11 个 REST 接口
│       └── dto/                  # 5 个请求/响应 DTO
│
├── travel-analysis-python/       # Python 推荐引擎
│   ├── config/db_config.py       # 数据库连接
│   ├── model/                    # 评分模型（纯函数）
│   ├── repository/               # 数据访问层
│   ├── jobs/                     # 编排层
│   ├── utils/helpers.py          # 工具函数
│   └── recommend_score_job.py    # CLI 入口（⭐ 核心算法）
│
├── travel-realtime-flink/        # Flink 实时计算
│   └── src/main/java/.../realtime/
│       └── HotDestinationTopNJob.java  # Kafka → 窗口聚合 → ClickHouse
│
├── travel-web/                   # Vue 3 前端
│   ├── vite.config.js            # 代理配置 /api → localhost:8080
│   └── src/
│       ├── layout/AppLayout.vue  # 侧边栏布局
│       ├── api/                  # 8 个 Axios API 模块
│       ├── components/           # 4 个可复用组件
│       ├── views/                # 8 个页面
│       ├── router/               # 路由
│       └── main.ts               # 入口
│
├── docker-compose.yml            # 8 个服务编排
├── .env.example                  # 环境变量模板
└── README.md
```

---

## 四、数据库设计

数据库名：`travel_decision_platform`，采用**数仓分层命名**（dwd = 明细层，ads = 应用层）：

```
┌─────────────────────────┐     ┌─────────────────────────┐
│  dwd_city_location      │     │  dwd_destination        │
│  城市坐标表              │     │  目的地维表              │
│  ─────────────────────  │     │  ─────────────────────  │
│  city_code (UK)         │     │  destination_code (UK)  │
│  city_name              │     │  destination_name       │
│  amap_adcode            │     │  amap_adcode            │
│  latitude / longitude   │     │  scenery_score          │
└─────────────────────────┘     │  popularity_score       │
                                │  facility_score         │
                                └──────────┬──────────────┘
                                           │
            ┌──────────────────────────────┼──────────────────────────┐
            │                              │                          │
            ▼                              ▼                          ▼
┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐
│ dwd_route_detail    │  │ dwd_weather_detail   │  │dwd_hotel_price_detail│
│ 交通路线明细         │  │ 天气明细              │  │ 酒店价格明细          │
│ origin_city         │  │ destination_code     │  │ destination_code    │
│ destination_code    │  │ weather_date         │  │ avg_price           │
│ distance_km         │  │ temp_max / temp_min  │  │ hotel_score         │
│ traffic_cost        │  │ weather_score        │  └──────────┬──────────┘
│ convenience_score   │  └─────────────────────┘             │
└─────────────────────┘                                      │
                                                             ▼
                ┌───────────────────────────────────────────────────────┐
                │  ads_destination_recommend_result (MySQL)             │
                │  推荐结果表（Python 写入，Java 读取）                  │
                │  request_id + origin_city + dates → 推荐排名/评分/理由│
                └───────────────────────────────────────────────────────┘

┌─────────────────────────────┐     ┌──────────────────────────────┐
│ dwd_recommend_request_event │     │ ads_hot_destination_topn     │
│ 推荐请求事件 (MySQL)         │ ──→ │ 热门目的地 TopN (ClickHouse) │
│ request_id, origin_city,    │Kafka│ window_start/end, dest_code, │
│ destinations, event_time    │Flink│ request_count, rank_no       │
└─────────────────────────────┘     └──────────────────────────────┘

运维表: etl_job_log (ETL 日志) | data_quality_check_result (质量检查)
```

---

## 五、推荐阅读顺序

按 **从底到顶** 的顺序，先理解数据和算法，再看后端接口，最后看前端展示和实时链路。

### 第 1 步：数据库 — 理解数据长什么样

**文件：`infra/mysql/init.sql`**

- 5 张初始表的字段含义（目的地、路线、天气、酒店、推荐结果）
- 模拟数据的取值逻辑（6 个目的地、6 条交通路线、30 条天气记录、6 条酒店价格）
- 重点关注 `dwd_destination` 的三个评分字段：`scenery_score`、`popularity_score`、`facility_score`
- 然后看 `upgrade_v2.sql` ~ `upgrade_v5.sql` 了解每次迭代新增了哪些表和字段

### 第 2 步：Python 推荐引擎 — 理解核心算法

**文件：`travel-analysis-python/recommend_score_job.py`**

这是整个项目的核心评分逻辑，阅读顺序：

1. **`main()` 函数** — 接收 5~6 个命令行参数（originCity, startDate, endDate, budget, requestId, [destinationCodes]）
2. **`build_recommend_result()` 函数** — 核心编排：
   - 查询 `dwd_route_detail` 获取交通费用
   - 查询 `dwd_destination` 获取目的地基础数据
   - 查询 `dwd_weather_detail` 获取天气评分
   - 查询 `dwd_hotel_price_detail` 获取酒店价格
   - pandas merge 关联 4 张表
   - **逐目的地计算**：
     - 费用估算 = 往返交通 + 酒店×天数 + 餐饮(150/天) + 门票(200)
     - 6 维评分权重：景色25% + 天气20% + 住宿15% + 交通15% + 性价比15% + 人流10%
     - 推荐等级：>=85 HIGH、>=70 MEDIUM、<70 LOW
     - 自动生成中文推荐理由
   - 写入 `ads_destination_recommend_result` 表

### 第 3 步：Java Entity + Mapper — 理解数据模型

**按顺序看：**

| 顺序 | 文件 | 作用 |
|------|------|------|
| 3.1 | `entity/Destination.java` | 目的地实体，对应 `dwd_destination` 表 |
| 3.2 | `entity/CityLocation.java` | 城市坐标实体，对应 `dwd_city_location` 表 |
| 3.3 | `entity/RecommendResult.java` | 推荐结果实体，对应 `ads_*` 表 |
| 3.4 | `entity/EtlJobLog.java` | ETL 日志实体（含 requestId, durationSeconds） |
| 3.5 | `mapper/DestinationMapper.java` | `@Select` 注解 SQL，查 `dwd_destination` |
| 3.6 | `mapper/CityLocationMapper.java` | 按城市名查坐标、查所有活跃城市 |
| 3.7 | `mapper/RecommendResultMapper.java` | 按 requestId / 按城市+日期查推荐结果 |

**重点理解：** MyBatis 的 `map-underscore-to-camel-case: true` 配置让 `destination_name` 自动映射到 `destinationName`。

### 第 4 步：Java Service — 理解业务编排

**按顺序看：**

| 顺序 | 文件 | 作用 |
|------|------|------|
| 4.1 | `service/RecommendService.java` | **核心** — 编排: 城市查询→路线采集→天气采集→酒店生成→Kafka 事件→Python 评分 |
| 4.2 | `service/collect/RouteCollectService.java` | 高德路线 API 采集，支持指定目的地集合 |
| 4.3 | `service/collect/WeatherCollectService.java` | 高德天气 API 采集 |
| 4.4 | `service/collect/HotelPriceGenerateService.java` | 酒店价格模型生成 |
| 4.5 | `service/RedisCacheService.java` | Redis 缓存封装（get/set/evict） |
| 4.6 | `service/DataQualityService.java` | 9 条数据质量规则 |
| 4.7 | `service/RecommendRequestProducer.java` | Kafka 事件发布 |

**`RecommendService.calculateRecommend()` 关键流程：**
```java
1. 查出发城市坐标 (CityLocationMapper)
2. 筛选候选目的地（支持用户指定 destinationCodes）
3. 发布 Kafka 事件 (RecommendRequestProducer)
4. 动态采集路线 (RouteCollectService → 高德 API)
5. 动态采集天气 (WeatherCollectService → 高德 API)
6. 生成酒店价格 (HotelPriceGenerateService)
7. 调用 Python 脚本 (ProcessBuilder)
8. 记录 ETL 日志
```

### 第 5 步：Java Controller + DTO — 理解接口

**按顺序看：**

| 顺序 | 文件 | 作用 |
|------|------|------|
| 5.1 | `common/result/ApiResponse.java` | 统一响应信封 |
| 5.2 | `common/exception/GlobalExceptionHandler.java` | 全局异常处理 |
| 5.3 | `dto/RecommendCalculateRequest.java` | 请求体：originCity / dates / budget / destinationCodes |
| 5.4 | `controller/RecommendController.java` | POST calculate + GET results/by-request |
| 5.5 | `controller/CityController.java` | GET /api/cities 城市列表 |
| 5.6 | `controller/DestinationController.java` | GET /api/destinations |
| 5.7 | `controller/DashboardController.java` | GET /api/dashboard/stats |
| 5.8 | `controller/HotDestinationController.java` | GET /api/hot-destinations/topn |

### 第 6 步：前端 — 理解页面和组件

**按顺序看：**

| 顺序 | 文件 | 作用 |
|------|------|------|
| 6.1 | `api/request.ts` | Axios 实例 + ApiResponse 自动解包 |
| 6.2 | `api/recommend.ts` | 推荐 API（含 getCities） |
| 6.3 | `layout/AppLayout.vue` | 可折叠侧边栏布局 |
| 6.4 | `components/SearchForm.vue` | 城市下拉 + 日期 + 预算 + 目的地多选 |
| 6.5 | `views/RecommendPage.vue` | 推荐首页 → 调 API → 跳转结果页 |
| 6.6 | `views/RecommendResult.vue` | 按 requestId 查询结果 + 表格 + 图表 |
| 6.7 | `views/Dashboard.vue` | 统计卡片 + ECharts + 实时热门 TopN |
| 6.8 | `views/DataCollect.vue` | 天气/路线/酒店/推荐采集操作面板 |
| 6.9 | `router/index.ts` | 8 条路由定义 |

### 第 7 步：实时计算链路 — 理解大数据组件

**按顺序看：**

| 顺序 | 文件 | 作用 |
|------|------|------|
| 7.1 | `service/RecommendRequestProducer.java` | 推荐请求 → MySQL + Kafka |
| 7.2 | `travel-realtime-flink/HotDestinationTopNJob.java` | Kafka → 窗口聚合 → ClickHouse |
| 7.3 | `service/HotDestinationService.java` | ClickHouse JDBC 查询 TopN |
| 7.4 | `infra/clickhouse/init.sql` | ReplacingMergeTree 表结构 |
| 7.5 | `infra/airflow/dags/` | 每日 ETL + 实时监控 DAG |

### 第 8 步：基础设施 — 理解部署编排

**按顺序看：**

| 顺序 | 文件 | 作用 |
|------|------|------|
| 8.1 | `docker-compose.yml` | 8 个服务（MySQL/Redis/Kafka/ClickHouse/Flink/Airflow） |
| 8.2 | `.env.example` | 24 个环境变量 |
| 8.3 | `infra/mysql/*.sql` | 数据库升级脚本链 |

---

## 六、接口清单

| 方法   | 路径                              | 说明             | 调用链                                    |
| ------ | --------------------------------- | ---------------- | ----------------------------------------- |
| GET    | `/api/cities`                     | 城市列表         | CityController → CityLocationService (Redis) |
| GET    | `/api/destinations`               | 目的地列表       | DestinationController → Service (Redis)    |
| GET    | `/api/destinations/{code}`        | 目的地详情       | DestinationController → Service            |
| POST   | `/api/recommend/calculate`        | 触发推荐计算     | RecommendController → Service → 采集+Kafka+Python |
| GET    | `/api/recommend/results`          | 查询推荐结果     | RecommendController → Service (Redis)      |
| GET    | `/api/recommend/results/by-request` | 按 requestId 查询 | RecommendController → Service (Redis)    |
| POST   | `/api/weather/collect`            | 天气采集         | WeatherController → WeatherCollectService  |
| POST   | `/api/route/collect`              | 路线采集         | RouteController → RouteCollectService      |
| POST   | `/api/hotel/generate`             | 酒店价格生成     | HotelPriceController → HotelPriceGenerateService |
| GET    | `/api/dashboard/stats`            | 看板统计         | DashboardController → DashboardService (Redis) |
| GET    | `/api/etl/jobs`                   | ETL 日志         | EtlJobController → JobLogService           |
| POST   | `/api/quality/run`                | 执行质量检查     | DataQualityController → DataQualityService |
| GET    | `/api/quality/results`            | 质量检查结果     | DataQualityController → DataQualityService |
| GET    | `/api/hot-destinations/topn`      | 热门 TopN        | HotDestinationController → HotDestinationService (ClickHouse) |

### POST /api/recommend/calculate 请求体

```json
{
  "originCity": "北京",
  "travelStartDate": "2026-07-01",
  "travelEndDate": "2026-07-05",
  "userBudget": 5000,
  "destinationCodes": ["DEST001", "DEST002"]
}
```

> `destinationCodes` 为空时系统推荐全部目的地。

### GET /api/recommend/results/by-request 查询参数

```
?requestId=REQ_20260701_a3b2c1d4
```

---

## 七、评分算法详解

Python 脚本对每个目的地计算 6 个维度评分，加权得到 `final_score`：

```
final_score = scenery   × 0.25   (景观分：dwd_destination.scenery_score)
            + weather   × 0.20   (天气分：dwd_weather_detail 旅行期间均值)
            + hotel     × 0.15   (住宿分：dwd_hotel_price_detail.hotel_score)
            + traffic   × 0.15   (交通分：dwd_route_detail.convenience_score)
            + cost      × 0.15   (性价比：预算内越高越好，超预算扣分)
            + crowd     × 0.10   (人流分：100 - 拥挤指数，越不挤越高)
```

**费用估算公式：**

```
总费用 = 往返交通费 + 酒店均价 × 天数 + 150(餐饮/天) + 200(门票)
```

**拥挤指数：**

```
crowd_index = popularity_score × 0.5
crowd_score = 100 - crowd_index
```

---

## 八、启动方式

```bash
# 1. 配置环境变量
cp .env.example .env
# 编辑 .env 填入 MYSQL_PASSWORD 和 AMAP_API_KEY

# 2. Docker Compose 启动基础设施
docker compose up -d mysql redis kafka kafka-topic-init clickhouse

# 3. Java 后端
cd travel-data-java
mvn spring-boot:run            # → localhost:8080

# 4. Python 依赖 (首次)
cd travel-analysis-python
pip install -r requirements.txt

# 5. Vue 前端
cd travel-web
npm install
npm run dev                    # → localhost:5173

# 6. (可选) Flink 实时计算
cd travel-realtime-flink && mvn -q -DskipTests package && cd ..
docker compose --profile realtime up -d flink-jobmanager flink-taskmanager
docker exec -it travel-flink-jobmanager flink run /opt/flink/usrlib/travel-realtime-flink-0.1.0.jar

# 7. (可选) Airflow 调度
docker compose --profile airflow up -d    # → localhost:8088 (admin/admin)
```

---

## 九、测试用例

在推荐首页输入以下参数测试完整链路：

| 参数     | 值           |
| -------- | ------------ |
| 出发城市 | 北京         |
| 开始日期 | 2026-07-01   |
| 结束日期 | 2026-07-05   |
| 预算     | 5000         |

系统会自动采集北京到各目的地的路线和天气数据，然后运行评分引擎生成推荐排名。

**测试要点：**

- 不选目的地：系统对所有启用目的地推荐
- 选择部分目的地（如 DEST001, DEST002）：仅对这些目的地排序
- 每次推荐生成独立 requestId，结果页按 requestId 查询
- 看板页面展示实时热门 TopN（需启动 Kafka + Flink + ClickHouse）
- ETL 监控页面可查看每次采集的任务日志（含 requestId 和耗时）
- 数据质量页面可一键执行 9 条质量规则
