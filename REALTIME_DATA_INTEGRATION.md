# 郑州市空气质量实时数据集成说明

## 概述

本系统已实现与 `https://citydev.gbqyun.com/index/zhengzhou` 网站的数据集成，数据概览和实时监测模块会显示来自该网站的实时空气质量数据。

## 数据获取方式

由于目标网站使用前端渲染技术，无法直接通过API获取数据，系统提供以下三种数据获取方式：

### 方式一：Python 抓取工具（推荐）

使用提供的 Python 脚本定期从网站抓取数据。

#### 1. 安装依赖

```bash
cd air-data-collector
pip install playwright pandas
playwright install chromium
```

#### 2. 运行抓取脚本

**单次抓取：**
```bash
python zhengzhou_data_fetcher.py
```

**使用模拟数据（测试用）：**
```bash
python zhengzhou_data_fetcher.py --mock
```

**定时抓取（每30分钟）：**
```bash
python zhengzhou_data_fetcher.py --schedule
```

#### 3. 数据文件位置

抓取的数据会保存到：`air-data-collector/data/zhengzhou_realtime.csv`

### 方式二：手动数据录入

直接编辑 `air-data-collector/data/zhengzhou_realtime.csv` 文件，格式如下：

```csv
stationCode,stationName,actualStation,date,aqi,qualityLevel,pm25,pm10,o3,so2,co,no2,primaryPollutant,isSandDustDay,updateTime,dataSource
410101,中原区,北区建设指挥部,2025-01-08,125,轻度污染,81.2,118.8,95.0,15.0,0.8,45.0,PM2.5,False,2025-01-08 14:30:00,gbqyun
410102,金水区,北区建设指挥部,2025-01-08,125,轻度污染,81.2,118.8,95.0,15.0,0.8,45.0,PM2.5,False,2025-01-08 14:30:00,gbqyun
410103,二七区,河医大,2025-01-08,158,中度污染,102.7,150.1,88.0,18.0,1.0,52.0,PM2.5,False,2025-01-08 14:30:00,gbqyun
410108,惠济区,惠济区政府,2025-01-08,85,良,55.2,80.8,102.0,12.0,0.6,38.0,PM10,False,2025-01-08 14:30:00,gbqyun
410104,郑东新区,经开区管委,2025-01-08,132,轻度污染,85.8,125.4,90.0,16.0,0.9,48.0,PM2.5,False,2025-01-08 14:30:00,gbqyun
```

### 方式三：自动降级机制

当没有实时数据时，系统会自动使用历史CSV数据或模拟数据，确保页面正常显示。

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                    前端 (Vue.js)                             │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────────┐   │
│  │  数据概览    │  │  实时监测    │  │  其他模块...    │   │
│  └──────────────┘  └──────────────┘  └─────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   后端 (Spring Boot)                         │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         ZhengzhouDataService 数据服务                 │  │
│  │  ┌────────────────────────────────────────────────┐  │  │
│  │  │  数据获取优先级：                               │  │  │
│  │  │  1. 实时数据CSV (zhengzhou_realtime.csv)        │  │  │
│  │  │  2. 网站API抓取 ( citydev.gbqyun.com )          │  │  │
│  │  │  3. 历史数据CSV (zhengzhou_districts_5years_)   │  │  │
│  │  │  4. 模拟数据 (默认后备)                          │  │  │
│  │  └────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     数据文件                                 │
│  ┌─────────────────────────┐  ┌─────────────────────────┐  │
│  │  zhengzhou_realtime.csv │  │ 历史数据 CSV (后备)      │  │
│  │  (Python脚本生成)        │  │                         │  │
│  └─────────────────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## 启动步骤

### 1. 启动数据抓取（可选但推荐）

```bash
cd air-data-collector
python zhengzhou_data_fetcher.py --schedule
```

### 2. 启动后端服务

```bash
cd air-web
mvn spring-boot:run
```

### 3. 启动前端服务

```bash
cd air-web-frontend
npm run dev
```

### 4. 访问系统

打开浏览器访问：`http://localhost:5173`

## 数据源显示

前端页面会显示当前数据的来源：

- **绿色标签** `citydev.gbqyun.com`：来自目标网站的实时数据
- **黄色标签** `本地历史数据`：来自历史CSV文件
- **灰色标签** `模拟数据`：系统生成的模拟数据

## 自动刷新机制

- **前端**：每30秒自动刷新一次数据
- **后端**：每5分钟尝试从网站抓取数据
- **Python脚本**：每30分钟抓取一次数据（如果使用 `--schedule` 模式）

## 注意事项

1. **网络要求**：Python抓取脚本需要能够访问 `https://citydev.gbqyun.com`
2. **数据时效性**：实时数据文件 `zhengzhou_realtime.csv` 中的数据会被视为最新数据
3. **数据格式**：确保CSV文件使用UTF-8编码，字段使用逗号分隔
4. **站点映射**：
   - 中原区、金水区 → 北区建设指挥部
   - 二七区 → 河医大
   - 惠济区 → 惠济区政府
   - 郑东新区 → 经开区管委

## 故障排查

### 问题：页面显示"模拟数据"

**原因**：
1. Python抓取脚本未运行
2. 实时数据CSV文件不存在或格式错误
3. 后端无法读取CSV文件

**解决方案**：
1. 检查 `air-data-collector/data/zhengzhou_realtime.csv` 是否存在
2. 手动运行 `python zhengzhou_data_fetcher.py` 生成数据
3. 检查后端日志确认CSV文件路径是否正确

### 问题：Python脚本抓取失败

**原因**：
1. 网站结构变化
2. 网络连接问题
3. Playwright未正确安装

**解决方案**：
1. 检查错误截图 `debug_screenshot.png` 或 `error_screenshot.png`
2. 使用模拟数据模式测试：`python zhengzhou_data_fetcher.py --mock`
3. 重新安装 Playwright：`pip install playwright && playwright install chromium`

### 问题：数据不更新

**原因**：
1. CSV文件未被重新加载
2. 浏览器缓存

**解决方案**：
1. 刷新页面（F5）
2. 清除浏览器缓存
3. 重启后端服务

## 自定义开发

如需修改数据抓取逻辑，可以编辑以下文件：

- **Python抓取脚本**：`air-data-collector/zhengzhou_data_fetcher.py`
- **后端数据服务**：`air-web/src/main/java/cn/edu/zzu/airweb/service/ZhengzhouDataService.java`
- **前端数据展示**：`air-web-frontend/src/views/Dashboard.vue` 和 `RealtimeDashboard.vue`
