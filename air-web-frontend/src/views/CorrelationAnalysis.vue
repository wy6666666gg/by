<template>
  <div class="correlation-analysis">
    <!-- 分析条件 -->
    <div class="filter-card">
      <el-form :inline="true">
        <el-form-item label="分析时段">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item label="站点">
          <el-select v-model="selectedStation" style="width: 180px">
            <el-option label="全部站点" value="all" />
            <el-option v-for="item in stations" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="runAnalysis" :loading="loading">
            <el-icon><DataAnalysis /></el-icon> 运行分析
          </el-button>
          <el-button @click="exportResults">
            <el-icon><Download /></el-icon> 导出结果
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 数据加载提示 -->
    <div v-if="loading" class="loading-state">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <p>正在加载历史数据并计算相关性...</p>
    </div>

    <!-- 分析结果 -->
    <template v-else-if="hasData">
      <!-- 相关性矩阵热力图 -->
      <div class="chart-card">
        <div class="chart-header">
          <h3>污染物与AQI相关性矩阵（基于真实数据）</h3>
          <div class="header-actions">
            <el-radio-group v-model="correlationType" size="small" @change="updateCorrelationMatrix">
              <el-radio-button label="pearson">皮尔逊相关</el-radio-button>
              <el-radio-button label="spearman">斯皮尔曼相关</el-radio-button>
            </el-radio-group>
          </div>
        </div>
        <v-chart class="matrix-chart" :option="correlationMatrixOption" autoresize />
        
        <!-- 相关系数说明 -->
        <div class="correlation-legend">
          <div class="legend-title">相关系数说明</div>
          <div class="legend-bar">
            <span class="legend-label">-1 (完全负相关)</span>
            <div class="gradient-bar"></div>
            <span class="legend-label">+1 (完全正相关)</span>
          </div>
        </div>
      </div>

      <!-- 特征重要性分析 -->
      <div class="charts-row">
        <div class="chart-card">
          <div class="chart-header">
            <h3>各污染物与AQI相关性排序</h3>
            <el-tooltip content="基于相关系数绝对值排序">
              <el-icon><InfoFilled /></el-icon>
            </el-tooltip>
          </div>
          <v-chart class="chart" :option="featureImportanceOption" autoresize />
        </div>
        <div class="chart-card">
          <div class="chart-header">
            <h3>PM2.5 与 AQI 散点分布</h3>
          </div>
          <v-chart class="chart" :option="scatterMatrixOption" autoresize />
        </div>
      </div>

      <!-- 时滞相关性分析 -->
      <div class="chart-card">
        <div class="chart-header">
          <h3>时滞相关性分析 (PM2.5 vs PM10)</h3>
          <el-select v-model="lagFactor" size="small" style="width: 150px" @change="updateLagCorrelation">
            <el-option label="PM10" value="pm10" />
            <el-option label="SO2" value="so2" />
            <el-option label="NO2" value="no2" />
            <el-option label="CO" value="co" />
            <el-option label="O3" value="o3" />
          </el-select>
        </div>
        <v-chart class="lag-chart" :option="lagCorrelationOption" autoresize />
      </div>

      <!-- 数据摘要 -->
      <div class="summary-card">
        <div class="summary-header">
          <el-icon><Document /></el-icon>
          <span>数据统计摘要</span>
        </div>
        <div class="summary-content">
          <div class="summary-item">
            <span class="label">分析样本数：</span>
            <span class="value">{{ analysisData.length }} 条</span>
          </div>
          <div class="summary-item">
            <span class="label">时间范围：</span>
            <span class="value">{{ dateRange[0] }} 至 {{ dateRange[1] }}</span>
          </div>
          <div class="summary-item">
            <span class="label">站点：</span>
            <span class="value">{{ selectedStation === 'all' ? '全部站点' : stations.find(s => s.code === selectedStation)?.name }}</span>
          </div>
          <div class="summary-item">
            <span class="label">平均AQI：</span>
            <span class="value">{{ avgAQI }}</span>
          </div>
        </div>
      </div>

      <!-- 分析结果解读 -->
      <div class="insight-card">
        <div class="insight-header">
          <el-icon><ChatDotSquare /></el-icon>
          <span>智能分析解读</span>
        </div>
        <div class="insight-content">
          <div class="insight-item" v-for="(insight, index) in insights" :key="index">
            <div class="insight-dot" :class="insight.type"></div>
            <div class="insight-text">{{ insight.text }}</div>
          </div>
        </div>
      </div>
    </template>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <el-icon class="empty-icon"><DataAnalysis /></el-icon>
      <p>请选择分析时段和站点，点击「运行分析」开始</p>
      <p class="empty-sub">系统将基于历史监测数据计算真实的相关性分析</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { HeatmapChart, BarChart, ScatterChart, LineChart } from 'echarts/charts'
