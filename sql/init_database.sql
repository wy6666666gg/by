-- ============================================================
-- 城市空气质量分析与预测系统 - 数据库初始化脚本
-- 数据库: air_quality_db
-- 版本: 1.0.0
-- ============================================================

CREATE DATABASE IF NOT EXISTS air_quality_db 
    DEFAULT CHARACTER SET utf8mb4 
    COLLATE utf8mb4_general_ci;

USE air_quality_db;

-- ============================================================
-- 1. 监测站点基础信息表
-- ============================================================
CREATE TABLE IF NOT EXISTS `station` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `station_code` VARCHAR(20) NOT NULL COMMENT '站点编码',
    `station_name` VARCHAR(100) NOT NULL COMMENT '站点名称',
    `district_name` VARCHAR(50) DEFAULT NULL COMMENT '所属区县',
    `latitude` DECIMAL(10, 6) DEFAULT NULL COMMENT '纬度',
    `longitude` DECIMAL(10, 6) DEFAULT NULL COMMENT '经度',
    `station_type` VARCHAR(30) DEFAULT NULL COMMENT '站点类型(国控/省控/市控)',
    `is_active` TINYINT DEFAULT 1 COMMENT '是否启用(1-启用 0-停用)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_station_code` (`station_code`),
    KEY `idx_district` (`district_name`),
    KEY `idx_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监测站点信息表';

-- ============================================================
-- 2. 空气质量监测数据表（核心业务表）
-- ============================================================
CREATE TABLE IF NOT EXISTS `air_quality_data` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `station_code` VARCHAR(20) NOT NULL COMMENT '站点编码',
    `city` VARCHAR(50) DEFAULT '郑州' COMMENT '城市',
    `aqi` INT DEFAULT NULL COMMENT '空气质量指数',
    `quality_level` VARCHAR(20) DEFAULT NULL COMMENT '空气质量等级',
    `primary_pollutant` VARCHAR(30) DEFAULT NULL COMMENT '首要污染物',
    `pm25` DECIMAL(8, 2) DEFAULT NULL COMMENT 'PM2.5浓度(μg/m³)',
    `pm10` DECIMAL(8, 2) DEFAULT NULL COMMENT 'PM10浓度(μg/m³)',
    `so2` DECIMAL(8, 2) DEFAULT NULL COMMENT 'SO2浓度(μg/m³)',
    `no2` DECIMAL(8, 2) DEFAULT NULL COMMENT 'NO2浓度(μg/m³)',
    `co` DECIMAL(8, 2) DEFAULT NULL COMMENT 'CO浓度(mg/m³)',
    `o3` DECIMAL(8, 2) DEFAULT NULL COMMENT 'O3浓度(μg/m³)',
    `temperature` DECIMAL(5, 1) DEFAULT NULL COMMENT '温度(℃)',
    `humidity` DECIMAL(5, 1) DEFAULT NULL COMMENT '相对湿度(%)',
    `wind_speed` DECIMAL(5, 1) DEFAULT NULL COMMENT '风速(m/s)',
    `wind_direction` VARCHAR(10) DEFAULT NULL COMMENT '风向',
    `pressure` DECIMAL(7, 1) DEFAULT NULL COMMENT '气压(hPa)',
    `monitor_time` DATETIME NOT NULL COMMENT '监测时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_station_time` (`station_code`, `monitor_time`),
    KEY `idx_city_time` (`city`, `monitor_time`),
    KEY `idx_monitor_time` (`monitor_time`),
    KEY `idx_aqi` (`aqi`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='空气质量监测数据表';

-- ============================================================
-- 3. 预测结果存储表
-- ============================================================
CREATE TABLE IF NOT EXISTS `aqi_prediction` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `station_code` VARCHAR(20) NOT NULL COMMENT '站点编码',
    `station_name` VARCHAR(100) DEFAULT NULL COMMENT '站点名称',
    `predict_time` DATETIME NOT NULL COMMENT '预测目标时间',
    `predict_hour` INT DEFAULT NULL COMMENT '预测时长(小时)',
    `aqi_pred` INT DEFAULT NULL COMMENT '预测AQI值',
    `aqi_level_pred` VARCHAR(20) DEFAULT NULL COMMENT '预测空气质量等级',
    `pm25_pred` DECIMAL(8, 2) DEFAULT NULL COMMENT '预测PM2.5浓度',
    `pm10_pred` DECIMAL(8, 2) DEFAULT NULL COMMENT '预测PM10浓度',
    `confidence` DECIMAL(5, 2) DEFAULT NULL COMMENT '预测置信度(0-1)',
    `model_type` VARCHAR(50) DEFAULT 'random_forest' COMMENT '模型类型',
    `model_version` VARCHAR(20) DEFAULT '1.0' COMMENT '模型版本',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '预测生成时间',
    PRIMARY KEY (`id`),
    KEY `idx_station_predict` (`station_code`, `predict_time`),
    KEY `idx_model` (`model_type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AQI预测结果表';

-- ============================================================
-- 4. 预警规则配置表
-- ============================================================
CREATE TABLE IF NOT EXISTS `alert_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name` VARCHAR(100) NOT NULL COMMENT '规则名称',
    `description` VARCHAR(500) DEFAULT NULL COMMENT '规则描述',
    `condition` VARCHAR(200) NOT NULL COMMENT '触发条件(如: pm25 > 75)',
    `level` VARCHAR(20) NOT NULL COMMENT '告警级别(info/warning/severe/emergency)',
    `enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
    `notify_methods` VARCHAR(200) DEFAULT NULL COMMENT '通知方式(email/sms/push)',
    `notify_targets` VARCHAR(500) DEFAULT NULL COMMENT '通知对象',
    `cooldown_minutes` INT DEFAULT 60 COMMENT '告警冷却时间(分钟)',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_enabled` (`enabled`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警规则配置表';

-- ============================================================
-- 5. 预警记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `alert_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `rule_id` BIGINT DEFAULT NULL COMMENT '关联规则ID',
    `station_code` VARCHAR(20) DEFAULT NULL COMMENT '站点编码',
    `alert_type` VARCHAR(50) NOT NULL COMMENT '告警类型(污染物名称)',
    `level` VARCHAR(20) NOT NULL COMMENT '告警级别',
    `threshold_value` DECIMAL(8, 2) DEFAULT NULL COMMENT '阈值',
    `actual_value` DECIMAL(8, 2) DEFAULT NULL COMMENT '实际监测值',
    `status` VARCHAR(20) DEFAULT 'active' COMMENT '状态(active/resolved/expired)',
    `resolve_time` DATETIME DEFAULT NULL COMMENT '处理时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '告警时间',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_rule` (`rule_id`),
    KEY `idx_station` (`station_code`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警记录表';

-- ============================================================
-- 6. 预测模型信息表
-- ============================================================
CREATE TABLE IF NOT EXISTS `prediction_model` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
    `algorithm` VARCHAR(50) NOT NULL COMMENT '算法类型(random_forest/lstm/arima)',
    `city` VARCHAR(50) DEFAULT '郑州' COMMENT '适用城市',
    `train_start_time` DATETIME DEFAULT NULL COMMENT '训练数据起始时间',
    `train_end_time` DATETIME DEFAULT NULL COMMENT '训练数据截止时间',
    `sample_count` INT DEFAULT NULL COMMENT '训练样本数',
    `feature_count` INT DEFAULT NULL COMMENT '特征数量',
    `num_trees` INT DEFAULT 100 COMMENT '决策树数量(RF)',
    `max_depth` INT DEFAULT 10 COMMENT '最大深度(RF)',
    `rmse` DECIMAL(10, 4) DEFAULT NULL COMMENT '均方根误差',
    `mae` DECIMAL(10, 4) DEFAULT NULL COMMENT '平均绝对误差',
    `r_squared` DECIMAL(10, 4) DEFAULT NULL COMMENT '决定系数R²',
    `model_path` VARCHAR(500) DEFAULT NULL COMMENT '模型文件存储路径',
    `status` TINYINT DEFAULT 1 COMMENT '状态(0-停用 1-启用)',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_algorithm_city` (`algorithm`, `city`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预测模型信息表';

-- ============================================================
-- 7. 初始化监测站点数据（郑州市）
-- ============================================================
INSERT INTO `station` (`station_code`, `station_name`, `district_name`, `latitude`, `longitude`, `station_type`) VALUES
('410101A', '郑州市监测站', '中原区', 34.757000, 113.665000, '国控'),
('410102A', '银行学校', '金水区', 34.780000, 113.710000, '国控'),
('410103A', '市环保监测站', '二七区', 34.748000, 113.655000, '国控'),
('410104A', '郑纺机', '管城区', 34.730000, 113.680000, '国控'),
('410105A', '烟厂', '惠济区', 34.752000, 113.725000, '省控'),
('410106A', '岗李水库', '高新区', 34.810000, 113.590000, '省控'),
('410107A', '供水公司', '中原区', 34.770000, 113.640000, '市控'),
('410108A', '四十七中', '金水区', 34.790000, 113.730000, '市控'),
('410109A', '经开区管委会', '经开区', 34.720000, 113.750000, '市控');

-- ============================================================
-- 8. 初始化预警规则
-- ============================================================
INSERT INTO `alert_rule` (`name`, `condition`, `level`, `description`, `enabled`) VALUES
('PM2.5轻度污染告警', 'pm25 > 75', 'warning', 'PM2.5浓度超过75μg/m³触发', 1),
('PM2.5重度污染告警', 'pm25 > 150', 'severe', 'PM2.5浓度超过150μg/m³触发', 1),
('AQI中度污染告警', 'aqi > 150', 'warning', 'AQI超过150触发', 1),
('AQI重度污染告警', 'aqi > 200', 'severe', 'AQI超过200触发', 1),
('AQI严重污染告警', 'aqi > 300', 'emergency', 'AQI超过300触发紧急告警', 1),
('O3超标告警', 'o3 > 160', 'warning', '臭氧8小时浓度超过160μg/m³', 1);
