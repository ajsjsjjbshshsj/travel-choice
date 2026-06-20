USE travel_decision_platform;

ALTER TABLE etl_job_log
    ADD COLUMN request_id VARCHAR(100) DEFAULT NULL COMMENT 'Request ID for tracing' AFTER data_date,
    ADD COLUMN duration_seconds BIGINT DEFAULT NULL COMMENT 'Job duration in seconds' AFTER request_id,
    ADD KEY idx_request_id (request_id);

ALTER TABLE ads_destination_recommend_result
    ADD KEY idx_request_id (request_id),
    ADD KEY idx_rank_request (request_id, recommend_rank);
