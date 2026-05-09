# 基于Hive的城市空气质量分析与预测系统

[![Java](https://img.shields.io/badge/Java-1.8-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7-green.svg)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3.0-green.svg)](https://vuejs.org/)

> 毕业设计 - 基于Hadoop生态的空气质量大数据分析与预测平台

## 📋 项目简介

本项目是一个完整的城市空气质量监测、分析与预测大数据平台，采用前后端分离架构。

### 核心功能

- **数据可视化**: Vue 3 + ECharts 实现交互式数据展示
- **数据分析**: 趋势分析、相关性分析、空间分析
- **预测模型**: AQI预测模型
- **Web服务**: Spring Boot 提供RESTful API

## 🏗️ 技术架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        应用层 (Presentation)                     │
│  ┌─────────────────┐  ┌─────────────────┐                      │
│  │   Web前端        │  │    Web后端       │                      │
│  │  (Vue/ECharts)  │  │  (Spring Boot)  │                      │
│  └────────┬────────┘  └────────┬────────┘                      │
└───────────┼────────────────────┼────────────────────────────────┘
            │                    │
            └────────────────────┘
```

## 📁 项目结构

```
air-quality-analysis/
├── 📂 air-web/                     # Web服务模块 (Spring Boot)
│   └── 📂 src/main/java/cn/edu/zzu/airweb/
│       ├── 📂 controller/          # API控制器
│       ├── 📂 service/             # 业务逻辑
│       ├── 📂 mapper/              # 数据访问
│       ├── 📂 entity/              # 实体类
│       ├── 📂 common/              # 公共组件
│       └── 📂 config/              # 配置类
│
├── 📂 air-web-frontend/            # 前端模块 (Vue 3)
│   ├── 📂 src/
│   │   ├── 📂 views/               # 页面组件
│   │   ├── 📂 api/                 # API接口
│   │   ├── 📂 router/              # 路由配置
│   │   └── 📂 utils/               # 工具函数
│   ├── index.html
│   └── package.json
│
├── pom.xml                         # Maven父POM
└── README.md                       # 项目说明
```

## 🚀 快速开始

### 环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| JDK | 1.8+ | Java开发环境 |
| MySQL | 8.0+ | 业务数据库 |
| Redis | 6.0+ | 缓存（可选） |
| Node.js | 16+ | 前端运行环境 |

### 1. 克隆项目

```bash
git clone https://github.com/your-repo/air-quality-analysis.git
cd air-quality-analysis
```

### 2. 启动后端服务

```bash
# 编译并启动Web模块
cd air-web
mvn clean spring-boot:run
```

后端服务地址: http://localhost:8080

### 3. 启动前端服务

```bash
cd air-web-frontend
npm install
npm run dev
```

前端地址: http://localhost:3000

## 📈 API接口

### 空气质量数据

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/aqi/realtime` | GET | 获取实时AQI数据 |
| `/api/aqi/history` | GET | 获取历史数据 |
| `/api/aqi/trend` | GET | 获取趋势数据 |
| `/api/aqi/statistics` | GET | 获取统计信息 |

### 预测数据

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/prediction/aqi` | GET | 获取AQI预测 |

### 空间分析

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/spatial/heatmap` | GET | 获取空间热力图数据 |
| `/api/spatial/district` | GET | 获取区县统计数据 |

### 相关性分析

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/correlation/matrix` | GET | 获取污染物相关性矩阵 |

## 📚 文档

- [前端开发指南](air-web-frontend/README.md)
- [后端开发指南](air-web/README.md)

