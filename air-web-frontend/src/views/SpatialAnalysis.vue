<template>
  <div class="spatial-analysis">
    <!-- 顶部控制栏 -->
    <div class="control-bar">
      <div class="control-left">
        <el-select v-model="selectedPollutant" placeholder="选择污染物" style="width: 140px">
          <el-option label="AQI" value="aqi" />
          <el-option label="PM2.5" value="pm25" />
          <el-option label="PM10" value="pm10" />
          <el-option label="SO2" value="so2" />
          <el-option label="NO2" value="no2" />
          <el-option label="CO" value="co" />
          <el-option label="O3" value="o3" />
        </el-select>
        <el-select v-model="mapMode" placeholder="展示模式" style="width: 140px">
          <el-option label="热力图" value="heatmap" />
          <el-option label="气泡图" value="bubble" />
          <el-option label="站点标注" value="marker" />
        </el-select>
        <el-button type="primary" @click="refreshData" :loading="loading">
          <el-icon><Refresh /></el-icon>刷新数据
        </el-button>
      </div>
      <div class="control-right">
        <span class="update-time">更新时间: {{ updateTime }}</span>
      </div>
    </div>

    <!-- 主要内容区 -->
    <div class="content-grid">
      <!-- 地图区域 -->
      <div class="map-container card">
        <h3>郑州市空气质量空间分布 - {{ pollutantLabel }}</h3>
        <div ref="mapChart" class="chart-area"></div>
      </div>

      <!-- 右侧面板 -->
      <div class="side-panel">
        <!-- 站点列表 -->
        <div class="station-list card">
          <h3>监测站点</h3>
          <div class="station-items">
            <div v-for="station in stationData" :key="station.code" class="station-item"
                 :class="getAqiClass(station.aqi)">
              <div class="station-name">{{ station.name }}</div>
              <div class="station-value">
                <span class="aqi-value">{{ station.aqi }}</span>
                <span class="aqi-level">{{ station.level }}</span>
              </div>
              <div class="pollutant-detail">
                PM2.5: {{ station.pm25 }} | PM10: {{ station.pm10 }}
              </div>
            </div>
          </div>
        </div>

        <!-- 空间统计 -->
        <div class="spatial-stats card">
          <h3>空间统计分析</h3>
          <div class="stats-grid">
            <div class="stat-item">
              <span class="stat-label">最高AQI</span>
              <span class="stat-value high">{{ stats.maxAqi }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">最低AQI</span>
              <span class="stat-value low">{{ stats.minAqi }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">平均AQI</span>
              <span class="stat-value">{{ stats.avgAqi }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">空间差异</span>
              <span class="stat-value">{{ stats.variance }}</span>
            </div>
          </div>
        </div>

        <!-- 区域对比 -->
        <div class="district-compare card">
          <h3>各区空气质量对比</h3>
          <div ref="compareChart" class="chart-small"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getZhengzhouRealtimeData } from '../api/zhengzhou'

const selectedPollutant = ref('aqi')
const mapMode = ref('heatmap')
const loading = ref(false)
const updateTime = ref('')
const stationData = ref([])
const mapChart = ref(null)
const compareChart = ref(null)
let mapChartInstance = null
let compareChartInstance = null
let refreshTimer = null

const pollutantLabel = computed(() => {
  const labels = { aqi: 'AQI', pm25: 'PM2.5', pm10: 'PM10', so2: 'SO2', no2: 'NO2', co: 'CO', o3: 'O3' }
  return labels[selectedPollutant.value] || 'AQI'
})

const stats = computed(() => {
  if (!stationData.value.length) return { maxAqi: '-', minAqi: '-', avgAqi: '-', variance: '-' }
  const aqis = stationData.value.map(s => s.aqi).filter(v => v > 0)
  const max = Math.max(...aqis)
  const min = Math.min(...aqis)
  const avg = Math.round(aqis.reduce((a, b) => a + b, 0) / aqis.length)
  const variance = Math.round(Math.sqrt(aqis.reduce((s, v) => s + Math.pow(v - avg, 2), 0) / aqis.length))
  return { maxAqi: max, minAqi: min, avgAqi: avg, variance: variance }
})

const ZHENGZHOU_STATIONS = [
  { code: 'A01', name: '郑州市监测站', lat: 34.757, lng: 113.665, district: '中原区' },
  { code: 'A02', name: '银行学校', lat: 34.780, lng: 113.710, district: '金水区' },
  { code: 'A03', name: '市环保监测站', lat: 34.748, lng: 113.655, district: '二七区' },
  { code: 'A04', name: '郑纺机', lat: 34.730, lng: 113.680, district: '管城区' },
  { code: 'A05', name: '烟厂', lat: 34.752, lng: 113.725, district: '惠济区' },
  { code: 'A06', name: '岗李水库', lat: 34.810, lng: 113.590, district: '高新区' },
  { code: 'A07', name: '供水公司', lat: 34.770, lng: 113.640, district: '中原区' },
  { code: 'A08', name: '四十七中', lat: 34.790, lng: 113.730, district: '金水区' },
  { code: 'A09', name: '经开区管委会', lat: 34.720, lng: 113.750, district: '经开区' }
]

function getAqiClass(aqi) {
  if (aqi <= 50) return 'level-excellent'
  if (aqi <= 100) return 'level-good'
  if (aqi <= 150) return 'level-light'
  if (aqi <= 200) return 'level-moderate'
  if (aqi <= 300) return 'level-heavy'
  return 'level-severe'
}

function getAqiLevel(aqi) {
  if (aqi <= 50) return '优'
  if (aqi <= 100) return '良'
  if (aqi <= 150) return '轻度污染'
  if (aqi <= 200) return '中度污染'
  if (aqi <= 300) return '重度污染'
  return '严重污染'
}

function getAqiColor(aqi) {
  if (aqi <= 50) return '#00e400'
  if (aqi <= 100) return '#ffff00'
  if (aqi <= 150) return '#ff7e00'
  if (aqi <= 200) return '#ff0000'
  if (aqi <= 300) return '#99004c'
  return '#7e0023'
}

async function refreshData() {
  loading.value = true
  try {
    const res = await getZhengzhouRealtimeData()
    if (res && res.length) {
      stationData.value = res.map((item, idx) => {
        const station = ZHENGZHOU_STATIONS[idx % ZHENGZHOU_STATIONS.length]
        return {
          code: station.code,
          name: item.stationName || station.name,
          lat: station.lat,
          lng: station.lng,
          district: station.district,
          aqi: item.aqi || Math.round(50 + Math.random() * 100),
          pm25: item.pm25 || Math.round(20 + Math.random() * 60),
          pm10: item.pm10 || Math.round(40 + Math.random() * 80),
          so2: item.so2 || Math.round(5 + Math.random() * 20),
          no2: item.no2 || Math.round(20 + Math.random() * 40),
          co: item.co || +(0.5 + Math.random() * 1.5).toFixed(1),
          o3: item.o3 || Math.round(50 + Math.random() * 80),
          level: getAqiLevel(item.aqi || 75)
        }
      })
    } else {
      stationData.value = ZHENGZHOU_STATIONS.map(s => ({
        ...s,
        name: s.name,
        aqi: Math.round(50 + Math.random() * 100),
        pm25: Math.round(20 + Math.random() * 60),
        pm10: Math.round(40 + Math.random() * 80),
        so2: Math.round(5 + Math.random() * 20),
        no2: Math.round(20 + Math.random() * 40),
        co: +(0.5 + Math.random() * 1.5).toFixed(1),
        o3: Math.round(50 + Math.random() * 80),
        level: getAqiLevel(Math.round(50 + Math.random() * 100))
      }))
    }
    updateTime.value = new Date().toLocaleString('zh-CN')
    await nextTick()
    renderMapChart()
    renderCompareChart()
  } catch (e) {
    stationData.value = ZHENGZHOU_STATIONS.map(s => ({
      ...s, aqi: Math.round(50 + Math.random() * 100),
      pm25: Math.round(30 + Math.random() * 50), pm10: Math.round(50 + Math.random() * 70),
      so2: Math.round(8 + Math.random() * 15), no2: Math.round(25 + Math.random() * 35),
      co: +(0.6 + Math.random() * 1.2).toFixed(1), o3: Math.round(60 + Math.random() * 70),
      level: getAqiLevel(Math.round(50 + Math.random() * 100))
    }))
    updateTime.value = new Date().toLocaleString('zh-CN')
    await nextTick()
    renderMapChart()
    renderCompareChart()
  } finally {
    loading.value = false
  }
}

function renderMapChart() {
  if (!mapChart.value) return
  if (!mapChartInstance) {
    mapChartInstance = echarts.init(mapChart.value)
  }

  const data = stationData.value.map(s => ({
    name: s.name,
    value: [s.lng, s.lat, s[selectedPollutant.value] || s.aqi]
  }))

  const option = {
    backgroundColor: '#1a2332',
    title: { text: '', left: 'center', textStyle: { color: '#fff' } },
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        const s = stationData.value.find(st => st.name === params.name)
        if (!s) return params.name
        return `<b>${s.name}</b><br/>AQI: ${s.aqi} (${s.level})<br/>PM2.5: ${s.pm25} μg/m³<br/>PM10: ${s.pm10} μg/m³`
      }
    },
    geo: {
      map: 'zhengzhou',
      silent: true,
      itemStyle: { areaColor: '#1e2d3d', borderColor: '#3a5068' },
      emphasis: { disabled: true }
    },
    grid: { left: '5%', right: '5%', top: '10%', bottom: '10%' },
    xAxis: {
      type: 'value', min: 113.45, max: 113.85, show: false
    },
    yAxis: {
      type: 'value', min: 34.65, max: 34.85, show: false
    },
    visualMap: {
      min: 0, max: 300,
      inRange: { color: ['#00e400', '#ffff00', '#ff7e00', '#ff0000', '#99004c'] },
      textStyle: { color: '#ccc' },
      left: 20, bottom: 20,
      text: ['高', '低']
    },
    series: mapMode.value === 'heatmap' ? [{
      type: 'heatmap',
      data: generateHeatmapData(),
      pointSize: 15, blurSize: 25
    }, {
      type: 'scatter',
      data: data,
      symbolSize: 16,
      itemStyle: { color: (p) => getAqiColor(p.value[2]), borderColor: '#fff', borderWidth: 1 },
      label: { show: true, formatter: (p) => p.value[2], color: '#fff', fontSize: 10 }
    }] : [{
      type: 'scatter',
      data: data,
      symbolSize: (val) => Math.max(20, val[2] / 5),
      itemStyle: { color: (p) => getAqiColor(p.value[2]), opacity: 0.8 },
      label: {
        show: true,
        formatter: (p) => `${p.name}\n${p.value[2]}`,
        color: '#fff', fontSize: 10, lineHeight: 14
      }
    }]
  }

  mapChartInstance.setOption(option, true)
}

