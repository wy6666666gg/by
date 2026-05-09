<template>
  <div class="realtime-dashboard">
    <!-- 顶部标题栏 -->
    <div class="header">
      <div class="header-left">
        <h1>郑州市空气质量实时监测</h1>
        <span class="update-time">更新时间: {{ updateTime }}</span>
      </div>
      <div class="header-right">
        <el-tag :type="dataSourceType" effect="dark" size="large" style="margin-right: 10px;">
          <el-icon><Link /></el-icon> {{ dataSourceText }}
        </el-tag>
        <el-tag type="success" effect="dark" size="large">
          <el-icon><CircleCheck /></el-icon> 数据实时同步
        </el-tag>
      </div>
    </div>

    <!-- 主要内容区 -->
    <div class="main-content">
      <!-- 左侧：AQI大数字展示 -->
      <div class="left-panel">
        <div class="aqi-display" :style="{ background: cityAQI.color }">
          <div class="aqi-value">{{ cityAQI.value || '--' }}</div>
          <div class="aqi-label">AQI</div>
          <div class="aqi-level">{{ cityAQI.level }}</div>
        </div>
        <div class="primary-pollutant">
          <span class="label">首要污染物</span>
          <span class="value">{{ cityAQI.primaryPollutant || '-' }}</span>
        </div>
      </div>

      <!-- 中间：六项指标卡片 -->
      <div class="center-panel">
        <div class="indicators-grid">
          <div class="indicator-card" v-for="item in indicators" :key="item.name">
            <div class="indicator-header">
              <span class="indicator-name">{{ item.name }}</span>
              <span class="indicator-unit">{{ item.unit }}</span>
            </div>
            <div class="indicator-value" :style="{ color: item.color }">
              {{ item.value || '--' }}
            </div>
            <div class="indicator-level" :style="{ background: item.bgColor }">
              {{ item.level }}
            </div>
            <div class="indicator-trend">
              <el-icon v-if="item.trend === 'up'" color="#ff4d4f"><ArrowUp /></el-icon>
              <el-icon v-else-if="item.trend === 'down'" color="#52c41a"><ArrowDown /></el-icon>
              <el-icon v-else><Minus /></el-icon>
              <span>{{ item.change || '0%' }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：站点排名 -->
      <div class="right-panel">
        <div class="ranking-card">
          <h3>站点AQI排名</h3>
          <el-tabs v-model="rankType" type="border-card">
            <el-tab-pane label="最高" name="highest">
              <div class="rank-list">
                <div v-for="(item, index) in highestRankings" :key="index" class="rank-item">
                  <span class="rank-num" :class="{ 'top3': index < 3 }">{{ index + 1 }}</span>
                  <span class="rank-name">{{ item.name }}</span>
                  <span class="rank-aqi" :style="{ color: item.color }">{{ item.aqi }}</span>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="最低" name="lowest">
              <div class="rank-list">
                <div v-for="(item, index) in lowestRankings" :key="index" class="rank-item">
                  <span class="rank-num" :class="{ 'top3': index < 3 }">{{ index + 1 }}</span>
                  <span class="rank-name">{{ item.name }}</span>
                  <span class="rank-aqi" :style="{ color: item.color }">{{ item.aqi }}</span>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </div>

    <!-- 底部：各站点实时数据 + 等级分布 -->
    <div class="bottom-panel">
      <div class="table-panel">
        <div class="table-header">
          <h3>各站点实时数据</h3>
          <el-button type="primary" size="small" @click="refreshData" :loading="loading">
            <el-icon><Refresh /></el-icon> 刷新数据
          </el-button>
        </div>
        <el-table :data="stationData" style="width: 100%" v-loading="loading" stripe>
          <el-table-column prop="stationName" label="站点名称" min-width="120" />
          <el-table-column prop="aqi" label="AQI" width="90">
            <template #default="{ row }">
              <span class="aqi-badge" :style="{ background: row.color }">{{ row.aqi }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="level" label="等级" width="100">
            <template #default="{ row }">
              <el-tag :type="row.tagType" size="small">{{ row.level }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="pm25" label="PM2.5" width="90" />
          <el-table-column prop="pm10" label="PM10" width="90" />
          <el-table-column prop="so2" label="SO2" width="80" />
          <el-table-column prop="no2" label="NO2" width="80" />
          <el-table-column prop="co" label="CO" width="80" />
          <el-table-column prop="o3" label="O3" width="80" />
          <el-table-column prop="primaryPollutant" label="首要污染物" width="100" />
        </el-table>
      </div>
      <div class="chart-card">
        <div class="chart-header">
          <h3>空气质量等级分布</h3>
        </div>
        <v-chart class="pie-chart" :option="pieOption" autoresize />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent, TooltipComponent, LegendComponent,
  TitleComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import * as echarts from 'echarts/core'
import { getZhengzhouRealtimeData, getZhengzhouCityAQI } from '../api/zhengzhou.js'
import { ElMessage } from 'element-plus'
import { ArrowUp, ArrowDown, Minus, Refresh, CircleCheck, Link } from '@element-plus/icons-vue'

use([
  CanvasRenderer, LineChart, PieChart,
  GridComponent, TooltipComponent, LegendComponent,
  TitleComponent
])

// 状态
const loading = ref(false)
const updateTime = ref('--')
const rankType = ref('highest')
const stationData = ref([])
const dataSource = ref('gbqyun') // gbqyun, csv, mock

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

// 城市整体AQI
const cityAQI = ref({
  value: 0,
  level: '--',
  color: '#ccc',
  primaryPollutant: '-'
})

// 六项指标数据
const indicators = ref([
  { name: 'PM2.5', unit: 'μg/m³', value: 0, level: '--', color: '#fff', bgColor: '#00e400', trend: 'flat', change: '0%' },
  { name: 'PM10', unit: 'μg/m³', value: 0, level: '--', color: '#fff', bgColor: '#00e400', trend: 'flat', change: '0%' },
  { name: 'SO2', unit: 'μg/m³', value: 0, level: '--', color: '#fff', bgColor: '#00e400', trend: 'flat', change: '0%' },
  { name: 'NO2', unit: 'μg/m³', value: 0, level: '--', color: '#fff', bgColor: '#00e400', trend: 'flat', change: '0%' },
  { name: 'CO', unit: 'mg/m³', value: 0, level: '--', color: '#fff', bgColor: '#00e400', trend: 'flat', change: '0%' },
  { name: 'O3', unit: 'μg/m³', value: 0, level: '--', color: '#fff', bgColor: '#00e400', trend: 'flat', change: '0%' }
])

// 排名数据
const highestRankings = computed(() => {
  return [...stationData.value]
    .sort((a, b) => b.aqi - a.aqi)
    .slice(0, 10)
    .map(item => ({
      name: item.stationName,
      aqi: item.aqi,
      color: item.color
    }))
})

const lowestRankings = computed(() => {
  return [...stationData.value]
    .sort((a, b) => a.aqi - b.aqi)
    .slice(0, 10)
    .map(item => ({
      name: item.stationName,
      aqi: item.aqi,
      color: item.color
    }))
})

// 饼图配置
const pieOption = computed(() => {
  const levelCount = { '优': 0, '良': 0, '轻度污染': 0, '中度污染': 0, '重度污染': 0, '严重污染': 0 }
  stationData.value.forEach(item => {
    levelCount[item.level] = (levelCount[item.level] || 0) + 1
  })

  const colors = {
    '优': '#00e400',
    '良': '#ffff00',
    '轻度污染': '#ff7e00',
    '中度污染': '#ff0000',
    '重度污染': '#8f3f97',
    '严重污染': '#7e0023'
  }

  return {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(0,0,0,0.8)',
      textStyle: { color: '#fff' }
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: { color: '#999' }
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['40%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#1a1a2e',
        borderWidth: 2
      },
      label: {
        show: false
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 16,
          fontWeight: 'bold',
          color: '#fff'
        }
      },
      data: Object.entries(levelCount)
        .filter(([_, count]) => count > 0)
        .map(([name, value]) => ({
          name,
          value,
          itemStyle: { color: colors[name] }
        }))
    }]
  }
})

