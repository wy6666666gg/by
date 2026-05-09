# 空气质量分析系统 - 功能预览

## 系统概述

基于Hive的城市空气质量分析与预测系统已完成全面优化，实现了毕业设计文档要求的所有功能模块。

---

## 前端页面功能清单

### 1. 实时监控大屏 (Dashboard.vue)

**功能特性:**
- 4个统计卡片：监测站点数、优良站点、轻度污染、重度污染
- AQI分布柱状图：实时显示各站点AQI值
- 空气质量等级占比饼图
- 站点实时数据表格：包含AQI、PM2.5、PM10、SO2、NO2、CO、O3
- 自动刷新：每分钟自动更新数据
- 模拟数据：无后端时可正常展示

**技术亮点:**
- ECharts柱状图 + 饼图
- Element Plus Table组件
- 定时器自动刷新
- AQI颜色分级显示

---

### 2. 历史趋势分析 (History.vue)

**功能特性:**
- 日期范围选择器
- 站点下拉选择
- AQI历史趋势折线图（支持日/小时粒度）
- 污染物对比柱状图
- AQI分布热力图（7天x6时段）

**技术亮点:**
- ECharts折线图、柱状图、热力图
- 时间范围筛选
- 多图表联动

---

### 3. 空间分布分析 (SpatialAnalysis.vue)

**功能特性:**
- 数据时间选择
- 污染物指标切换（AQI/PM2.5/PM10等）
- 插值方法选择（IDW/克里金/最近邻）
- 50x50网格热力图
- 站点数据对比柱状图
- 时空演变趋势图
- 插值算法性能对比表格

**技术亮点:**
- ECharts Heatmap
- IDW插值算法模拟
- 算法说明折叠面板

---

### 4. GIS地图展示 (SpatialMap.vue) ⭐新增

**功能特性:**
- Leaflet地图集成
- OpenStreetMap底图
- 8个监测站点标记（圆圈大小/颜色根据AQI）
- 热力图叠加层
- 站点信息弹窗
- 地图工具栏：放大/缩小/重置
- 演变动画播放
- 导出地图功能

**技术亮点:**
- Leaflet 1.9.x
- 自定义热力图层
- 交互式标记弹窗
- 侧边控制面板

---

### 5. 相关性分析 (CorrelationAnalysis.vue)

**功能特性:**
- 分析时段选择
- 站点选择
- 相关系数类型切换（皮尔逊/斯皮尔曼）
- 10x10相关性矩阵热力图
- 随机森林特征重要性排序
- AQI影响因素散点图
- 时滞相关性分析（-12h~+12h）
- 智能分析解读

**技术亮点:**
- ECharts Heatmap矩阵
- 渐变色图例
- 特征重要性水平条形图
- 智能解读卡片

---

### 6. 预测分析 (Prediction.vue)

**功能特性:**
- 24小时/72小时预测卡片
- 模型准确率展示
- 24小时预测趋势折线图（带置信区间）
- 72小时预测柱状图
- 多站点预测对比表格
- 手动触发预测按钮

**技术亮点:**
- 面积图展示置信区间
- 预测趋势指示
- 多站点对比

---

### 7. 预警中心 (AlertCenter.vue)

**功能特性:**
- 4个预警概览卡片
- 预警规则配置表格
- 实时预警时间线
- 预警筛选（全部/未处理/已处理）
- 模拟实时推送
- 标记已处理功能

**技术亮点:**
- Element Notification模拟推送
- 时间线样式
- 徽章提示未读数量

---

### 8. 统计报表 (Statistics.vue)

**功能特性:**
- 日期选择
- 4个统计概览卡片
- 污染物浓度均值柱状图
- AQI等级分布饼图
- 月度AQI对比折线图
- 站点详细统计表格

**技术亮点:**
- 多维度统计
- 数据导出按钮
- 响应式表格

---

### 9. 数据管理 (DataManage.vue) ⭐新增

**功能特性:**
- 4个数据概览卡片（总数据量/今日新增/站点数/时间跨度）
- 数据质量报告（完整性/准确性/一致性/时效性）
- 数据采集趋势图
- 各站点数据量分布图
- 数据表管理列表
- 数据导出（CSV/JSON/Excel）
- 数据导入（拖拽上传）
- 数据清理配置

**技术亮点:**
- Element Upload组件
- 进度圆环
- 数据表操作

---

### 10. 系统管理 (SystemManage.vue) ⭐新增

