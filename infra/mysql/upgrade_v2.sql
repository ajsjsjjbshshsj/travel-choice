USE travel_decision_platform;

CREATE TABLE IF NOT EXISTS etl_job_log (
                                           id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

                                           job_name VARCHAR(100) NOT NULL COMMENT '任务名称',
                                           job_type VARCHAR(50) DEFAULT NULL COMMENT '任务类型',
                                           job_desc VARCHAR(255) DEFAULT NULL COMMENT '任务描述',

                                           start_time DATETIME NOT NULL COMMENT '任务开始时间',
                                           end_time DATETIME DEFAULT NULL COMMENT '任务结束时间',

                                           status VARCHAR(20) NOT NULL COMMENT '任务状态：RUNNING/SUCCESS/FAILED',
                                           row_count INT DEFAULT 0 COMMENT '处理数据行数',

                                           data_date DATE DEFAULT NULL COMMENT '任务处理日期',
                                           error_message TEXT COMMENT '错误信息',

                                           trigger_type VARCHAR(50) DEFAULT NULL COMMENT '触发方式：MANUAL/SCHEDULED/API',

                                           created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                           updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                           KEY idx_job_name (job_name),
                                           KEY idx_status (status),
                                           KEY idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ETL任务运行日志表';


CREATE TABLE IF NOT EXISTS data_quality_check_result (
                                                         id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',

                                                         table_name VARCHAR(100) NOT NULL COMMENT '检查表名',
                                                         rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
                                                         rule_desc VARCHAR(255) DEFAULT NULL COMMENT '规则描述',

                                                         check_result VARCHAR(20) NOT NULL COMMENT '检查结果：PASS/WARNING/FAILED',
                                                         error_count INT DEFAULT 0 COMMENT '异常数量',

                                                         check_time DATETIME NOT NULL COMMENT '检查时间',
                                                         error_message TEXT COMMENT '错误信息',

                                                         created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

                                                         KEY idx_table_name (table_name),
                                                         KEY idx_check_result (check_result),
                                                         KEY idx_check_time (check_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据质量检查结果表';