# Travel Choice — 智能旅行推荐平台

## 一、项目概述

这是一个**前后端分离 + Python 数据分析**的旅行推荐系统。用户输入出发城市、旅行日期和预算后，系统通过多维度评分算法（景色、交通、住宿、天气、性价比、拥挤度）自动计算并排名最佳目的地。

**核心链路：**

```
浏览器 (Vue 3)
  │
  │  POST /api/recommend/calculate
  ▼
Spring Boot (Java 21)
  │
  │  ProcessBuilder 调用 Python 脚本
  ▼
Python 推荐引擎
  │
  │  读取 dwd_* 表 → 计算评分 → 写入 ads_* 表
  ▼
MySQL 8
  │
  │  Java 查询 ads 表返回结果
  ▼
浏览器展示表格 + ECharts 图表
```

---

## 二、技术栈

| 层       | 技术                                     | 说明                         |
| -------- | ---------------------------------------- | ---------------------------- |
| 前端     | Vue 3 + Element Plus + ECharts + Axios   | SPA，Composition API          |
| 后端     | Spring Boot 4 + MyBatis + Lombok         | REST API，端口 8080           |
| 数据分析 | Python 3 + pandas + SQLAlchemy + pymysql | 推荐评分计算脚本              |
| 数据库   | MySQL 8                                  | 5 张表，数仓 dwd/ads 分层命名 |
| 构建     | Vite 5 (前端) + Maven (后端)             |                              |

---

## 三、目录结构

```
travel_choice/
│
├── infra/mysql/
│   └── init.sql                  # 建库建表 + 模拟数据（⭐ 第一个看）
│
├── travel-data-java/             # Spring Boot 后端
│   ├── pom.xml
│   ├── src/main/resources/
│   │   └── application.yml       # 数据库连接、MyBatis 配置
│   └── src/main/java/.../
│       ├── entity/               # 数据库实体类
│       ├── mapper/               # MyBatis SQL 映射
│       ├── service/              # 业务逻辑（调 Python 在这里）
│       ├── controller/           # REST 接口
│       └── dto/                  # 请求参数封装
│
├── travel-analysis-python/       # Python 推荐引擎
│   ├── recommend_score_job.py    # 核心评分算法（⭐ 重点看）
│   └── requirements.txt
│
└── travel-web/                   # Vue 3 前端
    ├── vite.config.js            # 代理配置 /api → localhost:8080
    └── src/
        ├── api/                  # Axios 封装 + API 函数
        ├── components/           # 可复用组件
        ├── views/                # 页面
        ├── router/               # 路由
        ├── App.vue               # 根组件 + 导航栏
        └── main.ts               # 入口
```

---

## 四、数据库设计

数据库名：`travel_decision_platform`，采用**数仓分层命名**（dwd = 明细层，ads = 应用层）：

```
┌─────────────────────────┐     ┌─────────────────────────┐
│  dwd_destination        │     │  dwd_route_detail       │
│  目的地维表              │     │  交通路线明细            │
│  ─────────────────────  │     │  ─────────────────────  │
│  destination_code (PK)  │◄────│  destination_code (FK)  │
│  destination_name       │     │  origin_city            │
│  city / province        │     │  travel_date            │
│  destination_type       │     │  traffic_cost           │
│  scenery_score          │     │  duration_minutes       │
│  popularity_score       │     │  convenience_score      │
│  facility_score         │     └─────────────────────────┘
│  is_active              │
└──────────┬──────────────┘     ┌─────────────────────────┐
           │                    │  dwd_weather_detail      │
           │                    │  天气明细                 │
           │◄───────────────────│  destination_code (FK)  │
           │                    │  weather_date            │
           │                    │  weather_score           │
           │                    └─────────────────────────┘
           │
           │                    ┌─────────────────────────┐
           │                    │  dwd_hotel_price_detail  │
           │◄───────────────────│  酒店价格明细            │
                                │  destination_code (FK)  │
                                │  avg_price              │
                                │  hotel_score            │
                                └───────────┬─────────────┘
                                            │
                                            ▼
                    ┌───────────────────────────────────────────┐
                    │  ads_destination_recommend_result         │
                    │  推荐结果表（Python 写入，Java 读取）      │
                    │  ─────────────────────────────────────    │
                    │  origin_city + dates (查询条件)           │
                    │  destination_name                         │
                    │  final_score / recommend_rank             │
                    │  estimated_total_cost                     │
                    │  traffic/hotel/food/ticket_cost           │
                    │  scenery/traffic/hotel/weather/cost_score │
                    │  crowd_index / crowd_score                │
                    │  recommend_level (HIGH/MEDIUM/LOW)        │
                    │  recommend_reason (中文推荐理由)           │
                    └───────────────────────────────────────────┘
```

