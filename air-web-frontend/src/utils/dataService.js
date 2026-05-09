/**
 * 历史数据服务
 * 加载和管理郑州市五区2020-2024年历史空气质量数据
 */

import Papa from 'papaparse'

// 数据缓存
let cachedData = null

/**
 * 生成模拟历史数据（当CSV加载失败时使用）
 */
export const generateMockHistoryData = () => {
  const data = []
  const districts = ['中原区', '金水区', '二七区', '惠济区', '郑东新区']
  const startDate = new Date('2020-01-01')
  const endDate = new Date('2024-12-31')
  
  for (let d = new Date(startDate); d <= endDate; d.setDate(d.getDate() + 1)) {
    districts.forEach(district => {
      const dateStr = d.toISOString().split('T')[0]
      const month = d.getMonth() + 1
      
      // 根据季节调整基础AQI
      let seasonalFactor = 1
      if (month >= 12 || month <= 2) seasonalFactor = 1.4 // 冬季污染重
      else if (month >= 6 && month <= 8) seasonalFactor = 0.7 // 夏季较好
      
      // 沙尘天判断（春季3-5月概率较高）
      const isSandDust = (month >= 3 && month <= 5) && Math.random() < 0.08
      
      const baseAqi = Math.floor((Math.random() * 80 + 40) * seasonalFactor)
      const aqi = isSandDust ? baseAqi + Math.floor(Math.random() * 100 + 50) : baseAqi
      
      // 根据AQI确定等级
      let qualityLevel = '优'
      if (aqi > 50) qualityLevel = '良'
      if (aqi > 100) qualityLevel = '轻度污染'
      if (aqi > 150) qualityLevel = '中度污染'
      if (aqi > 200) qualityLevel = '重度污染'
      if (aqi > 300) qualityLevel = '严重污染'
      
      // 确定首要污染物
      let primaryPollutant = '无'
      if (aqi > 50) {
        const pollutants = ['PM2.5', 'PM10', 'O3']
        if (isSandDust) primaryPollutant = 'PM10'
        else primaryPollutant = pollutants[Math.floor(Math.random() * pollutants.length)]
      }
      
      data.push({
        station_code: district,
        station_name: district,
        date: dateStr,
        aqi: aqi,
        quality_level: qualityLevel,
        pm25: Math.floor((Math.random() * 60 + 20) * seasonalFactor),
        pm10: isSandDust 
          ? Math.floor(Math.random() * 200) + 250 
          : Math.floor((Math.random() * 80 + 40) * seasonalFactor),
        o3: Math.floor(Math.random() * 80 + 30),
        so2: Math.floor(Math.random() * 25 + 5),
        co: parseFloat((Math.random() * 1.5 + 0.4).toFixed(2)),
        no2: Math.floor((Math.random() * 40 + 10) * seasonalFactor),
        primary_pollutant: primaryPollutant,
        is_sand_dust_day: isSandDust
      })
    })
  }
  
  console.log(`生成了 ${data.length} 条模拟数据`)
  return data
}

/**
 * 加载历史数据
 */
export const loadHistoryData = async () => {
  if (cachedData) {
    return cachedData
  }
  
  try {
    // 尝试从多个可能的路径加载
    const possiblePaths = [
      '/zhengzhou_districts_5years_daily.csv',
      '/data/zhengzhou_districts_5years_daily.csv',
      './zhengzhou_districts_5years_daily.csv'
    ]
    
    for (const path of possiblePaths) {
      try {
        const response = await fetch(path)
        if (response.ok) {
          const csvText = await response.text()
          const result = Papa.parse(csvText, {
            header: true,
            dynamicTyping: true,
            skipEmptyLines: true
          })
          if (result.data && result.data.length > 0) {
            cachedData = result.data
            return cachedData
          }
        }
      } catch (e) {
        // 继续尝试下一个路径
      }
    }
    
    // 如果都失败，使用模拟数据
    console.warn('无法加载CSV文件，使用模拟数据')
    cachedData = generateMockHistoryData()
    return cachedData
  } catch (error) {
    console.error('加载历史数据失败:', error)
    cachedData = generateMockHistoryData()
    return cachedData
  }
}

/**
 * 根据条件筛选数据
 */
export const filterData = (data, { station, startDate, endDate }) => {
  return data.filter(item => {
    const matchStation = !station || item.station_name === station
    const matchStart = !startDate || item.date >= startDate
    const matchEnd = !endDate || item.date <= endDate
    return matchStation && matchStart && matchEnd
  }).sort((a, b) => new Date(a.date) - new Date(b.date))
}

/**
 * 计算统计数据
 */
export const calculateStats = (data) => {
  if (!data || data.length === 0) return null
  
  const aqis = data.map(item => item.aqi)
  const avgAqi = Math.round(aqis.reduce((a, b) => a + b, 0) / aqis.length)
  const maxAqi = Math.max(...aqis)
  const minAqi = Math.min(...aqis)
  const sandDustDays = data.filter(item => item.is_sand_dust_day).length
  
  // 计算AQI等级分布
  const levelCount = { '优': 0, '良': 0, '轻度污染': 0, '中度污染': 0, '重度污染': 0, '严重污染': 0 }
  data.forEach(item => {
    const level = item.quality_level
    if (levelCount[level] !== undefined) {
      levelCount[level]++
    }
  })
  
  // 计算首要污染物分布
  const primaryCount = { 'PM2.5': 0, 'PM10': 0, 'O3': 0, 'NO2': 0, 'SO2': 0, 'CO': 0, '无': 0 }
  data.forEach(item => {
    const primary = item.primary_pollutant
    if (primary && primaryCount[primary] !== undefined) {
      primaryCount[primary]++
    } else {
      primaryCount['无']++
    }
  })
  
  // 计算污染物均值
  const avgValues = {
    pm25: data.reduce((sum, item) => sum + item.pm25, 0) / data.length,
    pm10: data.reduce((sum, item) => sum + item.pm10, 0) / data.length,
    so2: data.reduce((sum, item) => sum + item.so2, 0) / data.length,
    no2: data.reduce((sum, item) => sum + item.no2, 0) / data.length,
    co: data.reduce((sum, item) => parseFloat(item.co) || 0, 0) / data.length,
    o3: data.reduce((sum, item) => sum + item.o3, 0) / data.length
  }
  
  return {
    avgAqi,
    maxAqi,
    minAqi,
    sandDustDays,
    totalDays: data.length,
    levelDistribution: levelCount,
    primaryDistribution: primaryCount,
    pollutantAverages: avgValues
  }
}

/**
 * 获取AQI等级样式类
 */
export const getAqiClass = (aqi) => {
  if (aqi <= 50) return 'aqi-excellent'
  if (aqi <= 100) return 'aqi-good'
  if (aqi <= 150) return 'aqi-moderate'
  if (aqi <= 200) return 'aqi-unhealthy'
  if (aqi <= 300) return 'aqi-very-unhealthy'
  return 'aqi-hazardous'
}

/**
 * 导出数据为CSV
 */
export const exportToCSV = (data, filename = 'export.csv') => {
  const csv = Papa.unparse(data)
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = filename
  link.click()
}

export default {
  loadHistoryData,
  filterData,
  calculateStats,
  getAqiClass,
  exportToCSV,
  generateMockHistoryData
}