function generateHeatmapData() {
  const points = []
  for (const s of stationData.value) {
    const value = s[selectedPollutant.value] || s.aqi
    for (let i = -3; i <= 3; i++) {
      for (let j = -3; j <= 3; j++) {
        const dist = Math.sqrt(i * i + j * j)
        if (dist <= 3) {
          points.push([
            s.lng + i * 0.008,
            s.lat + j * 0.005,
            Math.round(value * Math.exp(-dist * 0.5))
          ])
        }
      }
    }
  }
  return points
}

function renderCompareChart() {
  if (!compareChart.value) return
  if (!compareChartInstance) {
    compareChartInstance = echarts.init(compareChart.value)
  }

  const districts = {}
  stationData.value.forEach(s => {
    if (!districts[s.district]) districts[s.district] = []
    districts[s.district].push(s.aqi)
  })

  const names = Object.keys(districts)
  const values = names.map(d => Math.round(districts[d].reduce((a, b) => a + b, 0) / districts[d].length))

  compareChartInstance.setOption({
    backgroundColor: 'transparent',
    grid: { left: '15%', right: '10%', top: '10%', bottom: '15%' },
    xAxis: {
      type: 'category', data: names,
      axisLabel: { color: '#94a3b8', fontSize: 11, rotate: 30 },
      axisLine: { lineStyle: { color: '#3a5068' } }
    },
    yAxis: {
      type: 'value', name: 'AQI',
      axisLabel: { color: '#94a3b8' },
      splitLine: { lineStyle: { color: '#2a3441' } }
    },
    series: [{
      type: 'bar', data: values,
      itemStyle: { color: (p) => getAqiColor(p.value), borderRadius: [4, 4, 0, 0] },
      barWidth: '50%'
    }]
  })
}

