import axios from 'axios'
import { 
  getCustomData, 
  queryCustomData, 
  queryCustomDataByRange,
  mergeWithCustomData,
  mergeWithCustomDataRange
} from './dataManage'

// 后端API地址
const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  timeout: 10000
})

// CSV数据缓存
let csvDataCache = null
let csvLoadPromise = null

// 站点名称映射
const STATION_CODE_MAP = {
  '中原区': '410101',
  '金水区': '410102',
  '二七区': '410103',
  '惠济区': '410108',
  '郑东新区': '410104'
}

const STATION_NAME_MAP = {
  '410101': '中原区',
  '410102': '金水区',
  '410103': '二七区',
  '410108': '惠济区',
  '410104': '郑东新区'
}

const STATION_MAPPING = {
  '中原区': '北区建设指挥部',
  '金水区': '北区建设指挥部',
  '二七区': '河医大',
  '惠济区': '惠济区政府',
  '郑东新区': '经开区管委'
}

/**
 * 加载并解析CSV文件
 */
async function loadCSVData() {
  if (csvDataCache) return csvDataCache
  if (csvLoadPromise) return csvLoadPromise

  csvLoadPromise = new Promise(async (resolve, reject) => {
    try {
      const response = await fetch('/zhengzhou_districts_5years_daily.csv')
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      const csvText = await response.text()
      const data = parseCSV(csvText)
      csvDataCache = data
      console.log(`[CSV] 成功加载 ${data.length} 条数据`)
      resolve(data)
    } catch (error) {
      console.error('[CSV] 加载失败:', error)
      reject(error)
    }
  })

  return csvLoadPromise
}

/**
 * 解析CSV文本
 */
function parseCSV(csvText) {
  const lines = csvText.trim().split('\n')
  const headers = lines[0].split(',').map(h => h.trim())
  const data = []

  for (let i = 1; i < lines.length; i++) {
    const line = lines[i].trim()
    if (!line) continue

    const values = parseCSVLine(line)
    if (values.length < headers.length) continue

    const record = {
      stationCode: values[0],
      stationName: values[1],
      date: values[2],
      aqi: parseInt(values[3]) || 0,
      qualityLevel: values[4],
      pm25: parseFloat(values[5]) || 0,
      pm10: parseFloat(values[6]) || 0,
      o3: parseFloat(values[7]) || 0,
      so2: parseFloat(values[8]) || 0,
      co: parseFloat(values[9]) || 0,
      no2: parseFloat(values[10]) || 0,
      primaryPollutant: values[11] || '无',
      isSandDustDay: values[12] === 'True'
    }

    data.push(record)
  }

  return data
}

/**
 * 解析CSV行（处理引号）
 */
function parseCSVLine(line) {
  const values = []
  let current = ''
  let inQuotes = false

  for (let i = 0; i < line.length; i++) {
    const char = line[i]

    if (char === '"') {
      inQuotes = !inQuotes
    } else if (char === ',' && !inQuotes) {
      values.push(current.trim())
      current = ''
    } else {
      current += char
    }
  }
  values.push(current.trim())

  return values
}

/**
 * 从CSV数据中获取指定站点和日期的记录
 */
function getRecordFromCSV(data, stationName, date) {
  const record = data.find(d => d.stationName === stationName && d.date === date)
  return record || null
}

/**
 * 从CSV数据中获取指定站点和日期范围的记录
 */
function getRecordsFromCSVByRange(data, stationName, startDate, endDate) {
  const start = new Date(startDate)
  const end = new Date(endDate)

  return data.filter(d => {
    if (d.stationName !== stationName) return false
    const recordDate = new Date(d.date)
    return recordDate >= start && recordDate <= end
  }).sort((a, b) => new Date(a.date) - new Date(b.date))
}

/**
 * 计算AQI等级
 */
function getAQILevel(aqi) {
  if (aqi <= 50) return '优'
  if (aqi <= 100) return '良'
  if (aqi <= 150) return '轻度污染'
  if (aqi <= 200) return '中度污染'
  if (aqi <= 300) return '重度污染'
  return '严重污染'
}

/**
 * 获取郑州市各区实时AQI数据
 * 优先从后端API获取实时数据，失败时回退到CSV
 */