---

## 五、推荐阅读顺序

按 **从底到顶** 的顺序，先理解数据和算法，再看后端接口，最后看前端展示。

### 第 1 步：数据库 — 理解数据长什么样

**文件：`infra/mysql/init.sql`**

- 5 张表的字段含义
- 模拟数据的取值逻辑（6 个目的地、6 条交通路线、30 条天气记录、6 条酒店价格）
- 重点关注 `dwd_destination` 的三个评分字段：`scenery_score`、`popularity_score`、`facility_score`

### 第 2 步：Python 推荐引擎 — 理解核心算法

**文件：`travel-analysis-python/recommend_score_job.py`**

这是整个项目最有价值的文件，阅读顺序：

1. **`main()` 函数** (L255-270) — 接收 4 个命令行参数
2. **`build_recommend_result()` 函数** (L26-253) — 核心逻辑：
   - L33-47: 查询 `dwd_destination` 获取目的地基础数据
   - L49-65: 查询 `dwd_route_detail` 获取交通费用
   - L67-81: 查询 `dwd_weather_detail` 获取天气评分
   - L83-98: 查询 `dwd_hotel_price_detail` 获取酒店价格
   - L100-102: pandas merge 关联 4 张表
   - L106-215: **逐目的地计算**：
     - 费用估算 = 往返交通 + 酒店×天数 + 餐饮(180/天) + 门票(120/天)
     - 6 维评分权重：景色25% + 交通20% + 住宿15% + 天气15% + 性价比15% + 人流10%
     - 推荐等级：>=85 HIGH、>=75 MEDIUM、<75 LOW
     - 自动生成中文推荐理由
   - L217-242: 写入 `ads_destination_recommend_result` 表

### 第 3 步：Java Entity + Mapper — 理解数据模型

**按顺序看：**

| 顺序 | 文件 | 作用 |
|------|------|------|
| 3.1 | `entity/Destination.java` | 目的地实体，对应 `dwd_destination` 表 |
| 3.2 | `entity/RecommendResult.java` | 推荐结果实体，对应 `ads_*` 表 |
| 3.3 | `mapper/DestinationMapper.java` | `@Select` 注解直接写 SQL，查 `dwd_destination` |
| 3.4 | `mapper/RecommendResultMapper.java` | 按 originCity+日期 查推荐结果 |

**重点理解：** MyBatis 的 `map-underscore-to-camel-case: true` 配置让 `destination_name` 自动映射到 `destinationName`。

### 第 4 步：Java Service — 理解业务编排

**按顺序看：**

| 顺序 | 文件 | 作用 |
|------|------|------|
| 4.1 | `service/DestinationService.java` | 简单封装，直接返回目的地列表 |
| 4.2 | `service/RecommendService.java` | **核心** — `calculateRecommend()` 用 ProcessBuilder 调用 Python |

**`RecommendService` 关键代码：**
```java
ProcessBuilder processBuilder = new ProcessBuilder(
    "python",
    "../travel-analysis-python/recommend_score_job.py",
    originCity, startDate, endDate, budget  // 4 个参数传给 Python
);
```

### 第 5 步：Java Controller + DTO — 理解接口

**按顺序看：**

| 顺序 | 文件 | 作用 |
|------|------|------|
| 5.1 | `dto/RecommendCalculateRequest.java` | 请求体：originCity / dates / budget |
| 5.2 | `controller/DestinationController.java` | `GET /api/destinations` |
| 5.3 | `controller/RecommendController.java` | `POST /api/recommend/calculate` + `GET /api/recommend/results` |
| 5.4 | `resources/application.yml` | 数据库连接配置 |

### 第 6 步：前端 API 层 — 理解请求封装

**按顺序看：**

| 顺序 | 文件 | 作用 |
|------|------|------|
| 6.1 | `api/request.ts` | Axios 实例 + JWT 拦截器 |
| 6.2 | `api/destination.ts` | `getDestinations()` → `GET /api/destinations` |
| 6.3 | `api/recommend.ts` | `calculateRecommend()` + `getRecommendResults()` |

