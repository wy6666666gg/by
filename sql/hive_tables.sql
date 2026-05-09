-- ============================================================
-- 城市空气质量分析与预测系统 - Hive数据仓库建表脚本
-- 数据库: air_quality_db
-- 数仓分层: ODS -> DWD -> DWS -> ADS
-- ============================================================

CREATE DATABASE IF NOT EXISTS air_quality_db;
USE air_quality_db;

-- ============================================================
-- ODS层: 原始数据层 (Operational Data Store)
-- ============================================================

-- 原始空气质量监测数据（从MySQL同步）
CREATE TABLE IF NOT EXISTS ods_air_quality_raw (
    station_code STRING COMMENT '站点编码',
    station_name STRING COMMENT '站点名称',
    city STRING COMMENT '城市',
    aqi INT COMMENT '空气质量指数',
    quality_level STRING COMMENT '空气质量等级',
    primary_pollutant STRING COMMENT '首要污染物',
    pm25 DOUBLE COMMENT 'PM2.5浓度(ug/m3)',
    pm10 DOUBLE COMMENT 'PM10浓度(ug/m3)',
    so2 DOUBLE COMMENT 'SO2浓度(ug/m3)',
    no2 DOUBLE COMMENT 'NO2浓度(ug/m3)',
    co DOUBLE COMMENT 'CO浓度(mg/m3)',
    o3 DOUBLE COMMENT 'O3浓度(ug/m3)',
    temperature DOUBLE COMMENT '温度(℃)',
    humidity DOUBLE COMMENT '相对湿度(%)',
    wind_speed DOUBLE COMMENT '风速(m/s)',
    wind_direction STRING COMMENT '风向',
    pressure DOUBLE COMMENT '气压(hPa)',
    monitor_time TIMESTAMP COMMENT '监测时间',
    create_time TIMESTAMP COMMENT '入库时间'
)
PARTITIONED BY (dt STRING COMMENT '数据日期分区 yyyy-MM-dd')
STORED AS ORC
TBLPROPERTIES ('orc.compress'='SNAPPY');

-- ============================================================
-- DWD层: 明细数据层 (Data Warehouse Detail)
-- ============================================================

-- 空气质量明细数据（数据清洗后）
CREATE TABLE IF NOT EXISTS dwd_air_quality_dt (
    station_code STRING COMMENT '站点编码',
    station_name STRING COMMENT '站点名称',
    district_name STRING COMMENT '所属区县',
    latitude DOUBLE COMMENT '纬度',
    longitude DOUBLE COMMENT '经度',
    aqi INT COMMENT '空气质量指数',
    aqi_level INT COMMENT 'AQI等级(1-6)',
    quality_level STRING COMMENT '空气质量等级描述',
    primary_pollutant STRING COMMENT '首要污染物',
    pm25 DOUBLE COMMENT 'PM2.5浓度',
    pm10 DOUBLE COMMENT 'PM10浓度',
    so2 DOUBLE COMMENT 'SO2浓度',
    no2 DOUBLE COMMENT 'NO2浓度',
    co DOUBLE COMMENT 'CO浓度',
    o3 DOUBLE COMMENT 'O3浓度',
    temperature DOUBLE COMMENT '温度',
    humidity DOUBLE COMMENT '湿度',
    wind_speed DOUBLE COMMENT '风速',
    pressure DOUBLE COMMENT '气压',
    monitor_date STRING COMMENT '监测日期',
    monitor_hour INT COMMENT '监测小时',
    is_valid TINYINT COMMENT '数据有效标记(1-有效 0-无效)'
)
PARTITIONED BY (dt STRING COMMENT '分区日期')
STORED AS ORC
TBLPROPERTIES ('orc.compress'='SNAPPY');

-- ============================================================
-- DWS层: 汇总数据层 (Data Warehouse Summary)
-- ============================================================