export async function getZhengzhouRealtimeData() {
  try {
    // 优先从后端API获取实时数据
    const response = await apiClient.get('/zhengzhou/realtime')
    if (response.data.code === 200 && response.data.data?.length > 0) {
      // 添加数据来源标记
      return response.data.data.map(item => ({
        ...item,
        dataSource: 'realtime'
      }))
    }
    throw new Error(response.data.message || '后端无实时数据')
  } catch (apiError) {
    console.warn('后端API获取实时数据失败，尝试CSV:', apiError.message)
    
    // 后端API失败时，尝试从CSV加载数据作为后备
    try {
      const csvData = await loadCSVData()
      const stations = ['中原区', '金水区', '二七区', '惠济区', '郑东新区']
      const result = []

      for (const stationName of stations) {
        const stationRecords = csvData
          .filter(d => d.stationName === stationName)
          .sort((a, b) => new Date(b.date) - new Date(a.date))

        if (stationRecords.length > 0) {
          const latest = stationRecords[0]
          result.push({
            stationName: stationName,
            name: stationName,
            actualStation: STATION_MAPPING[stationName] || stationName,
            aqi: latest.aqi,
            pm25: latest.pm25,
            pm10: latest.pm10,
            so2: latest.so2,
            no2: latest.no2,
            co: latest.co,
            o3: latest.o3,
            primaryPollutant: latest.primaryPollutant,
            level: latest.qualityLevel,
            updateTime: latest.date,
            dataSource: 'csv'
          })
        }
      }

      if (result.length > 0) {
        console.log('已从CSV加载历史数据作为后备')
        return result
      }
      throw new Error('CSV中无数据')
    } catch (csvError) {
      console.error('CSV加载也失败:', csvError)
    }
    
    // 最后回退到模拟数据
    console.log('使用模拟数据')
    return getMockData().map(item => ({ ...item, dataSource: 'mock' }))
  }
}

/**
 * 获取郑州市整体AQI
 */
export async function getZhengzhouCityAQI() {
  try {
    const response = await apiClient.get('/zhengzhou/city-aqi')
    if (response.data.code === 200) {
      return response.data.data
    }
    throw new Error(response.data.message || '获取数据失败')
  } catch (error) {
    console.error('获取郑州城市AQI失败:', error)
    return getMockCityAQI()
  }
}

/**
 * 获取站点24小时历史数据
 */
export async function getZhengzhouHourlyData(stationName) {
  try {
    const response = await apiClient.get(`/zhengzhou/hourly/${stationName}`)
    if (response.data.code === 200) {
      return response.data.data
    }
    throw new Error(response.data.message || '获取数据失败')
  } catch (error) {
    console.error('获取小时数据失败:', error)
    return getMockHourlyData()
  }
}

// 模拟数据（后端不可用时使用）
function getMockData() {
  return [
    {
      stationName: '中原区',
      name: '中原区',
      actualStation: '北区建设指挥部',
      aqi: 125,
      pm25: 85,
      pm10: 120,
      so2: 15,
      no2: 45,
      co: 0.8,
      o3: 95,
      primaryPollutant: 'PM2.5',
      level: '轻度污染',
      updateTime: new Date().toISOString()
    },
    {
      stationName: '金水区',
      name: '金水区',
      actualStation: '北区建设指挥部',
      aqi: 125,
      pm25: 85,
      pm10: 120,
      so2: 15,
      no2: 45,
      co: 0.8,
      o3: 95,
      primaryPollutant: 'PM2.5',
      level: '轻度污染',
      updateTime: new Date().toISOString()
    },
    {
      stationName: '二七区',
      name: '二七区',
      actualStation: '河医大',
      aqi: 158,
      pm25: 110,
      pm10: 145,
      so2: 18,
      no2: 52,
      co: 1.0,
      o3: 88,
      primaryPollutant: 'PM2.5',
      level: '中度污染',
      updateTime: new Date().toISOString()
    },
    {
      stationName: '惠济区',
      name: '惠济区',
      actualStation: '惠济区政府',
      aqi: 85,
      pm25: 55,
      pm10: 85,
      so2: 12,
      no2: 38,
      co: 0.6,
      o3: 102,
      primaryPollutant: 'PM10',
      level: '良',
      updateTime: new Date().toISOString()
    },
    {
      stationName: '郑东新区',
      name: '郑东新区',
      actualStation: '经开区管委',
      aqi: 132,
      pm25: 92,
      pm10: 128,
      so2: 16,
      no2: 48,
      co: 0.9,
      o3: 90,
      primaryPollutant: 'PM2.5',
      level: '轻度污染',
      updateTime: new Date().toISOString()
    }
  ]
}

