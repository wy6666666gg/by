import axios from 'axios'

// Open-Meteo API 免费开源，无需API Key
const weatherClient = axios.create({
  baseURL: 'https://api.open-meteo.com/v1',
  timeout: 10000
})

// 地理编码API - 用于将城市名转换为经纬度
const geoClient = axios.create({
  baseURL: 'https://geocoding-api.open-meteo.com/v1',
  timeout: 10000
})

// 空气质量API
const airQualityClient = axios.create({
  baseURL: 'https://air-quality-api.open-meteo.com/v1',
  timeout: 10000
})

/**
 * 根据城市名称获取经纬度
 * @param {string} cityName - 城市名称
 * @returns {Promise<{latitude: number, longitude: number, name: string}>}
 */
export async function getCityCoordinates(cityName) {
  try {
    const response = await geoClient.get('/search', {
      params: {
        name: cityName,
        count: 1,
        language: 'zh',
        format: 'json'
      }
    })

    if (response.data.results && response.data.results.length > 0) {
      const result = response.data.results[0]
      return {
        latitude: result.latitude,
        longitude: result.longitude,
        name: result.name,
        country: result.country,
        admin1: result.admin1
      }
    }
    throw new Error('未找到该城市')
  } catch (error) {
    console.error('获取城市坐标失败:', error)
    throw error
  }
}

/**
 * 获取城市空气质量数据
 * @param {number} latitude - 纬度
 * @param {number} longitude - 经度
 * @returns {Promise<Object>}
 */
export async function getAirQuality(latitude, longitude) {
  try {
    const response = await airQualityClient.get('/air-quality', {
      params: {
        latitude,
        longitude,
        current: ['pm10', 'pm2_5', 'carbon_monoxide', 'nitrogen_dioxide', 'sulphur_dioxide', 'ozone', 'us_aqi'],
        timezone: 'auto'
      }
    })

    const current = response.data.current
    return {
      aqi: current.us_aqi,
      pm25: current.pm2_5,
      pm10: current.pm10,
      co: current.carbon_monoxide / 1000, // 转换为 mg/m³
      no2: current.nitrogen_dioxide,
      so2: current.sulphur_dioxide,
      o3: current.ozone,
      updateTime: new Date().toLocaleString('zh-CN')
    }
  } catch (error) {
    console.error('获取空气质量失败:', error)
    throw error
  }
}

/**
 * 获取天气和空气质量预报
 * @param {number} latitude - 纬度
 * @param {number} longitude - 经度
 * @returns {Promise<Object>}
 */
export async function getForecast(latitude, longitude) {
  try {
    const [weatherRes, airQualityRes] = await Promise.all([
      weatherClient.get('/forecast', {
        params: {
          latitude,
          longitude,
          daily: ['temperature_2m_max', 'temperature_2m_min', 'weathercode'],
          timezone: 'auto',
          forecast_days: 7
        }
      }),
      airQualityClient.get('/air-quality', {
        params: {
          latitude,
          longitude,
          hourly: ['pm10', 'pm2_5', 'us_aqi'],
          timezone: 'auto',
          forecast_days: 7
        }
      })
    ])

    return {
      weather: weatherRes.data,
      airQuality: airQualityRes.data
    }
  } catch (error) {
    console.error('获取预报数据失败:', error)
    throw error
  }
}

/**
 * 获取城市天气和空气质量（综合接口）
 * @param {string} cityName - 城市名称
 * @returns {Promise<Object>}
 */
export async function getCityWeatherAndAQI(cityName) {
  try {
    // 1. 获取城市坐标
    const coords = await getCityCoordinates(cityName)

    // 2. 获取空气质量数据
    const airQuality = await getAirQuality(coords.latitude, coords.longitude)

    return {
      city: coords.name,
      fullName: `${coords.name}${coords.admin1 ? ', ' + coords.admin1 : ''}`,
      ...airQuality
    }
  } catch (error) {
    console.error('获取城市天气数据失败:', error)
    throw error
  }
}

/**
 * 获取历史AQI数据（模拟24小时历史）
 * @param {number} latitude - 纬度
 * @param {number} longitude - 经度
 * @returns {Promise<Array>}
 */
export async function getHourlyAQI(latitude, longitude) {
  try {
    const response = await airQualityClient.get('/air-quality', {
      params: {
        latitude,
        longitude,
        hourly: ['us_aqi', 'pm10', 'pm2_5', 'nitrogen_dioxide', 'ozone'],
        timezone: 'auto',
        past_days: 1,
        forecast_days: 1
      }
    })

    const hourly = response.data.hourly
    const times = hourly.time.slice(-24) // 最近24小时
    const aqiValues = hourly.us_aqi.slice(-24)

    return times.map((time, index) => ({
      time: time.split('T')[1].substring(0, 5),
      fullTime: time,
      aqi: aqiValues[index],
      pm25: hourly.pm2_5[index],
      pm10: hourly.pm10[index],
      no2: hourly.nitrogen_dioxide[index],
      o3: hourly.ozone[index]
    }))
  } catch (error) {
    console.error('获取历史AQI失败:', error)
    throw error
  }
}

/**
 * 获取多天历史AQI数据
 * @param {number} latitude - 纬度
 * @param {number} longitude - 经度
 * @param {number} days - 天数
 * @returns {Promise<Array>}
 */