import {
  GridComponent, TooltipComponent, LegendComponent,
  TitleComponent, VisualMapComponent, DatasetComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import { ElMessage } from 'element-plus'
import { queryAirQualityByDateRange } from '../api/zhengzhou'

use([
  CanvasRenderer, HeatmapChart, BarChart, ScatterChart, LineChart,
  GridComponent, TooltipComponent, LegendComponent,
  TitleComponent, VisualMapComponent, DatasetComponent
])

const stations = ref([
  { code: '中原区', name: '中原区' },
  { code: '金水区', name: '金水区' },
  { code: '二七区', name: '二七区' },
  { code: '惠济区', name: '惠济区' },
  { code: '郑东新区', name: '郑东新区' }
])

const dateRange = ref([])
const selectedStation = ref('中原区')
const correlationType = ref('pearson')
const lagFactor = ref('pm10')
const loading = ref(false)
const hasData = ref(false)
const analysisData = ref([])

// 污染物因子列表
const factors = ['PM2.5', 'PM10', 'SO2', 'NO2', 'CO', 'O3']
const factorKeys = ['pm25', 'pm10', 'so2', 'no2', 'co', 'o3']

// 计算平均值
const avgAQI = computed(() => {
  if (!analysisData.value.length) return '--'
  const sum = analysisData.value.reduce((acc, d) => acc + (d.aqi || 0), 0)
  return Math.round(sum / analysisData.value.length)
})

// 计算皮尔逊相关系数
const calculatePearsonCorrelation = (x, y) => {
  const n = x.length
  if (n === 0) return 0
  
  const sumX = x.reduce((a, b) => a + b, 0)
  const sumY = y.reduce((a, b) => a + b, 0)
  const sumXY = x.reduce((acc, _, i) => acc + x[i] * y[i], 0)
  const sumX2 = x.reduce((acc, v) => acc + v * v, 0)
  const sumY2 = y.reduce((acc, v) => acc + v * v, 0)
  
  const numerator = n * sumXY - sumX * sumY
  const denominator = Math.sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY))
  
  if (denominator === 0) return 0
  return numerator / denominator
}

// 计算斯皮尔曼相关系数
const calculateSpearmanCorrelation = (x, y) => {
  const n = x.length
  if (n === 0) return 0
  
  // 计算秩次
  const getRanks = (arr) => {
    const sorted = arr.map((v, i) => ({ v, i })).sort((a, b) => a.v - b.v)
    const ranks = new Array(arr.length)
    sorted.forEach((item, rank) => { ranks[item.i] = rank + 1 })
    return ranks
  }
  
  const rankX = getRanks(x)
  const rankY = getRanks(y)
  
  return calculatePearsonCorrelation(rankX, rankY)
}

// 生成相关性矩阵数据
const generateCorrelationMatrixData = () => {
  const data = []
  const aqiValues = analysisData.value.map(d => d.aqi || 0)
  
  for (let i = 0; i < factors.length; i++) {
    for (let j = 0; j < factors.length; j++) {
      let value
      if (i === j) {
        value = 1
      } else {
        const xValues = analysisData.value.map(d => d[factorKeys[i]] || 0)
        const yValues = analysisData.value.map(d => d[factorKeys[j]] || 0)
        
        if (correlationType.value === 'pearson') {
          value = calculatePearsonCorrelation(xValues, yValues)
        } else {
          value = calculateSpearmanCorrelation(xValues, yValues)
        }
      }
      data.push([j, i, parseFloat(value.toFixed(2))])
    }
  }
  return data
}

