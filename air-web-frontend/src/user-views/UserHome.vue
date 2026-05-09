<template>
  <div class="user-home">
    <div class="hero-section">
      <h1>实时空气质量监测</h1>
      <p>为您提供准确、实时的空气质量数据查询服务</p>
      <div class="search-box">
        <el-input
          v-model="searchCity"
          placeholder="请输入城市名称查询空气质量"
          size="large"
          :disabled="loading"
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button type="primary" @click="handleSearch" :loading="loading">
              <i class="fas fa-search"></i> 查询
            </el-button>
          </template>
        </el-input>
      </div>
    </div>
    
    <div v-if="loading" class="loading-state">
      <el-skeleton :rows="3" animated />
    </div>
    
    <div class="aqi-display" v-else-if="currentAQI">
      <div class="aqi-card" :class="getAQIClass(currentAQI.aqi)">
        <div class="aqi-value">{{ currentAQI.aqi }}</div>
        <div class="aqi-level">{{ getAQILevel(currentAQI.aqi) }}</div>
        <div class="aqi-city">{{ currentAQI.city }}</div>
      </div>
      <div class="pollutant-grid">
        <div class="pollutant-item">
          <span class="label">PM2.5</span>
          <span class="value">{{ currentAQI.pm25?.toFixed(1) || '--' }} μg/m³</span>
        </div>
        <div class="pollutant-item">
          <span class="label">PM10</span>
          <span class="value">{{ currentAQI.pm10?.toFixed(1) || '--' }} μg/m³</span>
        </div>
        <div class="pollutant-item">
          <span class="label">SO₂</span>
          <span class="value">{{ currentAQI.so2?.toFixed(1) || '--' }} μg/m³</span>
        </div>
        <div class="pollutant-item">
          <span class="label">NO₂</span>
          <span class="value">{{ currentAQI.no2?.toFixed(1) || '--' }} μg/m³</span>
        </div>
        <div class="pollutant-item">
          <span class="label">O₃</span>
          <span class="value">{{ currentAQI.o3?.toFixed(1) || '--' }} μg/m³</span>
        </div>
        <div class="pollutant-item">
          <span class="label">CO</span>
          <span class="value">{{ currentAQI.co?.toFixed(2) || '--' }} mg/m³</span>
        </div>
      </div>
      <div class="update-time">
        <i class="fas fa-clock"></i> 更新时间: {{ currentAQI.updateTime }}
      </div>
    </div>
    
    <div class="features-section">
      <h2>功能服务</h2>
      <div class="features-grid">
        <div class="feature-card" @click="$router.push('/query')">
          <i class="fas fa-search-location"></i>
          <h3>空气质量查询</h3>
          <p>查询郑州市各区域实时空气质量指数和六项污染物浓度</p>
        </div>
        <div class="feature-card" @click="$router.push('/prediction')">
          <i class="fas fa-magic"></i>
          <h3>智能预测</h3>
          <p>基于随机森林算法预测次日空气质量状况</p>
        </div>
        <div class="feature-card" @click="$router.push('/compare')">
          <i class="fas fa-balance-scale"></i>
          <h3>历年对比</h3>
          <p>对比多个年份同一天期的空气质量变化趋势</p>
        </div>
      </div>
    </div>
    
    <div class="hot-cities">
      <h2>热门城市空气质量</h2>
      <div v-if="hotCitiesLoading" class="cities-loading">
        <el-skeleton :rows="2" animated />
      </div>
      <div v-else class="cities-grid">
        <div 
          v-for="city in hotCitiesData" 
          :key="city.name" 
          class="city-item"
          :class="getAQIClass(city.aqi)"
          @click="selectCity(city)"
        >
          <div class="city-name">{{ city.name }}</div>
          <div class="city-aqi">{{ city.aqi || '--' }}</div>
          <div class="city-level">{{ city.aqi ? getAQILevel(city.aqi) : '加载中' }}</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getZhengzhouCityAQI, getZhengzhouRealtimeData } from '../api/zhengzhou.js'

// 郑州市各区列表
const ZHENGZHOU_DISTRICTS = [
  { name: '中原区', stationName: '中原区' },
  { name: '金水区', stationName: '金水区' },
  { name: '二七区', stationName: '二七区' },
  { name: '惠济区', stationName: '惠济区' },
  { name: '郑东新区', stationName: '郑东新区' }
]

