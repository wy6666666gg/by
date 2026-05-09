<template>
  <div class="data-query">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2>空气质量数据查询</h2>
      <p class="subtitle">基于2020-2024年郑州市五区历史监测数据，支持按日期精确查询</p>
    </div>

    <!-- 查询区域 -->
    <div class="query-section">
      <el-card class="query-card">
        <template #header>
          <div class="card-header">
            <span><el-icon><Search /></el-icon> 查询条件</span>
          </div>
        </template>
        
        <el-form :model="queryForm" label-position="top">
          <el-row :gutter="20">
            <el-col :span="6">
              <el-form-item label="查询模式">
                <el-radio-group v-model="queryForm.mode" size="large" @change="handleModeChange">
                  <el-radio-button label="single">单日查询</el-radio-button>
                  <el-radio-button label="range">区间查询</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-col>
            
            <el-col :span="6">
              <el-form-item label="选择站点">
                <el-select v-model="queryForm.station" placeholder="请选择站点" size="large" style="width: 100%">
                  <el-option
                    v-for="item in stations"
                    :key="item.code"
                    :label="item.name"
                    :value="item.code"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            
            <el-col :span="8">
              <el-form-item :label="queryForm.mode === 'single' ? '选择日期' : '日期范围'">
                <el-date-picker
                  v-if="queryForm.mode === 'single'"
                  v-model="queryForm.date"
                  type="date"
                  placeholder="请选择日期"
                  size="large"
                  value-format="YYYY-MM-DD"
                  :disabled-date="disabledDate"
                  style="width: 100%"
                />
                <el-date-picker
                  v-else
                  v-model="queryForm.dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  size="large"
                  value-format="YYYY-MM-DD"
                  :disabled-date="disabledDate"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            
            <el-col :span="4">
              <el-form-item label="操作">
                <el-button type="primary" size="large" @click="handleQuery" :loading="loading" style="width: 100%">
                  <el-icon><Search /></el-icon> 查询数据
                </el-button>
              </el-form-item>
            </el-col>
          </el-row>
          
          <!-- 快捷日期选择 -->
          <el-row :gutter="10" style="margin-top: 10px">
            <el-col :span="24">
              <el-form-item label="快捷选择">
                <el-space>
                  <el-tag 
                    v-for="item in quickDates" 
                    :key="item.label"
                    class="quick-date-tag"
                    effect="plain"
                    @click="applyQuickDate(item)"
                  >
                    {{ item.label }}
                  </el-tag>
                </el-space>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </el-card>
    </div>

    <!-- 查询结果 - 单日模式 -->
    <template v-if="queryForm.mode === 'single' && singleResult">
      <!-- 数据概览卡片 -->
      <div class="result-overview">
        <el-row :gutter="20">
          <el-col :span="6">
            <div class="overview-card aqi-card" :class="getAqiClass(singleResult.data.aqi)">
              <div class="card-bg-icon"><el-icon><WindPower /></el-icon></div>
              <div class="overview-content">
                <div class="overview-label">AQI 指数</div>
                <div class="overview-value">{{ singleResult.data.aqi }}</div>
                <div class="overview-level">{{ singleResult.data.qualityLevel }}</div>
              </div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="overview-card">
              <div class="card-bg-icon" style="color: #ff5555;"><el-icon><Cloudy /></el-icon></div>
              <div class="overview-content">
                <div class="overview-label">首要污染物</div>
                <div class="overview-value" style="font-size: 28px;">{{ singleResult.data.primaryPollutant }}</div>
                <div class="overview-sub">PM2.5: {{ singleResult.data.pm25 }} μg/m³</div>
              </div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="overview-card">
              <div class="card-bg-icon" style="color: #00d4ff;"><el-icon><TrendCharts /></el-icon></div>
              <div class="overview-content">
                <div class="overview-label">当月平均水平</div>
                <div class="overview-value" style="font-size: 28px;">{{ singleResult.monthStats.avg }}</div>
                <div class="overview-sub">范围: {{ singleResult.monthStats.min }} - {{ singleResult.monthStats.max }}</div>
              </div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="overview-card">
              <div class="card-bg-icon" style="color: #00ff88;"><el-icon><Trophy /></el-icon></div>
              <div class="overview-content">
                <div class="overview-label">年度排名</div>
                <div class="overview-value" style="font-size: 28px;">{{ singleResult.yearRank.rank }}</div>
                <div class="overview-sub">优于 {{ singleResult.yearRank.betterThan }}% 的天数</div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 六项污染物详情 -->
      <el-card class="detail-card">
        <template #header>
          <div class="card-header">
            <span><el-icon><DataLine /></el-icon> 六项污染物详细数据</span>
            <span class="header-date">{{ queryForm.date }}</span>
          </div>
        </template>
        
        <el-row :gutter="20">
          <el-col :span="4" v-for="(item, index) in pollutantDetails" :key="index">
            <div class="pollutant-item" :style="{ borderLeftColor: item.color }">
              <div class="pollutant-name">{{ item.name }}</div>
              <div class="pollutant-value">{{ item.value }}</div>
              <div class="pollutant-unit">{{ item.unit }}</div>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- 前后对比图表 -->
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span><el-icon><TrendCharts /></el-icon> 前后7天AQI变化趋势</span>
          </div>
        </template>
        <v-chart class="chart" :option="nearbyChartOption" autoresize />
      </el-card>

      <!-- 历史同期对比 -->
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span><el-icon><Histogram /></el-icon> 过去5年同日数据对比</span>
          </div>
        </template>
        <el-row :gutter="20">
          <el-col :span="12">
            <v-chart class="chart" :option="historicalChartOption" autoresize />
          </el-col>
          <el-col :span="12">
            <el-table :data="singleResult.historicalSameDay" stripe style="width: 100%">
              <el-table-column prop="year" label="年份" width="100" />
              <el-table-column prop="aqi" label="AQI" width="100">
                <template #default="{ row }">
                  <span :class="getAqiClass(row.aqi)">{{ row.aqi }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="qualityLevel" label="等级" />
              <el-table-column prop="primaryPollutant" label="首要污染物" />
            </el-table>
          </el-col>
        </el-row>
      </el-card>
    </template>

    <!-- 查询结果 - 区间模式 -->
    <template v-if="queryForm.mode === 'range' && rangeResult.length > 0">
      <!-- 统计概览 -->
      <div class="result-overview">
        <el-row :gutter="20">
          <el-col :span="4" v-for="(stat, index) in rangeStats" :key="index">
            <div class="overview-card small">
              <div class="overview-content">
                <div class="overview-label">{{ stat.label }}</div>
                <div class="overview-value" :style="{ fontSize: '22px', color: stat.color }">{{ stat.value }}</div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 趋势图表 -->
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span><el-icon><TrendCharts /></el-icon> AQI变化趋势</span>
            <el-radio-group v-model="rangeChartType" size="small">
              <el-radio-button label="aqi">AQI</el-radio-button>
              <el-radio-button label="pm25">PM2.5</el-radio-button>
              <el-radio-button label="pm10">PM10</el-radio-button>
            </el-radio-group>
          </div>
        </template>
        <v-chart class="chart" :option="rangeChartOption" autoresize />
      </el-card>

      <!-- 数据表格 -->
      <el-card class="table-card">
        <template #header>
          <div class="card-header">
            <span><el-icon><List /></el-icon> 详细数据列表</span>
            <el-button type="primary" size="small" @click="exportData">
              <el-icon><Download /></el-icon> 导出数据
            </el-button>
          </div>
        </template>
        <el-table :data="rangeResult" stripe height="400" style="width: 100%">
          <el-table-column prop="date" label="日期" width="120" sortable />
          <el-table-column prop="aqi" label="AQI" width="100" sortable>
            <template #default="{ row }">
              <span :class="getAqiClass(row.aqi)">{{ row.aqi }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="qualityLevel" label="等级" width="100" />
          <el-table-column prop="pm25" label="PM2.5" width="90" />
          <el-table-column prop="pm10" label="PM10" width="90" />
          <el-table-column prop="o3" label="O₃" width="80" />
          <el-table-column prop="so2" label="SO₂" width="80" />
          <el-table-column prop="co" label="CO" width="80" />
          <el-table-column prop="no2" label="NO₂" width="80" />
          <el-table-column prop="primaryPollutant" label="首要污染物" />
        </el-table>
      </el-card>
    </template>

    <!-- 空状态 -->
    <el-empty v-if="!loading && queryForm.mode === 'single' && !singleResult" description="请选择查询条件并点击查询按钮" />
    <el-empty v-if="!loading && queryForm.mode === 'range' && rangeResult.length === 0" description="请选择日期范围并点击查询按钮" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import {
  GridComponent, TooltipComponent, LegendComponent,
  TitleComponent, MarkPointComponent, MarkLineComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import { ElMessage } from 'element-plus'
import {
  Search, WindPower, Cloudy, TrendCharts, Trophy,
  DataLine, Histogram, List, Download
} from '@element-plus/icons-vue'
import { queryAirQualityByDate, queryAirQualityByDateRange } from '../api/zhengzhou.js'

use([
  CanvasRenderer, LineChart, BarChart,
  GridComponent, TooltipComponent, LegendComponent,
  TitleComponent, MarkPointComponent, MarkLineComponent
])

// 站点列表
const stations = ref([
  { code: '中原区', name: '中原区' },
  { code: '金水区', name: '金水区' },
  { code: '二七区', name: '二七区' },
  { code: '惠济区', name: '惠济区' },
  { code: '郑东新区', name: '郑东新区' }
])

// 查询表单
const queryForm = ref({
  mode: 'single',
  station: '中原区',
  date: '',
  dateRange: []
})

// 快捷日期选项
const quickDates = ref([
  { label: '今天', type: 'today' },
  { label: '昨天', type: 'yesterday' },
  { label: '近7天', type: 'last7days' },
  { label: '近30天', type: 'last30days' },
  { label: '本月', type: 'thisMonth' },
  { label: '上月', type: 'lastMonth' }
])

const loading = ref(false)
const singleResult = ref(null)
const rangeResult = ref([])
const rangeChartType = ref('aqi')

// 污染物详情
const pollutantDetails = computed(() => {
  if (!singleResult.value) return []
  const data = singleResult.value.data
  return [
    { name: 'PM2.5', value: data.pm25, unit: 'μg/m³', color: '#ff5555' },
    { name: 'PM10', value: data.pm10, unit: 'μg/m³', color: '#ffaa00' },
    { name: 'O₃', value: data.o3, unit: 'μg/m³', color: '#00d4ff' },
    { name: 'SO₂', value: data.so2, unit: 'μg/m³', color: '#00ff88' },
    { name: 'NO₂', value: data.no2, unit: 'μg/m³', color: '#aa66ff' },
    { name: 'CO', value: data.co, unit: 'mg/m³', color: '#ff66aa' }
  ]
})

// 区间统计
const rangeStats = computed(() => {
  if (rangeResult.value.length === 0) return []
  
  const aqis = rangeResult.value.map(d => d.aqi)
  const avg = Math.round(aqis.reduce((a, b) => a + b, 0) / aqis.length)
  const max = Math.max(...aqis)
  const min = Math.min(...aqis)
  const goodDays = rangeResult.value.filter(d => d.aqi <= 100).length
  const pollutionDays = rangeResult.value.filter(d => d.aqi > 100).length
  
  return [
    { label: '平均AQI', value: avg, color: '#00d4ff' },
    { label: '最高AQI', value: max, color: '#ff5555' },
    { label: '最低AQI', value: min, color: '#00ff88' },
    { label: '优良天数', value: goodDays + '天', color: '#00ff88' },
    { label: '污染天数', value: pollutionDays + '天', color: '#ff5555' },
    { label: '总天数', value: rangeResult.value.length + '天', color: '#ffaa00' }
  ]
})

// 前后7天图表配置
const nearbyChartOption = computed(() => {
  if (!singleResult.value) return {}
  
  const data = singleResult.value.nearbyData
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map(d => d.date),
      axisLabel: { 
        color: '#94a3b8',
        formatter: (value) => {
          const date = new Date(value)
          return `${date.getMonth() + 1}/${date.getDate()}`
        }
      }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#94a3b8' },
      splitLine: { lineStyle: { color: '#2a3441' } }
    },
    series: [{
      data: data.map(d => ({
        value: d.aqi,
        itemStyle: d.isTarget ? { color: '#00d4ff', borderWidth: 2, borderColor: '#fff' } : { color: '#3b82f6' }
      })),
      type: 'line',
      smooth: true,
      symbolSize: (val, params) => params.data.itemStyle.borderWidth ? 12 : 8,
      lineStyle: { width: 3, color: '#3b82f6' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(59, 130, 246, 0.3)' },
            { offset: 1, color: 'rgba(59, 130, 246, 0.05)' }
          ]
        }
      },
      markPoint: {
        data: data.filter(d => d.isTarget).map(d => ({
          name: '查询日期',
          coord: [d.date, d.aqi],
          value: d.aqi
        }))
      }
    }]
  }
})

