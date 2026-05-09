-- ============================================
-- 基于Hive的城市空气质量分析与预测系统
-- 数据库设计文档
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS air_quality_db;
USE air_quality_db;

-- ============================================
-- 1. ODS层（原始数据层）
-- ============================================

-- ODS层：空气质量原始数据表
CREATE TABLE IF NOT EXISTS ods_air_quality_raw (
    id          BIGINT,
    station_code    STRING COMMENT '监测点编码',
    station_name    STRING COMMENT '监测点名称',
    monitor_time    STRING COMMENT '监测时间',
    pm25           DOUBLE COMMENT 'PM2.5浓度(μg/m³)',
    pm10           DOUBLE COMMENT 'PM10浓度(μg/m³)',
    so2           DOUBLE COMMENT 'SO2浓度(μg/m³)',
    no2           DOUBLE COMMENT 'NO2浓度(μg/m³)',
    co           DOUBLE COMMENT 'CO浓度(mg/m³)',
    o3           DOUBLE COMMENT 'O3浓度(μg/m³)',
    aqi           INT COMMENT '空气质量指数',
    primary_pollutant    STRING COMMENT '首要污染物',
    quality_level        STRING COMMENT '空气质量等级',
    data_status          STRING COMMENT '数据状态',
    create_time          STRING COMMENT '创建时间'
) COMMENT 'ODS层空气质量原始数据表'
PARTITIONED BY (dt STRING COMMENT '分区日期，格式YYYY-MM-DD')
ROW FORMAT DELIMITED
FIELDS TERMINATED BY '\t'
STORED AS TEXTFILE
TBLPROPERTIES ('parquet.compression'='SNAPPY');

-- ODS层：气象数据原始表
CREATE TABLE IF NOT EXISTS ods_weather_raw (
    id              BIGINT,
    station_code    STRING COMMENT '站点编码',
    monitor_time    STRING COMMENT '监测时间',
    temperature     DOUBLE COMMENT '温度(℃)',
    humidity        DOUBLE COMMENT '湿度(%)',
    wind_speed      DOUBLE COMMENT '风速(m/s)',
    wind_direction  STRING COMMENT '风向',
    pressure        DOUBLE COMMENT '气压(hPa)',
    visibility      DOUBLE COMMENT '能见度(km)',
    weather_type    STRING COMMENT '天气类型',
    create_time     STRING COMMENT '创建时间'
) COMMENT 'ODS层气象数据原始表'
PARTITIONED BY (dt STRING COMMENT '分区日期')
ROW FORMAT DELIMITED
FIELDS TERMINATED BY '\t'
STORED AS TEXTFILE;

-- ============================================
-- 2. DWD层（明细数据层）
-- ============================================

-- DWD层：空气质量明细表（事实表）
CREATE TABLE IF NOT EXISTS dwd_air_quality_dt (
    id              BIGINT,
    station_code    STRING COMMENT '监测点编码',
    station_name    STRING COMMENT '监测点名称',
    district_code   STRING COMMENT '区县编码',
    district_name   STRING COMMENT '区县名称',
    latitude        DOUBLE COMMENT '纬度',
    longitude       DOUBLE COMMENT '经度',
    monitor_time    TIMESTAMP COMMENT '监测时间',
    hour_part       INT COMMENT '小时时段',
    pm25            DOUBLE COMMENT 'PM2.5浓度',
    pm10            DOUBLE COMMENT 'PM10浓度',
    so2             DOUBLE COMMENT 'SO2浓度',
    no2             DOUBLE COMMENT 'NO2浓度',
    co              DOUBLE COMMENT 'CO浓度',
    o3              DOUBLE COMMENT 'O3浓度',
    aqi             INT COMMENT 'AQI',
    aqi_level       INT COMMENT 'AQI等级1-6',
    primary_pollutant   STRING COMMENT '首要污染物',
    is_valid        INT COMMENT '数据是否有效：1有效，0无效',
    etl_time        STRING COMMENT 'ETL处理时间'
) COMMENT 'DWD层空气质量明细表'
PARTITIONED BY (year STRING, month STRING, day STRING)
STORED AS ORC
TBLPROPERTIES ('orc.compression'='SNAPPY');