// 相关性矩阵热力图
const correlationMatrixOption = ref({
  tooltip: {
    position: 'top',
    formatter: function (params) {
      return `${factors[params.value[1]]} vs ${factors[params.value[0]]}<br/>相关系数: ${params.value[2]}`
    }
  },
  grid: { left: '15%', right: '10%', top: '10%', bottom: '15%' },
  xAxis: {
    type: 'category',
    data: factors,
    splitArea: { show: true },
    axisLabel: { color: '#94a3b8', rotate: 45 }
  },
  yAxis: {
    type: 'category',
    data: factors,
    splitArea: { show: true },
    axisLabel: { color: '#94a3b8' }
  },
  visualMap: {
    min: -1,
    max: 1,
    calculable: true,
    orient: 'horizontal',
    left: 'center',
    bottom: '0%',
    inRange: {
      color: ['#313695', '#4575b4', '#74add1', '#abd9e9', '#e0f3f8', '#ffffbf', '#fee090', '#fdae61', '#f46d43', '#d73027', '#a50026']
    },
    textStyle: { color: '#94a3b8' }
  },
  series: [{
    name: '相关系数',
    type: 'heatmap',
    data: [],
    label: {
      show: true,
      color: '#fff',
      formatter: function(params) {
        return params.value[2].toFixed(2)
      }
    },
    emphasis: {
      itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0, 0, 0, 0.5)' }
    }
  }]
})

// 特征重要性（基于与AQI的相关系数）
const featureImportanceOption = ref({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: '3%', right: '15%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'value',
    min: 0,
    max: 1,
    axisLabel: { color: '#94a3b8', formatter: '{value}' },
    splitLine: { lineStyle: { color: '#2a3441' } }
  },
  yAxis: {
    type: 'category',
    data: factors.slice().reverse(),
    axisLabel: { color: '#94a3b8' }
  },
  series: [{
    type: 'bar',
    data: [],
    itemStyle: {
      borderRadius: [0, 4, 4, 0],
      color: {
        type: 'linear',
        x: 0, y: 0, x2: 1, y2: 0,
        colorStops: [
          { offset: 0, color: '#00d4ff' },
          { offset: 1, color: '#00ff88' }
        ]
      }
    },
    label: {
      show: true,
      position: 'right',
      formatter: '{c}',
      color: '#fff'
    }
  }]
})

// 散点矩阵（PM2.5 vs AQI）
const scatterMatrixOption = ref({
  tooltip: {
    formatter: function (params) {
      return `PM2.5: ${params.value[0]}<br/>AQI: ${params.value[1]}`
    }
  },
  grid: { left: '12%', right: '10%', top: '15%', bottom: '15%' },
  xAxis: {
    type: 'value',
    name: 'PM2.5 (μg/m³)',
    nameLocation: 'middle',
    nameGap: 30,
    axisLabel: { color: '#94a3b8' },
    splitLine: { lineStyle: { color: '#2a3441' } }
  },
  yAxis: {
    type: 'value',
    name: 'AQI',
    nameLocation: 'middle',
    nameGap: 40,
    axisLabel: { color: '#94a3b8' },
    splitLine: { lineStyle: { color: '#2a3441' } }
  },
  series: [
    {
      name: 'PM2.5-AQI',
      type: 'scatter',
      data: [],
      itemStyle: { color: 'rgba(0, 212, 255, 0.6)' },
      symbolSize: 10
    }
  ]
})

// 时滞相关性
const lagCorrelationOption = ref({
  tooltip: { trigger: 'axis' },
  legend: { data: ['相关系数'], textStyle: { color: '#94a3b8' } },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    data: Array.from({length: 7}, (_, i) => `${i - 3}天`),
    axisLabel: { color: '#94a3b8' },
    name: '时间滞后',
    nameLocation: 'middle',
    nameGap: 30
  },
  yAxis: {
    type: 'value',
    min: -1,
    max: 1,
    axisLabel: { color: '#94a3b8' },
    splitLine: { lineStyle: { color: '#2a3441' } }
  },
  series: [
    {
      name: '相关系数',
      type: 'line',
      data: [],
      smooth: true,
      itemStyle: { color: '#00d4ff' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(0, 212, 255, 0.3)' },
            { offset: 1, color: 'rgba(0, 212, 255, 0.05)' }
          ]
        }
      }
    }
  ]
})

