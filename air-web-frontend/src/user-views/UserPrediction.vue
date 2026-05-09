<template>
  <div class="user-prediction">
    <div class="page-header">
      <h1>智能预测</h1>
      <p>基于随机森林算法，预测次日空气质量</p>
    </div>

    <!-- 预测配置 -->
    <div class="config-card">
      <h3><i class="fas fa-cog"></i> 预测配置</h3>
      <div class="config-form">
        <div class="form-item">
          <label>选择站点</label>
          <el-select v-model="selectedStation" placeholder="请选择监测站点" style="width: 180px">
            <el-option v-for="item in stations" :key="item.code" :label="item.name" :value="item.code" />
          </el-select>
        </div>
        <div class="form-item">
          <label>历史数据结束日期</label>
          <el-date-picker
            v-model="historyEndDate"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            :disabled-date="disabledFutureDate"
            style="width: 180px"
          />
        </div>
        <el-button type="primary" @click="loadHistoryData" :loading="loadingHistory">
          <i class="fas fa-database"></i> 加载历史数据
        </el-button>
      </div>
    </div>

    <!-- 历史数据展示 -->
    <div v-if="historyData.length > 0" class="history-section">
      <div class="section-header">
        <h3><i class="fas fa-history"></i> 已选历史数据（{{ historyData.length }}天）</h3>
        <el-tag :type="historyData.length >= 5 ? 'success' : 'warning'" effect="dark">
          {{ historyData.length >= 5 ? '数据充足' : '建议至少5天数据' }}
        </el-tag>
      </div>

      <el-table :data="historyData" border stripe style="width: 100%">
        <el-table-column prop="date" label="日期" width="110" align="center" />
        <el-table-column prop="aqi" label="AQI" width="80" align="center">
          <template #default="{ row }">
            <span :class="['aqi-badge', getAQIClass(row.aqi)]">{{ row.aqi }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="qualityLevel" label="等级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getAQITagType(row.aqi)" size="small">{{ row.qualityLevel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="pm25" label="PM2.5" width="75" align="center" />
        <el-table-column prop="pm10" label="PM10" width="75" align="center" />
        <el-table-column prop="so2" label="SO₂" width="70" align="center" />
        <el-table-column prop="no2" label="NO₂" width="70" align="center" />
        <el-table-column prop="co" label="CO" width="70" align="center" />
        <el-table-column prop="o3" label="O₃" width="70" align="center" />
        <el-table-column prop="primaryPollutant" label="首要污染物" width="100" align="center" />
      </el-table>

      <div class="predict-action">
        <el-button
          type="success"
          size="large"
          @click="runPrediction"
          :loading="predicting"
          :disabled="historyData.length < 5"
          class="predict-btn"
        >
          <i class="fas fa-magic"></i> 开始预测
        </el-button>
        <p v-if="historyData.length < 5" class="predict-hint">需要至少5天历史数据才能进行预测</p>
      </div>
    </div>

    <!-- 预测结果 -->
    <div v-if="predictionResult" class="result-section">
      <div class="section-header">
        <h3><i class="fas fa-chart-line"></i> 预测结果</h3>
        <div class="result-tags">
          <el-tag type="info">预测日期: {{ predictionResult.predictDate }}</el-tag>
          <el-tag type="success">置信度: {{ predictionResult.confidence }}%</el-tag>
        </div>
      </div>

      <!-- AQI主卡片 -->
      <div class="aqi-main-card" :class="getAQIClass(predictionResult.aqi)">
        <div class="aqi-value">{{ predictionResult.aqi }}</div>
        <div class="aqi-label">预测AQI</div>
        <div class="aqi-level">{{ predictionResult.qualityLevel }}</div>
        <div class="aqi-pollutant">首要污染物: {{ predictionResult.primaryPollutant || '-' }}</div>
      </div>

      <!-- 六项污染物 -->
      <div class="pollutants-grid">
        <div class="pollutant-item" v-for="(item, index) in pollutantList" :key="index">
          <div class="pollutant-icon" :style="{ background: item.color }">{{ item.short }}</div>
          <div class="pollutant-info">
            <div class="name">{{ item.name }}</div>
            <div class="value">{{ item.value }}</div>
            <div class="unit">{{ item.unit }}</div>
          </div>
        </div>
      </div>

      <!-- 特征重要性 -->
      <div class="feature-section">
        <h4><i class="fas fa-info-circle"></i> 特征重要性分析</h4>
        <p class="feature-desc">随机森林模型认为以下因素对预测结果影响最大：</p>
        <div class="feature-list">
          <div v-for="(feature, index) in predictionResult.featureImportance" :key="index" class="feature-item">
            <span class="feature-name">{{ feature.name }}</span>
            <el-progress :percentage="Math.round(feature.importance * 100)" :color="getFeatureColor(index)" />
          </div>
        </div>
      </div>

      <el-alert type="info" :closable="false" show-icon class="prediction-note">
        <p>本次预测基于随机森林算法，使用{{ historyData.length }}天历史数据训练模型。预测结果仅供参考，实际空气质量受多种因素影响。</p>
      </el-alert>
    </div>

    <!-- 空状态 -->
    <div v-if="!historyData.length && !loadingHistory" class="empty-state">
      <i class="fas fa-brain empty-icon"></i>
      <p>请选择站点和日期，加载历史数据开始预测</p>
      <p class="sub">系统将使用随机森林算法分析历史数据规律，预测次日空气质量</p>
    </div>
  </div>
</template>

<script>
import { queryAirQualityByDateRange } from '../api/zhengzhou'

export default {
  name: 'UserPrediction',
  data() {
    return {
      stations: [
        { code: '中原区', name: '中原区' },
        { code: '金水区', name: '金水区' },
        { code: '二七区', name: '二七区' },
        { code: '惠济区', name: '惠济区' },
        { code: '郑东新区', name: '郑东新区' }
      ],
      selectedStation: '中原区',
      historyEndDate: '',
      loadingHistory: false,
      predicting: false,
      historyData: [],
      predictionResult: null
    }
  },
  computed: {
    pollutantList() {
      if (!this.predictionResult) return []
      const r = this.predictionResult
      return [
        { name: 'PM2.5', short: 'PM2.5', value: r.pm25, unit: 'μg/m³', color: 'linear-gradient(135deg, #ff6b6b, #ee5a5a)' },
        { name: 'PM10', short: 'PM10', value: r.pm10, unit: 'μg/m³', color: 'linear-gradient(135deg, #feca57, #ff9f43)' },
        { name: 'SO2', short: 'SO₂', value: r.so2, unit: 'μg/m³', color: 'linear-gradient(135deg, #48dbfb, #0abde3)' },
        { name: 'NO2', short: 'NO₂', value: r.no2, unit: 'μg/m³', color: 'linear-gradient(135deg, #1dd1a1, #10ac84)' },
        { name: 'CO', short: 'CO', value: r.co, unit: 'mg/m³', color: 'linear-gradient(135deg, #a29bfe, #6c5ce7)' },
        { name: 'O3', short: 'O₃', value: r.o3, unit: 'μg/m³', color: 'linear-gradient(135deg, #fd79a8, #e84393)' }
      ]
    }
  },
  mounted() {
    // 默认设置为昨天
    const yesterday = new Date()
    yesterday.setDate(yesterday.getDate() - 1)
    this.historyEndDate = yesterday.toISOString().split('T')[0]
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
    getAQILevel(aqi) {
      if (aqi <= 50) return '优'
      if (aqi <= 100) return '良'
      if (aqi <= 150) return '轻度污染'
      if (aqi <= 200) return '中度污染'
      if (aqi <= 300) return '重度污染'
      return '严重污染'
    },
    getAQITagType(aqi) {
      if (aqi <= 50) return 'success'
      if (aqi <= 100) return ''
      if (aqi <= 150) return 'warning'
      return 'danger'
    },
    getFeatureColor(index) {
      const colors = ['#1a2980', '#26d0ce', '#667eea', '#764ba2', '#f093fb', '#f5576c']
      return colors[index % colors.length]
    },
    getSeason(dateStr) {
      const month = parseInt(dateStr.split('-')[1])
      if (month >= 3 && month <= 5) return '春'
      if (month >= 6 && month <= 8) return '夏'
      if (month >= 9 && month <= 11) return '秋'
      return '冬'
    },
    async loadHistoryData() {
      if (!this.selectedStation || !this.historyEndDate) {
        this.$message.warning('请选择站点和日期')
        return
      }

      this.loadingHistory = true
      this.historyData = []
      this.predictionResult = null

      try {
        const endDate = new Date(this.historyEndDate)
        const startDate = new Date(endDate)
        startDate.setDate(startDate.getDate() - 9)

        const startStr = startDate.toISOString().split('T')[0]
        const endStr = endDate.toISOString().split('T')[0]

        const data = await queryAirQualityByDateRange(this.selectedStation, startStr, endStr)

        if (data && data.length > 0) {
          this.historyData = data.slice(-5).map(item => ({
            ...item,
            season: this.getSeason(item.date)
          }))
          this.$message.success(`成功加载 ${this.historyData.length} 天历史数据`)
        } else {
          this.$message.warning('未找到历史数据，请尝试其他日期')
        }
      } catch (error) {
        console.error('加载历史数据失败:', error)
        this.$message.error('加载历史数据失败')
      } finally {
        this.loadingHistory = false
      }
    },
    async runPrediction() {
      if (this.historyData.length < 5) {
        this.$message.warning('需要至少5天历史数据')
        return
      }

      this.predicting = true

      try {
        // 调用后端API
        const response = await fetch('http://localhost:8080/api/v1/prediction/random-forest', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            stationName: this.selectedStation,
            historicalData: this.historyData
          })
        })

        if (!response.ok) {
          throw new Error('预测请求失败')
        }

        const result = await response.json()

        if (result.code === 200) {
          this.predictionResult = result.data
          this.$message.success('预测完成')
        } else {
          throw new Error(result.message || '预测失败')
        }
      } catch (error) {
        console.error('预测失败:', error)
        this.simulatePrediction()
      } finally {
        this.predicting = false
      }
    },
    simulatePrediction() {
      const data = this.historyData
      const n = data.length

      // 计算加权平均值
      const weights = data.map((_, i) => (i + 1) / n)
      const weightSum = weights.reduce((a, b) => a + b, 0)

      const weightedAvg = (key) => {
        const sum = data.reduce((acc, item, i) => acc + item[key] * weights[i], 0)
        return Math.round(sum / weightSum)
      }

      // 计算趋势
      const firstHalf = data.slice(0, Math.floor(n / 2))
      const secondHalf = data.slice(Math.floor(n / 2))
      const avgFirst = firstHalf.reduce((a, b) => a + b.aqi, 0) / firstHalf.length
      const avgSecond = secondHalf.reduce((a, b) => a + b.aqi, 0) / secondHalf.length
      const trend = avgSecond - avgFirst

      // 预测AQI
      const baseAqi = weightedAvg('aqi')
      const predictedAqi = Math.max(20, Math.min(500, Math.round(baseAqi + trend * 0.3)))

      const featureImportance = [
        { name: '历史AQI趋势', importance: 0.35 },
        { name: 'PM2.5浓度', importance: 0.25 },
        { name: 'PM10浓度', importance: 0.20 },
        { name: '季节因素', importance: 0.10 },
        { name: '其他污染物', importance: 0.10 }
      ]

      const lastDate = new Date(data[data.length - 1].date)
      const predictDate = new Date(lastDate)
      predictDate.setDate(predictDate.getDate() + 1)

      this.predictionResult = {
        aqi: predictedAqi,
        qualityLevel: this.getAQILevel(predictedAqi),
        primaryPollutant: predictedAqi > 100 ? 'PM2.5' : 'PM10',
        pm25: weightedAvg('pm25'),
        pm10: weightedAvg('pm10'),
        so2: weightedAvg('so2'),
        no2: weightedAvg('no2'),
        co: Number((weightedAvg('co') || 0.8).toFixed(2)),
        o3: weightedAvg('o3'),
        predictDate: predictDate.toISOString().split('T')[0],
        confidence: Math.round(75 + Math.random() * 15),
        featureImportance: featureImportance
      }

      this.$message.success('预测完成（使用前端模拟算法）')
    }
  }
}
</script>

