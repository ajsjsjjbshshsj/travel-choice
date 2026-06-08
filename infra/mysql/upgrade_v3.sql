USE travel_decision_platform;

-- -----------------------------------------------------------
-- 1. dwd_destination 新增 amap_adcode 字段
-- -----------------------------------------------------------
ALTER TABLE `dwd_destination`
    ADD COLUMN `amap_adcode` VARCHAR(20) DEFAULT NULL COMMENT '高德行政区划代码' AFTER `destination_type`;

-- -----------------------------------------------------------
-- 2. dwd_weather_detail 表结构升级：对齐 WeatherDetail 实体
-- -----------------------------------------------------------

-- 重命名字段以匹配实体命名
ALTER TABLE `dwd_weather_detail`
    CHANGE COLUMN `temperature_high` `temp_max`  DECIMAL(5,2) DEFAULT NULL,
    CHANGE COLUMN `temperature_low`  `temp_min`  DECIMAL(5,2) DEFAULT NULL;

-- 新增字段
ALTER TABLE `dwd_weather_detail`
    ADD COLUMN `destination_name`           VARCHAR(200)   DEFAULT NULL AFTER `destination_code`,
    ADD COLUMN `precipitation_probability`  DECIMAL(5,2)   DEFAULT NULL AFTER `temp_min`,
    ADD COLUMN `wind_speed`                 DECIMAL(5,2)   DEFAULT NULL AFTER `precipitation_probability`,
    ADD COLUMN `weather_risk_level`         VARCHAR(20)    DEFAULT NULL AFTER `weather_score`,
    ADD COLUMN `data_source`                VARCHAR(50)    DEFAULT 'amap_weather' AFTER `weather_risk_level`,
    ADD COLUMN `updated_at`                 DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `data_source`;

-- 更新旧数据填充 destination_name
UPDATE dwd_weather_detail w
    JOIN dwd_destination d ON w.destination_code = d.destination_code
    SET w.destination_name = d.destination_name
    WHERE w.destination_name IS NULL;

-- -----------------------------------------------------------
-- 3. dwd_destination 填充 amap_adcode + 经纬度
-- -----------------------------------------------------------
UPDATE `dwd_destination` SET `amap_adcode` = '430800', `latitude` = 29.1170, `longitude` = 110.4790 WHERE `destination_code` = 'DEST001'; -- 张家界
UPDATE `dwd_destination` SET `amap_adcode` = '330100', `latitude` = 30.2590, `longitude` = 120.1388 WHERE `destination_code` = 'DEST002'; -- 杭州
UPDATE `dwd_destination` SET `amap_adcode` = '450300', `latitude` = 25.2740, `longitude` = 110.2990 WHERE `destination_code` = 'DEST003'; -- 桂林
UPDATE `dwd_destination` SET `amap_adcode` = '540100', `latitude` = 29.6600, `longitude` = 91.1320  WHERE `destination_code` = 'DEST004'; -- 拉萨
UPDATE `dwd_destination` SET `amap_adcode` = '460200', `latitude` = 18.1910, `longitude` = 109.6400 WHERE `destination_code` = 'DEST005'; -- 三亚
UPDATE `dwd_destination` SET `amap_adcode` = '341000', `latitude` = 29.7147, `longitude` = 118.3385 WHERE `destination_code` = 'DEST006'; -- 黄山

-- -----------------------------------------------------------
-- 4. dwd_route_detail 表结构升级：对齐 RouteDetail 实体
-- -----------------------------------------------------------
ALTER TABLE `dwd_route_detail`
    ADD COLUMN `destination_name`    VARCHAR(200)   DEFAULT NULL AFTER `destination_code`,
    ADD COLUMN `distance_km`         DECIMAL(10,2)  DEFAULT NULL AFTER `transport_type`,
    ADD COLUMN `currency`            VARCHAR(10)    DEFAULT 'CNY' AFTER `traffic_cost`,
    ADD COLUMN `transfer_count`      INT            DEFAULT 0 AFTER `currency`,
    ADD COLUMN `data_source`         VARCHAR(50)    DEFAULT 'amap_route' AFTER `convenience_score`,
    ADD COLUMN `updated_at`          DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `data_source`;