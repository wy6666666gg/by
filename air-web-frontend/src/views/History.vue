<template>
  <div class="history">
    <!-- 查询条件 -->
    <div class="filter-card">
      <el-form :inline="true" :model="queryForm">
        <el-form-item label="站点">
          <el-select v-model="queryForm.stationCode" placeholder="选择站点" style="width: 180px">
            <el-option v-for="item in stations" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledDate"
            style="width: 280px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">
            <el-icon><Search /></el-icon> 查询
          </el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="exportData">
            <el-icon><Download /></el-icon> 导出
          </el-button>
        </el-form-item>
      </el-form>
      <el-alert
        title="数据说明"
        description="当前展示郑州市五区（中原区、金水区、二七区、惠济区、郑东新区）2020-2024年历史监测数据，包含AQI、六项污染物及沙尘天标识"
        type="info"
        :closable="false"
        style="margin-top: 10px"
      />
    </div>

    <!-- 统计概览 -->
    <div class="stats-overview" v-if="statsData">
      <div class="overview-card">
        <div class="overview-icon" style="background: linear-gradient(135deg, #00d4ff, #0099cc);">
          <el-icon><DataLine /></el-icon>
        </div>
        <div class="overview-info">
          <p class="overview-label">平均 AQI</p>
          <p class="overview-value">{{ statsData.avgAqi }}</p>
        </div>
      </div>
      <div class="overview-card">
        <div class="overview-icon" style="background: linear-gradient(135deg, #00ff88, #00cc66);">
          <el-icon><Top /></el-icon>
        </div>
        <div class="overview-info">
          <p class="overview-label">最高 AQI</p>
          <p class="overview-value">{{ statsData.maxAqi }}</p>
        </div>
      </div>
      <div class="overview-card">
        <div class="overview-icon" style="background: linear-gradient(135deg, #ffaa00, #cc8800);">
          <el-icon><Bottom /></el-icon>
        </div>
        <div class="overview-info">
          <p class="overview-label">最低 AQI</p>
          <p class="overview-value">{{ statsData.minAqi }}</p>
        </div>
      </div>
      <div class="overview-card">
        <div class="overview-icon" style="background: linear-gradient(135deg, #ff5555, #cc4444);">
          <el-icon><WindPower /></el-icon>
        </div>
        <div class="overview-info">
          <p class="overview-label">沙尘天数</p>
          <p class="overview-value">{{ statsData.sandDustDays }}</p>
        </div>
      </div>
    </div>

    <!-- 趋势图表 -->
    <div class="chart-card">
      <div class="chart-header">
        <h3>AQI 历史趋势</h3>
        <el-radio-group v-model="trendType" size="small" @change="loadTrend">
          <el-radio-button label="daily">日趋势</el-radio-button>
          <el-radio-button label="monthly">月趋势</el-radio-button>
        </el-radio-group>
      </div>
      <v-chart class="chart" :option="trendOption" autoresize />
    </div>

    <!-- 数据对比 -->
    <div class="charts-row">
      <div class="chart-card">
        <div class="chart-header">
          <h3>六项污染物对比</h3>
        </div>
        <v-chart class="chart" :option="pollutantOption" autoresize />
      </div>
      <div class="chart-card">
        <div class="chart-header">
          <h3>AQI 等级分布</h3>
        </div>
        <v-chart class="chart" :option="aqiLevelOption" autoresize />
      </div>
    </div>

    <!-- 首要污染物分析 -->
    <div class="chart-card">
      <div class="chart-header">
        <h3>首要污染物分布</h3>
      </div>
      <v-chart class="chart" :option="primaryPollutantOption" autoresize />
    </div>

    <!-- 数据表格 -->
    <div class="table-card">
      <div class="chart-header">
        <h3>历史数据明细（沙尘天标红显示）</h3>
      </div>
      <el-table :data="tableData" style="width: 100%" height="400" stripe>
        <el-table-column prop="date" label="日期" width="120" />
        <el-table-column prop="aqi" label="AQI" width="80">
          <template #default="{ row }">
            <span :class="getAqiClass(row.aqi)">{{ row.aqi }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="quality_level" label="等级" width="100" />
        <el-table-column prop="pm25" label="PM2.5" width="80" />
        <el-table-column prop="pm10" label="PM10" width="80" />
        <el-table-column prop="o3" label="O₃" width="80" />
        <el-table-column prop="so2" label="SO₂" width="80" />
        <el-table-column prop="co" label="CO" width="80" />
        <el-table-column prop="no2" label="NO₂" width="80" />
        <el-table-column prop="primary_pollutant" label="首要污染物" width="120" />
        <el-table-column label="沙尘天" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.is_sand_dust_day" type="danger" size="small">是</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import {
  GridComponent, TooltipComponent, LegendComponent,
  TitleComponent, DatasetComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import { Search, Download, DataLine, Top, Bottom, WindPower } from '@element-plus/icons-vue'
import { HOT_CITIES } from '../api/weather.js'
import { ElMessage } from 'element-plus'
import { loadHistoryData, filterData, calculateStats, getAqiClass, exportToCSV } from '../utils/dataService.js'

use([
  CanvasRenderer, LineChart, BarChart, PieChart,
  GridComponent, TooltipComponent, LegendComponent,
  TitleComponent, DatasetComponent
])

// 站点列表 - 郑州市各区
const stations = ref(HOT_CITIES.map(city => ({ code: city.name, name: city.name })))

const queryForm = ref({
  stationCode: '中原区'
})
const dateRange = ref([])
const trendType = ref('daily')
const statsData = ref(null)
const tableData = ref([])
const allData = ref([])

// 趋势图配置
const trendOption = ref({
  tooltip: { trigger: 'axis' },
  legend: { data: ['AQI', 'PM2.5', 'PM10'], textStyle: { color: '#94a3b8' } },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: [],
    axisLabel: { color: '#94a3b8' }
  },
  yAxis: {
    type: 'value',
    axisLabel: { color: '#94a3b8' },
    splitLine: { lineStyle: { color: '#2a3441' } }
  },
  series: [
    { name: 'AQI', type: 'line', smooth: true, data: [], itemStyle: { color: '#00d4ff' } },
    { name: 'PM2.5', type: 'line', smooth: true, data: [], itemStyle: { color: '#ff7e00' } },
    { name: 'PM10', type: 'line', smooth: true, data: [], itemStyle: { color: '#00ff88' } }
  ]
})

