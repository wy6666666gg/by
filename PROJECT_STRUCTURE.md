# 基于Hive的城市空气质量分析与预测系统 - 项目结构

## 系统架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           应用层 (Presentation)                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  前端 (Vue 3 + Element Plus + ECharts + Leaflet)               │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐   │   │
│  │  │Dashboard│ │ History │ │ Spatial │ │   Map   │ │Predict  │   │   │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘   │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐   │   │
│  │  │Correlation│ │ Alerts │ │Statistics│ │ Data  │ │ System │   │   │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                              ↓                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  后端 (Spring Boot + MyBatis Plus + Druid + Redis)              │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐   │   │
│  │  │AqiController│ │Spatial │ │Correlation│ │Prediction│ │Alert │   │   │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘   │   │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐                           │   │
│  │  │System   │ │Station  │ │Data ETL │                           │   │
│  │  └─────────┘ └─────────┘ └─────────┘                           │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                           分析层 (Analytics)                             │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  Spark + Spark MLlib + Spark SQL                                 │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐   │   │
│  │  │ ETL Job     │  │ 分析任务    │  │ 预测模型                 │   │   │
│  │  │ DataETL     │  │ Trend/Spatial│ │ RandomForest/XGBoost    │   │   │
│  │  └─────────────┘  │ Correlation  │ │ LSTM/CNN-LSTM           │   │   │
│  │                   └─────────────┘  └─────────────────────────┘   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                           存储层 (Storage)                               │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  Hive 数据仓库 (HDFS)                                            │   │
│  │  ┌─────┐    ┌─────┐    ┌─────┐    ┌─────┐                      │   │
│  │  │ ODS │ →  │ DWD │ →  │ DWS │ →  │ ADS │                      │   │
│  │  │原始  │    │明细  │    │汇总  │    │应用  │                      │   │
│  │  └─────┘    └─────┘    └─────┘    └─────┘                      │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  MySQL (业务数据) + Redis (缓存)                                 │   │
│  │  ┌───────────┐    ┌───────────┐                                 │   │
│  │  │ station   │    │ alert_rule│                                 │   │
│  │  │ alert     │    │ user      │                                 │   │
│  │  └───────────┘    └───────────┘                                 │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│                          采集层 (Collection)                             │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │  Python 数据采集模块 (air-data-collector)                        │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │   │
│  │  │ 空气质量爬虫 │  │ 气象数据爬虫 │  │ 定时调度器  │              │   │
│  │  │ CNEMC API   │  │ Weather API │  │ APScheduler │              │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘              │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

## 项目目录结构

```
air-quality-analysis/                     # 项目根目录
├── README.md                             # 项目主说明文档
├── pom.xml                               # Maven父POM
├── PROJECT_STRUCTURE.md                  # 项目结构文档 (本文档)
├── FRONTEND_GUIDE.md                     # 前端使用指南
├── OPTIMIZATION_SUMMARY.md               # 优化总结
├── start-data-collector.bat/.sh          # 数据采集启动脚本
├── start-web-frontend.bat                # 前端启动脚本
│
├── air-web/                              # Web服务模块 (Spring Boot)
│   ├── pom.xml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/cn/edu/zzu/airweb/
│   │   │   │   ├── controller/          # REST API控制器
│   │   │   │   │   ├── AqiController.java
│   │   │   │   │   ├── PredictionController.java
│   │   │   │   │   ├── SpatialController.java
│   │   │   │   │   ├── CorrelationController.java
│   │   │   │   │   ├── AlertController.java
│   │   │   │   │   └── SystemController.java
│   │   │   │   ├── service/             # 业务逻辑层
│   │   │   │   │   ├── AqiService.java
│   │   │   │   │   ├── PredictionService.java
│   │   │   │   │   ├── SpatialService.java
│   │   │   │   │   ├── CorrelationService.java
│   │   │   │   │   ├── AlertService.java
│   │   │   │   │   └── SystemService.java
│   │   │   │   ├── mapper/              # 数据访问层
│   │   │   │   │   ├── AqiMapper.java
│   │   │   │   │   ├── PredictionMapper.java
│   │   │   │   │   ├── SpatialMapper.java
│   │   │   │   │   ├── CorrelationMapper.java
│   │   │   │   │   └── AlertMapper.java
│   │   │   │   ├── entity/              # 实体类
│   │   │   │   │   ├── Station.java
│   │   │   │   │   ├── AqiPrediction.java
│   │   │   │   │   └── AlertRule.java
│   │   │   │   ├── common/              # 公共组件
│   │   │   │   │   └── Result.java
│   │   │   │   └── config/              # 配置类
│   │   │   └── resources/
│   │   │       ├── application.yml      # 应用配置
│   │   │       └── mapper/              # MyBatis XML
│   │   │           ├── AqiMapper.xml
│   │   │           └── PredictionMapper.xml
│   │   └── test/
│   └── README.md
│
├── air-spark-analysis/                   # Spark分析模块
│   ├── pom.xml
│   └── src/main/java/cn/edu/zzu/airanalysis/
│       ├── etl/                          # ETL作业
│       │   └── DataETL.java
│       ├── analysis/                     # 分析任务
│       │   ├── TrendAnalysis.java
│       │   ├── SpatialAnalysis.java
│       │   └── CorrelationAnalysis.java
│       ├── prediction/                   # 预测模型
│       │   └── AQIPredictionModel.java
│       └── entity/                       # 实体类
│   └── src/main/resources/
│       └── hive-sql/                     # Hive SQL脚本
│           └── 01_database_and_tables.sql
│
├── air-data-collector/                   # 数据采集模块 (Python)
│   ├── requirements.txt
│   ├── .env.example
│   ├── README.md
│   ├── collector/                        # 采集器实现
│   │   ├── base_crawler.py
│   │   ├── air_station_crawler.py
│   │   ├── weather_crawler.py
│   │   └── scheduler.py
│   └── config/                           # 配置管理
│       └── settings.py
│
├── air-web-frontend/                     # 前端模块 (Vue 3)
│   ├── package.json
│   ├── vite.config.js
│   ├── index.html
│   ├── README.md
│   └── src/
│       ├── main.js                       # 入口文件
│       ├── App.vue                       # 根组件
│       ├── router/index.js               # 路由配置
│       ├── api/                          # API接口
│       │   ├── aqi.js
│       │   ├── prediction.js
│       │   ├── spatial.js
│       │   ├── correlation.js
│       │   ├── alert.js
│       │   └── system.js
│       ├── utils/request.js              # Axios封装
│       └── views/                        # 页面组件
│           ├── Layout.vue                # 布局组件
│           ├── Dashboard.vue             # 实时监控
│           ├── History.vue               # 历史趋势
│           ├── SpatialAnalysis.vue       # 空间分析
│           ├── SpatialMap.vue            # GIS地图
│           ├── CorrelationAnalysis.vue   # 相关性分析
│           ├── Prediction.vue            # 预测分析
│           ├── AlertCenter.vue           # 预警中心
│           ├── Statistics.vue            # 统计报表
│           ├── DataManage.vue            # 数据管理
│           └── SystemManage.vue          # 系统管理
│
├── docs/                                 # 项目文档
│   ├── 毕业论文.md
│   └── 系统设计文档.md
│
└── .codebuddy/                           # AI助手配置
    └── agents/
        └── 毕业设计.md
```

