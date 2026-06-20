# Travel Realtime Flink

Consumes `recommend_request_event` from Kafka, calculates hot destination TopN in a processing-time window, and writes results to ClickHouse table `ads_hot_destination_topn`.

Build:

```bash
mvn -q -DskipTests package
```

Run locally:

```bash
java -jar target/travel-realtime-flink-0.1.0.jar
```

Useful environment variables:

```text
KAFKA_BOOTSTRAP_SERVERS=travel-kafka:29092
RECOMMEND_REQUEST_TOPIC=recommend_request_event
CLICKHOUSE_JDBC_URL=jdbc:clickhouse://clickhouse:8123/travel_realtime
HOT_DESTINATION_TOP_N=10
HOT_DESTINATION_WINDOW_MINUTES=5
```