function getMockCityAQI() {
  return {
    aqi: 125,
    level: '轻度污染',
    primaryPollutant: 'PM2.5',
    stationCount: 5,
    updateTime: new Date().toISOString()
  }
}

function getMockHourlyData() {
  const data = []
  for (let i = 23; i >= 0; i--) {
    const hour = new Date()
    hour.setHours(hour.getHours() - i)
    data.push({
      time: `${hour.getHours().toString().padStart(2, '0')}:00`,
      aqi: 80 + Math.floor(Math.random() * 60),
      pm25: 50 + Math.floor(Math.random() * 50),
      pm10: 70 + Math.floor(Math.random() * 60)
    })
  }
  return data
}

/**
 * 基于历史数据进行AQI预测
 * @param {string} stationName - 站点名称
 * @param {number} hours - 预测小时数（默认24）
 */
export async function getPrediction(stationName, hours = 24) {
  try {
    const response = await apiClient.get(`/zhengzhou/prediction/${stationName}`, {
      params: { hours }
    })
    if (response.data.code === 200) {
      return response.data.data
    }
    throw new Error(response.data.message || '获取预测数据失败')
  } catch (error) {
    console.error('获取预测数据失败:', error)
    return getMockPrediction(stationName, hours)
  }
}

/**
 * 获取多站点预测对比
 * @param {number} hours - 预测小时数
 */
export async function getMultiStationPrediction(hours = 24) {
  try {
    const response = await apiClient.get('/zhengzhou/prediction/multi', {
      params: { hours }
    })
    if (response.data.code === 200) {
      return response.data.data
    }
    throw new Error(response.data.message || '获取预测对比失败')
  } catch (error) {
    console.error('获取多站点预测失败:', error)
    return getMockMultiPrediction(hours)
  }
}

// 模拟预测数据
function getMockPrediction(stationName, hours) {
  const predictions = []
  const baseAQI = stationName === '惠济区' ? 85 : stationName === '二七区' ? 158 : 125
  
  for (let i = 1; i <= hours; i++) {
    const hour = (new Date().getHours() + i) % 24
    const variation = Math.sin(hour / 24 * Math.PI * 2) * 20
    const predictedAqi = Math.max(30, Math.round(baseAQI + variation + (Math.random() * 10 - 5)))
    
    predictions.push({
      hour: `${hour.toString().padStart(2, '0')}:00`,
      predictedAqi,
      confidenceLower: Math.round(predictedAqi * 0.9),
      confidenceUpper: Math.round(predictedAqi * 1.1),
      level: predictedAqi <= 100 ? '良' : predictedAqi <= 150 ? '轻度污染' : '中度污染',
      primaryPollutant: predictedAqi > 100 ? 'PM2.5' : 'PM10'
    })
  }
  
  return {
    stationName,
    currentAqi: baseAQI,
    predictions,
    confidence: 85.5,
    trend: '平稳',
    predictedAvg: Math.round(predictions.reduce((a, b) => a + b.predictedAqi, 0) / predictions.length),
    updateTime: new Date().toISOString()
  }
}

function getMockMultiPrediction(hours) {
  return [
    { stationName: '中原区', currentAqi: 125, confidence: 87.2, trend: '平稳', predEnd: 128, predMax: 145 },
    { stationName: '金水区', currentAqi: 125, confidence: 87.2, trend: '平稳', predEnd: 128, predMax: 145 },
    { stationName: '二七区', currentAqi: 158, confidence: 82.5, trend: '上升', predEnd: 165, predMax: 175 },
    { stationName: '惠济区', currentAqi: 85, confidence: 90.1, trend: '下降', predEnd: 82, predMax: 95 },
    { stationName: '郑东新区', currentAqi: 132, confidence: 85.8, trend: '平稳', predEnd: 135, predMax: 148 }
  ]
}

/**
 * 基于历史同期数据预测指定日期
 * @param {string} stationName - 站点名称
 * @param {string} date - 日期字符串 (YYYY-MM-DD)
 * @param {number} hours - 预测小时数
 */
export async function getHistoricalPrediction(stationName, date, hours = 24) {
  try {
    const [year, month, day] = date.split('-').map(Number)
    const response = await apiClient.get(`/zhengzhou/prediction/historical/${stationName}`, {
      params: { year, month, day, hours }
    })
    if (response.data.code === 200) {
      return response.data.data
    }
    throw new Error(response.data.message || '获取历史预测失败')
  } catch (error) {
    console.error('获取历史同期预测失败:', error)
    return getMockHistoricalPrediction(stationName, date, hours)
  }
}