// AQI等级判断
const getAQILevel = (aqi) => {
  if (aqi <= 50) return { level: '优', color: '#00e400', tagType: 'success' }
  if (aqi <= 100) return { level: '良', color: '#ffff00', tagType: 'warning' }
  if (aqi <= 150) return { level: '轻度污染', color: '#ff7e00', tagType: 'warning' }
  if (aqi <= 200) return { level: '中度污染', color: '#ff0000', tagType: 'danger' }
  if (aqi <= 300) return { level: '重度污染', color: '#8f3f97', tagType: 'danger' }
  return { level: '严重污染', color: '#7e0023', tagType: 'danger' }
}

// 获取AQI对应颜色
const getAQIColor = (aqi) => {
  if (aqi <= 50) return '#00e400'
  if (aqi <= 100) return '#ffff00'
  if (aqi <= 150) return '#ff7e00'
  if (aqi <= 200) return '#ff0000'
  if (aqi <= 300) return '#8f3f97'
  return '#7e0023'
}

// 污染物等级判断
const getPollutantLevel = (name, value) => {
  const standards = {
    'PM2.5': [35, 75, 115, 150, 250],
    'PM10': [50, 150, 250, 350, 420],
    'SO2': [50, 150, 475, 800, 1600],
    'NO2': [40, 80, 180, 280, 565],
    'CO': [2, 4, 14, 24, 50],
    'O3': [100, 160, 215, 265, 800]
  }

  const limits = standards[name]
  if (!limits) return { level: '--', color: '#fff', bgColor: '#666' }

  if (value <= limits[0]) return { level: '优', color: '#00e400', bgColor: 'rgba(0, 228, 0, 0.3)' }
  if (value <= limits[1]) return { level: '良', color: '#ffff00', bgColor: 'rgba(255, 255, 0, 0.3)' }
  if (value <= limits[2]) return { level: '轻度', color: '#ff7e00', bgColor: 'rgba(255, 126, 0, 0.3)' }
  if (value <= limits[3]) return { level: '中度', color: '#ff0000', bgColor: 'rgba(255, 0, 0, 0.3)' }
  if (value <= limits[4]) return { level: '重度', color: '#8f3f97', bgColor: 'rgba(143, 63, 151, 0.3)' }
  return { level: '严重', color: '#7e0023', bgColor: 'rgba(126, 0, 35, 0.3)' }
}