// 历史同期图表配置
const historicalChartOption = computed(() => {
  if (!singleResult.value) return {}
  
  const data = singleResult.value.historicalSameDay
  const currentAqi = singleResult.value.data.aqi
  
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: [...data.map(d => d.year), '今年'],
      axisLabel: { color: '#94a3b8' }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#94a3b8' },
      splitLine: { lineStyle: { color: '#2a3441' } }
    },
    series: [{
      data: [...data.map(d => d.aqi), { value: currentAqi, itemStyle: { color: '#00d4ff' } }],
      type: 'bar',
      barWidth: '50%',
      itemStyle: {
        borderRadius: [4, 4, 0, 0],
        color: (params) => params.dataIndex === data.length ? '#00d4ff' : '#3b82f6'
      }
    }]
  }
})

// 区间图表配置
const rangeChartOption = computed(() => {
  if (rangeResult.value.length === 0) return {}
  
  const field = rangeChartType.value
  const labelMap = { aqi: 'AQI', pm25: 'PM2.5', pm10: 'PM10' }
  const colorMap = { aqi: '#00d4ff', pm25: '#ff5555', pm10: '#ffaa00' }
  
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: rangeResult.value.map(d => d.date),
      axisLabel: { 
        color: '#94a3b8',
        rotate: 45,
        formatter: (value) => {
          const date = new Date(value)
          return `${date.getMonth() + 1}/${date.getDate()}`
        }
      }
    },
    yAxis: {
      type: 'value',
      name: labelMap[field],
      axisLabel: { color: '#94a3b8' },
      splitLine: { lineStyle: { color: '#2a3441' } }
    },
    series: [{
      data: rangeResult.value.map(d => d[field]),
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 2, color: colorMap[field] },
      itemStyle: { color: colorMap[field] },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: colorMap[field] + '40' },
            { offset: 1, color: colorMap[field] + '05' }
          ]
        }
      }
    }]
  }
})