/**
 * 基于历史同期数据预测日期区间
 * @param {string} stationName - 站点名称
 * @param {string} startDate - 开始日期 (YYYY-MM-DD)
 * @param {string} endDate - 结束日期 (YYYY-MM-DD)
 */
export async function getHistoricalPredictionRange(stationName, startDate, endDate) {
  try {
    const response = await apiClient.get(`/zhengzhou/prediction/historical/range/${stationName}`, {
      params: { startDate, endDate }
    })
    if (response.data.code === 200) {
      return response.data.data
    }
    throw new Error(response.data.message || '获取历史区间预测失败')
  } catch (error) {
    console.error('获取历史区间预测失败:', error)
    return getMockHistoricalRangePrediction(stationName, startDate, endDate)
  }
}

/**
 * 获取指定日期的历史同期数据
 * @param {string} stationName - 站点名称
 * @param {string} date - 日期字符串 (YYYY-MM-DD)
 */
export async function getHistoricalData(stationName, date) {
  try {
    const [year, month, day] = date.split('-').map(Number)
    const response = await apiClient.get(`/zhengzhou/historical/${stationName}`, {
      params: { year, month, day }
    })
    if (response.data.code === 200) {
      return response.data.data
    }
    throw new Error(response.data.message || '获取历史数据失败')
  } catch (error) {
    console.error('获取历史数据失败:', error)
    return getMockHistoricalData(stationName, date)
  }
}

// 模拟历史预测数据
function getMockHistoricalPrediction(stationName, date, hours) {
  const baseAQI = stationName === '惠济区' ? 85 : stationName === '二七区' ? 158 : 125
  const seasonalFactor = date.includes('-12-') || date.includes('-01-') || date.includes('-02-') ? 1.3 :
                         date.includes('-06-') || date.includes('-07-') || date.includes('-08-') ? 0.7 : 1.0
  const adjustedBase = Math.round(baseAQI * seasonalFactor)
  
  const predictions = []
  for (let i = 0; i < hours && i < 24; i++) {
    const hourFactor = 0.8 + Math.sin(i / 24 * Math.PI) * 0.4
    const predictedAqi = Math.max(30, Math.round(adjustedBase * hourFactor + (Math.random() * 20 - 10)))
    
    predictions.push({
      hour: `${i.toString().padStart(2, '0')}:00`,
      predictedAqi,
      confidenceLower: Math.round(predictedAqi * 0.85),
      confidenceUpper: Math.round(predictedAqi * 1.15),
      level: predictedAqi <= 100 ? '良' : predictedAqi <= 150 ? '轻度污染' : '中度污染',
      primaryPollutant: predictedAqi > 100 ? 'PM2.5' : 'PM10',
      historicalAvg: Math.round(predictedAqi * 1.05)
    })
  }
  
  return {
    stationName,
    targetDate: date,
    predictionType: '基于过去5年历史同期数据（模拟）',
    predictions,
    predictedAvg: Math.round(predictions.reduce((a, b) => a + b.predictedAqi, 0) / predictions.length),
    predictedMax: Math.max(...predictions.map(p => p.predictedAqi)),
    predictedMin: Math.min(...predictions.map(p => p.predictedAqi)),
    confidence: 78.5,
    historicalYears: [2019, 2020, 2021, 2022, 2023],
    fiveYearAvg: adjustedBase,
    fiveYearMax: Math.round(adjustedBase * 1.4),
    fiveYearMin: Math.round(adjustedBase * 0.6),
    updateTime: new Date().toISOString()
  }
}

function getMockHistoricalRangePrediction(stationName, startDate, endDate) {
  const dailyPredictions = []
  const start = new Date(startDate)
  const end = new Date(endDate)
  
  for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
    const dateStr = d.toISOString().split('T')[0]
    const mockDay = getMockHistoricalPrediction(stationName, dateStr, 24)
    
    dailyPredictions.push({
      date: dateStr,
      predictedAvg: mockDay.predictedAvg,
      predictedMax: mockDay.predictedMax,
      predictedMin: mockDay.predictedMin,
      confidence: mockDay.confidence,
      level: mockDay.predictions[0].level
    })
  }
  
  return {
    stationName,
    startDate,
    endDate,
    predictionType: '基于过去5年历史同期数据（模拟）',
    dailyPredictions,
    totalDays: dailyPredictions.length,
    periodAvg: Math.round(dailyPredictions.reduce((a, b) => a + b.predictedAvg, 0) / dailyPredictions.length),
    periodMax: Math.max(...dailyPredictions.map(d => d.predictedMax)),
    periodMin: Math.min(...dailyPredictions.map(d => d.predictedMin))
  }
}

