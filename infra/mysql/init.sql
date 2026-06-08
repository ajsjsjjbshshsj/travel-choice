-- ============================================================
-- Travel Decision Platform - Database Initialization
-- ============================================================

CREATE DATABASE IF NOT EXISTS travel_decision_platform
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE travel_decision_platform;

-- -----------------------------------------------------------
-- 1. dwd_destination  目的地维表
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `dwd_destination` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `destination_code`  VARCHAR(50)  NOT NULL,
    `destination_name`  VARCHAR(200) NOT NULL,
    `country`           VARCHAR(50)  DEFAULT '中国',
    `province`          VARCHAR(50)  DEFAULT NULL,
    `city`              VARCHAR(50)  DEFAULT NULL,
    `latitude`          DECIMAL(10,6) DEFAULT NULL,
    `longitude`         DECIMAL(10,6) DEFAULT NULL,
    `destination_type`  VARCHAR(50)  DEFAULT NULL COMMENT 'nature/culture/adventure/leisure',
    `scenery_score`     DECIMAL(5,2) DEFAULT 60,
    `popularity_score`  DECIMAL(5,2) DEFAULT 60,
    `facility_score`    DECIMAL(5,2) DEFAULT 60,
    `description`       TEXT         DEFAULT NULL,
    `is_active`         TINYINT      NOT NULL DEFAULT 1,
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_code` (`destination_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='目的地维表';

