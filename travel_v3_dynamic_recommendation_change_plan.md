

# 第三版增强：动态出发地与候选目的地推荐改造方案

> 适用项目：`travel-decision-platform`  
> 当前版本基础：第二版已完成推荐计算、任务日志、数据质量、Dashboard；第三版已开始接入高德天气和路线 API。  
> 本文目标：把当前“固定出发地 + 固定候选目的地”的演示逻辑，改造成“任意出发城市 + 可选目的地集合 + 动态采集路线/天气/酒店 + 实时排序”的旅游决策系统。

---

## 1. 当前问题

当前项目大致是：

```text
固定出发地，例如北京
    ↓
提前写入或采集几个固定目的地的路线、天气、酒店数据
    ↓
Python 读取数据库
    ↓
生成推荐结果
```

这个版本可以演示，但业务上不完整：

```text
1. 只能围绕一个固定出发点测试。
2. 用户不能从全国任意城市出发。
3. 用户不能选择自己想去的几个目的地让系统排序。
4. 路线数据不是围绕“本次请求”动态生成。
5. 推荐结果仍然主要依赖数据库中已有的数据，交互性不足。
```

---

## 2. 改造目标

改造后支持两种模式。

### 模式一：系统推荐目的地

用户只输入：

```json
{
  "originCity": "上海",
  "travelStartDate": "2026-10-01",
  "travelEndDate": "2026-10-05",
  "userBudget": 6000
}
```

系统自动从所有启用目的地中推荐最适合的目的地。

### 模式二：用户指定目的地排序

用户输入：

```json
{
  "originCity": "上海",
  "travelStartDate": "2026-10-01",
  "travelEndDate": "2026-10-05",
  "userBudget": 6000,
  "destinationCodes": ["DEST001", "DEST002", "DEST005"]
}
```

系统只对用户选择的目的地进行排序。

---

## 3. 新的数据流

```text
用户提交推荐请求
    ↓
Java 根据 originCity 查询出发城市坐标
    ↓
Java 根据 destinationCodes 获取候选目的地
    ↓
Java 调高德路线 API，动态生成 origin → destination 路线数据
    ↓
Java 调高德天气 API，补齐目的地天气数据
    ↓
Java 生成酒店价格数据
    ↓
Java 调 Python 推荐评分脚本
    ↓
Python 读取 MySQL 中本次请求相关数据
    ↓
Python 写入 ads_destination_recommend_result
    ↓
Java 返回 requestId
    ↓
前端根据 requestId 查询并展示推荐结果
```

---

## 4. 总体改动清单

| 模块 | 要改动的地方 | 目的 |
|---|---|---|
| MySQL | 新增 `dwd_city_location` 表 | 存全国出发城市坐标、adcode、citycode |
| MySQL | 推荐结果按 `request_id` 查询 | 避免不同请求互相覆盖 |
| Java DTO | `RecommendCalculateRequest` 增加 `destinationCodes` | 支持用户选择候选目的地 |
| Java Entity | 新增 `CityLocation.java` | 映射城市坐标表 |
| Java Mapper | 新增 `CityLocationMapper.java` | 根据城市名查坐标 |
| Java Service | 改造 `RecommendService` | 编排路线采集、天气采集、酒店生成、Python 评分 |
| Java Service | 改造 `RouteCollectService` | 支持本次请求的候选目的地集合 |
| Java Mapper | 改造 `RecommendResultMapper` | 支持按 `requestId` 查询 |
| Python | 脚本增加 `requestId`、`destinationCodes` 参数 | 保证推荐结果绑定本次请求 |
| Python | Repository 支持目的地过滤 | 只计算用户选择的目的地 |
| Vue | 推荐页增加出发城市下拉和目的地多选 | 支持两种推荐模式 |
| Vue | 结果页按 `requestId` 查询 | 避免按城市日期误查旧结果 |

---

# 5. 数据库改造

## 5.1 新增城市坐标表

建议新建文件：

```text
infra/mysql/upgrade_v3_dynamic_recommend.sql
```

写入：

