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

-- 插入常用出发城市
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