-- 站点小时级汇总
CREATE TABLE IF NOT EXISTS dws_station_hour (
    station_code STRING COMMENT '站点编码',
    station_name STRING COMMENT '站点名称',
    year INT COMMENT '年',
    month INT COMMENT '月',
    day INT COMMENT '日',
    hour INT COMMENT '小时',
    aqi_avg DOUBLE COMMENT '平均AQI',
    aqi_max INT COMMENT '最高AQI',
    aqi_min INT COMMENT '最低AQI',
    pm25_avg DOUBLE COMMENT '平均PM2.5',
    pm10_avg DOUBLE COMMENT '平均PM10',
    so2_avg DOUBLE COMMENT '平均SO2',
    no2_avg DOUBLE COMMENT '平均NO2',
    co_avg DOUBLE COMMENT '平均CO',
    o3_avg DOUBLE COMMENT '平均O3',
    record_count INT COMMENT '记录数'
)
PARTITIONED BY (dt STRING)
STORED AS ORC;

-- 月度汇总表
CREATE TABLE IF NOT EXISTS dws_month_summary (
    district_code STRING COMMENT '区县编码',
    district_name STRING COMMENT '区县名称',
    year INT COMMENT '年',
    month INT COMMENT '月',
    aqi_avg DOUBLE COMMENT '月均AQI',
    aqi_max INT COMMENT '月最高AQI',
    aqi_min INT COMMENT '月最低AQI',
    pm25_avg DOUBLE COMMENT '月均PM2.5',
    pm10_avg DOUBLE COMMENT '月均PM10',
    clean_days INT COMMENT '优良天数',
    polluted_days INT COMMENT '污染天数',
    primary_pollutant STRING COMMENT '月度首要污染物',
    record_count INT COMMENT '有效记录数'
)
PARTITIONED BY (dt STRING)
STORED AS ORC;

-- ============================================================
-- ADS层: 应用数据层 (Application Data Store)
-- ============================================================

-- 实时AQI应用表（供API查询）
CREATE TABLE IF NOT EXISTS ads_realtime_aqi (
    station_code STRING COMMENT '站点编码',
    station_name STRING COMMENT '站点名称',
    district_name STRING COMMENT '区县',
    latitude DOUBLE COMMENT '纬度',
    longitude DOUBLE COMMENT '经度',
    aqi INT COMMENT 'AQI',
    aqi_level INT COMMENT 'AQI等级',
    aqi_level_desc STRING COMMENT '等级描述',
    pm25 DOUBLE,
    pm10 DOUBLE,
    so2 DOUBLE,
    no2 DOUBLE,
    co DOUBLE,
    o3 DOUBLE,
    primary_pollutant STRING COMMENT '首要污染物',
    is_active INT COMMENT '是否在线',
    update_time TIMESTAMP COMMENT '最后更新时间'
)
STORED AS ORC;

-- AQI预测结果表
CREATE TABLE IF NOT EXISTS ads_aqi_prediction (
    station_code STRING COMMENT '站点编码',
    station_name STRING COMMENT '站点名称',
    predict_time TIMESTAMP COMMENT '预测目标时间',
    predict_hour INT COMMENT '预测时长(小时)',
    aqi_pred INT COMMENT '预测AQI',
    aqi_level_pred STRING COMMENT '预测等级',
    pm25_pred DOUBLE COMMENT '预测PM2.5',
    pm10_pred DOUBLE COMMENT '预测PM10',
    confidence DOUBLE COMMENT '置信度',
    model_type STRING COMMENT '模型类型',
    create_time TIMESTAMP COMMENT '预测生成时间'
)
PARTITIONED BY (dt STRING)
STORED AS ORC;

-- 每日首要污染物统计
CREATE TABLE IF NOT EXISTS ads_daily_pollutant (
    stat_date STRING COMMENT '统计日期',
    pollutant STRING COMMENT '首要污染物',
    station_count INT COMMENT '站点数',
    avg_aqi DOUBLE COMMENT '平均AQI'
)
STORED AS ORC;

-- ============================================================
-- DIM层: 维度表
-- ============================================================

-- 站点维度表
CREATE TABLE IF NOT EXISTS dim_station (
    station_code STRING COMMENT '站点编码',
    station_name STRING COMMENT '站点名称',
    district_name STRING COMMENT '区县',
    latitude DOUBLE COMMENT '纬度',
    longitude DOUBLE COMMENT '经度',
    station_type STRING COMMENT '站点类型',
    is_active INT COMMENT '是否启用'
)
STORED AS ORC;