// 获取AQI样式类
const getAqiClass = (aqi) => {
  if (aqi <= 50) return 'aqi-excellent'
  if (aqi <= 100) return 'aqi-good'
  if (aqi <= 150) return 'aqi-moderate'
  if (aqi <= 200) return 'aqi-unhealthy'
  if (aqi <= 300) return 'aqi-very-unhealthy'
  return 'aqi-hazardous'
}

// 禁用日期 - 允许选择2015-2030年的日期，支持自定义数据
const disabledDate = (time) => {
  const minDate = new Date('2015-01-01')
  const maxDate = new Date('2030-12-31')
  return time.getTime() < minDate.getTime() || time.getTime() > maxDate.getTime()
}

// 应用快捷日期
const applyQuickDate = (item) => {
  const today = new Date()
  const formatDate = (d) => d.toISOString().split('T')[0]
  
  switch (item.type) {
    case 'today':
      queryForm.value.date = formatDate(today)
      queryForm.value.mode = 'single'
      break
    case 'yesterday':
      const yesterday = new Date(today)
      yesterday.setDate(yesterday.getDate() - 1)
      queryForm.value.date = formatDate(yesterday)
      queryForm.value.mode = 'single'
      break
    case 'last7days':
      const last7 = new Date(today)
      last7.setDate(last7.getDate() - 7)
      queryForm.value.dateRange = [formatDate(last7), formatDate(today)]
      queryForm.value.mode = 'range'
      break
    case 'last30days':
      const last30 = new Date(today)
      last30.setDate(last30.getDate() - 30)
      queryForm.value.dateRange = [formatDate(last30), formatDate(today)]
      queryForm.value.mode = 'range'
      break
    case 'thisMonth':
      const firstDay = new Date(today.getFullYear(), today.getMonth(), 1)
      queryForm.value.dateRange = [formatDate(firstDay), formatDate(today)]
      queryForm.value.mode = 'range'
      break
    case 'lastMonth':
      const lastMonthStart = new Date(today.getFullYear(), today.getMonth() - 1, 1)
      const lastMonthEnd = new Date(today.getFullYear(), today.getMonth(), 0)
      queryForm.value.dateRange = [formatDate(lastMonthStart), formatDate(lastMonthEnd)]
      queryForm.value.mode = 'range'
      break
  }
}