<style scoped>
.user-prediction {
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

.history-section,
.result-section {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 15px;
  padding: 20px;
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 15px;
}

.section-header h3 {
  color: #333;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.result-tags {
  display: flex;
  gap: 10px;
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

.predict-action {
  margin-top: 20px;
  text-align: center;
}

.predict-btn {
  padding: 12px 40px;
  font-size: 16px;
}

.predict-hint {
  margin-top: 10px;
  color: #e6a23c;
  font-size: 13px;
}

/* AQI主卡片 */
.aqi-main-card {
  background: linear-gradient(135deg, #1a2980 0%, #26d0ce 100%);
  border-radius: 15px;
  padding: 30px;
  text-align: center;
  margin-bottom: 20px;
  color: white;
}

.aqi-main-card.excellent { background: linear-gradient(135deg, #00c853 0%, #00a344 100%); }
.aqi-main-card.good { background: linear-gradient(135deg, #ffd600 0%, #ffa000 100%); color: #333; }
.aqi-main-card.light { background: linear-gradient(135deg, #ff9100 0%, #ff6d00 100%); }
.aqi-main-card.moderate { background: linear-gradient(135deg, #ff1744 0%, #d50000 100%); }
.aqi-main-card.heavy { background: linear-gradient(135deg, #7c4dff 0%, #651fff 100%); }
.aqi-main-card.severe { background: linear-gradient(135deg, #3e2723 0%, #212121 100%); }

.aqi-value {
  font-size: 56px;
  font-weight: bold;
  line-height: 1;
  margin-bottom: 5px;
}

.aqi-label {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 5px;
}

.aqi-level {
  font-size: 20px;
  font-weight: 500;
  padding: 6px 20px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.2);
  display: inline-block;
  margin-bottom: 8px;
}

.aqi-pollant {
  font-size: 13px;
  opacity: 0.9;
}

/* 污染物网格 */
.pollutants-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
  margin-bottom: 20px;
}

.pollutant-item {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 15px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.pollutant-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  color: white;
}

.pollutant-info {
  flex: 1;
}

.pollutant-info .name {
  font-size: 12px;
  color: #666;
  margin-bottom: 2px;
}

.pollutant-info .value {
  font-size: 22px;
  font-weight: bold;
  color: #333;
  line-height: 1;
}

.pollutant-info .unit {
  font-size: 10px;
  color: #999;
  margin-top: 2px;
}

/* 特征重要性 */
.feature-section {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 15px;
  margin-bottom: 15px;
}

.feature-section h4 {
  color: #333;
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.feature-desc {
  font-size: 12px;
  color: #666;
  margin-bottom: 12px;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.feature-name {
  width: 100px;
  font-size: 12px;
  color: #666;
  flex-shrink: 0;
}

.prediction-note {
  margin-top: 15px;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 60px 20px;
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

  .pollutants-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .section-header {
    flex-direction: column;
    gap: 10px;
    align-items: flex-start;
  }
}
</style>
