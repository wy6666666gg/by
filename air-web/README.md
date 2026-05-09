# Air Quality Analysis System - Backend API

基于Spring Boot的空气质量数据查询与预测API服务

## 项目说明

本项目是《基于Hive的城市空气质量分析与预测系统》的Web可视化模块后端部分。

## 技术栈

- **框架**: Spring Boot 2.7.x
- **数据库**: MySQL 8.0 + Hive 3.x
- **ORM**: MyBatis-Plus
- **数据源**: Druid连接池
- **安全**: Spring Security + JWT
- **文档**: Swagger2

## API接口

### 1. 空气质量数据接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/v1/aqi/realtime` | GET | 获取实时AQI数据 |
| `/api/v1/aqi/history` | GET | 获取历史AQI数据 |
| `/api/v1/aqi/trend` | GET | 获取趋势数据 |
| `/api/v1/aqi/station/{code}` | GET | 获取指定站点数据 |

### 2. 统计分析接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/v1/analysis/spatial` | GET | 空间分布分析 |
| `/api/v1/analysis/correlation` | GET | 相关性分析 |
| `/api/v1/analysis/trend` | GET | 趋势分析 |
| `/api/v1/analysis/statistics` | GET | 统计报表 |

### 3. 预测接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/v1/predict/{stationCode}` | GET | 获取站点预测 |
| `/api/v1/predict/24h` | GET | 24小时预测 |
| `/api/v1/predict/72h` | GET | 72小时预测 |

### 4. 系统管理接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/v1/admin/stations` | GET/POST | 站点管理 |
| `/api/v1/admin/etl` | POST | ETL任务触发 |

## 启动方式

```bash
cd air-web
mvn spring-boot:run
```

## 环境变量

```properties
# MySQL配置
spring.datasource.url=jdbc:mysql://localhost:3306/air_quality_db
spring.datasource.username=root
spring.datasource.password=your_password

# Hive配置
spring.datasource.hive.url=jdbc:hive2://localhost:10000/air_quality_db
spring.datasource.hive.username=your_username

# Redis配置（可选，用于缓存）
spring.redis.host=localhost
spring.redis.port=6379
```