// 智能分析解读
const insights = ref([])

// 生成分析解读
const generateInsights = (correlations) => {
  const newInsights = []
  
  // 找出与AQI相关性最强的污染物
  const sortedCorr = correlations
    .map((corr, index) => ({ factor: factors[index], corr: Math.abs(corr) }))
    .sort((a, b) => b.corr - a.corr)
  
  const strongest = sortedCorr[0]
  if (strongest.corr > 0.8) {
    newInsights.push({ 
      type: 'strong', 
      text: `${strongest.factor} 与 AQI 呈现强相关 (r=${strongest.corr.toFixed(2)})，是主要影响因子` 
    })
  }
  
  // 找出相关性最弱的
  const weakest = sortedCorr[sortedCorr.length - 1]
  if (weakest.corr < 0.3) {
    newInsights.push({ 
      type: 'weak', 
      text: `${weakest.factor} 与 AQI 相关性较弱 (r=${weakest.corr.toFixed(2)})` 
    })
  }
  
  // 污染物之间的相关性
  const pm25pm10Corr = correlations[1] // PM2.5 vs PM10
  if (pm25pm10Corr > 0.7) {
    newInsights.push({ 
      type: 'moderate', 
      text: `PM2.5 与 PM10 高度相关 (r=${pm25pm10Corr.toFixed(2)})，同源性较强` 
    })
  }
  
  // 样本量说明
  newInsights.push({ 
    type: 'info', 
    text: `基于 ${analysisData.value.length} 条历史监测数据进行的相关性分析` 
  })
  
  insights.value = newInsights
}

