# 空气质量分析系统 - 前端使用指南

## 系统功能概览

本系统前端基于 Vue 3 + Element Plus + ECharts + Leaflet 构建，实现了毕业设计文档要求的所有可视化功能。

### 功能模块

| 模块 | 功能描述 | 技术实现 |
|------|----------|----------|
| **实时监控** | AQI实时数据、站点分布、统计卡片 | ECharts图表、定时刷新 |
| **历史趋势** | 多维度历史数据查询、趋势分析 | 日期选择器、折线图、柱状图 |
| **空间分析** | IDW/克里金插值热力图、站点对比 | ECharts Heatmap、Geo图表 |
| **GIS地图** | Leaflet地图、实时站点标记、热力图叠加 | Leaflet、Canvas热力图层 |
| **相关性分析** | 相关系数矩阵、特征重要性、时滞分析 | ECharts Heatmap、散点图 |
| **预测分析** | 24h/72h预测、多站点对比、准确率评估 | 折线图、柱状图、进度条 |
| **预警中心** | 实时预警推送、规则配置、历史记录 | WebSocket模拟、时间线组件 |
| **统计报表** | 日报/月报、达标率、多维度统计 | 饼图、折线图、数据表格 |
| **数据管理** | 数据表管理、导入导出、质量报告 | 上传组件、进度圆环 |
| **系统管理** | 系统监控、日志查看、配置管理 | 仪表盘、实时折线图 |

## 快速启动

### 方式一：使用启动脚本（推荐）

```bash
# Windows
start-web-frontend.bat

# 或手动进入目录启动
cd air-web-frontend
npm install
npm run dev
```

访问 http://localhost:3000

### 方式二：VSCode终端

```bash
cd air-web-frontend
npm install
npm run dev
```

## 项目结构

```
air-web-frontend/
├── index.html                 # HTML入口
├── vite.config.js            # Vite配置（含代理）
├── package.json              # 依赖配置
├── src/
│   ├── main.js              # 应用入口
│   ├── App.vue              # 根组件
│   ├── router/index.js      # 路由配置
│   ├── api/                 # API接口
│   │   ├── aqi.js          # 空气质量接口
│   │   ├── prediction.js   # 预测接口
│   │   ├── spatial.js      # 空间分析接口
│   │   ├── correlation.js  # 相关性分析接口
│   │   ├── alert.js        # 预警接口
│   │   └── system.js       # 系统管理接口
│   ├── utils/request.js    # Axios封装
│   └── views/              # 页面组件
│       ├── Layout.vue      # 布局组件
│       ├── Dashboard.vue   # 实时监控
│       ├── History.vue     # 历史趋势
│       ├── SpatialAnalysis.vue  # 空间分析
│       ├── SpatialMap.vue   # GIS地图
│       ├── CorrelationAnalysis.vue  # 相关性
│       ├── Prediction.vue   # 预测分析
│       ├── AlertCenter.vue  # 预警中心
│       ├── Statistics.vue   # 统计报表
│       ├── DataManage.vue   # 数据管理
│       └── SystemManage.vue # 系统管理
```

## 后端API对接

前端已配置代理，开发时自动转发到后端：

```javascript
// vite.config.js
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

确保后端服务运行在 http://localhost:8080

### 启动后端服务

```bash
cd air-web
mvn spring-boot:run
```

## 技术亮点

### 1. 可视化图表
- **ECharts 5.x**：支持热力图、散点图、矩阵图、时间轴等高级图表
- **响应式设计**：图表自适应窗口大小变化
- **交互丰富**：支持数据缩放、图例筛选、点击查看详情

### 2. GIS地图
- **Leaflet 1.9.x**：轻量级开源地图库
- **热力图层**：自定义Canvas热力叠加
- **站点标记**：动态圆圈标记，颜色根据AQI等级变化
- **交互弹窗**：点击站点显示详细信息

### 3. 实时数据
- **定时刷新**：Dashboard页面每分钟自动刷新
- **模拟推送**：预警中心模拟WebSocket实时推送
- **进度显示**：El-Progress展示各种统计指标

### 4. 主题设计
- **深色主题**：科技蓝绿色调，适合数据可视化
- **Element Plus**：组件风格统一
- **Tailwind-like**：自定义CSS工具类

## 毕业设计对应关系

| 毕业设计要求 | 前端实现 |
|-------------|---------|
| 实时AQI看板 | Dashboard.vue |
| 污染分布热力图 | SpatialAnalysis.vue + SpatialMap.vue |
| 相关性矩阵图 | CorrelationAnalysis.vue |
| 趋势折线图 | History.vue + Prediction.vue |
| 预测结果展示 | Prediction.vue |
| 预警推送 | AlertCenter.vue |
| 数据总览 | Dashboard.vue + Statistics.vue |
| 时空分析 | SpatialAnalysis.vue |
| 系统管理 | SystemManage.vue + DataManage.vue |

## 截图预览

各页面已填充模拟数据，可直接展示：
- **实时监控**：4个统计卡片 + 2个图表 + 数据表格
- **空间分析**：热力图 + 站点对比 + 算法说明
- **相关性分析**：10x10矩阵 + 特征排序 + 时滞分析
- **GIS地图**：8个监测站点 + 实时热力图叠加

## 常见问题

### Q: 地图显示空白？
A: 确保网络可访问 OpenStreetMap 瓦片服务，或更换国内地图源。

### Q: 图表不显示？
A: 检查是否安装了 echarts 依赖：`npm install echarts vue-echarts`

### Q: 后端接口404？
A: 确保后端服务已启动，并检查 `vite.config.js` 中的代理配置。

## 构建部署

```bash
# 生产构建
npm run build

# 构建输出在 dist/ 目录
# 可将 dist/ 部署到 Nginx/Apache
```

## 联系与反馈

郑州大学 大数据课程设计项目
基于Hive的城市空气质量分析与预测系统
