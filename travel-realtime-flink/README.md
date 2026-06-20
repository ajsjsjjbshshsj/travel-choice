# Travel Realtime Flink

Flink 实时计算模块，消费 Kafka 中的推荐请求事件，通过处理时间窗口聚合计算热门目的地 TopN，结果写入 ClickHouse。

## 数据链路

```
Spring Boot RecommendRequestProducer
  → Kafka topic: recommend_request_event (JSON)
  → Flink HotDestinationTopNJob (窗口聚合)
  → ClickHouse: travel_realtime.ads_hot_destination_topn
  → Spring Boot HotDestinationController
  → Dashboard 实时 TopN 表格
```

## 核心类

`HotDestinationTopNJob.java` 包含以下内部组件：

| 组件 | 说明 |
|------|------|
| `DestinationHit` | 目的地命中事件（code + name） |
| `DestinationHitExtractor` | FlatMap: 从 JSON 中提取 `destinations[]` 数组，每个目的地产生一条事件 |
| `TopNWindowFunction` | 窗口处理: 统计每个目的地的命中次数，排序取 TopN，生成排名行 |
| `HotDestinationRow` | 输出行（窗口起止时间、目的地、请求次数、排名） |

## 构建

```bash
cd travel-realtime-flink
mvn -q -DskipTests package
```

生成 `target/travel-realtime-flink-0.1.0.jar`。

## 运行

### Docker Compose 方式（推荐）

```bash
# 先打包
mvn -q -DskipTests package

# 启动 Flink 集群
docker compose --profile realtime up -d flink-jobmanager flink-taskmanager

# 提交作业
docker exec -it travel-flink-jobmanager flink run /opt/flink/usrlib/travel-realtime-flink-0.1.0.jar
```

Flink Web UI: `http://localhost:8081`

### 本地运行

```bash
java -jar target/travel-realtime-flink-0.1.0.jar
```

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `KAFKA_BOOTSTRAP_SERVERS` | `travel-kafka:29092` | Kafka 集群地址 |
| `RECOMMEND_REQUEST_TOPIC` | `recommend_request_event` | 消费 topic |
| `FLINK_GROUP_ID` | `travel-flink-group` | Kafka 消费组 |
| `CLICKHOUSE_JDBC_URL` | `jdbc:clickhouse://clickhouse:8123/travel_realtime` | ClickHouse JDBC |
| `CLICKHOUSE_USER` | `default` | ClickHouse 用户 |
| `CLICKHOUSE_PASSWORD` | *(空)* | ClickHouse 密码 |
| `HOT_DESTINATION_TOP_N` | `10` | TopN 数量 |
| `HOT_DESTINATION_WINDOW_MINUTES` | `5` | 滚动窗口大小（分钟） |

## 技术栈

- Flink 1.19.1 (Java 17)
- flink-connector-kafka 3.2.0-1.19
- flink-connector-jdbc 3.2.0-1.19
- ClickHouse JDBC 0.7.2
- Jackson 2.17.2