function getMockHistoricalData(stationName, date) {
  return {
    stationName,
    targetDate: date,
    fiveYearAvg: 120,
    fiveYearMax: 180,
    fiveYearMin: 60,
    historicalData: [2019, 2020, 2021, 2022, 2023].map(year => ({
      year,
      dailyAvg: 100 + Math.floor(Math.random() * 50),
      maxAqi: 150 + Math.floor(Math.random() * 80),
      minAqi: 50 + Math.floor(Math.random() * 40)
    }))
  }
}

/**
 * 查询指定日期的空气质量数据
 * 查询优先级：1.自定义数据 2.CSV文件 3.后端API 4.模拟数据
 * @param {string} stationName - 站点名称
 * @param {string} date - 日期字符串 (YYYY-MM-DD)
 */
export async function queryAirQualityByDate(stationName, date) {
  try {
    // 1. 首先尝试从自定义数据加载
    const customRecord = await queryCustomData(stationName, date)
    if (customRecord) {
      console.log('[Query] 从自定义数据中找到:', stationName, date)
      const csvData = await loadCSVData()
      const nearbyData = getNearbyDataFromCSV(csvData, stationName, date)
      const historicalSameDay = getHistoricalSameDayFromCSV(csvData, stationName, date)
      const monthStats = getMonthStatsFromCSV(csvData, stationName, date)
      const yearRank = getYearRankFromCSV(csvData, stationName, date, customRecord.aqi)

      return {
        success: true,
        isMock: false,
        isCustom: true,
        stationName,
        actualStation: STATION_MAPPING[stationName] || stationName,
        date,
        data: {
          date: customRecord.date,
          aqi: customRecord.aqi,
          qualityLevel: customRecord.qualityLevel,
          pm25: customRecord.pm25,
          pm10: customRecord.pm10,
          o3: customRecord.o3,
          so2: customRecord.so2,
          co: customRecord.co,
          no2: customRecord.no2,
          primaryPollutant: customRecord.primaryPollutant
        },
        nearbyData,
        monthStats,
        historicalSameDay,
        yearRank
      }
    }

    // 2. 然后尝试从CSV加载数据
    const csvData = await loadCSVData()
    const record = getRecordFromCSV(csvData, stationName, date)

    if (record) {
      // 获取前后7天的数据
      const nearbyData = getNearbyDataFromCSV(csvData, stationName, date)
      // 获取历史同期数据
      const historicalSameDay = getHistoricalSameDayFromCSV(csvData, stationName, date)
      // 获取月度统计
      const monthStats = getMonthStatsFromCSV(csvData, stationName, date)
      // 获取年度排名
      const yearRank = getYearRankFromCSV(csvData, stationName, date, record.aqi)

      return {
        success: true,
        isMock: false,
        isCustom: false,
        stationName,
        actualStation: STATION_MAPPING[stationName] || stationName,
        date,
        data: {
          date: record.date,
          aqi: record.aqi,
          qualityLevel: record.qualityLevel,
          pm25: record.pm25,
          pm10: record.pm10,
          o3: record.o3,
          so2: record.so2,
          co: record.co,
          no2: record.no2,
          primaryPollutant: record.primaryPollutant
        },
        nearbyData,
        monthStats,
        historicalSameDay,
        yearRank
      }
    }

    // 3. 如果CSV中没有找到数据，尝试后端API
    const response = await apiClient.get(`/zhengzhou/query/${stationName}`, {
      params: { date }
    })
    if (response.data.code === 200) {
      return response.data.data
    }
    throw new Error(response.data.message || '查询数据失败')
  } catch (error) {
    console.error('查询数据失败:', error)
    return getMockQueryResult(stationName, date)
  }
}

/**
 * 获取前后7天的数据
 */
