<template>
  <div class="prediction-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>AQI 智能预测中心</h1>
      <p class="subtitle">选择预测方式，获取空气质量预测结果</p>
    </div>

    <!-- 快速选择区域 -->
    <div class="quick-select-section">
      <div class="select-row">
        <div class="select-group">
          <label>选择站点</label>
          <el-select v-model="selectedStation" placeholder="请选择监测站点" size="large" style="width: 180px">
            <el-option 
              v-for="item in stations" 
              :key="item.code" 
              :label="item.name" 
              :value="item.code" 
            />
          </el-select>
        </div>
        
        <div class="select-group">
          <label>预测方式</label>
          <el-radio-group v-model="predictionMode" size="large">
            <el-radio-button label="current">
              <el-icon><Clock /></el-icon> 实时趋势预测
            </el-radio-button>
            <el-radio-button label="historical">
              <el-icon><Calendar /></el-icon> 历史同期预测
            </el-radio-button>
          </el-radio-group>
        </div>

        <el-button type="primary" size="large" @click="runPrediction" :loading="loading">
          <el-icon><Search /></el-icon> 开始预测
        </el-button>
      </div>
    </div>

    <!-- 预测配置卡片 -->
    <div class="config-cards">
      <!-- 实时趋势预测配置 -->
      <div v-if="predictionMode === 'current'" class="config-card">
        <div class="config-title">
          <el-icon><Timer /></el-icon>
          预测时长
        </div>
        <el-radio-group v-model="predictionHours" size="default">
          <el-radio-button :label="12">12小时</el-radio-button>
          <el-radio-button :label="24">24小时</el-radio-button>
          <el-radio-button :label="48">48小时</el-radio-button>
          <el-radio-button :label="72">72小时</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 历史同期预测配置 -->
      <div v-if="predictionMode === 'historical'" class="config-card">
        <div class="config-title">
          <el-icon><Calendar /></el-icon>
          预测日期
        </div>
        <div class="date-options">
          <el-radio-group v-model="historicalDateMode" size="default">
            <el-radio-button label="single">单日预测</el-radio-button>
            <el-radio-button label="range">区间预测</el-radio-button>
          </el-radio-group>
          
          <div class="date-picker-wrapper">
            <el-date-picker
              v-if="historicalDateMode === 'single'"
              v-model="historicalDate"
              type="date"
              placeholder="选择预测日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              :disabled-date="disabledFutureDate"
              size="default"
              style="width: 180px"
            />
            <el-date-picker
              v-else
              v-model="historicalDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              :disabled-date="disabledFutureDate"
              size="default"
              style="width: 280px"
            />
          </div>
        </div>
      </div>

      <!-- 快捷选项 -->
      <div class="config-card tips-card">
        <div class="config-title">
          <el-icon><InfoFilled /></el-icon>
          预测说明
        </div>
        <div class="tips-content">
          <p v-if="predictionMode === 'current'">
            <strong>实时趋势预测</strong>：基于最近24小时监测数据，通过趋势分析预测未来空气质量变化
          </p>
          <p v-else>
            <strong>历史同期预测</strong>：基于过去5年(2019-2023)同期历史数据，提供参考预测值
          </p>
        </div>
      </div>
    </div>

    <!-- 预测结果区域 -->
    <div v-if="hasResult" class="results-section">
      <div class="results-header">
        <h2>
          <el-icon><TrendCharts /></el-icon>
          预测结果
        </h2>
        <el-tag :type="resultLevel.type" size="large" effect="dark">
          {{ resultLevel.text }}
        </el-tag>
      </div>

      <!-- 核心数据卡片 -->
      <div class="main-cards">
        <div class="main-card primary">
          <div class="card-label">预测平均 AQI</div>
          <div class="card-value" :class="getAqiClass(resultOverview.predictedAvg)">
            {{ resultOverview.predictedAvg }}
          </div>
          <div class="card-level" :class="getAqiClass(resultOverview.predictedAvg)">
            {{ getAqiLevel(resultOverview.predictedAvg) }}
          </div>
        </div>
        
        <div class="main-card">
          <div class="card-label">预测峰值</div>
          <div class="card-value" :class="getAqiClass(resultOverview.predictedMax)">
            {{ resultOverview.predictedMax }}
          </div>
          <div class="card-sub">{{ getAqiLevel(resultOverview.predictedMax) }}</div>
        </div>
        
        <div class="main-card">
          <div class="card-label">预测谷值</div>
          <div class="card-value" :class="getAqiClass(resultOverview.predictedMin)">
            {{ resultOverview.predictedMin }}
          </div>
          <div class="card-sub">{{ getAqiLevel(resultOverview.predictedMin) }}</div>
        </div>
        
        <div class="main-card">
          <div class="card-label">预测置信度</div>
          <div class="card-value confidence">{{ resultOverview.confidence }}%</div>
          <el-progress 
            :percentage="resultOverview.confidence" 
            :color="confidenceColors"
            :stroke-width="8"
            class="confidence-bar"
          />
        </div>
      </div>

      <!-- 趋势图表 -->
      <div class="chart-section">
        <div class="chart-title">AQI 变化趋势预测</div>
        <v-chart class="trend-chart" :option="trendChartOption" autoresize />
      </div>

      <!-- 详细数据表格 -->
      <div class="detail-section">
        <div class="section-title">详细预测数据</div>
        <el-table 
          :data="predictionDetails" 
          style="width: 100%" 
          max-height="400"
          :stripe="true"
          border
        >
          <el-table-column prop="time" label="时间" width="120" align="center" />
          <el-table-column prop="aqi" label="预测AQI" width="100" align="center">
            <template #default="{ row }">
              <span :class="getAqiClass(row.aqi)" class="aqi-value">{{ row.aqi }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="level" label="等级" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="getAqiTagType(row.aqi)" size="small" effect="dark">
                {{ row.level }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="primaryPollutant" label="首要污染物" width="110" align="center" />
          <el-table-column label="健康建议" min-width="250">
            <template #default="{ row }">
              <span class="health-tip">{{ getHealthAdvice(row.aqi) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <el-icon class="empty-icon"><DataAnalysis /></el-icon>
      <p>请选择预测条件，点击"开始预测"按钮</p>
      <p class="empty-sub">系统将基于历史数据为您提供空气质量预测分析</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import {
  GridComponent, TooltipComponent, LegendComponent,
  TitleComponent, MarkLineComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import { Clock, Calendar, Search, Timer, InfoFilled, TrendCharts, DataAnalysis } from '@element-plus/icons-vue'
import { 
  getPrediction, 
  getHistoricalPrediction,
  getHistoricalPredictionRange
} from '../api/zhengzhou'
import { HOT_CITIES } from '../api/weather.js'
import { ElMessage } from 'element-plus'

use([
  CanvasRenderer, LineChart, BarChart,
  GridComponent, TooltipComponent, LegendComponent,
  TitleComponent, MarkLineComponent
])

// 基础数据
const stations = ref(HOT_CITIES.map(city => ({ code: city.name, name: city.name })))
const selectedStation = ref('中原区')
const predictionMode = ref('current')
const loading = ref(false)
const hasResult = ref(false)

// 预测配置
const predictionHours = ref(24)
const historicalDateMode = ref('single')
const historicalDate = ref('')
const historicalDateRange = ref([])

// 预测结果
const resultOverview = ref({
  predictedAvg: 0,
  predictedMax: 0,
  predictedMin: 0,
  confidence: 0
})
const predictionDetails = ref([])

// 置信度颜色
const confidenceColors = [
  { color: '#f56c6c', percentage: 60 },
  { color: '#e6a23c', percentage: 80 },
  { color: '#67c23a', percentage: 100 }
]

// 禁用未来日期
const disabledFutureDate = (date) => {
  return date.getTime() > Date.now()
}

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

// 健康建议
const getHealthAdvice = (aqi) => {
  if (aqi <= 50) return '空气质量优秀，适合各类户外活动'
  if (aqi <= 100) return '空气质量良好，可以正常进行户外活动'
  if (aqi <= 150) return '敏感人群应减少户外活动，一般人群适量减少'
  if (aqi <= 200) return '建议减少户外活动，外出佩戴口罩'
  if (aqi <= 300) return '避免户外活动，外出务必佩戴防护口罩'
  return '严重污染，请留在室内，关闭门窗'
}

// 结果等级标签
const resultLevel = computed(() => {
  const avg = resultOverview.value.predictedAvg
  if (avg <= 50) return { type: 'success', text: '优良' }
  if (avg <= 100) return { type: '', text: '良好' }
  if (avg <= 150) return { type: 'warning', text: '轻度污染' }
  if (avg <= 200) return { type: 'danger', text: '中度污染' }
  return { type: 'danger', text: '重度污染' }
})

// 趋势图表配置
const trendChartOption = ref({
  tooltip: {
    trigger: 'axis',
    formatter: function(params) {
      const data = params[0]
      return `<div style="font-weight:bold;margin-bottom:5px">${data.axisValue}</div>
              <div style="display:flex;align-items:center;gap:8px">
                <span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${data.color}"></span>
                <span>AQI: <b>${data.value}</b></span>
              </div>`
    }
  },
  grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: [],
    axisLabel: { color: '#94a3b8' },
    axisLine: { lineStyle: { color: '#2a3441' } }
  },
  yAxis: {
    type: 'value',
    min: 0,
    max: 300,
    axisLabel: { color: '#94a3b8' },
    splitLine: { lineStyle: { color: '#2a3441', type: 'dashed' } }
  },
  visualMap: {
    show: false,
    pieces: [
      { min: 0, max: 50, color: '#00e400' },
      { min: 51, max: 100, color: '#ffff00' },
      { min: 101, max: 150, color: '#ff7e00' },
      { min: 151, max: 200, color: '#ff0000' },
      { min: 201, max: 300, color: '#8f3f97' }
    ],
    outOfRange: { color: '#7e0023' }
  },
  series: [{
    type: 'line',
    smooth: true,
    data: [],
    lineStyle: { width: 3 },
    areaStyle: {
      opacity: 0.3,
      color: {
        type: 'linear',
        x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(0, 212, 255, 0.5)' },
          { offset: 1, color: 'rgba(0, 212, 255, 0.05)' }
        ]
      }
    },
    symbol: 'circle',
    symbolSize: 8
  }]
})

// 运行预测
const runPrediction = async () => {
  if (predictionMode.value === 'historical') {
    if (historicalDateMode.value === 'single' && !historicalDate.value) {
      ElMessage.warning('请选择预测日期')
      return
    }
    if (historicalDateMode.value === 'range' && (!historicalDateRange.value || historicalDateRange.value.length !== 2)) {
      ElMessage.warning('请选择日期区间')
      return
    }
  }

  loading.value = true
  hasResult.value = false

  try {
    if (predictionMode.value === 'current') {
      // 实时趋势预测
      const data = await getPrediction(selectedStation.value, predictionHours.value)
      
      resultOverview.value = {
        predictedAvg: Math.round(data.predictedAvg) || 0,
        predictedMax: data.predictions ? Math.max(...data.predictions.map(p => p.predictedAqi)) : 0,
        predictedMin: data.predictions ? Math.min(...data.predictions.map(p => p.predictedAqi)) : 0,
        confidence: data.confidence || 85
      }
      
      predictionDetails.value = data.predictions.map(p => ({
        time: p.hour,
        aqi: p.predictedAqi,
        level: p.level,
        primaryPollutant: p.primaryPollutant
      }))
      
      // 更新图表
      trendChartOption.value.xAxis.data = data.predictions.map(p => p.hour)
      trendChartOption.value.series[0].data = data.predictions.map(p => p.predictedAqi)
    } else {
      // 历史同期预测
      let data
      if (historicalDateMode.value === 'single') {
        data = await getHistoricalPrediction(selectedStation.value, historicalDate.value, 24)
      } else {
        data = await getHistoricalPredictionRange(
          selectedStation.value, 
          historicalDateRange.value[0], 
          historicalDateRange.value[1]
        )
      }
      
      resultOverview.value = {
        predictedAvg: data.predictedAvg || data.periodAvg || 0,
        predictedMax: data.predictedMax || data.periodMax || 0,
        predictedMin: data.predictedMin || data.periodMin || 0,
        confidence: data.confidence || 78
      }
      
      if (historicalDateMode.value === 'single') {
        predictionDetails.value = data.predictions.map(p => ({
          time: p.hour,
          aqi: p.predictedAqi,
          level: p.level,
          primaryPollutant: p.primaryPollutant
        }))
        trendChartOption.value.xAxis.data = data.predictions.map(p => p.hour)
        trendChartOption.value.series[0].data = data.predictions.map(p => p.predictedAqi)
      } else {
        predictionDetails.value = data.dailyPredictions.map(p => ({
          time: p.date,
          aqi: p.predictedAvg,
          level: p.level,
          primaryPollutant: 'PM2.5'
        }))
        trendChartOption.value.xAxis.data = data.dailyPredictions.map(p => p.date.slice(5))
        trendChartOption.value.series[0].data = data.dailyPredictions.map(p => p.predictedAvg)
      }
    }
    
    hasResult.value = true
    ElMessage.success('预测完成')
  } catch (error) {
    console.error('预测失败:', error)
    ElMessage.error('预测失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  // 设置默认历史日期为昨天
  const yesterday = new Date()
  yesterday.setDate(yesterday.getDate() - 1)
  historicalDate.value = yesterday.toISOString().split('T')[0]
})
</script>

<style scoped>
.prediction-page {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 30px;
}

.page-header h1 {
  font-size: 28px;
  color: #c5e0f5;
  margin-bottom: 8px;
}

.page-header .subtitle {
  font-size: 14px;
  color: #8ec5e8;
}

/* 快速选择区域 */
.quick-select-section {
  background: linear-gradient(135deg, #1a2332 0%, #243447 100%);
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  border: 1px solid #2a3441;
}

.select-row {
  display: flex;
  align-items: flex-end;
  gap: 24px;
  flex-wrap: wrap;
}

.select-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.select-group label {
  font-size: 13px;
  color: #a0c4e8;
  font-weight: 500;
}

:deep(.el-radio-button__inner) {
  background: #1a2332;
  border-color: #2a3441;
  color: #8ec5e8;
  display: flex;
  align-items: center;
  gap: 6px;
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: #1e3a5f;
  border-color: #00d4ff;
  color: #c5e0f5;
  box-shadow: 0 0 10px rgba(0, 212, 255, 0.3);
}

/* 配置卡片 */
.config-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.config-card {
  background: #1a2332;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #2a3441;
}

.config-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 500;
  color: #c5e0f5;
  margin-bottom: 16px;
}

.date-options {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tips-card {
  background: linear-gradient(135deg, #1e3a5f 0%, #1a2332 100%);
}

.tips-content {
  color: #8ec5e8;
  font-size: 13px;
  line-height: 1.8;
}

.tips-content strong {
  color: #00d4ff;
}

/* 结果区域 */
.results-section {
  animation: fadeIn 0.5s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.results-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #2a3441;
}

.results-header h2 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  color: #c5e0f5;
}

/* 核心数据卡片 */
.main-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.main-card {
  background: linear-gradient(135deg, #1a2332 0%, #243447 100%);
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  border: 1px solid #2a3441;
  transition: transform 0.3s;
}

.main-card:hover {
  transform: translateY(-4px);
}

.main-card.primary {
  background: linear-gradient(135deg, #1e3a5f 0%, #2d5a87 100%);
  border-color: #00d4ff;
}

.card-label {
  font-size: 13px;
  color: #a0c4e8;
  margin-bottom: 12px;
}

.card-value {
  font-size: 42px;
  font-weight: bold;
  margin-bottom: 8px;
}

.card-value.confidence {
  font-size: 36px;
  color: #00d4ff;
}

.card-level {
  font-size: 14px;
  padding: 4px 12px;
  border-radius: 12px;
  display: inline-block;
  background: rgba(0, 212, 255, 0.2);
}

.card-sub {
  font-size: 13px;
  color: #8ec5e8;
}

.confidence-bar {
  margin-top: 12px;
}

/* 图表区域 */
.chart-section {
  background: #1a2332;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #2a3441;
  margin-bottom: 24px;
}

.chart-title {
  font-size: 16px;
  color: #c5e0f5;
  margin-bottom: 16px;
  font-weight: 500;
}

.trend-chart {
  height: 350px;
}

/* 详细数据区域 */
.detail-section {
  background: #1a2332;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #2a3441;
}

.section-title {
  font-size: 16px;
  color: #c5e0f5;
  margin-bottom: 16px;
  font-weight: 500;
}

.health-tip {
  font-size: 13px;
  color: #8ec5e8;
}

.aqi-value {
  font-weight: bold;
  font-size: 15px;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #8ec5e8;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
  opacity: 0.5;
}

.empty-sub {
  font-size: 13px;
  margin-top: 8px;
  opacity: 0.7;
}

/* AQI颜色 */
.aqi-excellent { color: #00e400; }
.aqi-good { color: #ffff00; }
.aqi-moderate { color: #ff7e00; }
.aqi-unhealthy { color: #ff0000; }
.aqi-very-unhealthy { color: #8f3f97; }
.aqi-hazardous { color: #7e0023; }

/* 表格样式 */
:deep(.el-table) {
  background: transparent;
  border-radius: 8px;
  overflow: hidden;
}

:deep(.el-table th) {
  background: #243447;
  color: #a0c4e8;
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
  .main-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .main-cards {
    grid-template-columns: 1fr;
  }
  
  .select-row {
    flex-direction: column;
    align-items: stretch;
  }
  
  .select-group {
    width: 100%;
  }
}
</style>