```sql
USE travel_decision_platform;

CREATE TABLE IF NOT EXISTS dwd_city_location (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

    city_code VARCHAR(50) NOT NULL COMMENT '城市编码',
    city_name VARCHAR(100) NOT NULL COMMENT '城市名称',
    province VARCHAR(100) DEFAULT NULL COMMENT '省份',

    amap_adcode VARCHAR(20) DEFAULT NULL COMMENT '高德行政区adcode',
    amap_citycode VARCHAR(20) DEFAULT NULL COMMENT '高德citycode',

    latitude DECIMAL(10, 6) NOT NULL COMMENT '纬度',
    longitude DECIMAL(10, 6) NOT NULL COMMENT '经度',

    is_active TINYINT DEFAULT 1 COMMENT '是否启用',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_city_code (city_code),
    KEY idx_city_name (city_name),
    KEY idx_amap_adcode (amap_adcode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DWD城市坐标表';
```

## 5.2 插入常用出发城市

```sql
INSERT INTO dwd_city_location
(city_code, city_name, province, amap_adcode, amap_citycode, latitude, longitude)
VALUES
('BEIJING', '北京', '北京市', '110000', '010', 39.904200, 116.407400),
('SHANGHAI', '上海', '上海市', '310000', '021', 31.230400, 121.473700),
('GUANGZHOU', '广州', '广东省', '440100', '020', 23.129100, 113.264400),
('SHENZHEN', '深圳', '广东省', '440300', '0755', 22.543100, 114.057900),
('HANGZHOU', '杭州', '浙江省', '330100', '0571', 30.274100, 120.155100),
('NANJING', '南京', '江苏省', '320100', '025', 32.060300, 118.796900),
('CHENGDU', '成都', '四川省', '510100', '028', 30.572800, 104.066800),
('CHONGQING', '重庆', '重庆市', '500000', '023', 29.563000, 106.551600),
('XIAN', '西安', '陕西省', '610100', '029', 34.341600, 108.939800),
('WUHAN', '武汉', '湖北省', '420100', '027', 30.592800, 114.305500),
('CHANGSHA', '长沙', '湖南省', '430100', '0731', 28.228200, 112.938800),
('XIAMEN', '厦门', '福建省', '350200', '0592', 24.479800, 118.089400),
('QINGDAO', '青岛', '山东省', '370200', '0532', 36.067100, 120.382600),
('KUNMING', '昆明', '云南省', '530100', '0871', 25.043800, 102.710000)
ON DUPLICATE KEY UPDATE
    city_name = VALUES(city_name),
    province = VALUES(province),
    amap_adcode = VALUES(amap_adcode),
    amap_citycode = VALUES(amap_citycode),
    latitude = VALUES(latitude),
    longitude = VALUES(longitude),
    updated_at = CURRENT_TIMESTAMP;
```

## 5.3 推荐结果表确认 `request_id`

你的表里如果已有 `request_id`，不用重复加。确认：

```sql
DESC ads_destination_recommend_result;
```

如果没有，执行：

```sql
ALTER TABLE ads_destination_recommend_result
ADD COLUMN request_id VARCHAR(100) DEFAULT NULL COMMENT '推荐请求ID' AFTER id;

CREATE INDEX idx_recommend_request_id
ON ads_destination_recommend_result(request_id);
```

---

# 6. Java 后端改造

## 6.1 新增 `CityLocation.java`

路径：

```text
travel-data-java/src/main/java/com/travel/platform/entity/CityLocation.java
```

```java
package com.travel.platform.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CityLocation {
    private Long id;

    private String cityCode;
    private String cityName;
    private String province;

    private String amapAdcode;
    private String amapCitycode;

    private BigDecimal latitude;
    private BigDecimal longitude;

    private Integer isActive;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

## 6.2 新增 `CityLocationMapper.java`

路径：

```text
travel-data-java/src/main/java/com/travel/platform/mapper/CityLocationMapper.java
```

```java
package com.travel.platform.mapper;

