package com.travel.platform.service;

import com.travel.platform.entity.HotDestination;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class HotDestinationService {

    private final String clickHouseUrl;
    private final String username;
    private final String password;

    public HotDestinationService(
            @Value("${realtime.clickhouse.url}") String clickHouseUrl,
            @Value("${realtime.clickhouse.username}") String username,
            @Value("${realtime.clickhouse.password}") String password
    ) {
        this.clickHouseUrl = clickHouseUrl;
        this.username = username;
        this.password = password;
    }

    public List<HotDestination> listTopN(int limit) {
        String sql = """
                SELECT
                    toString(window_start) AS window_start,
                    toString(window_end) AS window_end,
                    destination_code,
                    destination_name,
                    request_count,
                    rank_no,
                    updated_at
                FROM ads_hot_destination_topn
                ORDER BY window_end DESC, rank_no ASC
                LIMIT %d
                """.formatted(Math.max(1, Math.min(limit, 100)));

        List<HotDestination> results = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(clickHouseUrl, username, password);
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            while (rs.next()) {
                HotDestination item = new HotDestination();
                item.setWindowStart(rs.getString("window_start"));
                item.setWindowEnd(rs.getString("window_end"));
                item.setDestinationCode(rs.getString("destination_code"));
                item.setDestinationName(rs.getString("destination_name"));
                item.setRequestCount(rs.getLong("request_count"));
                item.setRankNo(rs.getInt("rank_no"));
                item.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
                results.add(item);
            }
        } catch (Exception e) {
            log.warn("Failed to query ClickHouse hot destination topN", e);
        }
        return results;
    }
}
