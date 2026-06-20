USE travel_decision_platform;

CREATE TABLE IF NOT EXISTS dwd_recommend_request_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'primary key',
    request_id VARCHAR(100) NOT NULL COMMENT 'request id',
    origin_city VARCHAR(100) NOT NULL COMMENT 'origin city',
    travel_start_date DATE DEFAULT NULL COMMENT 'travel start date',
    travel_end_date DATE DEFAULT NULL COMMENT 'travel end date',
    user_budget DECIMAL(10, 2) DEFAULT NULL COMMENT 'user budget',
    destination_count INT DEFAULT 0 COMMENT 'candidate destination count',
    event_time DATETIME NOT NULL COMMENT 'event time',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_request_id (request_id),
    KEY idx_event_time (event_time),
    KEY idx_origin_city (origin_city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='DWD recommend request event';