import com.travel.platform.entity.CityLocation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CityLocationMapper {

    @Select("""
            SELECT
                id,
                city_code,
                city_name,
                province,
                amap_adcode,
                amap_citycode,
                latitude,
                longitude,
                is_active,
                created_at,
                updated_at
            FROM dwd_city_location
            WHERE city_name = #{cityName}
              AND is_active = 1
            LIMIT 1
            """)
    CityLocation selectByCityName(String cityName);

    @Select("""
            SELECT
                id,
                city_code,
                city_name,
                province,
                amap_adcode,
                amap_citycode,
                latitude,
                longitude,
                is_active,
                created_at,
                updated_at
            FROM dwd_city_location
            WHERE is_active = 1
            ORDER BY city_name
            """)
    List<CityLocation> selectAllActive();
}
```

## 6.3 修改 `RecommendCalculateRequest.java`

当前路径：

```text
travel-data-java/src/main/java/com/travel/platform/dto/RecommendCalculateRequest.java
```

改成：

```java
package com.travel.platform.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class RecommendCalculateRequest {

    private String originCity;

    private LocalDate travelStartDate;

    private LocalDate travelEndDate;

    private BigDecimal userBudget;

    /**
     * 候选目的地编码。
     * 为空：系统对所有启用目的地排序。
     * 不为空：只对用户选择的目的地排序。
     */
    private List<String> destinationCodes;
}
```

## 6.4 新增返回 DTO：`RecommendCalculateResponse.java`

路径：

```text
travel-data-java/src/main/java/com/travel/platform/dto/RecommendCalculateResponse.java
```

```java
package com.travel.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecommendCalculateResponse {
    private String requestId;
    private String message;
}
```

## 6.5 修正包名拼写

你当前项目里有：

```text
com.travel.platform.cilent.amap
```

建议改成：

```text
com.travel.platform.client.amap
```

需要改动：

```text
travel-data-java/src/main/java/com/travel/platform/cilent/amap/
```

改为：

```text
travel-data-java/src/main/java/com/travel/platform/client/amap/
```

并修改 Java 文件中的包名：

```java
package com.travel.platform.client.amap;
```

所有引用同步改成：

```java
import com.travel.platform.client.amap.AmapRouteClient;
import com.travel.platform.client.amap.AmapWeatherClient;
```

## 6.6 改造 `RouteCollectService`

目标：让路线采集支持“本次请求的候选目的地集合”，而不是永远采集所有目的地。

建议新增一个方法：

```java
public int collectForDestinations(
        String originCity,
        BigDecimal originLng,
        BigDecimal originLat,
        LocalDate travelDate,
        List<Destination> destinations
) {
    // 逻辑基本复用 collectAll
    // 但目的地列表使用传入的 destinations
    // 写入 dwd_route_detail 前，删除本次 originCity + travelDate 下这些 destinationCodes 的旧路线
}
```

建议实现要点：

```text
1. 方法接收 destinations，不再内部固定 selectAllActive。
2. 对每个目的地调用高德路线 API。
3. 写入 dwd_route_detail。
4. data_source = amap_route。
5. 任务日志仍然记录 collect_route_amap。
```

当前 `AmapRouteClient.getTransitRoute()` 建议改成支持目的城市：

```java
public Map<String, Object> getTransitRoute(
        BigDecimal originLng,
        BigDecimal originLat,
        BigDecimal destLng,
        BigDecimal destLat,
        String originCity,
        String destCity
) {
    // city = originCity
    // cityd = destCity
}
```

不要把 `cityd` 也传成 `originCity`。

---

# 7. 推荐编排逻辑改造

## 7.1 当前问题

当前 `RecommendService` 主要是：

```text
Java 调 Python 脚本
```

增强后应该变成：

```text
Java 负责任务编排：
1. 生成 requestId
2. 查出发城市坐标
3. 查候选目的地
4. 采集路线
5. 采集天气
6. 生成酒店价格
7. 调 Python 评分
8. 返回 requestId
```

## 7.2 `RecommendService` 建议流程

```java
public RecommendCalculateResponse calculateRecommend(RecommendCalculateRequest request) {
    String requestId = buildRequestId(request);

    EtlJobLog jobLog = jobLogService.startJob(
            "build_destination_recommend",
            "RECOMMEND",
            "动态目的地推荐评分计算",
            request.getTravelStartDate()
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
        routeCollectService.collectForDestinations(
                request.getOriginCity(),
                origin.getLongitude(),
                origin.getLatitude(),
                request.getTravelStartDate(),
                destinations
        );

        // 4. 采集天气，可以采所有，也可以只采本次目的地
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
        int rowCount = recommendResultMapper.countByRequestId(requestId);
        jobLogService.finishSuccess(jobLog.getId(), rowCount);

        return new RecommendCalculateResponse(requestId, "推荐计算完成");

    } catch (Exception e) {
        jobLogService.finishFailed(jobLog.getId(), e.getMessage());
        throw new RuntimeException("执行推荐计算失败：" + e.getMessage(), e);
    }
}
```

你不一定要一次性完全照抄，但推荐最终朝这个结构改。

## 7.3 Python 调用参数改造

当前类似：

```java
python recommend_score_job.py originCity startDate endDate userBudget
```

改成：

```java
python recommend_score_job.py originCity startDate endDate userBudget requestId destinationCodes
```

示例：

```text
python recommend_score_job.py 上海 2026-10-01 2026-10-05 6000 REQ_20261001_SHANGHAI_xxxx DEST001,DEST002,DEST005
```

如果用户没有选择目的地，最后一个参数传空字符串：

```text
""
```

---

# 8. 推荐结果查询接口改造

## 8.1 `RecommendResultMapper` 新增按 requestId 查询

路径：

```text
travel-data-java/src/main/java/com/travel/platform/mapper/RecommendResultMapper.java
```

新增：

```java
@Select("""
        SELECT
            id,
            request_id,
            origin_city,
            destination_code,
            destination_name,
            travel_start_date,
            travel_end_date,
            travel_days,
            user_budget,
            estimated_total_cost,
            traffic_cost,
            hotel_cost,
            food_cost,
            ticket_cost,
            scenery_score,
            traffic_score,
            hotel_score,
            weather_score,
            cost_score,
            crowd_index,
            crowd_score,
            final_score,
            recommend_rank,
            recommend_level,
            recommend_reason,
            model_version,
            created_at,
            updated_at
        FROM ads_destination_recommend_result
        WHERE request_id = #{requestId}
        ORDER BY recommend_rank ASC
        """)