function getNearbyDataFromCSV(data, stationName, targetDate) {
  const target = new Date(targetDate)
  const nearbyData = []

  for (let i = -3; i <= 3; i++) {
    const d = new Date(target)
    d.setDate(d.getDate() + i)
    const dateStr = d.toISOString().split('T')[0]
    const record = data.find(item => item.stationName === stationName && item.date === dateStr)

    if (record) {
      nearbyData.push({
        date: dateStr,
        aqi: record.aqi,
        qualityLevel: record.qualityLevel,
        pm25: record.pm25,
        pm10: record.pm10,
        primaryPollutant: record.primaryPollutant,
        isTarget: i === 0
      })
    } else {
      // 如果没有数据，插入一个空记录
      nearbyData.push({
        date: dateStr,
        aqi: null,
        qualityLevel: '无数据',
        pm25: null,
        pm10: null,
        primaryPollutant: '-',
        isTarget: i === 0
      })
    }
  }

  return nearbyData
}

/**
 * 获取历史同期数据（过去5年的同一天）
 */
function getHistoricalSameDayFromCSV(data, stationName, targetDate) {
  const target = new Date(targetDate)
  const month = target.getMonth() + 1
  const day = target.getDate()
  const currentYear = target.getFullYear()

  const historicalData = []
  const years = [2020, 2021, 2022, 2023].filter(y => y < currentYear)

  for (const year of years) {
    const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    const record = data.find(item => item.stationName === stationName && item.date === dateStr)

    if (record) {
      historicalData.push({
        year,
        date: dateStr,
        aqi: record.aqi,
        qualityLevel: record.qualityLevel,
        pm25: record.pm25,
        pm10: record.pm10,
        primaryPollutant: record.primaryPollutant
      })
    }
  }

  return historicalData
}

/**
 * 获取月度统计
 */
function getMonthStatsFromCSV(data, stationName, targetDate) {
  const target = new Date(targetDate)
  const year = target.getFullYear()
  const month = target.getMonth() + 1

  const monthRecords = data.filter(item => {
    const d = new Date(item.date)
    return item.stationName === stationName &&
           d.getFullYear() === year &&
           d.getMonth() + 1 === month
  })

  if (monthRecords.length === 0) {
    return { avg: 0, max: 0, min: 0, totalDays: 0 }
  }

  const aqis = monthRecords.map(r => r.aqi)
  return {
    avg: Math.round(aqis.reduce((a, b) => a + b, 0) / aqis.length),
    max: Math.max(...aqis),
    min: Math.min(...aqis),
    totalDays: monthRecords.length
  }
}

/**
 * 获取年度排名
 */
function getYearRankFromCSV(data, stationName, targetDate, currentAqi) {
  const target = new Date(targetDate)
  const year = target.getFullYear()

  const yearRecords = data.filter(item => {
    const d = new Date(item.date)
    return item.stationName === stationName && d.getFullYear() === year
  })

  if (yearRecords.length === 0) {
    return { rank: 0, total: 0, betterThan: 0 }
  }

  const sorted = yearRecords.map(r => r.aqi).sort((a, b) => a - b)
  const rank = sorted.findIndex(aqi => aqi >= currentAqi) + 1 || sorted.length
  const betterThan = Math.round((sorted.filter(aqi => aqi < currentAqi).length / sorted.length) * 100)

  return {
    rank,
    total: sorted.length,
    betterThan
  }
}

/**
 * 查询日期区间的空气质量数据
 * 查询优先级：1.自定义数据+CSV合并 2.后端API 3.模拟数据
 * @param {string} stationName - 站点名称
 * @param {string} startDate - 开始日期 (YYYY-MM-DD)
 * @param {string} endDate - 结束日期 (YYYY-MM-DD)
 */
export async function queryAirQualityByDateRange(stationName, startDate, endDate) {
  try {
    // 1. 获取CSV数据
    const csvData = await loadCSVData()
    
    // 2. 合并自定义数据和CSV数据（自定义数据优先级更高）
    const mergedRecords = await mergeWithCustomDataRange(csvData, stationName, startDate, endDate)

    if (mergedRecords && mergedRecords.length > 0) {
      return mergedRecords.map(record => ({
        date: record.date,
        aqi: record.aqi,
        qualityLevel: record.qualityLevel,
        pm25: record.pm25,
        pm10: record.pm10,
        o3: record.o3,
        so2: record.so2,
        co: record.co,
        no2: record.no2,
        primaryPollutant: record.primaryPollutant,
        isMock: false,
        isCustom: record.isCustom || false
      }))
    }

    // 3. 如果CSV和自定义数据中都没有找到数据，尝试后端API
    const response = await apiClient.get(`/zhengzhou/query/range/${stationName}`, {
      params: { startDate, endDate }
    })
    if (response.data.code === 200) {
      return response.data.data
    }
    throw new Error(response.data.message || '查询数据失败')
  } catch (error) {
    console.error('查询区间数据失败:', error)
    return getMockQueryRangeResult(stationName, startDate, endDate)
  }
}