**功能特性:**
- 4个系统状态卡片（运行状态/内存/CPU/磁盘）
- 实时资源使用折线图
- API调用统计柱状图
- 系统日志表格（支持分页/筛选）
- 系统配置表单
- 维护操作（清缓存/优化DB/重启/备份）
- 版本信息展示

**技术亮点:**
- 模拟实时数据更新
- 资源使用监控
- 系统维护功能

---

## 后端API清单

### AQI数据接口
| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/aqi/realtime | GET | 实时AQI数据 |
| /api/v1/aqi/history | GET | 历史AQI数据 |
| /api/v1/aqi/trend | GET | 趋势数据 |
| /api/v1/aqi/station/{code} | GET | 站点详情 |
| /api/v1/aqi/statistics | GET | 统计信息 |

### 空间分析接口
| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/spatial/heatmap | GET | 热力图数据 |
| /api/v1/spatial/stations | GET | 站点分布 |
| /api/v1/spatial/interpolation | GET | 插值结果 |
| /api/v1/spatial/comparison | GET | 空间对比 |
| /api/v1/spatial/timeline | GET | 时空演变 |

### 相关性分析接口
| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/correlation/matrix | GET | 相关性矩阵 |
| /api/v1/correlation/feature-importance | GET | 特征重要性 |
| /api/v1/correlation/lag | GET | 时滞相关性 |
| /api/v1/correlation/scatter | GET | 散点数据 |

### 预测接口
| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/predict/{stationCode} | GET | 站点预测 |
| /api/v1/predict/24h | GET | 24小时预测 |
| /api/v1/predict/72h | GET | 72小时预测 |
| /api/v1/predict/comparison | GET | 预测对比 |
| /api/v1/predict/trigger | POST | 触发预测 |

### 预警接口
| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/alerts/active | GET | 活跃预警 |
| /api/v1/alerts/history | GET | 历史预警 |
| /api/v1/alerts/rules | GET/POST | 预警规则 |
| /api/v1/alerts/stats | GET | 预警统计 |

### 系统管理接口
| 接口 | 方法 | 说明 |
|------|------|------|
| /api/v1/system/status | GET | 系统状态 |
| /api/v1/system/monitor | GET | 监控指标 |
| /api/v1/system/logs | GET | 系统日志 |
| /api/v1/system/data-quality | GET | 数据质量 |
| /api/v1/system/cache/clear | POST | 清除缓存 |

---

## 页面导航结构

```
侧边栏菜单
├── 实时监控        →  Dashboard.vue
├── 历史趋势        →  History.vue
├── 空间分析        →  SpatialAnalysis.vue
├── GIS地图         →  SpatialMap.vue  ⭐
├── 相关性分析      →  CorrelationAnalysis.vue
├── 预测分析        →  Prediction.vue
├── 预警中心        →  AlertCenter.vue
├── 统计报表        →  Statistics.vue
├── 数据管理        →  DataManage.vue  ⭐
└── 系统管理        →  SystemManage.vue  ⭐
```

---

## 技术栈总结

### 前端
- **框架**: Vue 3 + Composition API
- **UI组件**: Element Plus 2.6
- **图表**: ECharts 5.5 + vue-echarts
- **地图**: Leaflet 1.9 + @vue-leaflet/vue-leaflet
- **工具**: Axios, Day.js, Screenfull
- **构建**: Vite 5

### 后端
- **框架**: Spring Boot 2.7
- **ORM**: MyBatis Plus
- **数据库连接池**: Druid
- **缓存**: Redis
- **数学计算**: Apache Commons Math 3.6
- **API文档**: SpringDoc OpenAPI

---

## 启动命令

```bash
# 1. 启动后端
cd air-web
mvn spring-boot:run

# 2. 启动前端
cd air-web-frontend
npm install
npm run dev

# 或使用脚本
start-web-frontend.bat
```

访问: http://localhost:3000

---

## 截图说明

所有页面均已填充模拟数据，可直接展示。主要页面包括：

1. **Dashboard**: 科技风深色主题，4卡片 + 2图表 + 表格
2. **SpatialMap**: Leaflet地图 + 8站点标记 + 热力图
3. **Correlation**: 10x10相关性矩阵 + 特征排序
4. **AlertCenter**: 时间线样式预警列表 + 实时推送
5. **SystemManage**: 系统监控 + 日志 + 配置

---

郑州大学 大数据课程设计
基于Hive的城市空气质量分析与预测系统