List<RecommendResult> selectByRequestId(String requestId);

@Select("""
        SELECT COUNT(*)
        FROM ads_destination_recommend_result
        WHERE request_id = #{requestId}
        """)
Integer countByRequestId(String requestId);
```

## 8.2 `RecommendController` 修改

`POST /api/recommend/calculate` 改为返回 `requestId`：

```java
@PostMapping("/calculate")
public RecommendCalculateResponse calculateRecommend(@RequestBody RecommendCalculateRequest request) {
    return recommendService.calculateRecommend(request);
}
```

新增：

```java
@GetMapping("/results/by-request")
public List<RecommendResult> listByRequestId(@RequestParam String requestId) {
    return recommendService.listByRequestId(requestId);
}
```

---

# 9. Python 脚本改造

## 9.1 `recommend_score_job.py` 参数改造

当前入口大概率是 4 个参数。改成：

```python
if len(sys.argv) < 6:
    print("用法: python recommend_score_job.py originCity startDate endDate userBudget requestId [destinationCodes]")
    sys.exit(1)

origin_city = sys.argv[1]
start_date = sys.argv[2]
end_date = sys.argv[3]
user_budget = Decimal(sys.argv[4])
request_id = sys.argv[5]

destination_codes = []
if len(sys.argv) >= 7 and sys.argv[6].strip():
    destination_codes = [x.strip() for x in sys.argv[6].split(",") if x.strip()]