-- DWD层：气象数据明细表
CREATE TABLE IF NOT EXISTS dwd_weather_dt (
    id              BIGINT,
    station_code    STRING COMMENT '站点编码',
    latitude        DOUBLE COMMENT '纬度',
    longitude       DOUBLE COMMENT '经度',
    monitor_time    TIMESTAMP COMMENT '监测时间',
    hour_part       INT COMMENT '小时时段',
    temperature     DOUBLE COMMENT '温度',
    humidity        DOUBLE COMMENT '湿度',
    wind_speed      DOUBLE COMMENT '风速',
    wind_direction  STRING COMMENT '风向',
    wind_speed_level INT COMMENT '风力等级',
    pressure        DOUBLE COMMENT '气压',
    pressure_level  INT COMMENT '气压等级',
    visibility      DOUBLE COMMENT '能见度',
    weather_code    STRING COMMENT '天气代码',
    is_holiday      INT COMMENT '是否节假日：1是，0否',
    holiday_name    STRING COMMENT '节假日名称',
    etl_time        STRING COMMENT 'ETL处理时间'
) COMMENT 'DWD层气象数据明细表'
PARTITIONED BY (year STRING, month STRING, day STRING)
STORED AS ORC
TBLPROPERTIES ('orc.compression'='SNAPPY');

-- ============================================
-- 3. DWS层（汇总数据层）
-- ============================================

-- DWS层：站点小时汇总表
CREATE TABLE IF NOT EXISTS dws_station_hour (
    station_code    STRING COMMENT '站点编码',
    station_name    STRING COMMENT '站点名称',
    district_code   STRING COMMENT '区县编码',
    district_name   STRING COMMENT '区县名称',
    hour_time       TIMESTAMP COMMENT '小时时间点',
    year            INT COMMENT '年',
    month           INT COMMENT '月',
    day             INT COMMENT '日',
    hour            INT COMMENT '小时',
    week_of_year    INT COMMENT '年内第几周',
    day_of_week     INT COMMENT '星期几',
    is_workday      INT COMMENT '是否工作日',
    pm25_avg        DOUBLE COMMENT 'PM2.5均值',
    pm10_avg        DOUBLE COMMENT 'PM10均值',
    so2_avg         DOUBLE COMMENT 'SO2均值',
    no2_avg         DOUBLE COMMENT 'NO2均值',
    co_avg          DOUBLE COMMENT 'CO均值',
    o3_avg          DOUBLE COMMENT 'O3均值',
    aqi_avg         DOUBLE COMMENT 'AQI均值',
    aqi_max         INT COMMENT 'AQI最大值',
    aqi_min         INT COMMENT 'AQI最小值',
    temp_avg        DOUBLE COMMENT '温度均值',
    humidity_avg    DOUBLE COMMENT '湿度均值',
    wind_speed_avg  DOUBLE COMMENT '风速均值',
    valid_count     INT COMMENT '有效数据条数'
) COMMENT 'DWS层站点小时汇总表'
PARTITIONED BY (year STRING, month STRING)
STORED AS ORC
TBLPROPERTIES ('orc.compression'='SNAPPY');

