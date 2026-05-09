# 空气质量数据采集模块

基于Python的空气质量与气象数据采集系统，支持定时调度和数据持久化。

## 功能特性

- **多站点采集**: 支持郑州市15个空气质量监测站点
- **气象数据**: 采集温度、湿度、风速、气压等气象要素
- **智能调度**: 基于APScheduler的定时任务调度
- **数据验证**: AQI自动计算与数据质量校验
- **灵活配置**: 支持YAML配置文件和环境变量
- **日志系统**: 结构化日志记录与自动轮转

## 快速开始

### 安装

```bash
# 创建虚拟环境
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# 安装依赖
pip install -r requirements.txt

# 配置环境变量
cp .env.example .env
# 编辑 .env 配置数据库等信息
```

### 使用

```bash
# 立即采集空气质量数据
python -m collector.air_station_crawler

# 立即采集气象数据
python -m collector.weather_crawler

# 启动定时调度（默认每小时采集空气质量，每3小时采集气象数据）
python -m collector.scheduler

# 查看帮助
python -m collector.scheduler --help
```

## 配置说明

编辑 `.env` 文件配置以下参数：

```bash
# API配置
API_BASE_URL=http://www.cnemc.cn
API_TIMEOUT=30
REQUEST_RETRY_TIMES=3

# 数据存储
OUTPUT_DIR=./data
OUTPUT_FORMAT=both  # json, csv, both

# 数据库配置
DB_HOST=localhost
DB_PORT=3306
DB_NAME=air_quality_db
DB_USER=root
DB_PASSWORD=your_password
```

## 项目结构

```
collector/
├── __init__.py
├── base_crawler.py          # 采集器基类
├── air_station_crawler.py   # 空气质量采集器
├── weather_crawler.py       # 气象数据采集器
└── scheduler.py             # 任务调度器

config/
├── __init__.py
└── settings.py              # 配置管理

data/                        # 数据输出目录
logs/                        # 日志目录
```

## 数据格式

### 空气质量数据

```json
{
  "station_code": "410101",
  "station_name": "郑州郑纺机",
  "monitor_time": "2024-01-15 14:00:00",
  "pm25": 45.5,
  "pm10": 78.0,
  "so2": 15.2,
  "no2": 32.1,
  "co": 0.8,
  "o3": 65.0,
  "aqi": 78,
  "primary_pollutant": "PM2.5",
  "quality_level": "良",
  "aqi_level_id": 2
}
```

### 气象数据

```json
{
  "station_code": "54511",
  "station_name": "郑州",
  "monitor_time": "2024-01-15 14:00:00",
  "temperature": 15.5,
  "humidity": 65.0,
  "wind_speed": 3.2,
  "wind_direction": 135,
  "wind_direction_name": "东南",
  "pressure": 1013.2,
  "visibility": 10.5,
  "weather_type": "多云"
}
```

## 定时任务

| 任务 | 频率 | 说明 |
|------|------|------|
| 空气质量采集 | 每小时 | 采集所有站点实时数据 |
| 气象数据采集 | 每3小时 | 采集所有气象站点数据 |
| 每日报告 | 每天8:00 | 生成数据质量报告 |
| 数据清理 | 每周一2:00 | 清理30天前的过期数据 |

## 扩展开发

### 自定义采集器

```python
from collector.base_crawler import BaseCrawler

class MyCrawler(BaseCrawler):
    def collect(self):
        # 实现采集逻辑
        data = []
        # ...
        return data

# 使用
with MyCrawler() as crawler:
    report = crawler.run()
```

## 许可证

MIT License