### 第 7 步：前端组件 — 理解 UI 拆分

**按顺序看：**

| 顺序 | 文件 | 作用 |
|------|------|------|
| 7.1 | `components/SearchForm.vue` | 表单：城市 / 日期 / 预算 → emit('search') |
| 7.2 | `components/RecommendTable.vue` | 表格：排名 / 评分 / 费用 / 推荐理由 |
| 7.3 | `components/ScoreBarChart.vue` | ECharts 柱状图：6 维评分对比 |

### 第 8 步：前端页面 — 理解页面组合

**按顺序看：**

| 顺序 | 文件 | 作用 |
|------|------|------|
| 8.1 | `views/RecommendPage.vue` | 推荐首页：SearchForm → 调 API → 跳转结果页 |
| 8.2 | `views/RecommendResult.vue` | 结果页：从 URL query 取参数 → 调 results API → 表格+图表 |
| 8.3 | `views/DestinationList.vue` | 目的地列表：直接调 destinations API |

### 第 9 步：前端配置 — 理解项目骨架

**按顺序看：**

| 顺序 | 文件 | 作用 |
|------|------|------|
| 9.1 | `router/index.ts` | 3 条路由定义 |
| 9.2 | `App.vue` | 导航栏 + `<router-view />` |
| 9.3 | `main.ts` | 入口：注册 ElementPlus + Router |
| 9.4 | `vite.config.js` | 代理 `/api` → `localhost:8080` |

---

## 六、接口清单

| 方法   | 路径                       | 说明           | 调用链                          |
| ------ | -------------------------- | -------------- | ------------------------------- |
| GET    | `/api/destinations`        | 目的地列表     | Controller → Service → Mapper   |
| POST   | `/api/recommend/calculate` | 触发推荐计算   | Controller → Service → Python   |
| GET    | `/api/recommend/results`   | 查询推荐结果   | Controller → Service → Mapper   |

### POST /api/recommend/calculate 请求体

```json
{
  "originCity": "北京",
  "travelStartDate": "2026-07-01",
  "travelEndDate": "2026-07-05",
  "userBudget": 5000
}
```

### GET /api/recommend/results 查询参数

```
?originCity=北京&startDate=2026-07-01&endDate=2026-07-05
```

---

## 七、评分算法详解

Python 脚本对每个目的地计算 6 个维度评分，加权得到 `final_score`：

```
final_score = scenery   × 0.25   (景观分：dwd_destination.scenery_score)
            + traffic   × 0.20   (交通分：convenience_score - 耗时惩罚)
            + hotel     × 0.15   (住宿分：dwd_hotel_price_detail.hotel_score)
            + weather   × 0.15   (天气分：dwd_weather_detail 均值)
            + cost      × 0.15   (性价比：预算内越高越好，超预算扣分)
            + crowd     × 0.10   (人流分：100 - 拥挤指数，越不挤越高)
```

**费用估算公式：**

```
总费用 = 往返交通费 + 酒店均价 × 天数 + 180(餐饮/天) + 120(门票/天)
```

**拥挤指数：**

```
crowd_index = popularity_score × 0.65 + (avg_hotel_price / 10) × 0.35
```

---

## 八、启动方式

```bash
# 1. MySQL — 确保本地 MySQL 运行 (root/1234)
mysql -u root -p1234 < infra/mysql/init.sql

# 2. Java 后端
cd travel-data-java
mvn spring-boot:run        # → localhost:8080

# 3. Python 依赖 (首次)
cd travel-analysis-python
pip install -r requirements.txt

# 4. Vue 前端
cd travel-web
npm install
npm run dev                # → localhost:5173
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

**预期排名（budget=5000）：**

| 排名 | 目的地       | 综合评分 | 预估费用 | 等级   |
| ---- | ------------ | -------- | -------- | ------ |
| 1    | 黄山         | 79.43    | ¥3,300   | MEDIUM |
| 2    | 西湖         | 79.40    | ¥4,380   | MEDIUM |
| 3    | 张家界       | 78.97    | ¥4,720   | MEDIUM |
| 4    | 漓江         | 77.72    | ¥4,200   | MEDIUM |
| 5    | 布达拉宫     | 71.96    | ¥5,600   | LOW    |
| 6    | 亚龙湾       | 70.29    | ¥6,520   | LOW    |

**试试改预算为 8000：** 布达拉宫和亚龙湾的 cost_score 会提高，排名会变化。