-- DWS层：区县日汇总表
CREATE TABLE IF NOT EXISTS dws_district_day (
    district_code   STRING COMMENT '区县编码',
    district_name   STRING COMMENT '区县名称',
    day_time        TIMESTAMP COMMENT '日期',
    year            INT COMMENT '年',
    month           INT COMMENT '月',
    day             INT COMMENT '日',
    season          INT COMMENT '季节：1春2夏3秋4冬',
    pm25_avg        DOUBLE COMMENT 'PM2.5日均值',
    pm10_avg        DOUBLE COMMENT 'PM10日均值',
    so2_avg         DOUBLE COMMENT 'SO2日均值',
    no2_avg         DOUBLE COMMENT 'NO2日均值',
    co_avg          DOUBLE COMMENT 'CO日均值',
    o3_avg          DOUBLE COMMENT 'O3日均值',
    aqi_avg         DOUBLE COMMENT 'AQI日均值',
    aqi_max         INT COMMENT 'AQI日最大值',
    aqi_min         INT COMMENT 'AQI日最小值',
    aqi_quality     STRING COMMENT '空气质量类别',
    clean_days      INT COMMENT '优良天数',
    clean_rate      DOUBLE COMMENT '优良率',
    station_count   INT COMMENT '站点数量'
) COMMENT 'DWS层区县日汇总表'
PARTITIONED BY (year STRING, month STRING)
STORED AS ORC;

-- DWS层：月份汇总表
CREATE TABLE IF NOT EXISTS dws_month_summary (
    district_code   STRING COMMENT '区县编码',
    district_name   STRING COMMENT '区县名称',
    year            INT COMMENT '年份',
    month           INT COMMENT '月份',
    month_name      STRING COMMENT '月份名称',
    season          INT COMMENT '季节',
    pm25_avg        DOUBLE COMMENT 'PM2.5月均值',
    pm10_avg        DOUBLE COMMENT 'PM10月均值',
    so2_avg         DOUBLE COMMENT 'SO2月均值',
    no2_avg         DOUBLE COMMENT 'NO2月均值',
    co_avg          DOUBLE COMMENT 'CO月均值',
    o3_avg          DOUBLE COMMENT 'O3月均值',
    aqi_avg         DOUBLE COMMENT 'AQI月均值',
    clean_days      INT COMMENT '优良天数',
    clean_rate      DOUBLE COMMENT '优良率',
    polluted_days   INT COMMENT '污染天数',
    polluted_rate   DOUBLE COMMENT '污染率'
) COMMENT 'DWS层月份汇总表'
STORED AS ORC;

-- ============================================
-- 4. ADS层（应用数据层）
-- ============================================

-- ADS层：实时AQI数据表
CREATE TABLE IF NOT EXISTS ads_realtime_aqi (
    station_code    STRING COMMENT '站点编码',
    station_name    STRING COMMENT '站点名称',
    district_name   STRING COMMENT '区县名称',
    latitude        DOUBLE COMMENT '纬度',
    longitude       DOUBLE COMMENT '经度',
    aqi             INT COMMENT '当前AQI',
    aqi_level       INT COMMENT 'AQI等级',
    aqi_level_desc  STRING COMMENT '等级描述',
    primary_pollutant   STRING COMMENT '首要污染物',
    pm25            DOUBLE COMMENT 'PM2.5浓度',
    pm10            DOUBLE COMMENT 'PM10浓度',
    so2             DOUBLE COMMENT 'SO2浓度',
    no2             DOUBLE COMMENT 'NO2浓度',
    co              DOUBLE COMMENT 'CO浓度',
    o3              DOUBLE COMMENT 'O3浓度',
    update_time     TIMESTAMP COMMENT '更新时间'
) COMMENT 'ADS层实时AQI数据表'
STORED AS ORC
TBLPROPERTIES ('orc.compression'='SNAPPY');