// 模拟查询结果 - 使用固定值，确保结果一致
function getMockQueryResult(stationName, date) {
  const baseAqi = stationName === '惠济区' ? 85 : stationName === '二七区' ? 158 : 125
  
  // 使用固定值代替随机数
  return {
    success: true,
    isMock: true,  // 标记为模拟数据
    mockWarning: '当前显示的是模拟数据，后端可能未正确加载CSV文件或API调用失败',
    stationName,
    actualStation: STATION_MAPPING[stationName] || stationName,
    date,
    data: {
      date,
      aqi: baseAqi,
      qualityLevel: baseAqi <= 100 ? '良' : baseAqi <= 150 ? '轻度污染' : '中度污染',
      pm25: Math.round(baseAqi * 0.65),
      pm10: Math.round(baseAqi * 0.95),
      o3: 105,  // 固定值
      so2: 25,  // 固定值
      co: 0.9,  // 固定值
      no2: 50,  // 固定值
      primaryPollutant: baseAqi > 100 ? 'PM2.5' : 'PM10'
    },
    nearbyData: generateNearbyData(date, baseAqi),
    monthStats: {
      avg: Math.round(baseAqi * 0.95),
      max: Math.round(baseAqi * 1.3),
      min: Math.round(baseAqi * 0.6),
      totalDays: 30
    },
    historicalSameDay: [2020, 2021, 2022, 2023].map((year, index) => ({
      year,
      date: `${year}${date.substring(4)}`,
      aqi: Math.round(baseAqi * (0.92 + index * 0.02)),  // 固定变化
      qualityLevel: '良',
      pm25: Math.round(baseAqi * 0.65),
      pm10: Math.round(baseAqi * 0.95),
      primaryPollutant: 'PM2.5'
    })),
    yearRank: {
      rank: 50,  // 固定值
      total: 365,
      betterThan: 65  // 固定值
    }
  }
}

function generateNearbyData(date, baseAqi) {
  const data = []
  const baseDate = new Date(date)
  
  for (let i = -3; i <= 3; i++) {
    const d = new Date(baseDate)
    d.setDate(d.getDate() + i)
    const dateStr = d.toISOString().split('T')[0]
    const variation = (i % 2 === 0 ? 10 : -10)  // 固定变化
    const aqi = Math.max(30, Math.round(baseAqi + variation))
    
    data.push({
      date: dateStr,
      aqi,
      qualityLevel: aqi <= 50 ? '优' : aqi <= 100 ? '良' : aqi <= 150 ? '轻度污染' : '中度污染',
      pm25: Math.round(aqi * 0.65),
      pm10: Math.round(aqi * 0.95),
      primaryPollutant: aqi > 100 ? 'PM2.5' : 'PM10',
      isTarget: i === 0
    })
  }
  
  return data
}

function getMockQueryRangeResult(stationName, startDate, endDate) {
  const data = []
  const start = new Date(startDate)
  const end = new Date(endDate)
  const baseAqi = stationName === '惠济区' ? 85 : stationName === '二七区' ? 158 : 125
  let dayIndex = 0
  
  for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
    const dateStr = d.toISOString().split('T')[0]
    const variation = (dayIndex % 3 - 1) * 15  // 固定变化模式
    const aqi = Math.max(30, Math.round(baseAqi + variation))
    dayIndex++
    
    data.push({
      date: dateStr,
      aqi,
      isMock: true,  // 标记为模拟数据
      qualityLevel: aqi <= 50 ? '优' : aqi <= 100 ? '良' : aqi <= 150 ? '轻度污染' : '中度污染',
      pm25: Math.round(aqi * 0.65),
      pm10: Math.round(aqi * 0.95),
      o3: 100,  // 固定值
      so2: 22,  // 固定值
      co: 0.85, // 固定值
      no2: 48,  // 固定值
      primaryPollutant: aqi > 100 ? 'PM2.5' : 'PM10'
    })
  }
  
  return data
}