// 污染物对比图
const pollutantOption = ref({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  legend: { textStyle: { color: '#94a3b8' } },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    data: ['PM2.5', 'PM10', 'SO2', 'NO2', 'CO', 'O3'],
    axisLabel: { color: '#94a3b8' }
  },
  yAxis: {
    type: 'value',
    name: 'μg/m³',
    axisLabel: { color: '#94a3b8' },
    splitLine: { lineStyle: { color: '#2a3441' } }
  },
  series: [{
    data: [45, 78, 18, 42, 1.1, 85],
    type: 'bar',
    barWidth: '50%',
    itemStyle: {
      borderRadius: [4, 4, 0, 0],
      color: (params) => {
        const colors = ['#ff5555', '#ffaa00', '#00d4ff', '#00ff88', '#aa66ff', '#ff66aa']
        return colors[params.dataIndex]
      }
    }
  }]
})

// AQI等级分布图
const aqiLevelOption = ref({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: {
    orient: 'vertical',
    left: 'left',
    textStyle: { color: '#94a3b8' }
  },
  series: [{
    type: 'pie',
    radius: ['40%', '70%'],
    center: ['60%', '50%'],
    avoidLabelOverlap: false,
    itemStyle: { borderRadius: 8, borderColor: '#1a2332', borderWidth: 2 },
    label: { show: false },
    emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold', color: '#fff' } },
    data: [
      { value: 0, name: '优', itemStyle: { color: '#00e400' } },
      { value: 0, name: '良', itemStyle: { color: '#ffff00' } },
      { value: 0, name: '轻度污染', itemStyle: { color: '#ff7e00' } },
      { value: 0, name: '中度污染', itemStyle: { color: '#ff0000' } },
      { value: 0, name: '重度污染', itemStyle: { color: '#8f3f97' } },
      { value: 0, name: '严重污染', itemStyle: { color: '#7e0023' } }
    ]
  }]
})