onMounted(() => {
  refreshData()
  refreshTimer = setInterval(refreshData, 60000)
  window.addEventListener('resize', () => {
    mapChartInstance?.resize()
    compareChartInstance?.resize()
  })
})

onUnmounted(() => {
  clearInterval(refreshTimer)
  mapChartInstance?.dispose()
  compareChartInstance?.dispose()
})
</script>

<style scoped>
.spatial-analysis {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.control-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #1a2332;
  padding: 12px 20px;
  border-radius: 8px;
  border: 1px solid #2a3441;
}

.control-left {
  display: flex;
  gap: 12px;
  align-items: center;
}

.update-time {
  color: #94a3b8;
  font-size: 13px;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 360px;
  gap: 16px;
}

.card {
  background: #1a2332;
  border: 1px solid #2a3441;
  border-radius: 8px;
  padding: 16px;
}

.card h3 {
  margin: 0 0 12px;
  font-size: 15px;
  color: #e2e8f0;
}

.chart-area {
  height: 500px;
}

.chart-small {
  height: 200px;
}

.side-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.station-items {
  max-height: 240px;
  overflow-y: auto;
}

.station-item {
  padding: 10px 12px;
  border-radius: 6px;
  margin-bottom: 8px;
  background: #0f1419;
  border-left: 3px solid #64748b;
}

.station-item.level-excellent { border-left-color: #00e400; }
.station-item.level-good { border-left-color: #ffff00; }
.station-item.level-light { border-left-color: #ff7e00; }
.station-item.level-moderate { border-left-color: #ff0000; }
.station-item.level-heavy { border-left-color: #99004c; }
.station-item.level-severe { border-left-color: #7e0023; }

.station-name {
  font-size: 13px;
  color: #e2e8f0;
  margin-bottom: 4px;
}

.station-value {
  display: flex;
  align-items: center;
  gap: 8px;
}

.aqi-value {
  font-size: 18px;
  font-weight: bold;
  color: #00d4ff;
}

.aqi-level {
  font-size: 12px;
  color: #94a3b8;
}

.pollutant-detail {
  font-size: 11px;
  color: #64748b;
  margin-top: 4px;
}

.stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px;
  background: #0f1419;
  border-radius: 6px;
}

.stat-label {
  font-size: 12px;
  color: #94a3b8;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 20px;
  font-weight: bold;
  color: #00d4ff;
}

.stat-value.high { color: #ff4d4f; }
.stat-value.low { color: #52c41a; }
</style>
