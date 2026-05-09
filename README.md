# 基于Hive的城市空气质量分析与预测系统

[![Java](https://img.shields.io/badge/Java-1.8-blue.svg)](https://www.oracle.com/java/)
[![Spark](https://img.shields.io/badge/Spark-3.2.0-orange.svg)](https://spark.apache.org/)
[![Hive](https://img.shields.io/badge/Hive-3.1.2-yellow.svg)](https://hive.apache.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7-green.svg)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.8+-blue.svg)](https://www.python.org/)

> 郑州大学大数据课程设计项目 - 基于Hadoop生态的空气质量大数据分析与预测平台

## 📋 项目简介

本项目是一个完整的城市空气质量监测、分析与预测大数据平台，基于Hadoop生态系统构建，采用Lambda架构实现离线分析与实时处理的结合。

### 核心功能

- **数据采集**: Python多线程爬虫自动采集空气质量与气象数据
- **数据存储**: 基于HDFS + Hive的分层数据仓库（ODS/DWD/DWS/ADS）
- **数据处理**: Spark SQL实现高性能ETL与数据分析
- **机器学习**: Spark MLlib构建AQI预测模型（随机森林/线性回归）
- **可视化**: Spring Boot + ECharts实现交互式数据展示

## 🏗️ 技术架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        应用层 (Presentation)                     │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  │
│  │   Web可视化      │  │    数据API      │  │   报表导出      │  │
│  │  (Vue/ECharts)  │  │  (Spring Boot)  │  │   (Excel/PDF)   │  │
│  └────────┬────────┘  └────────┬────────┘  └─────────────────┘  │
└───────────┼────────────────────┼────────────────────────────────┘
            │                    │
            ▼                    ▼
┌─────────────────────────────────────────────────────────────────┐
│                        分析层 (Analytics)                        │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              Spark MLlib 预测模型                          │  │
│  │         (随机森林 / 线性回归 / 时间序列分析)                  │  │
│  └──────────────────────────────────────────────────────────┘  │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  │
│  │   趋势分析       │  │   相关性分析     │  │   空间分析       │  │
│  │  (时间序列分解)  │  │  (皮尔逊/斯皮尔曼)│  │  (地理分布热力图)│  │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        存储层 (Storage)                          │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    Hive 数据仓库                          │  │
│  │  ┌─────┐  ┌─────┐  ┌─────┐  ┌─────┐  ┌────────────────┐ │  │
│  │  │ ODS │→ │ DWD │→ │ DWS │→ │ ADS │  │   维度表        │ │  │
│  │  │(原始)│  │(明细)│  │(汇总)│  │(应用)│  │(站点/时间/AQI)  │ │  │
│  │  └─────┘  └─────┘  └─────┘  └─────┘  └────────────────┘ │  │
│  └────────────────────────┬─────────────────────────────────┘  │
│                           │ HDFS                               │
└───────────────────────────┼────────────────────────────────────┘
                            │
┌───────────────────────────┼────────────────────────────────────┐
│                        采集层 (Collection)                     │
│                           │                                     │
│  ┌────────────────────────┴────────────────────────┐           │
│  │              Python 数据采集模块                  │           │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────┐  │           │
│  │  │ 空气质量爬虫 │  │ 气象数据爬虫 │  │ 定时调度 │  │           │
│  │  │(CNEMC API)  │  │(气象API)    │  │(APScheduler)│        │
│  │  └─────────────┘  └─────────────┘  └─────────┘  │           │
│  └─────────────────────────────────────────────────┘           │
└─────────────────────────────────────────────────────────────────┘
```

## 📁 项目结构

```
air-quality-analysis/
├── 📂 air-data-collector/          # 数据采集模块 (Python)
│   ├── 📂 config/                  # 配置管理
│   │   ├── __init__.py
│   │   └── settings.py             # 统一配置中心
│   ├── 📂 collector/               # 采集器实现
│   │   ├── __init__.py
│   │   ├── base_crawler.py         # 采集基类
│   │   ├── air_station_crawler.py  # 空气质量采集
│   │   ├── weather_crawler.py      # 气象数据采集
│   │   └── scheduler.py            # 任务调度器
│   ├── 📂 data/                    # 数据存储目录 (gitignore)
│   ├── 📂 logs/                    # 日志目录 (gitignore)
│   ├── requirements.txt            # Python依赖
│   ├── .env.example                # 环境变量示例
│   └── README.md
│
├── 📂 air-spark-analysis/          # 数据分析模块 (Java/Spark)
│   └── 📂 src/main/java/cn/edu/zzu/airanalysis/
│       ├── 📂 etl/                 # ETL数据处理
│       │   └── DataETL.java        # 分层ETL主程序
│       ├── 📂 analysis/            # 分析模块
│       │   ├── TrendAnalysis.java  # 趋势分析
│       │   ├── CorrelationAnalysis.java # 相关性分析
│       │   └── SpatialAnalysis.java# 空间分析
│       ├── 📂 prediction/          # 预测模型
│       │   └── AQIPredictionModel.java  # AQI预测
│       └── 📂 entity/              # 实体类
│
├── 📂 air-web/                     # Web服务模块 (Spring Boot)
│   └── 📂 src/main/java/cn/edu/zzu/airweb/
│       ├── 📂 controller/          # API控制器
│       ├── 📂 service/             # 业务逻辑
│       ├── 📂 mapper/              # 数据访问
│       ├── 📂 entity/              # 实体类
│       ├── 📂 common/              # 公共组件
│       └── 📂 config/              # 配置类
│
├── 📂 docs/                        # 项目文档
│   ├── 系统设计文档.md
│   └── 毕业论文.md
│
├── pom.xml                         # Maven父POM
└── README.md                       # 项目说明
```

## 🚀 快速开始

### 环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 1.8+ | Java开发环境 |
| Python | 3.8+ | 数据采集 |
| Hadoop | 3.3.1+ | 分布式存储 |
| Hive | 3.1.2+ | 数据仓库 |
| Spark | 3.2.0+ | 计算引擎 |
| MySQL | 8.0+ | 业务数据库 |
| Redis | 6.0+ | 缓存（可选） |

### 1. 克隆项目

```bash
git clone https://github.com/your-repo/air-quality-analysis.git
cd air-quality-analysis
```

### 2. 初始化Hive数据仓库

```bash
hive -f air-spark-analysis/src/main/resources/hive-sql/01_database_and_tables.sql
```

### 3. 配置Python数据采集

```bash
cd air-data-collector

# 创建虚拟环境
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 安装依赖
pip install -r requirements.txt

# 配置环境变量
cp .env.example .env
# 编辑 .env 文件配置数据库等信息
```

### 4. 编译Java模块

```bash
# 编译所有模块
mvn clean package -DskipTests

# 或仅编译Web模块
cd air-web
mvn clean package
```

### 5. 启动服务

```bash
# 启动数据采集（立即执行一次）
cd air-data-collector
python -m collector.scheduler --mode once

# 或启动定时调度
python -m collector.scheduler --mode schedule

# 启动Web服务
cd air-web
java -jar target/air-web-1.0-SNAPSHOT.jar
```

访问: http://localhost:8080/api

## 📊 数据仓库分层

### ODS层 (原始数据层)

| 表名 | 说明 | 存储格式 |
|------|------|----------|
| `ods_air_quality_raw` | 空气质量原始数据 | TEXTFILE |
| `ods_weather_raw` | 气象原始数据 | TEXTFILE |

### DWD层 (明细数据层)

| 表名 | 说明 | 存储格式 |
|------|------|----------|
| `dwd_air_quality_dt` | 空气质量明细 | ORC |
| `dwd_weather_dt` | 气象数据明细 | ORC |

### DWS层 (汇总数据层)

| 表名 | 说明 | 存储格式 |
|------|------|----------|
| `dws_station_hour` | 站点小时汇总 | ORC |
| `dws_district_day` | 区县日汇总 | ORC |
| `dws_month_summary` | 月度汇总 | ORC |

### ADS层 (应用数据层)

| 表名 | 说明 | 存储格式 |
|------|------|----------|
| `ads_realtime_aqi` | 实时AQI数据 | ORC |
| `ads_aqi_prediction` | AQI预测结果 | ORC |
| `ads_statistics_report` | 统计报表 | ORC |

## 🔧 核心功能

### 数据采集

```bash
# 采集空气质量数据
python -m collector.air_station_crawler

# 采集气象数据
python -m collector.weather_crawler

# 启动定时调度
python -m collector.scheduler
```

### ETL数据处理

```bash
# 提交Spark ETL任务
spark-submit \
  --class cn.edu.zzu.airanalysis.etl.DataETL \
  --master yarn \
  --deploy-mode cluster \
  air-spark-analysis/target/air-spark-analysis-1.0-SNAPSHOT.jar
```

### 趋势分析

```bash
spark-submit \
  --class cn.edu.zzu.airanalysis.analysis.TrendAnalysis \
  --master yarn \
  air-spark-analysis/target/air-spark-analysis-1.0-SNAPSHOT.jar
```

### 预测模型

```bash
spark-submit \
  --class cn.edu.zzu.airanalysis.prediction.AQIPredictionModel \
  --master yarn \
  --driver-memory 4g \
  --executor-memory 4g \
  air-spark-analysis/target/air-spark-analysis-1.0-SNAPSHOT.jar
```

## 📈 API接口

### 空气质量数据

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/aqi/realtime` | GET | 获取实时AQI数据 |
| `/api/v1/aqi/history` | GET | 获取历史数据 |
| `/api/v1/aqi/trend` | GET | 获取趋势数据 |
| `/api/v1/aqi/statistics` | GET | 获取统计信息 |

### 预测数据

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/prediction/aqi` | GET | 获取AQI预测 |

## 🧪 测试

```bash
# Python单元测试
cd air-data-collector
pytest tests/ -v --cov=collector

# Java单元测试
mvn test
```

## 📚 文档

- [系统设计文档](docs/系统设计文档.md)
- [Hive数据仓库设计](air-spark-analysis/src/main/resources/hive-sql/01_database_and_tables.sql)
- [API接口文档](http://localhost:8080/api/swagger-ui.html)

## 🤝 贡献

欢迎提交Issue和Pull Request！

## 📄 许可证

本项目仅供学习和研究使用。

## 👥 团队

- **郑州大学** - 计算机与人工智能学院
- 大数据技术与应用课程设计项目

---

<p align="center">
  <sub>Built with ❤️ by ZZU Air Quality Team</sub>
</p>
