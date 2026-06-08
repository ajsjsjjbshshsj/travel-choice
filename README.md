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
- [页面截图](#页面截图)
- [第二版新增能力](#第二版新增能力)

---

## 项目简介

本平台解决一个核心问题：**给定出发城市、旅行日期和预算，去哪个目的地性价比最高？**

系统从 4 张数据仓库明细层（DWD）表采集数据，通过 Python 评分引擎计算 6 个维度的加权得分，将结果写入应用数据服务层（ADS），再由 Java 后端提供 API 供前端展示。

**项目结构：**

```
travel-choice/
├── infra/                        # 基础设施
│   └── mysql/
│       ├── init.sql              # 建表 + 种子数据（6 个目的地）
│       ├── mock_data.sql         # 扩展测试数据
│       └── upgrade_v2.sql        # V2 版本增量脚本
│
├── travel-data-java/             # 后端服务 (Spring Boot)
│   └── src/main/java/com/travel/platform/
│       ├── controller/           # 5 个 Controller
│       ├── service/              # 5 个 Service
│       ├── mapper/               # 8 个 MyBatis Mapper
│       ├── entity/               # 7 个实体类
│       ├── dto/                  # DashboardStats, RecommendCalculateRequest
│       └── vo/                   # DestinationDetailVO
│
├── travel-analysis-python/       # 评分引擎 (Python)
│   ├── config/db_config.py       # 数据库连接配置
│   ├── model/                    # 评分模型（纯函数）
│   │   ├── score_model.py        # 加权总分 + 等级判定 + 理由生成
│   │   ├── budget_model.py       # 费用估算 + 预算匹配度
│   │   └── crowd_index_model.py  # 拥挤度评分
│   ├── repository/               # 数据访问层
│   │   └── recommend_repository.py
│   ├── jobs/                     # 编排层
│   │   └── build_recommend_result_job.py
│   ├── utils/helpers.py          # 工具函数
│   └── recommend_score_job.py    # CLI 入口
│
├── travel-web/                   # 前端 (Vue 3)
│   └── src/
│       ├── layout/AppLayout.vue  # 侧边栏布局
│       ├── views/                # 7 个页面
│       ├── components/           # 4 个复用组件
│       └── api/                  # 5 个 API 模块
│
├── .env.example                  # 环境变量模板
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
| **HTTP 客户端** | Axios | 1.7+ | 统一请求拦截 + Vite 代理 |
| **路由** | Vue Router | 4.4+ | 嵌套路由 + 懒加载 |
| **后端框架** | Spring Boot | 4.0+ | REST API 服务 |
| **ORM** | MyBatis | 3.x | 注解式 SQL |
| **数据库** | MySQL | 8.0+ | 数据仓库分层存储 |
| **评分引擎** | Python | 3.10+ | Pandas + SQLAlchemy |
| **依赖管理** | Maven / npm / pip | — | Java / JS / Python |

---

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                      浏览器 (Vue 3)                         │
│  Dashboard │ 推荐页 │ 结果页 │ 目的地 │ ETL监控 │ 数据质量   │
└──────────────────────────┬──────────────────────────────────┘
                           │ Axios (/api/*)
                           ▼
┌──────────────────────────────────────────────────────────────┐
│                 Spring Boot (port 8080)                      │
│                                                              │
│  DashboardController    → DashboardService → DashboardMapper │
│  RecommendController    → RecommendService  → 调 Python 脚本 │
│  DestinationController  → DestinationService                 │
│  EtlJobController       → JobLogService                      │
│  DataQualityController  → DataQualityService                 │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌──────────────────────────────────────────────────────────────┐
│                      MySQL 8.0                               │
│                                                              │
│  DWD 层 (明细):                                              │
│    dwd_destination        目的地维表                          │
│    dwd_route_detail       交通路线                            │
│    dwd_weather_detail     天气明细                            │
│    dwd_hotel_price_detail 酒店价格                            │
│                                                              │
│  ADS 层 (应用):                                              │
│    ads_destination_recommend_result  推荐结果                 │
│                                                              │
│  运维表:                                                     │
│    etl_job_log                    ETL 任务日志               │
│    data_quality_check_result      数据质量检查结果            │
└──────────────────────────────────────────────────────────────┘
                           ▲
                           │ SQLAlchemy
┌──────────────────────────┴───────────────────────────────────┐
│                 Python 评分引擎                               │
│                                                              │
│  recommend_score_job.py (CLI 入口)                           │
│    → jobs/build_recommend_result_job.py (编排)               │
│      → repository/recommend_repository.py (查询 4 张 DWD 表) │
│      → model/budget_model.py     (费用估算 + 预算评分)        │
│      → model/crowd_index_model.py (拥挤度评分)               │
│      → model/score_model.py      (加权总分 + 等级 + 理由)    │
│    → repository/recommend_repository.py (写入 ADS 表)        │
└──────────────────────────────────────────────────────────────┘
```

**调用链路：**

```
用户点击"生成推荐"
  → 前端 POST /api/recommend/calculate
    → RecommendController 接收参数
      → RecommendService 调用 Python 脚本 (ProcessBuilder)
        → recommend_score_job.py 执行
          → 查询 4 张 DWD 表
          → 6 维度加权评分
          → 结果写入 ads_destination_recommend_result
      → 返回 "推荐计算完成"
  → 前端跳转结果页 GET /api/recommend/results
    → RecommendController 查询 ADS 表返回结果
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
| `dwd_destination` | 目的地维表 | 编码、名称、省市、类型、3 项评分 | 6+ |
| `dwd_route_detail` | 交通路线 | 出发城市、目的地、交通类型、费用、时长、便利度 | 按城市×日期 |
| `dwd_weather_detail` | 天气预报 | 目的地、日期、高低温、天气类型、天气评分 | 按目的地×天 |
| `dwd_hotel_price_detail` | 酒店价格 | 目的地、入住/退房日期、均价、酒店评分 | 按目的地×日期区间 |

### ADS 层（Application Data Service）— 应用结果数据

| 表名 | 写入方 | 关键字段 |
|------|--------|----------|
| `ads_destination_recommend_result` | Python 评分引擎 | 6 维度得分、费用明细、总分、排名、等级、理由 |

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
| `scenery_score` | DECIMAL(5,2) | 景观评分 (0~100) |
| `popularity_score` | DECIMAL(5,2) | 人气评分 (0~100) |
| `facility_score` | DECIMAL(5,2) | 配套评分 (0~100) |
| `description` | TEXT | 简介 |
| `is_active` | TINYINT | 是否启用 (1=启用) |

### `dwd_route_detail` — 交通路线明细

| 字段 | 类型 | 说明 |
|------|------|------|
| `origin_city` | VARCHAR(50) | 出发城市 |
| `destination_code` | VARCHAR(50) | 目的地编码 |
| `travel_date` | DATE | 出行日期 |
| `transport_type` | VARCHAR(20) | 交通类型: train / flight / bus |
| `traffic_cost` | DECIMAL(10,2) | 交通费用 |
| `duration_minutes` | INT | 行程时长（分钟） |
| `convenience_score` | DECIMAL(5,2) | 交通便利度评分 |

### `dwd_weather_detail` — 天气明细

| 字段 | 类型 | 说明 |
|------|------|------|
| `destination_code` | VARCHAR(50) | 目的地编码 |
| `weather_date` | DATE | 天气日期 |
| `temperature_high` / `temperature_low` | INT | 最高/最低温度 |
| `weather_type` | VARCHAR(20) | 天气类型: sunny / cloudy / rainy / snowy |
| `weather_score` | DECIMAL(5,2) | 天气评分 (0~100) |

### `dwd_hotel_price_detail` — 酒店价格明细

| 字段 | 类型 | 说明 |
|------|------|------|
| `destination_code` | VARCHAR(50) | 目的地编码 |
| `checkin_date` / `checkout_date` | DATE | 入住/退房日期 |
| `hotel_star` | TINYINT | 星级 |
| `avg_price` | DECIMAL(10,2) | 均价 |
| `hotel_score` | DECIMAL(5,2) | 酒店评分 |

### `ads_destination_recommend_result` — 推荐结果

| 字段 | 类型 | 说明 |
|------|------|------|
| `request_id` | VARCHAR(100) | 请求批次 ID (UUID) |
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

### 推荐模块 `/api/recommend`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| `POST` | `/api/recommend/calculate` | Body: `{ originCity, travelStartDate, travelEndDate, userBudget }` | 触发推荐计算（调用 Python 脚本） |
| `GET` | `/api/recommend/results` | `originCity`, `startDate`, `endDate` | 查询推荐结果 |

### 目的地模块 `/api/destinations`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| `GET` | `/api/destinations` | — | 获取所有活跃目的地（按景观评分降序） |
| `GET` | `/api/destinations/{code}` | `destinationCode` (path) | 获取单个目的地详情 |

### 数据看板 `/api/dashboard`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| `GET` | `/api/dashboard/stats` | — | 获取看板统计数据 |

**返回结构：**
```json
{
  "destinationCount": 6,
  "etlTotalCount": 10,
  "etlSuccessCount": 8,
  "etlFailedCount": 1,
  "etlRunningCount": 1,
  "qualityTotalCount": 4,
  "qualityPassCount": 3,
  "qualityWarningCount": 1,
  "qualityFailedCount": 0,
  "recommendTotalCount": 30,
  "avgFinalScore": 78.50,
  "avgCrowdIndex": 65.20,
  "topDestinations": [...],
  "latestQualityResults": [...]
}
```

### ETL 监控 `/api/etl/jobs`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| `GET` | `/api/etl/jobs` | — | 获取最近 50 条 ETL 任务日志 |

### 数据质量 `/api/quality`

| 方法 | 路径 | 参数 | 说明 |
|------|------|------|------|
| `POST` | `/api/quality/run` | — | 执行全部数据质量检查 |
| `GET` | `/api/quality/results` | — | 获取最近 100 条检查结果 |

---

## 本地启动方式

### 环境要求

| 依赖 | 最低版本 | 安装检查 |
|------|---------|---------|
| Java | 17+ | `java -version` |
| Node.js | 18+ | `node -v` |
| Python | 3.10+ | `python --version` |
| MySQL | 8.0+ | `mysql --version` |

### 1. 初始化数据库

```bash
mysql -u root -p < infra/mysql/init.sql
```

这会创建 `travel_decision_platform` 库并建表 + 插入 6 个目的地及其路线/天气/酒店种子数据。

### 2. 配置环境变量

```bash
cp .env.example .env
```

编辑 `.env` 填入数据库连接信息：

```properties
MYSQL_USER=root
MYSQL_PASSWORD=your_password
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_DATABASE=travel_decision_platform
```

### 3. 启动后端

```bash
cd travel-data-java
./mvnw spring-boot:run
```

后端启动在 `http://localhost:8080`。

### 4. 启动前端

```bash
cd travel-web
npm install
npm run dev
```

前端启动在 `http://localhost:5173`，`/api/*` 请求自动代理到后端。

### 5. 运行 Python 评分脚本（可选）

```bash
cd travel-analysis-python
pip install -r requirements.txt
python recommend_score_job.py 北京 2026-07-01 2026-07-05 5000
```

也可以通过前端「智能推荐」页面触发（后端会自动调用 Python 脚本）。

### 启动顺序

```
① MySQL → ② Spring Boot (8080) → ③ Vue Dev Server (5173)
```

---

## 页面截图

> 以下为 V2 版本页面，启动后访问 `http://localhost:5173` 查看。

| 页面 | 路由 | 说明 |
|------|------|------|
| 数据看板 | `/` | 6 项统计指标 + 目的地评分柱状图 + 质量分布饼图 |
| 智能推荐 | `/recommend` | 输入出发城市/日期/预算，触发推荐计算 |
| 推荐结果 | `/result` | 结果表格 + Top5 评分对比柱状图 |
| 目的地列表 | `/destinations` | 全部目的地表格，点击行进入详情 |
| 目的地详情 | `/destinations/:code` | 基本信息 + 雷达图 + 进度条评分 |
| ETL 监控 | `/etl-monitor` | 任务状态统计卡 + 筛选 + 自动刷新表格 |
| 数据质量 | `/data-quality` | 执行检查按钮 + 通过/警告/失败统计 + 结果表 |

---

## 第二版新增能力

### 对比 V1 → V2

| 能力 | V1 | V2 |
|------|----|----|
| **布局** | 顶部水平导航 | 侧边栏管理后台布局（可折叠） |
| **页面数** | 3 个 | 7 个 |
| **数据看板** | 无 | Dashboard — 6 项统计指标 + 2 个 ECharts 图表 |
| **ETL 监控** | 无 | 任务日志表格 + 状态筛选 + 30s 自动刷新 |
| **数据质量** | 无 | 一键执行检查 + PASS/WARNING/FAILED 统计 |
| **目的地详情** | 无 | 详情卡片 + 雷达图 + 进度条评分 |
| **后端统计接口** | 无 | `GET /api/dashboard/stats` 聚合查询 |
| **Python 模块化** | 单文件 276 行 | 6 模块分层（config/model/repository/jobs） |

### V2 新增文件清单

**前端新增 (9 文件)：**

```
travel-web/src/
├── layout/AppLayout.vue              # 侧边栏布局
├── views/Dashboard.vue               # 数据看板
├── views/EtlJobMonitor.vue           # ETL 监控
├── views/DataQuality.vue             # 数据质量
├── views/DestinationDetail.vue       # 目的地详情
├── components/ScoreRadarChart.vue    # ECharts 雷达图
├── api/dashboard.ts                  # 看板 API
├── api/job.ts                        # ETL API
└── api/quality.ts                    # 质量 API
```

**后端新增 (4 文件)：**

```
travel-data-java/.../platform/
├── controller/DashboardController.java
├── service/DashboardService.java
├── mapper/DashboardMapper.java
└── dto/DashboardStats.java
```

**Python 重构 (11 文件，原 1 文件)：**

```
travel-analysis-python/
├── config/db_config.py
├── model/score_model.py
├── model/budget_model.py
├── model/crowd_index_model.py
├── repository/recommend_repository.py
├── jobs/build_recommend_result_job.py
└── utils/helpers.py
```

---

## License

MIT