export default {
  name: 'UserHome',
  data() {
    return {
      searchCity: '',
      loading: false,
      hotCitiesLoading: false,
      currentAQI: null,
      hotCitiesData: ZHENGZHOU_DISTRICTS.map(c => ({ ...c, aqi: null })),
      refreshTimer: null
    }
  },
  mounted() {
    // 默认加载郑州市整体数据
    this.loadCityData()
    // 加载各区数据
    this.loadDistrictsData()
    // 启动自动刷新
    this.startAutoRefresh()
  },
  beforeUnmount() {
    // 清除定时器
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer)
    }
  },
  methods: {
    getAQIClass(aqi) {
      if (!aqi) return ''
      if (aqi <= 50) return 'excellent'
      if (aqi <= 100) return 'good'
      if (aqi <= 150) return 'light'
      if (aqi <= 200) return 'moderate'
      if (aqi <= 300) return 'heavy'
      return 'severe'
    },
    getAQILevel(aqi) {
      if (!aqi) return '--'
      if (aqi <= 50) return '优'
      if (aqi <= 100) return '良'
      if (aqi <= 150) return '轻度污染'
      if (aqi <= 200) return '中度污染'
      if (aqi <= 300) return '重度污染'
      return '严重污染'
    },
    // 从localStorage读取自定义实时数据
    loadCustomRealtimeData() {
      try {
        const storageKey = 'air_quality_realtime_custom'
        const customData = JSON.parse(localStorage.getItem(storageKey) || '{}')
        return customData
      } catch (error) {
        console.error('读取自定义数据失败:', error)
        return {}
      }
    },
    // 加载郑州市整体实时数据
    async loadCityData() {
      this.loading = true
      try {
        // 先加载各区数据（包含自定义数据合并）
        await this.loadDistrictsData()
        
        // 基于各站点数据计算城市整体平均值
        const validStations = this.hotCitiesData.filter(d => d.aqi > 0)
        if (validStations.length > 0) {
          const avgAQI = Math.round(validStations.reduce((sum, s) => sum + s.aqi, 0) / validStations.length)
          const avgPM25 = validStations.reduce((sum, s) => sum + (s.pm25 || 0), 0) / validStations.length
          const avgPM10 = validStations.reduce((sum, s) => sum + (s.pm10 || 0), 0) / validStations.length
          const avgSO2 = validStations.reduce((sum, s) => sum + (s.so2 || 0), 0) / validStations.length
          const avgNO2 = validStations.reduce((sum, s) => sum + (s.no2 || 0), 0) / validStations.length
          const avgCO = validStations.reduce((sum, s) => sum + (s.co || 0), 0) / validStations.length
          const avgO3 = validStations.reduce((sum, s) => sum + (s.o3 || 0), 0) / validStations.length
          
          this.currentAQI = {
            city: '郑州市',
            aqi: avgAQI,
            pm25: avgPM25,
            pm10: avgPM10,
            so2: avgSO2,
            no2: avgNO2,
            co: avgCO,
            o3: avgO3,
            primaryPollutant: validStations[0].primaryPollutant || '-',
            updateTime: new Date().toLocaleString('zh-CN')
          }
        } else {
          //  fallback：使用API数据
          const cityData = await getZhengzhouCityAQI()
          this.currentAQI = {
            city: '郑州市',
            aqi: cityData.aqi,
            pm25: cityData.pm25,
            pm10: cityData.pm10,
            so2: cityData.so2,
            no2: cityData.no2,
            co: cityData.co,
            o3: cityData.o3,
            primaryPollutant: cityData.primaryPollutant,
            updateTime: cityData.updateTime || new Date().toLocaleString('zh-CN')
          }
        }
      } catch (error) {
        console.error('获取郑州实时数据失败:', error)
        this.$message.error('获取实时数据失败，请稍后重试')
      } finally {
        this.loading = false
      }
    },
    // 加载各区实时数据
    async loadDistrictsData() {
      this.hotCitiesLoading = true
      try {
        const districtsData = await getZhengzhouRealtimeData()
        
        // 读取自定义数据
        const customData = this.loadCustomRealtimeData()
        
        // 合并数据（自定义数据优先级更高）
        this.hotCitiesData = ZHENGZHOU_DISTRICTS.map(district => {
          const data = districtsData.find(d => d.stationName === district.stationName)
          const custom = customData[district.stationName]
          
          // 优先使用自定义数据，确保即使API数据为undefined也能显示
          const mergedData = {
            aqi: data?.aqi || 0,
            pm25: data?.pm25 || 0,
            pm10: data?.pm10 || 0,
            so2: data?.so2 || 0,
            no2: data?.no2 || 0,
            co: data?.co || 0,
            o3: data?.o3 || 0,
            updateTime: data?.updateTime,
            ...custom  // 自定义数据覆盖API数据
          }
          
          return {
            ...district,
            aqi: mergedData.aqi,
            pm25: mergedData.pm25,
            pm10: mergedData.pm10,
            so2: mergedData.so2,
            no2: mergedData.no2,
            co: mergedData.co,
            o3: mergedData.o3,
            updateTime: new Date().toLocaleString('zh-CN')
          }
        })
      } catch (error) {
        console.error('获取各区数据失败:', error)
      } finally {
        this.hotCitiesLoading = false
      }
    },
    // 启动自动刷新
    startAutoRefresh() {
      // 每30秒刷新一次数据
      this.refreshTimer = setInterval(() => {
        console.log('自动刷新实时数据...')
        this.loadCityData()
        this.loadDistrictsData()
      }, 30000)
    },
    handleSearch() {
      if (this.searchCity.trim()) {
        // 搜索时查找对应区域
        const district = ZHENGZHOU_DISTRICTS.find(d => 
          d.name.includes(this.searchCity.trim()) || 
          this.searchCity.trim().includes(d.name)
        )
        if (district) {
          const data = this.hotCitiesData.find(d => d.name === district.name)
          if (data && data.aqi) {
            this.currentAQI = {
              city: data.name,
              aqi: data.aqi,
              pm25: data.pm25,
              pm10: data.pm10,
              so2: data.so2,
              no2: data.no2,
              co: data.co,
              o3: data.o3,
              updateTime: data.updateTime
            }
          }
        } else {
          this.$message.warning('仅支持查询郑州市内各区')
        }
      }
    },
    selectCity(city) {
      if (city.aqi) {
        this.currentAQI = {
          city: city.name,
          aqi: city.aqi,
          pm25: city.pm25,
          pm10: city.pm10,
          so2: city.so2,
          no2: city.no2,
          co: city.co,
          o3: city.o3,
          updateTime: city.updateTime
        }
      }
    }
  }
}
</script>