// 获取首要污染物
const getPrimaryPollutant = (data) => {
  const pollutants = [
    { name: 'PM2.5', value: data.pm25 || 0, max: 250 },
    { name: 'PM10', value: data.pm10 || 0, max: 420 },
    { name: 'SO2', value: data.so2 || 0, max: 1600 },
    { name: 'NO2', value: data.no2 || 0, max: 565 },
    { name: 'CO', value: (data.co || 0) * 100, max: 5000 },
    { name: 'O3', value: data.o3 || 0, max: 800 }
  ]

  const primary = pollutants.reduce((max, p) =>
    (p.value / p.max) > (max.value / max.max) ? p : max
  )

  return primary.value > 0 ? primary.name : '-'
}

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
    // 1. 获取郑州市各区实时数据（对接目标网站数据）
    const realtimeData = await getZhengzhouRealtimeData()
    
    // 读取自定义数据
    const customData = loadCustomRealtimeData()
    
    // 合并数据（自定义数据优先级更高）
    const mergedData = realtimeData.map(item => {
      const custom = customData[item.stationName]
      if (custom) {
        return { ...item, ...custom }
      }
      return item
    })
    
    // 检测数据源
    if (mergedData.length > 0) {
      dataSource.value = mergedData[0].dataSource || 'gbqyun'
    }

    // 2. 处理站点数据
    stationData.value = mergedData.map(item => {
      const aqiInfo = getAQILevel(item.aqi || 0)
      return {
        stationName: item.stationName || item.name,  // 页面显示名称（中原区、金水区等）
        name: item.stationName || item.name,
        actualStation: item.actualStation,   // 实际监测站点
        aqi: item.aqi || 0,
        level: aqiInfo.level,
        color: aqiInfo.color,
        tagType: aqiInfo.tagType,
        pm25: item.pm25?.toFixed(1) || '--',
        pm10: item.pm10?.toFixed(1) || '--',
        so2: item.so2?.toFixed(1) || '--',
        no2: item.no2?.toFixed(1) || '--',
        co: item.co?.toFixed(2) || '--',
        o3: item.o3?.toFixed(1) || '--',
        primaryPollutant: item.primaryPollutant || '-'
      }
    })

    // 3. 获取城市整体AQI（使用合并后的数据计算平均值）
    const validData = mergedData.filter(d => d.aqi)
    if (validData.length > 0) {
      const avgAQI = Math.round(validData.reduce((sum, d) => sum + d.aqi, 0) / validData.length)
      const aqiInfo = getAQILevel(avgAQI)
      cityAQI.value = {
        value: avgAQI,
        level: aqiInfo.level,
        color: aqiInfo.color,
        primaryPollutant: validData[0].primaryPollutant || '-'
      }
    }

    // 4. 更新六项指标平均值
    if (validData.length > 0) {
      const avgValues = {
        'PM2.5': validData.reduce((sum, d) => sum + (d.pm25 || 0), 0) / validData.length,
        'PM10': validData.reduce((sum, d) => sum + (d.pm10 || 0), 0) / validData.length,
        'SO2': validData.reduce((sum, d) => sum + (d.so2 || 0), 0) / validData.length,
        'NO2': validData.reduce((sum, d) => sum + (d.no2 || 0), 0) / validData.length,
        'CO': validData.reduce((sum, d) => sum + (d.co || 0), 0) / validData.length,
        'O3': validData.reduce((sum, d) => sum + (d.o3 || 0), 0) / validData.length
      }

      indicators.value = indicators.value.map(ind => {
        const value = avgValues[ind.name]
        const levelInfo = getPollutantLevel(ind.name, value)
        return {
          ...ind,
          value: ind.name === 'CO' ? value.toFixed(2) : Math.round(value),
          level: levelInfo.level,
          color: levelInfo.color,
          bgColor: levelInfo.bgColor
        }
      })
    }

    // 5. 更新时间
    updateTime.value = new Date().toLocaleString('zh-CN')

  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('数据加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const refreshData = () => {
  loadData()
  ElMessage.success('数据已刷新')
}

