# 空气质量监测前端项目

基于 Vue 3 + ECharts 的城市空气质量分析与预测可视化平台。

## 技术栈

- **Vue 3** - 渐进式 JavaScript 框架
- **Vite** - 下一代前端构建工具
- **Element Plus** - 基于 Vue 3 的组件库
- **ECharts** - 数据可视化图表库
- **Vue-ECharts** - ECharts 的 Vue 封装
- **Axios** - HTTP 客户端

## 功能模块

1. **实时监控大屏**
   - 站点实时 AQI 数据展示
   - 空气质量分布图表
   - 实时数据表格

2. **历史趋势分析**
   - 多站点历史数据查询
   - AQI 趋势曲线图
   - 污染物对比分析
   - 热力图展示

3. **AQI 预测分析**
   - 24小时/72小时预测
   - 多站点预测对比
   - 预测准确率展示
   - 趋势预测图表

4. **统计报表**
   - 日均/最高/最低 AQI 统计
   - 污染物浓度均值
   - AQI 等级分布
   - 月度对比分析
   - 站点达标率统计

## 项目结构

```
air-web-frontend/
├── index.html              # HTML 入口
├── vite.config.js          # Vite 配置
├── package.json            # 项目依赖
├── README.md               # 项目说明
└── src/
    ├── main.js             # 应用入口
    ├── App.vue             # 根组件
    ├── router/
    │   └── index.js        # 路由配置
    ├── api/
    │   ├── aqi.js          # AQI 数据接口
    │   └── prediction.js   # 预测接口
    ├── utils/
    │   └── request.js      # Axios 封装
    └── views/
        ├── Layout.vue      # 布局组件
        ├── Dashboard.vue   # 实时监控
        ├── History.vue     # 历史趋势
        ├── Prediction.vue  # 预测分析
        └── Statistics.vue  # 统计报表
```

## 安装运行

```bash
# 进入前端目录
cd air-web-frontend

# 安装依赖
npm install

# 开发模式运行
npm run dev

# 构建生产版本
npm run build
```

## 后端接口配置

在 `vite.config.js` 中配置代理：

```javascript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  // 后端服务地址
      changeOrigin: true
    }
  }
}
```

## API 接口列表

### AQI 数据接口
- `GET /api/v1/aqi/realtime` - 实时AQI数据
- `GET /api/v1/aqi/history` - 历史AQI数据
- `GET /api/v1/aqi/trend` - AQI趋势数据
- `GET /api/v1/aqi/station/{code}` - 站点详情
- `GET /api/v1/aqi/statistics` - 统计信息

### 预测接口
- `GET /api/v1/predict/{stationCode}` - 站点预测
- `GET /api/v1/predict/24h` - 24小时预测
- `GET /api/v1/predict/72h` - 72小时预测
- `GET /api/v1/predict/comparison` - 多站点对比
- `POST /api/v1/predict/trigger` - 触发预测任务

## 界面截图

- 深色主题设计
- 响应式布局
- 实时数据更新
- 丰富的图表展示

## 浏览器支持

- Chrome >= 80
- Firefox >= 75
- Safari >= 13
- Edge >= 80

## 许可证

本项目仅供学习和研究使用。