```

然后调用：

```python
results = build_recommend_result(
    origin_city,
    start_date,
    end_date,
    user_budget,
    request_id=request_id,
    destination_codes=destination_codes
)
```

## 9.2 `build_recommend_result_job.py` 改造

函数签名改成：

```python
def build_recommend_result(origin_city, start_date, end_date, user_budget, request_id, destination_codes=None):
```

逻辑：

```text
1. 如果 destination_codes 为空，按 origin_city + start_date 查询所有可达路线。
2. 如果 destination_codes 不为空，只查询这些目的地的路线、天气、酒店。
3. 写入结果时统一使用传入的 request_id。
```

关键点：不要在 Python 里再生成随机 `request_id`，否则 Java 拿不到本次请求结果。

原来类似：

```python
request_id = str(uuid.uuid4())
```

改成使用参数：

```python
# 使用 Java 传入的 request_id
```

## 9.3 `recommend_repository.py` 改造

`query_routes` 支持 `destination_codes`：

```python
def query_routes(origin_city, start_date, destination_codes=None):
    engine = get_engine()

    base_sql = """
        SELECT r.destination_code,
               r.transport_type,
               r.traffic_cost,
               r.duration_minutes,
               r.convenience_score
        FROM dwd_route_detail r
        WHERE r.origin_city = :origin_city
          AND r.travel_date = :start_date
    """

    params = {"origin_city": origin_city, "start_date": start_date}

    if destination_codes:
        placeholders = []
        for i, code in enumerate(destination_codes):
            key = f"code_{i}"
            placeholders.append(f":{key}")
            params[key] = code
        base_sql += f" AND r.destination_code IN ({', '.join(placeholders)})"

    return pd.read_sql(text(base_sql), engine, params=params)
```

`save_results` 改成按 `request_id` 删除旧数据：

```python
def save_results(results, request_id):
    if not results:
        print("[INFO] 无推荐结果可写入")
        return

    engine = get_engine()
    result_df = pd.DataFrame(results)

    with engine.begin() as conn:
        conn.execute(
            text("DELETE FROM ads_destination_recommend_result WHERE request_id = :rid"),
            {"rid": request_id}
        )

        result_df.to_sql(
            "ads_destination_recommend_result",
            con=conn,
            if_exists="append",
            index=False,
            method="multi",
        )
```

---

# 10. 前端改造

## 10.1 推荐页表单新增字段

当前表单：

```text
originCity
travelStartDate
travelEndDate
userBudget
```

改成：

```text
originCity：下拉选择
travelDateRange：日期范围
userBudget：预算输入
destinationCodes：目的地多选，可为空
```

逻辑：

```text
不选择 destinationCodes：系统对所有启用目的地推荐。
选择 destinationCodes：系统只对选中的目的地排序。
```

## 10.2 推荐接口返回 requestId

前端调用：

```ts
const res = await calculateRecommend(formData)
const requestId = res.data.requestId
```

然后跳转：

```ts
router.push({
  name: 'RecommendResult',
  query: { requestId }
})
```

## 10.3 结果页按 requestId 查

新增 API：

```ts
export function getRecommendResultsByRequestId(requestId: string) {
  return request.get('/api/recommend/results/by-request', {
    params: { requestId }
  })
}
```

结果页读取：

```ts
const requestId = route.query.requestId as string
const res = await getRecommendResultsByRequestId(requestId)
```

---

# 11. 建议新增接口

## 11.1 城市列表接口

前端出发城市下拉需要接口。

新增：

```text
GET /api/cities
```

返回：

```json
[
  {
    "cityCode": "SHANGHAI",
    "cityName": "上海",
    "province": "上海市"
  }
]
```

可以新建：

```text
CityController.java
CityLocationService.java
```

## 11.2 目的地列表接口继续复用

已有：

```text
GET /api/destinations
```

前端用它做目的地多选。

---

# 12. 推荐的开发顺序

按这个顺序改，风险最低：

```text
1. 新增 dwd_city_location 表和城市数据。
2. 新增 CityLocation.java。
3. 新增 CityLocationMapper.java。
4. 新增 GET /api/cities，测试城市下拉数据。
5. 修改 RecommendCalculateRequest，增加 destinationCodes。
6. 修改 Python 脚本，支持 requestId 和 destinationCodes。
7. 修改 Python repository，支持按 destinationCodes 过滤。
8. 修改 RecommendResultMapper，支持 requestId 查询。
9. 修改 RecommendController，calculate 返回 requestId。
10. 修改 RecommendService，先生成 requestId 并传给 Python。
11. 改造 RouteCollectService，支持本次请求的候选目的地。
12. 推荐计算前自动采集路线、天气、酒店数据。
13. 前端 SearchForm 增加目的地多选。
14. 前端 RecommendResult 改成按 requestId 查询。
15. 测试“上海出发，不选目的地”。
16. 测试“上海出发，只选张家界、杭州、三亚”。
17. 更新 README。
```

---

# 13. 接口测试示例

## 13.1 系统自动推荐全部目的地

```powershell
$body = @{
    originCity = "上海"
    travelStartDate = "2026-10-01"
    travelEndDate = "2026-10-05"
    userBudget = 6000
    destinationCodes = @()
} | ConvertTo-Json -Compress

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/recommend/calculate" `
  -Method POST `
  -ContentType "application/json" `
  -Body $body
