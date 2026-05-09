<template>
  <div class="dashboard">
    <!-- 城市整体AQI展示 -->
    <div class="city-overview" v-if="cityAQI.aqi">
      <div class="city-aqi-display">
        <div class="aqi-circle" :class="getAqiClass(cityAQI.aqi)">
          <span class="aqi-number">{{ cityAQI.aqi }}</span>
          <span class="aqi-level">{{ cityAQI.level }}</span>
        </div>
        <div class="city-info">
          <h2>郑州市空气质量</h2>
          <p class="primary-pollutant">首要污染物: {{ cityAQI.primaryPollutant || '--' }}</p>
          <p class="update-time">更新时间: {{ formatTime(cityAQI.updateTime) }}</p>
          <p class="data-source">
            <el-tag size="small" :type="dataSourceType" effect="dark">
              <el-icon><Link /></el-icon>
              数据来源: {{ dataSourceText }}
            </el-tag>
          </p>
        </div>
      </div>
    </div>

    <!-- 数据概览卡片 -->
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #00d4ff, #0099cc);">
          <i class="fas fa-map-marker-alt"></i>
        </div>
        <div class="stat-info">
          <p class="stat-label">监测站点</p>
          <p class="stat-value">{{ stats.stationCount || '--' }}</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #00ff88, #00cc66);">
          <i class="fas fa-smile"></i>
        </div>
        <div class="stat-info">
          <p class="stat-label">优良站点</p>
          <p class="stat-value">{{ stats.goodCount || '--' }}</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #ffaa00, #cc8800);">
          <i class="fas fa-meh"></i>
        </div>
        <div class="stat-info">
          <p class="stat-label">轻度污染</p>
          <p class="stat-value">{{ stats.moderateCount || '--' }}</p>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background: linear-gradient(135deg, #ff5555, #cc4444);">
          <i class="fas fa-frown"></i>
        </div>
        <div class="stat-info">
          <p class="stat-label">中重度污染</p>
          <p class="stat-value">{{ stats.heavyCount || '--' }}</p>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-row">
      <div class="chart-card">
        <div class="chart-header">
          <h3>实时AQI分布</h3>
          <el-tag type="success" effect="dark">
            <el-icon><Refresh /></el-icon> 实时同步
          </el-tag>
        </div>
        <v-chart class="chart" :option="aqiBarOption" autoresize />
      </div>
      <div class="chart-card">
        <div class="chart-header">
          <h3>空气质量等级占比</h3>
        </div>
        <v-chart class="chart" :option="aqiPieOption" autoresize />
      </div>
    </div>

    <!-- 实时数据表格 -->
    <div class="table-card">
      <div class="chart-header">
        <h3>站点实时数据</h3>
        <div class="header-actions">
          <el-switch
            v-model="autoRefresh"
            active-text="自动刷新"
            inline-prompt
            style="margin-right: 12px"
          />
          <el-button type="primary" size="small" @click="refreshData" :loading="loading">
            <el-icon><Refresh /></el-icon> 立即刷新
          </el-button>
        </div>
      </div>
      <el-table :data="realtimeData" style="width: 100%" v-loading="loading" :stripe="true">
        <el-table-column prop="stationName" label="站点名称" min-width="120">
          <template #default="{ row }">
            <span class="station-name">{{ row.stationName }}</span>
            <el-tag size="small" type="info" effect="plain" class="station-tag">
              {{ row.actualStation }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="aqi" label="AQI" width="100" align="center">
          <template #default="{ row }">
            <span class="aqi-badge" :class="getAqiClass(row.aqi)">{{ row.aqi }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="等级" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getAqiTagType(row.aqi)" size="small" effect="dark">
              {{ row.level }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pm25" label="PM2.5" width="100" align="center">
          <template #default="{ row }">
            <span class="pollutant-value">{{ row.pm25 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="pm10" label="PM10" width="100" align="center">
          <template #default="{ row }">
            <span class="pollutant-value">{{ row.pm10 }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="so2" label="SO2" width="90" align="center" />
        <el-table-column prop="no2" label="NO2" width="90" align="center" />
        <el-table-column prop="co" label="CO" width="80" align="center" />
        <el-table-column prop="o3" label="O3" width="90" align="center" />
        <el-table-column prop="primaryPollutant" label="首要污染物" width="110" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.primaryPollutant" type="danger" size="small" effect="plain">
              {{ row.primaryPollutant }}
            </el-tag>
            <span v-else>--</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="data-source">
        <el-icon><InfoFilled /></el-icon>
        <span>数据来源: citydev.gbqyun.com | 数据映射: 中原区/金水区→北区建设指挥部, 二七区→河医大, 惠济区→惠济区政府, 郑东新区→经开区管委</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, PieChart } from 'echarts/charts'
import {
  GridComponent, TooltipComponent, LegendComponent,
  TitleComponent, DatasetComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import { Refresh, InfoFilled, Link } from '@element-plus/icons-vue'
import { getZhengzhouRealtimeData, getZhengzhouCityAQI } from '../api/zhengzhou'
import { ElMessage } from 'element-plus'

use([
  CanvasRenderer, BarChart, PieChart,
  GridComponent, TooltipComponent, LegendComponent,
  TitleComponent, DatasetComponent
])

const loading = ref(false)
const autoRefresh = ref(true)
const realtimeData = ref([])
const cityAQI = ref({})
const stats = ref({})
const dataSource = ref('gbqyun') // gbqyun, csv, mock
let refreshTimer = null

// 数据源显示
const dataSourceText = computed(() => {
  const map = {
    'gbqyun': 'citydev.gbqyun.com',
    'csv': '本地历史数据',
    'mock': '模拟数据'
  }
  return map[dataSource.value] || '未知来源'
})

const dataSourceType = computed(() => {
  const map = {
    'gbqyun': 'success',
    'csv': 'warning',
    'mock': 'info'
  }
  return map[dataSource.value] || 'info'
})

// AQI等级判断
const getAqiLevel = (aqi) => {
  if (aqi <= 50) return '优'
  if (aqi <= 100) return '良'
  if (aqi <= 150) return '轻度污染'
  if (aqi <= 200) return '中度污染'
  if (aqi <= 300) return '重度污染'
  return '严重污染'
}

const getAqiClass = (aqi) => {
  if (aqi <= 50) return 'aqi-excellent'
  if (aqi <= 100) return 'aqi-good'
  if (aqi <= 150) return 'aqi-moderate'
  if (aqi <= 200) return 'aqi-unhealthy'
  if (aqi <= 300) return 'aqi-very-unhealthy'
  return 'aqi-hazardous'
}

const getAqiTagType = (aqi) => {
  if (aqi <= 50) return 'success'
  if (aqi <= 100) return ''
  if (aqi <= 150) return 'warning'
  return 'danger'
}

const getAqiColor = (aqi) => {
  if (aqi <= 50) return '#00e400'
  if (aqi <= 100) return '#ffff00'
  if (aqi <= 150) return '#ff7e00'
  if (aqi <= 200) return '#ff0000'
  if (aqi <= 300) return '#8f3f97'
  return '#7e0023'
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return '--'
  const date = new Date(time)
  return date.toLocaleString('zh-CN')
}

// 柱状图配置
const aqiBarOption = ref({
  tooltip: { 
    trigger: 'axis',
    formatter: function(params) {
      const data = params[0]
      return `<div style="font-weight:bold">${data.name}</div>
              <div>AQI: <span style="color:${data.color};font-weight:bold">${data.value}</span></div>`
    }
  },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    data: [],
    axisLabel: { color: '#94a3b8', rotate: 0 },
    axisLine: { lineStyle: { color: '#2a3441' } }
  },
  yAxis: {
    type: 'value',
    min: 0,
    max: 300,
    axisLabel: { color: '#94a3b8' },
    splitLine: { lineStyle: { color: '#2a3441' } }
  },
  series: [{
    data: [],
    type: 'bar',
    barWidth: '50%',
    itemStyle: {
      borderRadius: [8, 8, 0, 0],
      color: (params) => getAqiColor(params.value)
    },
    label: {
      show: true,
      position: 'top',
      color: '#c5e0f5',
      formatter: '{c}'
    }
  }]
})

// 饼图配置
const aqiPieOption = ref({
  tooltip: { 
    trigger: 'item',
    formatter: '{b}: {c}个站点 ({d}%)'
  },
  legend: { 
    orient: 'vertical', 
    right: 10, 
    top: 'center', 
    textStyle: { color: '#94a3b8' } 
  },
  series: [{
    name: '空气质量等级',
    type: 'pie',
    radius: ['40%', '70%'],
    center: ['35%', '50%'],
    avoidLabelOverlap: false,
    itemStyle: { 
      borderRadius: 10, 
      borderColor: '#1a2332', 
      borderWidth: 2 
    },
    label: { show: false },
    emphasis: { 
      label: { 
        show: true, 
        fontSize: 16, 
        fontWeight: 'bold', 
        color: '#c5e0f5' 
      } 
    },
    data: []
  }]
})

// 加载自定义实时数据
const loadCustomRealtimeData = () => {
  try {
    const storageKey = 'air_quality_realtime_custom'
    const customData = JSON.parse(localStorage.getItem(storageKey) || '{}')
    return customData
  } catch (error) {
    console.error('读取自定义数据失败:', error)
    return {}
  }
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    // 获取实时数据
    const data = await getZhengzhouRealtimeData()
    
    // 读取自定义数据
    const customData = loadCustomRealtimeData()
    
    // 合并数据（自定义数据优先级更高）
    const mergedData = data?.map(item => {
      const custom = customData[item.stationName]
      if (custom) {
        return { ...item, ...custom }
      }
      return item
    }) || []
    
    realtimeData.value = mergedData
    
    // 检测数据源
    if (mergedData.length > 0) {
      dataSource.value = mergedData[0].dataSource || 'gbqyun'
    }

    // 基于各站点数据计算城市整体AQI（与RealtimeDashboard保持一致）
    const validData = mergedData.filter(d => d.aqi)
    if (validData.length > 0) {
      const avgAQI = Math.round(validData.reduce((sum, d) => sum + d.aqi, 0) / validData.length)
      cityAQI.value = {
        aqi: avgAQI,
        level: getAqiLevel(avgAQI),
        primaryPollutant: validData[0].primaryPollutant || '-',
        updateTime: new Date().toLocaleString('zh-CN')
      }
    } else {
      // fallback: 使用API数据
      const cityData = await getZhengzhouCityAQI()
      cityAQI.value = cityData || {}
    }

    // 更新柱状图
    const stations = mergedData.map(item => item.stationName)
    const aqis = mergedData.map(item => item.aqi)
    aqiBarOption.value.xAxis.data = stations
    aqiBarOption.value.series[0].data = aqis

    // 更新饼图数据
    const levelCount = { '优': 0, '良': 0, '轻度污染': 0, '中度污染': 0, '重度污染': 0, '严重污染': 0 }
    mergedData.forEach(item => {
      if (item.aqi) {
        const level = getAqiLevel(item.aqi)
        levelCount[level] = (levelCount[level] || 0) + 1
      }
    })
    
    const pieColors = {
      '优': '#00e400',
      '良': '#ffff00',
      '轻度污染': '#ff7e00',
      '中度污染': '#ff0000',
      '重度污染': '#8f3f97',
      '严重污染': '#7e0023'
    }
    
    aqiPieOption.value.series[0].data = Object.entries(levelCount)
      .filter(([_, count]) => count > 0)
      .map(([name, value]) => ({ 
        name, 
        value,
        itemStyle: { color: pieColors[name] }
      }))

    // 更新统计
    stats.value = {
      stationCount: mergedData.length,
      goodCount: (levelCount['优'] || 0) + (levelCount['良'] || 0),
      moderateCount: (levelCount['轻度污染'] || 0) + (levelCount['中度污染'] || 0),
      heavyCount: (levelCount['重度污染'] || 0) + (levelCount['严重污染'] || 0)
    }
  } catch (error) {
    console.error('获取数据失败:', error)
  } finally {
    loading.value = false
  }
}

const refreshData = () => {
  loadData()
  ElMessage.success('数据已刷新')
}

// 监听自动刷新开关
watch(autoRefresh, (newVal) => {
  if (newVal) {
    refreshTimer = setInterval(loadData, 30000) // 30秒刷新
  } else {
    clearInterval(refreshTimer)
  }
})

onMounted(() => {
  loadData()
  if (autoRefresh.value) {
    refreshTimer = setInterval(loadData, 30000) // 30秒刷新
  }
})

onUnmounted(() => {
  clearInterval(refreshTimer)
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

/* 城市整体AQI展示 */
.city-overview {
  background: linear-gradient(135deg, #1a2332 0%, #243447 100%);
  border-radius: 16px;
  padding: 30px;
  margin-bottom: 24px;
  border: 1px solid #2a3441;
}

.city-aqi-display {
  display: flex;
  align-items: center;
  gap: 30px;
}

.aqi-circle {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 4px solid;
  background: rgba(0, 0, 0, 0.3);
}

.aqi-circle.aqi-excellent { border-color: #00e400; box-shadow: 0 0 20px rgba(0, 228, 0, 0.3); }
.aqi-circle.aqi-good { border-color: #ffff00; box-shadow: 0 0 20px rgba(255, 255, 0, 0.3); }
.aqi-circle.aqi-moderate { border-color: #ff7e00; box-shadow: 0 0 20px rgba(255, 126, 0, 0.3); }
.aqi-circle.aqi-unhealthy { border-color: #ff0000; box-shadow: 0 0 20px rgba(255, 0, 0, 0.3); }
.aqi-circle.aqi-very-unhealthy { border-color: #8f3f97; box-shadow: 0 0 20px rgba(143, 63, 151, 0.3); }
.aqi-circle.aqi-hazardous { border-color: #7e0023; box-shadow: 0 0 20px rgba(126, 0, 35, 0.3); }

.aqi-number {
  font-size: 48px;
  font-weight: bold;
  color: #fff;
}

.aqi-level {
  font-size: 14px;
  color: #c5e0f5;
  margin-top: 4px;
}

.city-info h2 {
  font-size: 24px;
  color: #c5e0f5;
  margin-bottom: 12px;
}

.primary-pollutant {
  font-size: 16px;
  color: #ff7e00;
  margin-bottom: 8px;
}

.update-time {
  font-size: 13px;
  color: #8ec5e8;
}

.data-source {
  margin-top: 8px;
}

/* 统计卡片 */
.stats-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: linear-gradient(135deg, #1a2332 0%, #243447 100%);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid #2a3441;
  transition: transform 0.3s;
}

.stat-card:hover {
  transform: translateY(-4px);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
}

.stat-label {
  color: #94a3b8;
  font-size: 14px;
  margin-bottom: 8px;
}

.stat-value {
  color: #c5e0f5;
  font-size: 28px;
  font-weight: bold;
}

/* 图表区域 */
.charts-row {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.chart-card, .table-card {
  background: #1a2332;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #2a3441;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.chart-header h3 {
  font-size: 16px;
  font-weight: 500;
  color: #c5e0f5;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chart {
  height: 300px;
}

/* 表格样式 */
.station-name {
  font-weight: 500;
  color: #c5e0f5;
  display: block;
}

.station-tag {
  margin-top: 4px;
  font-size: 11px;
}

.aqi-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-weight: bold;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.1);
}

.pollutant-value {
  color: #8ec5e8;
  font-weight: 500;
}

.data-source {
  margin-top: 16px;
  padding: 12px;
  background: rgba(0, 212, 255, 0.05);
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: #8ec5e8;
}

/* AQI颜色 */
.aqi-excellent { color: #00e400; }
.aqi-good { color: #ffff00; }
.aqi-moderate { color: #ff7e00; }
.aqi-unhealthy { color: #ff0000; }
.aqi-very-unhealthy { color: #8f3f97; }
.aqi-hazardous { color: #7e0023; }

:deep(.el-table) {
  background: transparent;
}

:deep(.el-table th) {
  background: #243447;
  color: #8ec5e8;
  font-weight: 500;
}

:deep(.el-table td) {
  background: transparent;
  color: #b8d4f0;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: rgba(36, 52, 71, 0.3);
}

:deep(.el-table tr:hover td) {
  background: rgba(0, 212, 255, 0.1);
}

/* 响应式 */
@media (max-width: 1200px) {
  .charts-row {
    grid-template-columns: 1fr;
  }
  
  .stats-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-cards {
    grid-template-columns: 1fr;
  }
  
  .city-aqi-display {
    flex-direction: column;
    text-align: center;
  }
}
</style>
