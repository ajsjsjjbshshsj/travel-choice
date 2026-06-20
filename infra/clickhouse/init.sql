CREATE DATABASE IF NOT EXISTS travel_realtime;

CREATE TABLE IF NOT EXISTS travel_realtime.ads_hot_destination_topn (
    window_start DateTime,
    window_end DateTime,
    destination_code String,
    destination_name String,
    request_count UInt64,
    rank_no UInt32,
    updated_at DateTime DEFAULT now()
)
ENGINE = ReplacingMergeTree(updated_at)
PARTITION BY toYYYYMMDD(window_end)
ORDER BY (window_end, rank_no, destination_code);