export async function getDailyAQI(latitude, longitude, days = 7) {
  try {
    const response = await airQualityClient.get('/air-quality', {
      params: {
        latitude,
        longitude,
        hourly: ['us_aqi', 'pm10', 'pm2_5', 'nitrogen_dioxide', 'ozone'],
        timezone: 'auto',
        past_days: days,
        forecast_days: 1
      }
    })

    const hourly = response.data.hourly
    const dailyData = []
    const today = new Date()

    for (let i = days - 1; i >= 0; i--) {
      const date = new Date(today)
      date.setDate(date.getDate() - i)
      const dateStr = `${date.getMonth() + 1}/${date.getDate()}`
      const fullDateStr = date.toLocaleDateString('zh-CN')

      // 计算该日平均值（从小时数据中抽样）
      const dayStartIndex = (days - 1 - i) * 24
      const dayEndIndex = dayStartIndex + 24
      const dayAqi = hourly.us_aqi.slice(dayStartIndex, dayEndIndex)
      const dayPm25 = hourly.pm2_5.slice(dayStartIndex, dayEndIndex)
      const dayPm10 = hourly.pm10.slice(dayStartIndex, dayEndIndex)
      const dayNo2 = hourly.nitrogen_dioxide.slice(dayStartIndex, dayEndIndex)
      const dayO3 = hourly.ozone.slice(dayStartIndex, dayEndIndex)

      dailyData.push({
        time: dateStr,
        fullTime: fullDateStr,
        aqi: Math.round(dayAqi.reduce((a, b) => a + b, 0) / dayAqi.length),
        pm25: Math.round(dayPm25.reduce((a, b) => a + b, 0) / dayPm25.length * 10) / 10,
        pm10: Math.round(dayPm10.reduce((a, b) => a + b, 0) / dayPm10.length * 10) / 10,
        no2: Math.round(dayNo2.reduce((a, b) => a + b, 0) / dayNo2.length * 10) / 10,
        o3: Math.round(dayO3.reduce((a, b) => a + b, 0) / dayO3.length * 10) / 10
      })
    }

    return dailyData
  } catch (error) {
    console.error('获取历史AQI失败:', error)
    throw error
  }
}

/**
 * 获取多城市实时AQI数据
 * @returns {Promise<Array>}
 */
export async function getMultiCityAQI() {
  try {
    const promises = HOT_CITIES.map(async (city) => {
      try {
        const data = await getAirQuality(city.latitude, city.longitude)
        return {
          stationName: city.name,           // 页面显示名称
          actualStation: city.actualStation, // 实际监测站点
          city: city.name,
          ...data
        }
      } catch (e) {
        return {
          stationName: city.name,
          actualStation: city.actualStation,
          city: city.name,
          aqi: null,
          pm25: null,
          pm10: null,
          so2: null,
          no2: null,
          o3: null,
          co: null,
          updateTime: '--'
        }
      }
    })
    return await Promise.all(promises)
  } catch (error) {
    console.error('获取多城市AQI失败:', error)
    throw error
  }
}

/**
 * 获取空气质量预报数据（用于预测）
 * @param {number} latitude - 纬度
 * @param {number} longitude - 经度
 * @param {number} hours - 预报小时数
 * @returns {Promise<Array>}
 */
export async function getAQIForecast(latitude, longitude, hours = 72) {
  try {
    const response = await airQualityClient.get('/air-quality', {
      params: {
        latitude,
        longitude,
        hourly: ['us_aqi', 'pm10', 'pm2_5'],
        timezone: 'auto',
        forecast_days: Math.ceil(hours / 24)
      }
    })

    const hourly = response.data.hourly
    const forecastData = []
    const now = new Date()
    const currentHour = now.getHours()

    for (let i = 0; i < hours; i++) {
      const hour = (currentHour + i) % 24
      const dayOffset = Math.floor((currentHour + i) / 24)
      const date = new Date(now)
      date.setDate(date.getDate() + dayOffset)

      forecastData.push({
        hour: `${hour}:00`,
        time: date.toLocaleDateString('zh-CN'),
        aqi: hourly.us_aqi[i],
        pm25: hourly.pm2_5[i],
        pm10: hourly.pm10[i],
        predictedAqi: hourly.us_aqi[i]
      })
    }

    return forecastData
  } catch (error) {
    console.error('获取AQI预报失败:', error)
    throw error
  }
}

// 郑州市站点列表（显示名称 vs 实际监测站点坐标）
// 中原区 -> 北区建设指挥部 (34.7486, 113.6112)
// 金水区 -> 北区建设指挥部 (34.7486, 113.6112)
// 二七区 -> 河医大 (34.7170, 113.6450)
// 惠济区 -> 惠济区政府 (34.8200, 113.6200)
// 郑东新区 -> 经开区管委 (34.7600, 113.7300)
export const HOT_CITIES = [
  { name: '中原区', actualStation: '北区建设指挥部', latitude: 34.7486, longitude: 113.6112 },
  { name: '金水区', actualStation: '北区建设指挥部', latitude: 34.7486, longitude: 113.6112 },
  { name: '二七区', actualStation: '河医大', latitude: 34.7170, longitude: 113.6450 },
  { name: '惠济区', actualStation: '惠济区政府', latitude: 34.8200, longitude: 113.6200 },
  { name: '郑东新区', actualStation: '经开区管委', latitude: 34.7600, longitude: 113.7300 }
]