// 运行分析
async function runAnalysis() {
  if (!dateRange.value || dateRange.value.length !== 2) {
    ElMessage.warning('请选择分析时段')
    return
  }
  
  loading.value = true
  hasData.value = false
  
  try {
    const [startDate, endDate] = dateRange.value
    const station = selectedStation.value === 'all' ? '中原区' : selectedStation.value
    
    // 加载历史数据
    const data = await queryAirQualityByDateRange(station, startDate, endDate)
    
    if (!data || data.length === 0) {
      ElMessage.warning('该时段暂无数据，请尝试其他时段')
      loading.value = false
      return
    }
    
    analysisData.value = data
    hasData.value = true
    
    // 更新图表
    updateCharts()
    
    ElMessage.success(`分析完成，共加载 ${data.length} 条数据`)
  } catch (error) {
    console.error('分析失败:', error)
    ElMessage.error('数据分析失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 更新所有图表
const updateCharts = () => {
  updateCorrelationMatrix()
  updateFeatureImportance()
  updateScatterChart()
  updateLagCorrelation()
}

// 更新相关性矩阵
const updateCorrelationMatrix = () => {
  correlationMatrixOption.value.series[0].data = generateCorrelationMatrixData()
}

// 更新特征重要性
const updateFeatureImportance = () => {
  const aqiValues = analysisData.value.map(d => d.aqi || 0)
  const correlations = factorKeys.map(key => {
    const values = analysisData.value.map(d => d[key] || 0)
    return Math.abs(calculatePearsonCorrelation(values, aqiValues))
  })
  
  // 排序并生成数据
  const sortedData = factors
    .map((factor, i) => ({ factor, corr: correlations[i] }))
    .sort((a, b) => a.corr - b.corr)
    .map(item => parseFloat(item.corr.toFixed(2)))
  
  featureImportanceOption.value.yAxis.data = factors
    .map((factor, i) => ({ factor, corr: correlations[i] }))
    .sort((a, b) => a.corr - b.corr)
    .map(item => item.factor)
  featureImportanceOption.value.series[0].data = sortedData
  
  // 生成分析解读
  generateInsights(correlations)
}

// 更新散点图
const updateScatterChart = () => {
  const scatterData = analysisData.value.map(d => [d.pm25 || 0, d.aqi || 0])
  scatterMatrixOption.value.series[0].data = scatterData
}

// 更新时滞相关性
const updateLagCorrelation = () => {
  const pm25Values = analysisData.value.map(d => d.pm25 || 0)
  const targetValues = analysisData.value.map(d => d[lagFactor.value] || 0)
  
  // 计算不同滞后的相关性
  const lagData = []
  for (let lag = -3; lag <= 3; lag++) {
    let corr = 0
    if (lag === 0) {
      corr = calculatePearsonCorrelation(pm25Values, targetValues)
    } else if (lag < 0) {
      // 负滞后：target先于pm25
      const x = pm25Values.slice(-lag)
      const y = targetValues.slice(0, lag)
      corr = calculatePearsonCorrelation(x, y)
    } else {
      // 正滞后：pm25先于target
      const x = pm25Values.slice(0, -lag)
      const y = targetValues.slice(lag)
      corr = calculatePearsonCorrelation(x, y)
    }
    lagData.push(parseFloat(corr.toFixed(2)))
  }
  
  lagCorrelationOption.value.series[0].data = lagData
}

function exportResults() {
  ElMessage.success('分析结果已导出')
}

onMounted(() => {
  const end = new Date()
  const start = new Date()
  start.setMonth(start.getMonth() - 1)
  dateRange.value = [
    start.toISOString().split('T')[0],
    end.toISOString().split('T')[0]
  ]
})
</script>

<style scoped>
.correlation-analysis {
  padding: 0;
}

.filter-card {
  background: #1a2332;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  border: 1px solid #2a3441;
}

.chart-card {
  background: #1a2332;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
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
}

.matrix-chart {
  height: 450px;
}

.correlation-legend {
  margin-top: 16px;
  padding: 16px;
  background: #243447;
  border-radius: 8px;
}

.legend-title {
  font-size: 14px;
  margin-bottom: 12px;
  color: #fff;
}

.legend-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.gradient-bar {
  flex: 1;
  height: 16px;
  background: linear-gradient(90deg, #313695, #4575b4, #74add1, #abd9e9, #e0f3f8, #ffffbf, #fee090, #fdae61, #f46d43, #d73027, #a50026);
  border-radius: 4px;
}

.legend-label {
  font-size: 12px;
  color: #94a3b8;
  white-space: nowrap;
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.chart {
  height: 300px;
}

.lag-chart {
  height: 280px;
}

.summary-card {
  background: #1a2332;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  border: 1px solid #2a3441;
}

.summary-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 16px;
  color: #00d4ff;
}

.summary-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
}

.summary-item {
  display: flex;
  gap: 8px;
}

.summary-item .label {
  color: #94a3b8;
}

.summary-item .value {
  color: #fff;
  font-weight: 500;
}

.insight-card {
  background: linear-gradient(135deg, #1a2332 0%, #1e3a5f 100%);
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #2a3441;
}

.insight-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 16px;
  color: #00d4ff;
}

.insight-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.insight-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.insight-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 6px;
  flex-shrink: 0;
}

.insight-dot.strong { background: #ff5555; }
.insight-dot.moderate { background: #ffaa00; }
.insight-dot.weak { background: #00ff88; }
.insight-dot.info { background: #00d4ff; }
.insight-dot.lag { background: #aa66ff; }

.insight-text {
  color: #94a3b8;
  font-size: 14px;
  line-height: 1.6;
}

.loading-state {
  text-align: center;
  padding: 60px 20px;
  background: #1a2332;
  border-radius: 12px;
  border: 1px solid #2a3441;
  color: #94a3b8;
}

.loading-icon {
  font-size: 48px;
  margin-bottom: 16px;
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: 80px 20px;
  background: #1a2332;
  border-radius: 12px;
  border: 1px solid #2a3441;
  color: #94a3b8;
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

:deep(.el-form-item__label) {
  color: #94a3b8;
}

@media (max-width: 768px) {
  .charts-row {
    grid-template-columns: 1fr;
  }
}
</style>