## 模块说明

### 1. air-web (Web服务模块)
- **技术栈**: Spring Boot 2.7 + MyBatis Plus + Druid + Redis
- **端口**: 8080
- **功能**: RESTful API、数据缓存、业务逻辑处理
- **API文档**: http://localhost:8080/api/swagger-ui.html

### 2. air-spark-analysis (Spark分析模块)
- **技术栈**: Spark 3.2 + Spark MLlib + Spark SQL
- **功能**: ETL数据处理、多维分析、预测建模
- **部署**: 提交到YARN集群运行

### 3. air-data-collector (数据采集模块)
- **技术栈**: Python 3.8 + Requests + APScheduler
- **功能**: 定时爬取空气质量与气象数据
- **调度**: 支持crontab或Airflow

### 4. air-web-frontend (前端模块)
- **技术栈**: Vue 3 + Element Plus + ECharts + Leaflet
- **端口**: 3000
- **功能**: 数据可视化、GIS地图、实时监控

## 数据流向

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  数据采集       │────→│  数据清洗       │────→│  数据存储       │
│  (Python)       │     │  (Spark)        │     │  (Hive/HDFS)    │
└─────────────────┘     └─────────────────┘     └────────┬────────┘
                                                         │
                              ┌──────────────────────────┘
                              ↓
                    ┌─────────────────┐
                    │  数据分析       │
                    │  (Spark SQL)    │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              ↓              ↓              ↓
       ┌────────────┐ ┌────────────┐ ┌────────────┐
       │  趋势分析   │ │ 空间分析   │ │ 预测模型   │
       └─────┬──────┘ └─────┬──────┘ └─────┬──────┘
             │              │              │
             └──────────────┼──────────────┘
                            ↓
                   ┌─────────────────┐
                   │  Web API        │
                   │  (Spring Boot)  │
                   └────────┬────────┘
                            │
                            ↓
                   ┌─────────────────┐
                   │  前端展示       │
                   │  (Vue 3)        │
                   └─────────────────┘
```

## 运行流程

```bash
# 1. 启动后端服务
cd air-web
mvn spring-boot:run

# 2. 启动前端服务
cd air-web-frontend
npm run dev

# 3. 启动数据采集(可选)
cd air-data-collector
python -m collector.scheduler

# 4. 运行Spark分析(可选)
cd air-spark-analysis
spark-submit --class cn.edu.zzu.airanalysis.etl.DataETL target/air-spark-analysis-1.0-SNAPSHOT.jar
```

## 核心功能对应毕业设计要求

| 毕业设计要求 | 对应模块 | 实现文件 |
|-------------|---------|---------|
| 数据采集与预处理 | air-data-collector | air_station_crawler.py, weather_crawler.py |
| 分层数据仓库 | air-spark-analysis | 01_database_and_tables.sql, DataETL.java |
| 时空分布分析 | air-web-frontend + air-web | SpatialAnalysis.vue, SpatialController.java |
| 相关性分析 | air-web-frontend + air-web | CorrelationAnalysis.vue, CorrelationController.java |
| 趋势分析 | air-web-frontend + air-web | History.vue, TrendAnalysis.java |
| 预测模型 | air-spark-analysis | AQIPredictionModel.java |
| 可视化Web | air-web-frontend | Dashboard.vue, Prediction.vue, AlertCenter.vue |
| 预警推送 | air-web-frontend + air-web | AlertCenter.vue, AlertController.java |

## 数据库设计

### MySQL (业务库)
- station: 监测站点信息
- alert_rule: 预警规则配置
- alert_record: 预警记录
- user: 系统用户

### Hive (数据仓库)
- ods_air_quality_raw: 空气质量原始数据
- ods_weather_raw: 气象原始数据
- dwd_air_quality_dt: 空气质量明细
- dwd_weather_dt: 气象明细
- dws_station_hour: 站点小时汇总
- dws_district_day: 区县日汇总
- ads_realtime_aqi: 实时AQI应用表
- ads_aqi_prediction: AQI预测结果

## 联系方式

郑州大学 计算机与人工智能学院
大数据技术与应用课程设计
