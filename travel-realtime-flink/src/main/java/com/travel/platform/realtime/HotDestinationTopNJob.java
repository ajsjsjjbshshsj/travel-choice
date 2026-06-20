package com.travel.platform.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessAllWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.flink.api.common.serialization.SimpleStringSchema;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HotDestinationTopNJob {

    public static void main(String[] args) throws Exception {
        String bootstrapServers = env("KAFKA_BOOTSTRAP_SERVERS", "travel-kafka:29092");
        String topic = env("RECOMMEND_REQUEST_TOPIC", "recommend_request_event");
        String groupId = env("FLINK_GROUP_ID", "travel-hot-destination-topn");
        String clickHouseUrl = env("CLICKHOUSE_JDBC_URL", "jdbc:clickhouse://clickhouse:8123/travel_realtime");
        String clickHouseUser = env("CLICKHOUSE_USER", "default");
        String clickHousePassword = env("CLICKHOUSE_PASSWORD", "");
        int topN = Integer.parseInt(env("HOT_DESTINATION_TOP_N", "10"));
        int windowMinutes = Integer.parseInt(env("HOT_DESTINATION_WINDOW_MINUTES", "5"));

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers(bootstrapServers)
                .setTopics(topic)
                .setGroupId(groupId)
                .setStartingOffsets(OffsetsInitializer.latest())
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        DataStream<DestinationHit> hits = env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "recommend-request-event")
                .flatMap(new DestinationHitExtractor());

        DataStream<HotDestinationRow> topRows = hits
                .windowAll(TumblingProcessingTimeWindows.of(Time.minutes(windowMinutes)))
                .process(new TopNWindowFunction(topN));

        topRows.addSink(JdbcSink.sink(
                """
                INSERT INTO ads_hot_destination_topn
                (window_start, window_end, destination_code, destination_name, request_count, rank_no, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                (statement, row) -> {
                    statement.setTimestamp(1, row.windowStart);
                    statement.setTimestamp(2, row.windowEnd);
                    statement.setString(3, row.destinationCode);
                    statement.setString(4, row.destinationName);
                    statement.setLong(5, row.requestCount);
                    statement.setInt(6, row.rankNo);
                    statement.setTimestamp(7, row.updatedAt);
                },
                JdbcExecutionOptions.builder()
                        .withBatchSize(50)
                        .withBatchIntervalMs(2000)
                        .withMaxRetries(3)
                        .build(),
                new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                        .withUrl(clickHouseUrl)
                        .withDriverName("com.clickhouse.jdbc.ClickHouseDriver")
                        .withUsername(clickHouseUser)
                        .withPassword(clickHousePassword)
                        .build()
        )).name("clickhouse-hot-destination-topn");

        env.execute("Travel Hot Destination TopN");
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public static class DestinationHit {
        public String destinationCode;
        public String destinationName;

        public DestinationHit() {
        }

        public DestinationHit(String destinationCode, String destinationName) {
            this.destinationCode = destinationCode;
            this.destinationName = destinationName;
        }
    }

    public static class HotDestinationRow {
        public Timestamp windowStart;
        public Timestamp windowEnd;
        public String destinationCode;
        public String destinationName;
        public long requestCount;
        public int rankNo;
        public Timestamp updatedAt;
    }

    public static class DestinationHitExtractor implements FlatMapFunction<String, DestinationHit> {
        private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

        @Override
        public void flatMap(String value, Collector<DestinationHit> out) throws Exception {
            JsonNode root = OBJECT_MAPPER.readTree(value);
            JsonNode destinations = root.path("destinations");
            if (!destinations.isArray()) {
                return;
            }

            for (JsonNode item : destinations) {
                String code = item.path("destinationCode").asText("");
                String name = item.path("destinationName").asText(code);
                if (!code.isBlank()) {
                    out.collect(new DestinationHit(code, name));
                }
            }
        }
    }

    public static class TopNWindowFunction extends ProcessAllWindowFunction<DestinationHit, HotDestinationRow, TimeWindow> {
        private final int topN;

        public TopNWindowFunction(int topN) {
            this.topN = topN;
        }

        @Override
        public void process(Context context, Iterable<DestinationHit> elements, Collector<HotDestinationRow> out) {
            Map<String, Counter> counters = new HashMap<>();
            for (DestinationHit hit : elements) {
                Counter counter = counters.computeIfAbsent(
                        hit.destinationCode,
                        code -> new Counter(hit.destinationCode, hit.destinationName)
                );
                counter.count++;
            }

            List<Counter> sorted = new ArrayList<>(counters.values());
            sorted.sort(Comparator
                    .comparingLong((Counter counter) -> counter.count)
                    .reversed()
                    .thenComparing(counter -> counter.destinationCode));

            Timestamp windowStart = Timestamp.from(Instant.ofEpochMilli(context.window().getStart()));
            Timestamp windowEnd = Timestamp.from(Instant.ofEpochMilli(context.window().getEnd()));
            Timestamp now = Timestamp.from(Instant.now());

            int rank = 1;
            for (Counter counter : sorted.stream().limit(topN).toList()) {
                HotDestinationRow row = new HotDestinationRow();
                row.windowStart = windowStart;
                row.windowEnd = windowEnd;
                row.destinationCode = counter.destinationCode;
                row.destinationName = counter.destinationName;
                row.requestCount = counter.count;
                row.rankNo = rank++;
                row.updatedAt = now;
                out.collect(row);
            }
        }
    }

    private static class Counter {
        private final String destinationCode;
        private final String destinationName;
        private long count;

        private Counter(String destinationCode, String destinationName) {
            this.destinationCode = destinationCode;
            this.destinationName = destinationName;
        }
    }
}