// 首要污染物分布图
const primaryPollutantOption = ref({
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: {
    orient: 'vertical',
    left: 'left',
    textStyle: { color: '#94a3b8' }
  },
  series: [{
    type: 'pie',
    radius: '60%',
    center: ['60%', '50%'],
    data: [
      { value: 0, name: 'PM2.5', itemStyle: { color: '#ff5555' } },
      { value: 0, name: 'PM10', itemStyle: { color: '#ffaa00' } },
      { value: 0, name: 'O3', itemStyle: { color: '#00d4ff' } },
      { value: 0, name: 'NO2', itemStyle: { color: '#00ff88' } },
      { value: 0, name: '无', itemStyle: { color: '#94a3b8' } }
    ],
    emphasis: {
      itemStyle: {
        shadowBlur: 10,
        shadowOffsetX: 0,
        shadowColor: 'rgba(0, 0, 0, 0.5)'
      }
    }
  }]
})

// 处理数据并更新图表
const processAndDisplayData = (data) => {
  if (!data || data.length === 0) {
    ElMessage.warning('所选日期范围内暂无数据')
    return
  }

  // 更新表格数据（显示前100条）
  tableData.value = data.slice(0, 100)

  // 使用数据服务计算统计信息
  const stats = calculateStats(data)
  statsData.value = stats

  // 更新趋势图
  const dates = data.map(item => item.date)
  trendOption.value.xAxis.data = dates
  trendOption.value.series[0].data = data.map(item => item.aqi)
  trendOption.value.series[1].data = data.map(item => item.pm25)
  trendOption.value.series[2].data = data.map(item => item.pm10)

  // 更新污染物均值图
  const avg = stats.pollutantAverages
  pollutantOption.value.series[0].data = [
    Math.round(avg.pm25 * 10) / 10,
    Math.round(avg.pm10 * 10) / 10,
    Math.round(avg.so2 * 10) / 10,
    Math.round(avg.no2 * 10) / 10,
    Math.round(avg.co * 100) / 100,
    Math.round(avg.o3 * 10) / 10
  ]

  // 更新AQI等级分布
  const levelDist = stats.levelDistribution
  aqiLevelOption.value.series[0].data = [
    { value: levelDist['优'], name: '优', itemStyle: { color: '#00e400' } },
    { value: levelDist['良'], name: '良', itemStyle: { color: '#ffff00' } },
    { value: levelDist['轻度污染'], name: '轻度污染', itemStyle: { color: '#ff7e00' } },
    { value: levelDist['中度污染'], name: '中度污染', itemStyle: { color: '#ff0000' } },
    { value: levelDist['重度污染'], name: '重度污染', itemStyle: { color: '#8f3f97' } },
    { value: levelDist['严重污染'], name: '严重污染', itemStyle: { color: '#7e0023' } }
  ].filter(d => d.value > 0)

  // 更新首要污染物分布
  const primaryDist = stats.primaryDistribution
  primaryPollutantOption.value.series[0].data = [
    { value: primaryDist['PM2.5'] || 0, name: 'PM2.5', itemStyle: { color: '#ff5555' } },
    { value: primaryDist['PM10'] || 0, name: 'PM10', itemStyle: { color: '#ffaa00' } },
    { value: primaryDist['O3'] || 0, name: 'O3', itemStyle: { color: '#00d4ff' } },
    { value: primaryDist['NO2'] || 0, name: 'NO2', itemStyle: { color: '#00ff88' } },
    { value: primaryDist['无'] || 0, name: '无', itemStyle: { color: '#94a3b8' } }
  ].filter(d => d.value > 0)
}

