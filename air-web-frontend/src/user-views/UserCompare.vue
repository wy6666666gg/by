<template>
  <div class="user-compare">
    <div class="page-header">
      <h1>历年对比分析</h1>
      <p>选取多个年份的同一日期，对比空气质量变化趋势</p>
    </div>

    <!-- 查询配置 -->
    <div class="config-card">
      <h3><i class="fas fa-sliders-h"></i> 查询配置</h3>
      <div class="config-form">
        <div class="form-item">
          <label>选择站点</label>
          <el-select v-model="selectedStation" placeholder="请选择监测站点" style="width: 150px">
            <el-option v-for="item in stations" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
        </div>
        <div class="form-item">
          <label>选择日期</label>
          <el-date-picker
            v-model="selectedDate"
            type="date"
            placeholder="选择日期"
            format="MM-DD"
            value-format="MM-DD"
            :disabled-date="disabledFutureDate"
            style="width: 150px"
          />
        </div>
        <div class="form-item year-select">
          <label>对比年份（最多选3年）</label>
          <el-checkbox-group v-model="selectedYears" :max="3">
            <el-checkbox v-for="year in availableYears" :key="year" :label="year">
              {{ year }}年
            </el-checkbox>
          </el-checkbox-group>
        </div>
        <el-button type="primary" @click="runCompare" :loading="loading" :disabled="!canQuery">
          <i class="fas fa-chart-bar"></i> 开始对比
        </el-button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <p>正在查询历史数据...</p>
    </div>

    <!-- 对比结果 -->
    <template v-else-if="hasData">
      <!-- AQI对比 -->
      <div class="chart-card">
        <div class="chart-header">
          <h3><i class="fas fa-chart-bar"></i> AQI 历年对比</h3>
          <div class="legend">
            <span v-for="(item, index) in compareData" :key="index" class="legend-item">
              <span class="dot" :style="{ background: colors[index] }"></span>
              {{ item.year }}年
            </span>
          </div>
        </div>
        <v-chart class="chart" :option="aqiChartOption" autoresize />
      </div>

      <!-- 六项污染物对比 -->
      <div class="chart-card">
        <div class="chart-header">
          <h3><i class="fas fa-chart-bar"></i> 污染物指标对比</h3>
          <div class="pollutant-tabs">
            <div
              v-for="item in pollutantTabs"
              :key="item.key"
              class="tab-item"
              :class="{ active: selectedPollutant === item.key }"
              @click="selectedPollutant = item.key"
            >
              {{ item.label }}
            </div>
          </div>
        </div>
        <v-chart class="chart" :option="pollutantChartOption" autoresize />
      </div>

      <!-- 详细数据表格 -->
      <div class="data-card">
        <div class="card-header">
          <h3><i class="fas fa-table"></i> 详细对比数据</h3>
        </div>
        <el-table :data="compareData" border stripe style="width: 100%">
          <el-table-column prop="year" label="年份" width="100" align="center">
            <template #default="{ row }">
              <el-tag type="primary" effect="dark">{{ row.year }}年</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="date" label="日期" width="120" align="center" />
          <el-table-column prop="aqi" label="AQI" width="90" align="center">
            <template #default="{ row }">
              <span :class="['aqi-badge', getAQIClass(row.aqi)]">{{ row.aqi }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="qualityLevel" label="等级" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getAQITagType(row.aqi)" size="small">{{ row.qualityLevel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="pm25" label="PM2.5" width="90" align="center" />
          <el-table-column prop="pm10" label="PM10" width="90" align="center" />
          <el-table-column prop="so2" label="SO₂" width="80" align="center" />
          <el-table-column prop="no2" label="NO₂" width="80" align="center" />
          <el-table-column prop="co" label="CO" width="80" align="center" />
          <el-table-column prop="o3" label="O₃" width="80" align="center" />
          <el-table-column prop="primaryPollutant" label="首要污染物" width="110" align="center" />
        </el-table>
      </div>

      <!-- 分析结论 -->
      <div class="insight-card">
        <div class="insight-header">
          <i class="fas fa-lightbulb"></i>
          <span>对比分析结论</span>
        </div>
        <div class="insight-content">
          <p v-for="(insight, index) in insights" :key="index">
            <i class="fas fa-check-circle"></i> {{ insight }}
          </p>
        </div>
      </div>
    </template>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <i class="fas fa-balance-scale empty-icon"></i>
      <p>请选择站点、日期和对比年份，开始历年数据对比</p>
      <p class="sub">系统将自动查询各年份同一天期的空气质量数据</p>
    </div>
  </div>
</template>

<script>
import { queryAirQualityByDate } from '../api/zhengzhou'

export default {
  name: 'UserCompare',
  data() {
    return {
      stations: [
        { code: '中原区', name: '中原区' },
        { code: '金水区', name: '金水区' },
        { code: '二七区', name: '二七区' },
        { code: '惠济区', name: '惠济区' },
        { code: '郑东新区', name: '郑东新区' }
      ],
      availableYears: [2020, 2021, 2022, 2023, 2024, 2025, 2026],
      colors: ['#1a2980', '#26d0ce', '#667eea', '#f093fb', '#f5576c'],
      pollutantTabs: [
        { key: 'pm25', label: 'PM2.5', unit: 'μg/m³' },
        { key: 'pm10', label: 'PM10', unit: 'μg/m³' },
        { key: 'so2', label: 'SO₂', unit: 'μg/m³' },
        { key: 'no2', label: 'NO₂', unit: 'μg/m³' },
        { key: 'co', label: 'CO', unit: 'mg/m³' },
        { key: 'o3', label: 'O₃', unit: 'μg/m³' }
      ],
      selectedStation: '中原区',
      selectedDate: '',
      selectedYears: [2021, 2022, 2023],
      selectedPollutant: 'pm25',
      loading: false,
      hasData: false,
      compareData: []
    }
  },
  computed: {
    canQuery() {
      return this.selectedStation && this.selectedDate && this.selectedYears.length >= 2
    },
    insights() {
      if (!this.compareData.length) return []
      
      const insights = []
      const sorted = [...this.compareData].sort((a, b) => a.aqi - b.aqi)
      const best = sorted[0]
      const worst = sorted[sorted.length - 1]
      
      insights.push(`${best.year}年空气质量最好，AQI为${best.aqi}（${best.qualityLevel}）`)
      insights.push(`${worst.year}年空气质量最差，AQI为${worst.aqi}（${worst.qualityLevel}）`)
      
      // 计算AQI变化趋势
      if (this.compareData.length >= 2) {
        const first = this.compareData[0]
        const last = this.compareData[this.compareData.length - 1]
        const change = last.aqi - first.aqi
        if (change < 0) {
          insights.push(`从${first.year}年到${last.year}年，AQI下降了${Math.abs(change)}，空气质量有所改善`)
        } else if (change > 0) {
          insights.push(`从${first.year}年到${last.year}年，AQI上升了${change}，空气质量有所恶化`)
        } else {
          insights.push(`从${first.year}年到${last.year}年，AQI基本持平`)
        }
      }
      
      return insights
    },
    aqiChartOption() {
      const years = this.compareData.map(d => d.year + '年')
      const aqis = this.compareData.map(d => d.aqi)
      const barColors = this.compareData.map((d, i) => this.colors[i % this.colors.length])
      
      return {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' },
          formatter: '{b}<br/>AQI: {c}'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: years,
          axisLabel: { color: '#666', fontSize: 14 },
          axisLine: { lineStyle: { color: '#ddd' } }
        },
        yAxis: {
          type: 'value',
          name: 'AQI',
          nameTextStyle: { color: '#666' },
          axisLabel: { color: '#666' },
          splitLine: { lineStyle: { color: '#f0f0f0' } }
        },
        series: [{
          type: 'bar',
          data: aqis.map((value, index) => ({
            value,
            itemStyle: {
              color: barColors[index],
              borderRadius: [8, 8, 0, 0]
            }
          })),
          barWidth: '50%',
          label: {
            show: true,
            position: 'top',
            formatter: '{c}',
            fontSize: 14,
            fontWeight: 'bold'
          }
        }]
      }
    },
    pollutantChartOption() {
      const years = this.compareData.map(d => d.year + '年')
      const currentTab = this.pollutantTabs.find(t => t.key === this.selectedPollutant)
      const barColors = this.compareData.map((d, i) => this.colors[i % this.colors.length])

      return {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' },
          formatter: `{b}<br/>${currentTab.label}: {c} ${currentTab.unit}`
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: 20,
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: years,
          axisLabel: { color: '#666', fontSize: 14 },
          axisLine: { lineStyle: { color: '#ddd' } }
        },
        yAxis: {
          type: 'value',
          name: currentTab.unit,
          nameTextStyle: { color: '#666' },
          axisLabel: { color: '#666' },
          splitLine: { lineStyle: { color: '#f0f0f0' } }
        },
        series: [{
          type: 'bar',
          data: this.compareData.map((d, index) => ({
            value: d[this.selectedPollutant] || 0,
            itemStyle: {
              color: barColors[index],
              borderRadius: [8, 8, 0, 0]
            }
          })),
          barWidth: '50%',
          label: {
            show: true,
            position: 'top',
            formatter: `{c}`,
            fontSize: 14,
            fontWeight: 'bold'
          }
        }]
      }
    }
  },
  mounted() {
    // 默认设置为昨天
    const yesterday = new Date()
    yesterday.setDate(yesterday.getDate() - 1)
    this.selectedDate = `${String(yesterday.getMonth() + 1).padStart(2, '0')}-${String(yesterday.getDate()).padStart(2, '0')}`
  },
  methods: {
    disabledFutureDate(date) {
      return date.getTime() > Date.now()
    },
    getAQIClass(aqi) {
      if (aqi <= 50) return 'excellent'
      if (aqi <= 100) return 'good'
      if (aqi <= 150) return 'light'
      if (aqi <= 200) return 'moderate'
      if (aqi <= 300) return 'heavy'
      return 'severe'
    },
    getAQITagType(aqi) {
      if (aqi <= 50) return 'success'
      if (aqi <= 100) return ''
      if (aqi <= 150) return 'warning'
      return 'danger'
    },
    async runCompare() {
      if (!this.canQuery) {
        this.$message.warning('请选择站点、日期和至少2个对比年份')
        return
      }
      
      this.loading = true
      this.hasData = false
      this.compareData = []
      
      try {
        // 查询每个年份的数据
        for (const year of this.selectedYears.sort()) {
          const date = `${year}-${this.selectedDate}`
          const result = await queryAirQualityByDate(this.selectedStation, date)
          
          if (result && result.success && result.data) {
            this.compareData.push({
              year,
              date: result.data.date,
              aqi: result.data.aqi,
              qualityLevel: result.data.qualityLevel,
              pm25: result.data.pm25,
              pm10: result.data.pm10,
              so2: result.data.so2,
              no2: result.data.no2,
              co: result.data.co,
              o3: result.data.o3,
              primaryPollutant: result.data.primaryPollutant
            })
          } else {
            // 如果没有数据，添加空记录
            this.compareData.push({
              year,
              date,
              aqi: 0,
              qualityLevel: '无数据',
              pm25: '--',
              pm10: '--',
              so2: '--',
              no2: '--',
              co: '--',
              o3: '--',
              primaryPollutant: '-'
            })
          }
        }
        
        this.hasData = true
        this.$message.success(`成功查询 ${this.compareData.length} 个年份的数据`)
      } catch (error) {
        console.error('查询失败:', error)
        this.$message.error('数据查询失败，请稍后重试')
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.user-compare {
  max-width: 1000px;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 25px;
  color: white;
}

.page-header h1 {
  font-size: 2rem;
  margin-bottom: 8px;
}

.config-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 15px;
  padding: 20px;
  margin-bottom: 20px;
}

.config-card h3 {
  color: #333;
  margin-bottom: 15px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.config-form {
  display: flex;
  align-items: flex-end;
  gap: 20px;
  flex-wrap: wrap;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

.year-select {
  min-width: 280px;
}

.chart-card,
.data-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 15px;
  padding: 20px;
  margin-bottom: 20px;
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 15px;
}

.chart-header h3 {
  color: #333;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend {
  display: flex;
  gap: 15px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #666;
}

.legend-item .dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.pollutant-tabs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.tab-item {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  background: #f0f0f0;
  color: #666;
  transition: all 0.3s ease;
}

.tab-item:hover {
  background: #e0e0e0;
}

.tab-item.active {
  background: linear-gradient(135deg, #1a2980 0%, #26d0ce 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(26, 41, 128, 0.3);
}

.chart {
  height: 300px;
}

.card-header {
  margin-bottom: 15px;
}

.card-header h3 {
  color: #333;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.aqi-badge {
  font-weight: bold;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 12px;
}

.aqi-badge.excellent { background: #00c853; color: white; }
.aqi-badge.good { background: #ffd600; color: #333; }
.aqi-badge.light { background: #ff9100; color: white; }
.aqi-badge.moderate { background: #ff1744; color: white; }
.aqi-badge.heavy { background: #7c4dff; color: white; }
.aqi-badge.severe { background: #3e2723; color: white; }

.insight-card {
  background: linear-gradient(135deg, #1a2980 0%, #26d0ce 100%);
  border-radius: 15px;
  padding: 20px;
  color: white;
}

.insight-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 15px;
}

.insight-content p {
  margin: 8px 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.loading-state {
  text-align: center;
  padding: 60px 20px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 15px;
  color: #666;
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
  background: rgba(255, 255, 255, 0.95);
  border-radius: 15px;
  color: #666;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 15px;
  color: #1a2980;
}

.empty-state .sub {
  font-size: 12px;
  margin-top: 8px;
  color: #999;
}

@media (max-width: 768px) {
  .config-form {
    flex-direction: column;
    align-items: stretch;
  }
  
  .legend {
    flex-wrap: wrap;
  }
}
</style>