// 模式切换
const handleModeChange = () => {
  singleResult.value = null
  rangeResult.value = []
}

// 查询
const handleQuery = async () => {
  if (!queryForm.value.station) {
    ElMessage.warning('请选择站点')
    return
  }
  
  loading.value = true
  
  try {
    if (queryForm.value.mode === 'single') {
      if (!queryForm.value.date) {
        ElMessage.warning('请选择日期')
        loading.value = false
        return
      }
      const result = await queryAirQualityByDate(queryForm.value.station, queryForm.value.date)
      if (result.success) {
        singleResult.value = result
        if (result.isMock) {
          ElMessage.warning('当前显示的是模拟数据，CSV文件中未找到该日期数据')
        } else if (result.isCustom) {
          ElMessage.success('查询成功（数据来源：用户自定义数据）')
        } else {
          ElMessage.success('查询成功（数据来源：CSV文件）')
        }
      } else {
        ElMessage.error(result.message || '查询失败')
      }
    } else {
      if (!queryForm.value.dateRange || queryForm.value.dateRange.length !== 2) {
        ElMessage.warning('请选择日期范围')
        loading.value = false
        return
      }
      const result = await queryAirQualityByDateRange(
        queryForm.value.station,
        queryForm.value.dateRange[0],
        queryForm.value.dateRange[1]
      )
      rangeResult.value = result
      // 检查是否为模拟数据（通过检查第一条数据是否有isMock标记）
      if (result.length > 0 && result[0].isMock) {
        ElMessage.warning('当前显示的是模拟数据，CSV文件中未找到该日期范围数据')
      } else {
        // 检查是否包含自定义数据
        const customCount = result.filter(r => r.isCustom).length
        if (customCount > 0) {
          ElMessage.success(`查询成功，共 ${result.length} 条数据（其中 ${customCount} 条为用户自定义数据）`)
        } else {
          ElMessage.success(`查询成功，共 ${result.length} 条数据（数据来源：CSV文件）`)
        }
      }
    }
  } catch (error) {
    ElMessage.error('查询失败，请稍后重试')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 导出数据
const exportData = () => {
  if (rangeResult.value.length === 0) {
    ElMessage.warning('没有数据可导出')
    return
  }
  
  const headers = ['日期', 'AQI', '等级', 'PM2.5', 'PM10', 'O3', 'SO2', 'CO', 'NO2', '首要污染物']
  const rows = rangeResult.value.map(d => [
    d.date, d.aqi, d.qualityLevel, d.pm25, d.pm10, d.o3, d.so2, d.co, d.no2, d.primaryPollutant
  ])
  
  const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `${queryForm.value.station}_数据查询_${queryForm.value.dateRange[0]}_${queryForm.value.dateRange[1]}.csv`
  link.click()
  
  ElMessage.success('数据导出成功')
}

onMounted(() => {
  // 默认选择昨天
  const yesterday = new Date()
  yesterday.setDate(yesterday.getDate() - 1)
  queryForm.value.date = yesterday.toISOString().split('T')[0]
})
</script>

<style scoped>
.data-query {
  padding: 0;
}

.page-header {
  text-align: center;
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 24px;
  margin-bottom: 8px;
  background: linear-gradient(90deg, #00d4ff, #00ff88);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.subtitle {
  color: #94a3b8;
  font-size: 14px;
}

.query-section {
  margin-bottom: 24px;
}

.query-card {
  background: #1a2332;
  border: 1px solid #2a3441;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 500;
}

.card-header span:first-child {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-date {
  color: #94a3b8;
  font-size: 14px;
}

.quick-date-tag {
  cursor: pointer;
  transition: all 0.3s;
}

.quick-date-tag:hover {
  color: #00d4ff;
  border-color: #00d4ff;
}

.result-overview {
  margin-bottom: 24px;
}

.overview-card {
  background: linear-gradient(135deg, #1a2332 0%, #243447 100%);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid #2a3441;
  position: relative;
  overflow: hidden;
  height: 120px;
}

.overview-card.small {
  height: 90px;
  padding: 15px;
}

.card-bg-icon {
  position: absolute;
  right: -10px;
  bottom: -10px;
  font-size: 80px;
  color: #3b82f6;
  opacity: 0.1;
}

.overview-card.small .card-bg-icon {
  font-size: 50px;
}

.overview-content {
  position: relative;
  z-index: 1;
}

.overview-label {
  color: #94a3b8;
  font-size: 13px;
  margin-bottom: 8px;
}

.overview-value {
  font-size: 36px;
  font-weight: bold;
  color: #fff;
  line-height: 1.2;
}

.overview-level {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  margin-top: 8px;
  background: rgba(255, 255, 255, 0.1);
}

.overview-sub {
  color: #94a3b8;
  font-size: 12px;
  margin-top: 4px;
}

.aqi-card {
  border: 2px solid transparent;
}

.aqi-card.aqi-excellent { border-color: #00e400; }
.aqi-card.aqi-good { border-color: #ffff00; }
.aqi-card.aqi-moderate { border-color: #ff7e00; }
.aqi-card.aqi-unhealthy { border-color: #ff0000; }
.aqi-card.aqi-very-unhealthy { border-color: #8f3f97; }
.aqi-card.aqi-hazardous { border-color: #7e0023; }

.detail-card, .chart-card, .table-card {
  background: #1a2332;
  border: 1px solid #2a3441;
  margin-bottom: 24px;
}

.pollutant-item {
  background: #243447;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  border-left: 4px solid;
}

.pollutant-name {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 8px;
}

.pollutant-value {
  font-size: 24px;
  font-weight: bold;
  color: #fff;
  margin-bottom: 4px;
}

.pollutant-unit {
  font-size: 11px;
  color: #64748b;
}

.chart {
  height: 350px;
}

.aqi-excellent { color: #00e400; font-weight: bold; }
.aqi-good { color: #ffff00; font-weight: bold; }
.aqi-moderate { color: #ff7e00; font-weight: bold; }
.aqi-unhealthy { color: #ff0000; font-weight: bold; }
.aqi-very-unhealthy { color: #8f3f97; font-weight: bold; }
.aqi-hazardous { color: #7e0023; font-weight: bold; }

:deep(.el-card__header) {
  border-bottom: 1px solid #2a3441;
  background: rgba(0, 212, 255, 0.05);
}

:deep(.el-form-item__label) {
  color: #94a3b8;
}

:deep(.el-input__wrapper),
:deep(.el-textarea__inner) {
  background: #243447;
  box-shadow: 0 0 0 1px #2a3441 inset;
}

:deep(.el-input__inner) {
  color: #fff;
}

:deep(.el-radio-button__inner) {
  background: #243447;
  border-color: #2a3441;
  color: #94a3b8;
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: #00d4ff;
  border-color: #00d4ff;
  color: #000;
}

:deep(.el-table) {
  background: transparent;
}

:deep(.el-table th) {
  background: #1a2332;
  color: #00d4ff;
  font-weight: 600;
  border-bottom: 2px solid #00d4ff;
}

:deep(.el-table td) {
  background: transparent;
  color: #e2e8f0;
  font-weight: 500;
}

/* 斑马纹效果 - 奇数行 */
:deep(.el-table__body tr:nth-child(odd) td) {
  background: rgba(36, 52, 71, 0.6);
}

/* 斑马纹效果 - 偶数行 */
:deep(.el-table__body tr:nth-child(even) td) {
  background: rgba(26, 35, 50, 0.8);
}

/* 行悬停效果 */
:deep(.el-table__body tr:hover td) {
  background: rgba(0, 212, 255, 0.2) !important;
  color: #fff;
}

/* 固定列背景 */
:deep(.el-table__fixed-right-patch) {
  background: #1a2332;
}

/* 表格边框 */
:deep(.el-table::before) {
  background: #2a3441;
}

:deep(.el-table--border) {
  border-color: #2a3441;
}

:deep(.el-table--border td),
:deep(.el-table--border th) {
  border-color: #2a3441;
}
</style>