-- ADS层：预测结果表
CREATE TABLE IF NOT EXISTS ads_aqi_prediction (
    id              BIGINT,
    station_code    STRING COMMENT '站点编码',
    station_name    STRING COMMENT '站点名称',
    predict_time    TIMESTAMP COMMENT '预测时间点',
    predict_hour    INT COMMENT '预测小时数：24/48/72',
    pm25_pred       DOUBLE COMMENT 'PM2.5预测值',
    pm10_pred       DOUBLE COMMENT 'PM10预测值',
    aqi_pred        INT COMMENT 'AQI预测值',
    aqi_level_pred  INT COMMENT 'AQI等级预测',
    confidence      DOUBLE COMMENT '预测置信度',
    model_type      STRING COMMENT '模型类型',
    create_time     TIMESTAMP COMMENT '创建时间'
) COMMENT 'ADS层AQI预测结果表'
PARTITIONED BY (dt STRING)
STORED AS ORC;

-- ADS层：相关性分析结果表
CREATE TABLE IF NOT EXISTS ads_correlation_result (
    id              BIGINT,
    factor_x        STRING COMMENT '因素X',
    factor_y        STRING COMMENT '因素Y',
    pearson_corr    DOUBLE COMMENT '皮尔逊相关系数',
    spearman_corr   DOUBLE COMMENT '斯皮尔曼相关系数',
    p_value         DOUBLE COMMENT 'P值',
    significance    STRING COMMENT '显著性',
    analysis_time   TIMESTAMP COMMENT '分析时间',
    data_range      STRING COMMENT '数据范围'
) COMMENT 'ADS层相关性分析结果表'
STORED AS ORC;

-- ADS层：统计报表表
CREATE TABLE IF NOT EXISTS ads_statistics_report (
    id              BIGINT,
    report_type     STRING COMMENT '报表类型：daily/monthly/seasonly/yearly',
    district_code   STRING COMMENT '区县编码',
    district_name   STRING COMMENT '区县名称',
    report_date     TIMESTAMP COMMENT '报表日期',
    avg_aqi         DOUBLE COMMENT '平均AQI',
    max_aqi         INT COMMENT '最大AQI',
    min_aqi         INT COMMENT '最小AQI',
    clean_days      INT COMMENT '优良天数',
    clean_rate      DOUBLE COMMENT '优良率',
    main_pollutant  STRING COMMENT '主要污染物',
    trend           STRING COMMENT '变化趋势',
    create_time     TIMESTAMP COMMENT '创建时间'
) COMMENT 'ADS层统计报表表'
PARTITIONED BY (year STRING, month STRING)
STORED AS ORC;

-- ============================================
-- 维度表
-- ============================================

-- 站点维度表
CREATE TABLE IF NOT EXISTS dim_station (
    station_code    STRING COMMENT '站点编码',
    station_name    STRING COMMENT '站点名称',
    district_code   STRING COMMENT '区县编码',
    district_name   STRING COMMENT '区县名称',
    city_name       STRING COMMENT '城市名称',
    latitude        DOUBLE COMMENT '纬度',
    longitude       DOUBLE COMMENT '经度',
    station_type    STRING COMMENT '站点类型：国控/省控/市控',
    monitor_level    STRING COMMENT '监测级别',
    is_active       INT COMMENT '是否启用',
    start_date      STRING COMMENT '启用日期',
    end_date        STRING COMMENT '停用日期'
) COMMENT '站点维度表'
STORED AS ORC;

-- 时间维度表
CREATE TABLE IF NOT EXISTS dim_time (
    time_id         BIGINT COMMENT '时间ID',
    full_time       TIMESTAMP COMMENT '完整时间',
    year            INT COMMENT '年',
    quarter         INT COMMENT '季度',
    month           INT COMMENT '月',
    month_name      STRING COMMENT '月份名称',
    week_of_year    INT COMMENT '年内周数',
    day_of_year     INT COMMENT '年内天数',
    day_of_month    INT COMMENT '月内天数',
    day_of_week     INT COMMENT '星期几',
    day_name        STRING COMMENT '星期名称',
    hour            INT COMMENT '小时',
    hour_part       STRING COMMENT '时段：凌晨/上午/下午/晚上',
    is_workday      INT COMMENT '是否工作日',
    is_holiday      INT COMMENT '是否节假日',
    holiday_name    STRING COMMENT '节假日名称',
    season          INT COMMENT '季节',
    season_name     STRING COMMENT '季节名称'
) COMMENT '时间维度表'
STORED AS ORC;