// 查询历史数据
const handleQuery = async () => {
  if (!dateRange.value || dateRange.value.length !== 2) {
    ElMessage.warning('请选择日期范围')
    return
  }

  try {
    ElMessage.info('正在加载历史数据...')
    
    // 缓存数据避免重复加载
    if (allData.value.length === 0) {
      allData.value = await loadHistoryData()
    }

    // 使用数据服务筛选数据
    const filteredData = filterData(allData.value, {
      station: queryForm.value.stationCode,
      startDate: dateRange.value[0],
      endDate: dateRange.value[1]
    })

    processAndDisplayData(filteredData)
    ElMessage.success(`查询完成，共 ${filteredData.length} 条记录`)

  } catch (error) {
    ElMessage.error('获取历史数据失败')
    console.error(error)
  }
}

// 加载趋势数据
const loadTrend = async () => {
  try {
    // 缓存数据
    if (allData.value.length === 0) {
      allData.value = await loadHistoryData()
    }

    // 使用当前日期范围或默认2024年
    const startDate = dateRange.value?.[0] || '2024-01-01'
    const endDate = dateRange.value?.[1] || '2024-12-31'

    const filteredData = filterData(allData.value, {
      station: queryForm.value.stationCode,
      startDate,
      endDate
    })

    processAndDisplayData(filteredData)
  } catch (error) {
    console.error('加载趋势数据失败:', error)
  }
}

const handleReset = () => {
  queryForm.value.stationCode = '中原区'
  dateRange.value = ['2024-01-01', '2024-12-31']
  loadTrend()
}

// 导出数据
const exportData = () => {
  if (tableData.value.length === 0) {
    ElMessage.warning('请先查询数据')
    return
  }
  exportToCSV(tableData.value, `${queryForm.value.stationCode}_历史数据.csv`)
  ElMessage.success('数据导出成功')
}

// 限制日期选择范围（支持2015-2030年的日期，兼容自定义数据）
const disabledDate = (time) => {
  const minDate = new Date('2015-01-01')
  const maxDate = new Date('2030-12-31')
  return time.getTime() < minDate.getTime() || time.getTime() > maxDate.getTime()
}

onMounted(() => {
  // 设置默认日期范围为2024年
  dateRange.value = ['2024-01-01', '2024-12-31']
  loadTrend()
})
</script>

<style scoped>
.history {
  padding: 0;
}

.filter-card {
  background: #1a2332;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  border: 1px solid #2a3441;
}

.stats-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
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
}

.overview-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #fff;
}

.overview-label {
  color: #94a3b8;
  font-size: 13px;
  margin-bottom: 6px;
}

.overview-value {
  color: #fff;
  font-size: 24px;
  font-weight: bold;
}

.chart-card {
  background: #1a2332;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  border: 1px solid #2a3441;
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
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

.chart {
  height: 350px;
}

.table-card {
  background: #1a2332;
  border-radius: 12px;
  padding: 20px;
  border: 1px solid #2a3441;
}

.aqi-excellent { color: #00e400; font-weight: bold; }
.aqi-good { color: #ffff00; font-weight: bold; }
.aqi-moderate { color: #ff7e00; font-weight: bold; }
.aqi-unhealthy { color: #ff0000; font-weight: bold; }
.aqi-very-unhealthy { color: #8f3f97; font-weight: bold; }
.aqi-hazardous { color: #7e0023; font-weight: bold; }

:deep(.el-form-item__label) {
  color: #94a3b8;
}

:deep(.el-input__wrapper),
:deep(.el-date-editor) {
  background: #243447;
  box-shadow: 0 0 0 1px #2a3441 inset;
}

:deep(.el-input__inner) {
  color: #fff;
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

/* 沙尘天行高亮显示 */
:deep(.el-table__body tr.sand-dust-row td) {
  background: rgba(255, 85, 85, 0.15) !important;
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