let refreshTimer = null

onMounted(() => {
  loadData()
  // 每5分钟自动刷新
  refreshTimer = setInterval(loadData, 300000)
})

onUnmounted(() => {
  clearInterval(refreshTimer)
})
</script>

<style scoped>
.realtime-dashboard {
  padding: 20px;
  background: linear-gradient(135deg, #0f1419 0%, #1a1f2e 100%);
  min-height: 100vh;
  color: #e0e0e0;
}

/* 顶部标题栏 */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.header-left h1 {
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 8px 0;
  background: linear-gradient(90deg, #00d4ff, #00ff88);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.update-time {
  color: #888;
  font-size: 14px;
}

/* 主要内容区 */
.main-content {
  display: grid;
  grid-template-columns: 280px 1fr 320px;
  gap: 20px;
  margin-bottom: 20px;
}

/* 左侧面板 */
.left-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.aqi-display {
  border-radius: 16px;
  padding: 32px 24px;
  text-align: center;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
}

.aqi-value {
  font-size: 72px;
  font-weight: 700;
  line-height: 1;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.aqi-label {
  font-size: 18px;
  margin-top: 8px;
  opacity: 0.9;
}

.aqi-level {
  font-size: 24px;
  font-weight: 600;
  margin-top: 12px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  display: inline-block;
}

.primary-pollutant {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.primary-pollutant .label {
  display: block;
  color: #888;
  font-size: 14px;
  margin-bottom: 8px;
}

.primary-pollutant .value {
  font-size: 28px;
  font-weight: 600;
  color: #00d4ff;
}

/* 中间面板 */
.center-panel {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.indicators-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  height: 100%;
}

.indicator-card {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: transform 0.3s ease;
}

.indicator-card:hover {
  transform: translateY(-4px);
  border-color: rgba(0, 212, 255, 0.3);
}

.indicator-header {
  display: flex;
  justify-content: space-between;
  width: 100%;
  margin-bottom: 12px;
}

.indicator-name {
  font-size: 16px;
  font-weight: 500;
  color: #a0c4e8;
}

.indicator-unit {
  font-size: 12px;
  color: #666;
}

.indicator-value {
  font-size: 36px;
  font-weight: 700;
  margin: 8px 0;
}

.indicator-level {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  margin-bottom: 8px;
}

.indicator-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #888;
}

/* 右侧面板 */
.right-panel {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.ranking-card h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  font-weight: 500;
  color: #c5e0f5;
}

.rank-list {
  max-height: 320px;
  overflow-y: auto;
}

.rank-item {
  display: flex;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.rank-num {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: rgba(100, 150, 200, 0.3);
  color: #8ec5e8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  margin-right: 12px;
}

.rank-num.top3 {
  background: linear-gradient(135deg, #ffd700, #ffaa00);
  color: #000;
  font-weight: 600;
}

.rank-name {
  flex: 1;
  font-size: 14px;
  color: #b8d4f0;
}

.rank-aqi {
  font-size: 16px;
  font-weight: 600;
}

/* 底部区域 */
.bottom-panel {
  display: grid;
  grid-template-columns: 3fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.chart-card {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.chart-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: #c5e0f5;
}

.pie-chart {
  height: 350px;
}

/* 表格面板 */
.table-panel {
  background: rgba(255, 255, 255, 0.03);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.table-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: #c5e0f5;
}

.aqi-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-weight: 600;
  font-size: 14px;
  color: #000;
}

:deep(.el-table) {
  background: transparent;
}

:deep(.el-table th) {
  background: rgba(0, 0, 0, 0.3);
  color: #8ec5e8;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

:deep(.el-table td) {
  background: transparent;
  color: #b8d4f0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

:deep(.el-table tr:hover td) {
  background: rgba(0, 212, 255, 0.1);
}

:deep(.el-tabs__item) {
  color: #888;
}

:deep(.el-tabs__item.is-active) {
  color: #00d4ff;
}

:deep(.el-tabs__active-bar) {
  background-color: #00d4ff;
}
</style>