-- AQI等级维度表
CREATE TABLE IF NOT EXISTS dim_aqi_level (
    level_id        INT COMMENT '等级ID',
    level_name      STRING COMMENT '等级名称',
    level_range     STRING COMMENT '范围描述',
    aqi_min         INT COMMENT 'AQI最小值',
    aqi_max         INT COMMENT 'AQI最大值',
    color_code      STRING COMMENT '颜色代码',
    health_advice   STRING COMMENT '健康建议',
    affect_level    STRING COMMENT '影响程度'
) COMMENT 'AQI等级维度表'
ROW FORMAT DELIMITED
FIELDS TERMINATED BY '\t'
STORED AS TEXTFILE;

-- 污染物信息维度表
CREATE TABLE IF NOT EXISTS dim_pollutant (
    pollutant_code  STRING COMMENT '污染物代码',
    pollutant_name  STRING COMMENT '污染物名称',
    pollutant_unit  STRING COMMENT '单位',
    formula         STRING COMMENT '化学式',
    source_desc     STRING COMMENT '来源描述',
    hazard_desc     STRING COMMENT '危害描述'
) COMMENT '污染物信息维度表'
STORED AS ORC;

-- ============================================
-- 数据质量检查表
-- ============================================

CREATE TABLE IF NOT EXISTS data_quality_log (
    id              BIGINT,
    table_name      STRING COMMENT '表名',
    check_type      STRING COMMENT '检查类型',
    check_rule      STRING COMMENT '检查规则',
    total_count     BIGINT COMMENT '总记录数',
    valid_count     BIGINT COMMENT '有效记录数',
    invalid_count   BIGINT COMMENT '无效记录数',
    invalid_rate    DOUBLE COMMENT '无效率',
    check_result    STRING COMMENT '检查结果',
    check_time      TIMESTAMP COMMENT '检查时间'
) COMMENT '数据质量检查日志表'
PARTITIONED BY (dt STRING)
STORED AS ORC;

-- ============================================
-- 分区示例
-- ============================================

-- 动态分区设置
SET hive.exec.dynamic.partition=true;
SET hive.exec.dynamic.partition.mode=nonstrict;
SET hive.exec.max.dynamic.partitions=1000;
SET hive.exec.max.dynamic.partitions.per.node=100;

-- 插入数据示例
INSERT INTO ods_air_quality_raw PARTITION(dt='2024-01-15')
VALUES (1, '410101', '郑州', '2024-01-15 10:00:00', 45.0, 78.0, 15.0, 32.0, 0.8, 65.0, 78, 'PM2.5', '良');

-- ============================================
-- 视图定义
-- ============================================

-- 实时AQI视图
CREATE VIEW v_realtime_aqi AS
SELECT 
    station_code,
    station_name,
    district_name,
    aqi,
    CASE 
        WHEN aqi <= 50 THEN '优'
        WHEN aqi <= 100 THEN '良'
        WHEN aqi <= 150 THEN '轻度污染'
        WHEN aqi <= 200 THEN '中度污染'
        WHEN aqi <= 300 THEN '重度污染'
        ELSE '严重污染'
    END AS level_desc,
    primary_pollutant,
    update_time
FROM ads_realtime_aqi;

-- 月度趋势视图
CREATE VIEW v_monthly_trend AS
SELECT 
    year,
    month,
    AVG(pm25_avg) as avg_pm25,
    AVG(pm10_avg) as avg_pm10,
    AVG(aqi_avg) as avg_aqi,
    SUM(clean_days) as total_clean_days,
    AVG(clean_rate) as avg_clean_rate
FROM dws_month_summary
GROUP BY year, month
ORDER BY year, month;