-- -----------------------------------------------------------
-- 2. dwd_route_detail  交通路线明细
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `dwd_route_detail` (
    `id`                 BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `origin_city`        VARCHAR(50)  NOT NULL,
    `destination_code`   VARCHAR(50)  NOT NULL,
    `travel_date`        DATE         NOT NULL,
    `transport_type`     VARCHAR(20)  DEFAULT 'train' COMMENT 'train/flight/bus',
    `traffic_cost`       DECIMAL(10,2) DEFAULT 0,
    `duration_minutes`   INT          DEFAULT 0,
    `convenience_score`  DECIMAL(5,2) DEFAULT 60,
    `created_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_origin_date` (`origin_city`, `travel_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交通路线明细';

-- -----------------------------------------------------------
-- 3. dwd_weather_detail  天气明细
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `dwd_weather_detail` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `destination_code`  VARCHAR(50)  NOT NULL,
    `weather_date`      DATE         NOT NULL,
    `temperature_high`  INT          DEFAULT 25,
    `temperature_low`   INT          DEFAULT 15,
    `weather_type`      VARCHAR(20)  DEFAULT 'sunny' COMMENT 'sunny/cloudy/rainy/snowy',
    `weather_score`     DECIMAL(5,2) DEFAULT 70,
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_dest_date` (`destination_code`, `weather_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='天气明细';

-- -----------------------------------------------------------
-- 4. dwd_hotel_price_detail  酒店价格明细
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `dwd_hotel_price_detail` (
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `destination_code`  VARCHAR(50)  NOT NULL,
    `checkin_date`      DATE         NOT NULL,
    `checkout_date`     DATE         NOT NULL,
    `hotel_star`        TINYINT      DEFAULT 3,
    `avg_price`         DECIMAL(10,2) DEFAULT 300,
    `hotel_score`       DECIMAL(5,2) DEFAULT 70,
    `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_dest_checkin` (`destination_code`, `checkin_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='酒店价格明细';

-- -----------------------------------------------------------
-- 5. ads_destination_recommend_result  推荐结果 (ads层)
-- -----------------------------------------------------------
CREATE TABLE IF NOT EXISTS `ads_destination_recommend_result` (
    `id`                    BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `request_id`            VARCHAR(100) DEFAULT NULL,
    `origin_city`           VARCHAR(50)  NOT NULL,
    `destination_code`      VARCHAR(50)  NOT NULL,
    `destination_name`      VARCHAR(200) DEFAULT NULL,
    `travel_start_date`     DATE         NOT NULL,
    `travel_end_date`       DATE         NOT NULL,
    `travel_days`           INT          DEFAULT 1,
    `user_budget`           DECIMAL(10,2) DEFAULT 0,
    `estimated_total_cost`  DECIMAL(10,2) DEFAULT 0,
    `traffic_cost`          DECIMAL(10,2) DEFAULT 0,
    `hotel_cost`            DECIMAL(10,2) DEFAULT 0,
    `food_cost`             DECIMAL(10,2) DEFAULT 0,
    `ticket_cost`           DECIMAL(10,2) DEFAULT 0,
    `scenery_score`         DECIMAL(5,2) DEFAULT 0,
    `traffic_score`         DECIMAL(5,2) DEFAULT 0,
    `hotel_score`           DECIMAL(5,2) DEFAULT 0,
    `weather_score`         DECIMAL(5,2) DEFAULT 0,
    `cost_score`            DECIMAL(5,2) DEFAULT 0,
    `crowd_index`           DECIMAL(5,2) DEFAULT 0,
    `crowd_score`           DECIMAL(5,2) DEFAULT 0,
    `final_score`           DECIMAL(5,2) DEFAULT 0,
    `recommend_rank`        INT          DEFAULT 0,
    `recommend_level`       VARCHAR(20)  DEFAULT 'LOW',
    `recommend_reason`      TEXT         DEFAULT NULL,
    `model_version`         VARCHAR(50)  DEFAULT NULL,
    `created_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_query` (`origin_city`, `travel_start_date`, `travel_end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推荐结果表';


-- ============================================================
-- 模拟数据
-- ============================================================

-- 6 个目的地
INSERT INTO `dwd_destination` (`destination_code`, `destination_name`, `province`, `city`, `destination_type`, `scenery_score`, `popularity_score`, `facility_score`, `description`) VALUES
    ('DEST001', '张家界国家森林公园', '湖南', '张家界', 'nature',    92.50, 85.00, 78.00, '阿凡达取景地，石英砂岩峰林地貌，世界自然遗产'),
    ('DEST002', '西湖',               '浙江', '杭州',   'culture',   88.00, 95.00, 92.00, 'UNESCO 世界文化遗产，中国十大风景名胜之一'),
    ('DEST003', '漓江',               '广西', '桂林',   'nature',    90.00, 82.00, 75.00, '桂林山水甲天下，典型喀斯特地貌水上画廊'),
    ('DEST004', '布达拉宫',           '西藏', '拉萨',   'culture',   95.00, 70.00, 65.00, '世界上海拔最高的宫殿，藏传佛教圣地'),
    ('DEST005', '亚龙湾',             '海南', '三亚',   'leisure',   85.00, 90.00, 88.00, '热带海滨度假胜地，碧海蓝天白沙'),
    ('DEST006', '黄山',               '安徽', '黄山',   'adventure', 93.00, 80.00, 80.00, '五岳归来不看山，黄山归来不看岳');

-- 交通路线 (从北京出发, 旅行日期 2026-07-01)
INSERT INTO `dwd_route_detail` (`origin_city`, `destination_code`, `travel_date`, `transport_type`, `traffic_cost`, `duration_minutes`, `convenience_score`) VALUES
    ('北京', 'DEST001', '2026-07-01', 'flight', 1200.00, 150, 82.00),
    ('北京', 'DEST002', '2026-07-01', 'train',   550.00, 270, 90.00),
    ('北京', 'DEST003', '2026-07-01', 'flight', 1000.00, 160, 80.00),
    ('北京', 'DEST004', '2026-07-01', 'flight', 1500.00, 240, 65.00),
    ('北京', 'DEST005', '2026-07-01', 'flight', 1300.00, 180, 85.00),
    ('北京', 'DEST006', '2026-07-01', 'train',   450.00, 330, 88.00);

-- 天气 (2026-07-01 ~ 2026-07-05)
INSERT INTO `dwd_weather_detail` (`destination_code`, `weather_date`, `temperature_high`, `temperature_low`, `weather_type`, `weather_score`) VALUES
    ('DEST001', '2026-07-01', 32, 24, 'cloudy', 72.00),
    ('DEST001', '2026-07-02', 33, 25, 'rainy',  55.00),
    ('DEST001', '2026-07-03', 30, 23, 'sunny',  85.00),
    ('DEST001', '2026-07-04', 31, 24, 'sunny',  82.00),
    ('DEST001', '2026-07-05', 29, 22, 'cloudy', 70.00),
    ('DEST002', '2026-07-01', 34, 26, 'sunny',  80.00),
    ('DEST002', '2026-07-02', 33, 25, 'cloudy', 72.00),
    ('DEST002', '2026-07-03', 35, 27, 'sunny',  85.00),
    ('DEST002', '2026-07-04', 32, 25, 'rainy',  50.00),
    ('DEST002', '2026-07-05', 31, 24, 'cloudy', 68.00),
    ('DEST003', '2026-07-01', 33, 26, 'sunny',  82.00),
    ('DEST003', '2026-07-02', 34, 27, 'sunny',  88.00),
    ('DEST003', '2026-07-03', 31, 25, 'cloudy', 70.00),
    ('DEST003', '2026-07-04', 32, 25, 'sunny',  80.00),
    ('DEST003', '2026-07-05', 30, 24, 'rainy',  48.00),
    ('DEST004', '2026-07-01', 20, 8,  'sunny',  90.00),
    ('DEST004', '2026-07-02', 19, 7,  'sunny',  92.00),
    ('DEST004', '2026-07-03', 21, 9,  'cloudy', 75.00),
    ('DEST004', '2026-07-04', 18, 6,  'sunny',  88.00),
    ('DEST004', '2026-07-05', 20, 8,  'sunny',  90.00),
    ('DEST005', '2026-07-01', 33, 27, 'sunny',  92.00),
    ('DEST005', '2026-07-02', 34, 28, 'sunny',  90.00),
    ('DEST005', '2026-07-03', 32, 26, 'cloudy', 70.00),
    ('DEST005', '2026-07-04', 33, 27, 'sunny',  88.00),
    ('DEST005', '2026-07-05', 31, 26, 'rainy',  45.00),
    ('DEST006', '2026-07-01', 28, 20, 'cloudy', 75.00),
    ('DEST006', '2026-07-02', 27, 19, 'rainy',  50.00),
    ('DEST006', '2026-07-03', 29, 21, 'sunny',  85.00),
    ('DEST006', '2026-07-04', 28, 20, 'sunny',  82.00),
    ('DEST006', '2026-07-05', 26, 18, 'cloudy', 72.00);

-- 酒店价格 (入住 2026-07-01, 退房 2026-07-05)
INSERT INTO `dwd_hotel_price_detail` (`destination_code`, `checkin_date`, `checkout_date`, `hotel_star`, `avg_price`, `hotel_score`) VALUES
    ('DEST001', '2026-07-01', '2026-07-05', 3, 280.00, 72.00),
    ('DEST002', '2026-07-01', '2026-07-05', 4, 520.00, 88.00),
    ('DEST003', '2026-07-01', '2026-07-05', 3, 250.00, 70.00),
    ('DEST004', '2026-07-01', '2026-07-05', 3, 350.00, 68.00),
    ('DEST005', '2026-07-01', '2026-07-05', 4, 680.00, 90.00),
    ('DEST006', '2026-07-01', '2026-07-05', 3, 300.00, 75.00);