```

期望返回：

```json
{
  "requestId": "REQ_20261001_SHANGHAI_xxxx",
  "message": "推荐计算完成"
}
```

## 13.2 对用户选择的目的地排序

```powershell
$body = @{
    originCity = "上海"
    travelStartDate = "2026-10-01"
    travelEndDate = "2026-10-05"
    userBudget = 6000
    destinationCodes = @("DEST001", "DEST002", "DEST005")
} | ConvertTo-Json -Compress

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/recommend/calculate" `
  -Method POST `
  -ContentType "application/json" `
  -Body $body
```

## 13.3 按 requestId 查询结果

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/recommend/results/by-request?requestId=REQ_20261001_SHANGHAI_xxxx" `
  -Method GET
```

---

# 14. 验收标准

完成后应满足：

```text
✅ 支持任意已维护城市作为出发地。
✅ 支持不选择目的地时，系统自动从全部目的地推荐。
✅ 支持选择多个目的地时，仅对这些目的地排序。
✅ 每次推荐都有独立 requestId。
✅ 推荐结果可以按 requestId 查询。
✅ 推荐前会动态采集出发地到目的地路线。
✅ 推荐使用最新天气、路线、酒店数据。
✅ 前端可以选择出发城市和候选目的地。
✅ 任务日志记录本次推荐和采集过程。
✅ 数据质量检查仍然可用。
```

---

# 15. README 需要新增的说明

建议在 README 中新增一节：

```text
## V3 增强：动态出发地与候选目的地推荐

本版本支持用户从全国主要城市出发，系统可根据预算、出行日期和候选目的地集合动态生成推荐排序。

- 不选择候选目的地：系统从全部启用目的地中推荐。
- 选择候选目的地：系统仅对用户选择的目的地进行排序。
- 路线数据：推荐前通过高德路径规划 API 动态采集。
- 天气数据：通过高德天气 API 采集。
- 酒店价格：通过节假日与热度模型生成。
- 推荐结果：按 requestId 隔离，避免不同请求互相覆盖。
```

---

# 16. 当前项目顺手修正项

根据当前压缩包，建议顺手修：

```text
1. 把 com.travel.platform.cilent.amap 改成 com.travel.platform.client.amap。
2. application.yml 中 api.amap.host 建议改成固定字符串：
   host: https://restapi.amap.com
3. application.yml 中 AMAP_KEY / AMAP_API_KEY 命名统一。
4. 不要提交 .env、target、node_modules、dist、__pycache__、.idea。
5. 如果上传过真实高德 Key，建议到高德开放平台重置 Key。
```

推荐 `.gitignore` 包含：

```gitignore
target/
node_modules/
dist/
.idea/
.env
__pycache__/
*.pyc
*.log
```

---

## 结论

这次改造的核心不是换算法，而是把系统从：

```text
固定出发地 + 固定数据 + 固定推荐
```

升级为：

```text
动态出发地 + 动态候选目的地 + 动态采集真实路线/天气 + 本次请求独立排序
```

完成后，你的项目会更接近真实旅游决策产品，也更适合面试时讲“数据采集、数据处理、业务指标、推荐排序、前后端联动”。
