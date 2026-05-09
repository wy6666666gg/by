<template>
  <div class="user-query">
    <!-- 页面标题 -->
    <div class="query-header">
      <h1>空气质量数据查询</h1>
      <p class="subtitle">基于郑州市五区历史监测数据，支持按日期精确查询</p>
    </div>

    <!-- 查询区域 -->
    <div class="query-section">
      <el-card class="query-card">
        <el-form :model="queryForm" label-position="top">
          <el-row :gutter="20">
            <el-col :xs="24" :sm="8">
              <el-form-item label="查询模式">
                <el-radio-group v-model="queryForm.mode" size="large" @change="handleModeChange">
                  <el-radio-button label="single">单日查询</el-radio-button>
                  <el-radio-button label="range">区间查询</el-radio-button>
                </el-radio-group>
              </el-form-item>
            </el-col>
            
            <el-col :xs="24" :sm="8">
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
            
            <el-col :xs="24" :sm="8">
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
          </el-row>
          
          <!-- 快捷日期选择和查询按钮 -->
          <el-row :gutter="20" style="margin-top: 10px">
            <el-col :xs="24" :sm="16">
              <el-form-item label="快捷选择">
                <el-space wrap>
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
            <el-col :xs="24" :sm="8">
              <el-form-item label="操作">
                <el-button type="primary" size="large" @click="handleQuery" :loading="loading" style="width: 100%">
                  <i class="fas fa-search"></i> 查询数据
                </el-button>
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
        <el-row :gutter="15">
          <el-col :xs="12" :sm="6">
            <div class="overview-card aqi-card" :class="getAqiClass(singleResult.data.aqi)">
              <div class="overview-content">
                <div class="overview-label">AQI 指数</div>
                <div class="overview-value">{{ singleResult.data.aqi }}</div>
                <div class="overview-level">{{ singleResult.data.qualityLevel }}</div>
              </div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="6">
            <div class="overview-card">
              <div class="overview-content">
                <div class="overview-label">首要污染物</div>
                <div class="overview-value" style="font-size: 20px;">{{ singleResult.data.primaryPollutant || '无' }}</div>
                <div class="overview-sub">PM2.5: {{ singleResult.data.pm25 }} μg/m³</div>
              </div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="6">
            <div class="overview-card">
              <div class="overview-content">
                <div class="overview-label">当月平均水平</div>
                <div class="overview-value" style="font-size: 20px;">{{ singleResult.monthStats.avg }}</div>
                <div class="overview-sub">范围: {{ singleResult.monthStats.min }} - {{ singleResult.monthStats.max }}</div>
              </div>
            </div>
          </el-col>
          <el-col :xs="12" :sm="6">
            <div class="overview-card">
              <div class="overview-content">
                <div class="overview-label">年度排名</div>
                <div class="overview-value" style="font-size: 20px;">{{ singleResult.yearRank.rank }}</div>
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
            <span><i class="fas fa-chart-bar"></i> 六项污染物详细数据</span>
            <span class="header-date">{{ queryForm.date }}</span>
          </div>
        </template>
        
        <el-row :gutter="15">
          <el-col :xs="12" :sm="4" v-for="(item, index) in pollutantDetails" :key="index">
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
            <span><i class="fas fa-chart-line"></i> 前后7天AQI变化趋势</span>
          </div>
        </template>
        <v-chart class="chart" :option="nearbyChartOption" autoresize />
      </el-card>

      <!-- 历史同期对比 -->
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span><i class="fas fa-history"></i> 过去5年同日数据对比</span>
          </div>
        </template>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12">
            <v-chart class="chart" :option="historicalChartOption" autoresize />
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-table :data="singleResult.historicalSameDay" stripe style="width: 100%" size="small">
              <el-table-column prop="year" label="年份" width="80" />
              <el-table-column prop="aqi" label="AQI" width="80">
                <template #default="{ row }">
                  <span :class="getAqiClass(row.aqi)">{{ row.aqi }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="qualityLevel" label="等级" width="90" />
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
        <el-row :gutter="15">
          <el-col :xs="12" :sm="4" v-for="(stat, index) in rangeStats" :key="index">
            <div class="overview-card small">
              <div class="overview-content">
                <div class="overview-label">{{ stat.label }}</div>
                <div class="overview-value" :style="{ fontSize: '20px', color: stat.color }">{{ stat.value }}</div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>

      <!-- 趋势图表 -->
      <el-card class="chart-card">
        <template #header>
          <div class="card-header">
            <span><i class="fas fa-chart-area"></i> AQI变化趋势</span>
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
            <span><i class="fas fa-table"></i> 详细数据列表</span>
            <el-button type="primary" size="small" @click="exportData">
              <i class="fas fa-download"></i> 导出数据
            </el-button>
          </div>
        </template>
        <el-table :data="rangeResult" stripe style="width: 100%" size="small">
          <el-table-column prop="date" label="日期" width="110" sortable />
          <el-table-column prop="aqi" label="AQI" width="80" sortable>
            <template #default="{ row }">
              <span :class="getAqiClass(row.aqi)">{{ row.aqi }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="qualityLevel" label="等级" width="90" />
          <el-table-column prop="pm25" label="PM2.5" width="80" />
          <el-table-column prop="pm10" label="PM10" width="80" />
          <el-table-column prop="o3" label="O₃" width="70" />
          <el-table-column prop="so2" label="SO₂" width="70" />
          <el-table-column prop="co" label="CO" width="70" />
          <el-table-column prop="no2" label="NO₂" width="70" />
          <el-table-column prop="primaryPollutant" label="首要污染物" />
        </el-table>
      </el-card>
    </template>

    <!-- 空状态 -->
    <div v-if="!loading && queryForm.mode === 'single' && !singleResult" class="empty-state">
      <el-empty description="请选择查询条件并点击查询按钮">
        <template #image>
          <i class="fas fa-search" style="font-size: 60px; color: #ddd;"></i>
        </template>
      </el-empty>
    </div>
    <div v-if="!loading && queryForm.mode === 'range' && rangeResult.length === 0" class="empty-state">
      <el-empty description="请选择日期范围并点击查询按钮">
        <template #image>
          <i class="fas fa-calendar-alt" style="font-size: 60px; color: #ddd;"></i>
        </template>
      </el-empty>
    </div>
  </div>
</template>

<script>
import { queryAirQualityByDate, queryAirQualityByDateRange } from '../api/zhengzhou.js'

export default {
  name: 'UserQuery',
  data() {
    return {
      // 站点列表
      stations: [
        { code: '中原区', name: '中原区' },
        { code: '金水区', name: '金水区' },
        { code: '二七区', name: '二七区' },
        { code: '惠济区', name: '惠济区' },
        { code: '郑东新区', name: '郑东新区' }
      ],
      // 查询表单
      queryForm: {
        mode: 'single',
        station: '中原区',
        date: '',
        dateRange: []
      },
      // 快捷日期选项
      quickDates: [
        { label: '今天', type: 'today' },
        { label: '昨天', type: 'yesterday' },
        { label: '近7天', type: 'last7days' },
        { label: '近30天', type: 'last30days' },
        { label: '本月', type: 'thisMonth' },
        { label: '上月', type: 'lastMonth' }
      ],
      loading: false,
      singleResult: null,
      rangeResult: [],
      rangeChartType: 'aqi'
    }
  },
  computed: {
    // 污染物详情
    pollutantDetails() {
      if (!this.singleResult) return []
      const data = this.singleResult.data
      return [
        { name: 'PM2.5', value: data.pm25, unit: 'μg/m³', color: '#ff5555' },
        { name: 'PM10', value: data.pm10, unit: 'μg/m³', color: '#ffaa00' },
        { name: 'O₃', value: data.o3, unit: 'μg/m³', color: '#00d4ff' },
        { name: 'SO₂', value: data.so2, unit: 'μg/m³', color: '#00ff88' },
        { name: 'NO₂', value: data.no2, unit: 'μg/m³', color: '#aa66ff' },
        { name: 'CO', value: data.co, unit: 'mg/m³', color: '#ff66aa' }
      ]
    },
    // 区间统计
    rangeStats() {
      if (this.rangeResult.length === 0) return []
      
      const aqis = this.rangeResult.map(d => d.aqi)
      const avg = Math.round(aqis.reduce((a, b) => a + b, 0) / aqis.length)
      const max = Math.max(...aqis)
      const min = Math.min(...aqis)
      const goodDays = this.rangeResult.filter(d => d.aqi <= 100).length
      const pollutionDays = this.rangeResult.filter(d => d.aqi > 100).length
      
      return [
        { label: '平均AQI', value: avg, color: '#00d4ff' },
        { label: '最高AQI', value: max, color: '#ff5555' },
        { label: '最低AQI', value: min, color: '#00ff88' },
        { label: '优良天数', value: goodDays + '天', color: '#00ff88' },
        { label: '污染天数', value: pollutionDays + '天', color: '#ff5555' },
        { label: '总天数', value: this.rangeResult.length + '天', color: '#ffaa00' }
      ]
    },
    // 前后7天图表配置
    nearbyChartOption() {
      if (!this.singleResult) return {}
      
      const data = this.singleResult.nearbyData
      return {
        tooltip: { trigger: 'axis' },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: {
          type: 'category',
          data: data.map(d => d.date),
          axisLabel: { 
            color: '#666',
            formatter: (value) => {
              const date = new Date(value)
              return `${date.getMonth() + 1}/${date.getDate()}`
            }
          }
        },
        yAxis: {
          type: 'value',
          axisLabel: { color: '#666' },
          splitLine: { lineStyle: { color: '#f0f0f0' } }
        },
        series: [{
          data: data.map(d => ({
            value: d.aqi,
            itemStyle: d.isTarget ? { color: '#1a2980', borderWidth: 2, borderColor: '#fff' } : { color: '#3b82f6' }
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
    },
    // 历史同期图表配置
    historicalChartOption() {
      if (!this.singleResult) return {}
      
      const data = this.singleResult.historicalSameDay
      const currentAqi = this.singleResult.data.aqi
      
      return {
        tooltip: { trigger: 'axis' },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: {
          type: 'category',
          data: [...data.map(d => d.year), '今年'],
          axisLabel: { color: '#666' }
        },
        yAxis: {
          type: 'value',
          axisLabel: { color: '#666' },
          splitLine: { lineStyle: { color: '#f0f0f0' } }
        },
        series: [{
          data: [...data.map(d => d.aqi), { value: currentAqi, itemStyle: { color: '#1a2980' } }],
          type: 'bar',
          barWidth: '50%',
          itemStyle: {
            borderRadius: [4, 4, 0, 0],
            color: (params) => params.dataIndex === data.length ? '#1a2980' : '#3b82f6'
          }
        }]
      }
    },
    // 区间图表配置
    rangeChartOption() {
      if (this.rangeResult.length === 0) return {}
      
      const field = this.rangeChartType
      const labelMap = { aqi: 'AQI', pm25: 'PM2.5', pm10: 'PM10' }
      const colorMap = { aqi: '#1a2980', pm25: '#ff5555', pm10: '#ffaa00' }
      
      return {
        tooltip: { trigger: 'axis' },
        grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true },
        xAxis: {
          type: 'category',
          data: this.rangeResult.map(d => d.date),
          axisLabel: { 
            color: '#666',
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
          axisLabel: { color: '#666' },
          splitLine: { lineStyle: { color: '#f0f0f0' } }
        },
        series: [{
          data: this.rangeResult.map(d => d[field]),
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
    }
  },
  mounted() {
    // 默认选择昨天
    const yesterday = new Date()
    yesterday.setDate(yesterday.getDate() - 1)
    this.queryForm.date = yesterday.toISOString().split('T')[0]
  },
  methods: {
    // 获取AQI样式类
    getAqiClass(aqi) {
      if (aqi <= 50) return 'aqi-excellent'
      if (aqi <= 100) return 'aqi-good'
      if (aqi <= 150) return 'aqi-moderate'
      if (aqi <= 200) return 'aqi-unhealthy'
      if (aqi <= 300) return 'aqi-very-unhealthy'
      return 'aqi-hazardous'
    },
    // 禁用日期
    disabledDate(time) {
      const minDate = new Date('2015-01-01')
      const maxDate = new Date('2030-12-31')
      return time.getTime() < minDate.getTime() || time.getTime() > maxDate.getTime()
    },
    // 应用快捷日期
    applyQuickDate(item) {
      const today = new Date()
      const formatDate = (d) => d.toISOString().split('T')[0]
      
      switch (item.type) {
        case 'today':
          this.queryForm.date = formatDate(today)
          this.queryForm.mode = 'single'
          break
        case 'yesterday':
          const yesterday = new Date(today)
          yesterday.setDate(yesterday.getDate() - 1)
          this.queryForm.date = formatDate(yesterday)
          this.queryForm.mode = 'single'
          break
        case 'last7days':
          const last7 = new Date(today)
          last7.setDate(last7.getDate() - 7)
          this.queryForm.dateRange = [formatDate(last7), formatDate(today)]
          this.queryForm.mode = 'range'
          break
        case 'last30days':
          const last30 = new Date(today)
          last30.setDate(last30.getDate() - 30)
          this.queryForm.dateRange = [formatDate(last30), formatDate(today)]
          this.queryForm.mode = 'range'
          break
        case 'thisMonth':
          const firstDay = new Date(today.getFullYear(), today.getMonth(), 1)
          this.queryForm.dateRange = [formatDate(firstDay), formatDate(today)]
          this.queryForm.mode = 'range'
          break
        case 'lastMonth':
          const lastMonthStart = new Date(today.getFullYear(), today.getMonth() - 1, 1)
          const lastMonthEnd = new Date(today.getFullYear(), today.getMonth(), 0)
          this.queryForm.dateRange = [formatDate(lastMonthStart), formatDate(lastMonthEnd)]
          this.queryForm.mode = 'range'
          break
      }
    },
    // 模式切换
    handleModeChange() {
      this.singleResult = null
      this.rangeResult = []
    },
    // 查询
    async handleQuery() {
      if (!this.queryForm.station) {
        this.$message.warning('请选择站点')
        return
      }
      
      this.loading = true
      
      try {
        if (this.queryForm.mode === 'single') {
          if (!this.queryForm.date) {
            this.$message.warning('请选择日期')
            this.loading = false
            return
          }
          const result = await queryAirQualityByDate(this.queryForm.station, this.queryForm.date)
          if (result.success) {
            this.singleResult = result
            if (result.isMock) {
              this.$message.warning('当前显示的是模拟数据，CSV文件中未找到该日期数据')
            } else if (result.isCustom) {
              this.$message.success('查询成功（数据来源：用户自定义数据）')
            } else {
              this.$message.success('查询成功（数据来源：CSV文件）')
            }
          } else {
            this.$message.error(result.message || '查询失败')
          }
        } else {
          if (!this.queryForm.dateRange || this.queryForm.dateRange.length !== 2) {
            this.$message.warning('请选择日期范围')
            this.loading = false
            return
          }
          const result = await queryAirQualityByDateRange(
            this.queryForm.station,
            this.queryForm.dateRange[0],
            this.queryForm.dateRange[1]
          )
          this.rangeResult = result
          if (result.length > 0 && result[0].isMock) {
            this.$message.warning('当前显示的是模拟数据，CSV文件中未找到该日期范围数据')
          } else {
            const customCount = result.filter(r => r.isCustom).length
            if (customCount > 0) {
              this.$message.success(`查询成功，共 ${result.length} 条数据（其中 ${customCount} 条为用户自定义数据）`)
            } else {
              this.$message.success(`查询成功，共 ${result.length} 条数据（数据来源：CSV文件）`)
            }
          }
        }
      } catch (error) {
        this.$message.error('查询失败，请稍后重试')
        console.error(error)
      } finally {
        this.loading = false
      }
    },
    // 导出数据
    exportData() {
      if (this.rangeResult.length === 0) {
        this.$message.warning('没有数据可导出')
        return
      }
      
      const headers = ['日期', 'AQI', '等级', 'PM2.5', 'PM10', 'O3', 'SO2', 'CO', 'NO2', '首要污染物']
      const rows = this.rangeResult.map(d => [
        d.date, d.aqi, d.qualityLevel, d.pm25, d.pm10, d.o3, d.so2, d.co, d.no2, d.primaryPollutant
      ])
      
      const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
      const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' })
      const link = document.createElement('a')
      link.href = URL.createObjectURL(blob)
      link.download = `${this.queryForm.station}_数据查询_${this.queryForm.dateRange[0]}_${this.queryForm.dateRange[1]}.csv`
      link.click()
      
      this.$message.success('数据导出成功')
    }
  }
}
</script>

<style scoped>
.user-query {
  max-width: 1000px;
  margin: 0 auto;
}

.query-header {
  text-align: center;
  margin-bottom: 30px;
}

.query-header h1 {
  color: white;
  margin-bottom: 10px;
}

.subtitle {
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
}

.query-section {
  margin-bottom: 20px;
}

.query-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 15px;
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
  color: #999;
  font-size: 14px;
}

.quick-date-tag {
  cursor: pointer;
  transition: all 0.3s;
}

.quick-date-tag:hover {
  color: #1a2980;
  border-color: #1a2980;
}

.result-overview {
  margin-bottom: 20px;
}

.overview-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 12px;
  padding: 20px;
  text-align: center;
  border: 2px solid transparent;
  height: 130px;
}

.overview-card.small {
  height: 100px;
  padding: 15px;
}

.overview-label {
  color: #666;
  font-size: 13px;
  margin-bottom: 8px;
}

.overview-value {
  font-size: 32px;
  font-weight: bold;
  color: #333;
  line-height: 1.2;
}

.overview-level {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  margin-top: 8px;
  background: rgba(26, 41, 128, 0.1);
  color: #1a2980;
}

.overview-sub {
  color: #999;
  font-size: 12px;
  margin-top: 4px;
}

.aqi-card {
  border: 2px solid transparent;
}

.aqi-card.aqi-excellent { border-color: #00e400; background: linear-gradient(135deg, rgba(0, 228, 0, 0.1) 0%, rgba(255, 255, 255, 0.95) 100%); }
.aqi-card.aqi-good { border-color: #ffff00; background: linear-gradient(135deg, rgba(255, 255, 0, 0.1) 0%, rgba(255, 255, 255, 0.95) 100%); }
.aqi-card.aqi-moderate { border-color: #ff7e00; background: linear-gradient(135deg, rgba(255, 126, 0, 0.1) 0%, rgba(255, 255, 255, 0.95) 100%); }
.aqi-card.aqi-unhealthy { border-color: #ff0000; background: linear-gradient(135deg, rgba(255, 0, 0, 0.1) 0%, rgba(255, 255, 255, 0.95) 100%); }
.aqi-card.aqi-very-unhealthy { border-color: #8f3f97; background: linear-gradient(135deg, rgba(143, 63, 151, 0.1) 0%, rgba(255, 255, 255, 0.95) 100%); }
.aqi-card.aqi-hazardous { border-color: #7e0023; background: linear-gradient(135deg, rgba(126, 0, 35, 0.1) 0%, rgba(255, 255, 255, 0.95) 100%); }

.detail-card, .chart-card, .table-card {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 15px;
  margin-bottom: 20px;
}

.pollutant-item {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 15px;
  text-align: center;
  border-left: 4px solid;
  margin-bottom: 10px;
}

.pollutant-name {
  font-size: 12px;
  color: #666;
  margin-bottom: 8px;
}

.pollutant-value {
  font-size: 22px;
  font-weight: bold;
  color: #333;
  margin-bottom: 4px;
}

.pollutant-unit {
  font-size: 11px;
  color: #999;
}

.chart {
  height: 300px;
}

.empty-state {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 15px;
  padding: 60px 20px;
}

.aqi-excellent { color: #00e400; font-weight: bold; }
.aqi-good { color: #eab308; font-weight: bold; }
.aqi-moderate { color: #ff7e00; font-weight: bold; }
.aqi-unhealthy { color: #ff0000; font-weight: bold; }
.aqi-very-unhealthy { color: #8f3f97; font-weight: bold; }
.aqi-hazardous { color: #7e0023; font-weight: bold; }

@media (max-width: 768px) {
  .overview-card {
    margin-bottom: 10px;
    height: auto;
    min-height: 100px;
  }
  .chart {
    height: 250px;
  }
}
</style>