<style scoped>
.user-home {
  max-width: 1000px;
  margin: 0 auto;
}

.hero-section {
  text-align: center;
  padding: 40px 20px;
  color: white;
}

.hero-section h1 {
  font-size: 2.5rem;
  margin-bottom: 10px;
  text-shadow: 2px 2px 4px rgba(0,0,0,0.2);
}

.hero-section p {
  font-size: 1.1rem;
  opacity: 0.9;
  margin-bottom: 30px;
}

.search-box {
  max-width: 500px;
  margin: 0 auto;
}

.loading-state {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  padding: 30px;
  margin-bottom: 30px;
}

.aqi-display {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  padding: 30px;
  margin-bottom: 30px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

.aqi-card {
  text-align: center;
  padding: 30px;
  border-radius: 15px;
  margin-bottom: 20px;
  color: white;
}

.aqi-card.excellent { background: linear-gradient(135deg, #00c853 0%, #64dd17 100%); }
.aqi-card.good { background: linear-gradient(135deg, #ffd600 0%, #ffab00 100%); color: #333; }
.aqi-card.light { background: linear-gradient(135deg, #ff9100 0%, #ff6d00 100%); }
.aqi-card.moderate { background: linear-gradient(135deg, #ff1744 0%, #f50057 100%); }
.aqi-card.heavy { background: linear-gradient(135deg, #7c4dff 0%, #651fff 100%); }
.aqi-card.severe { background: linear-gradient(135deg, #3e2723 0%, #5d4037 100%); }

.aqi-value {
  font-size: 5rem;
  font-weight: bold;
  line-height: 1;
}

.aqi-level {
  font-size: 1.5rem;
  margin-top: 10px;
}

.aqi-city {
  font-size: 1.1rem;
  margin-top: 5px;
  opacity: 0.9;
}

.pollutant-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
}

.pollutant-item {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 10px;
  text-align: center;
}

.pollutant-item .label {
  display: block;
  color: #666;
  font-size: 0.9rem;
  margin-bottom: 5px;
}

.pollutant-item .value {
  display: block;
  color: #333;
  font-size: 1.1rem;
  font-weight: bold;
}

.update-time {
  text-align: center;
  margin-top: 20px;
  color: #999;
  font-size: 0.9rem;
}

.update-time i {
  margin-right: 5px;
}

.features-section {
  margin-bottom: 30px;
}

.features-section h2 {
  color: white;
  text-align: center;
  margin-bottom: 20px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.feature-card {
  background: rgba(255, 255, 255, 0.95);
  padding: 30px;
  border-radius: 15px;
  text-align: center;
  cursor: pointer;
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.feature-card i {
  font-size: 3rem;
  color: #1a2980;
  margin-bottom: 15px;
}

.feature-card h3 {
  color: #333;
  margin-bottom: 10px;
}

.feature-card p {
  color: #666;
  font-size: 0.9rem;
}

.hot-cities {
  margin-bottom: 30px;
}

.hot-cities h2 {
  color: white;
  text-align: center;
  margin-bottom: 20px;
}

.cities-loading {
  background: rgba(255, 255, 255, 0.95);
  border-radius: 15px;
  padding: 20px;
}

.cities-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 15px;
}

.city-item {
  background: rgba(255, 255, 255, 0.95);
  padding: 20px;
  border-radius: 12px;
  text-align: center;
  cursor: pointer;
  transition: transform 0.3s ease;
  border-top: 4px solid transparent;
}

.city-item:hover {
  transform: scale(1.05);
}

.city-item.excellent { border-top-color: #00c853; }
.city-item.good { border-top-color: #ffd600; }
.city-item.light { border-top-color: #ff9100; }
.city-item.moderate { border-top-color: #ff1744; }
.city-item.heavy { border-top-color: #7c4dff; }
.city-item.severe { border-top-color: #3e2723; }

.city-name {
  font-size: 1.1rem;
  color: #333;
  margin-bottom: 8px;
}

.city-aqi {
  font-size: 2rem;
  font-weight: bold;
  color: #1a2980;
}

.city-level {
  font-size: 0.85rem;
  color: #666;
  margin-top: 5px;
}

@media (max-width: 768px) {
  .features-grid {
    grid-template-columns: 1fr;
  }
  .cities-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .pollutant-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
