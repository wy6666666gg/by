import request from '../utils/request'

// 获取实时AQI数据
export function getRealtimeAqi() {
  return request.get('/v1/aqi/realtime')
}

// 获取历史AQI数据
export function getHistoryAqi(stationCode, startDate, endDate) {
  return request.get('/v1/aqi/history', {
    params: { stationCode, startDate, endDate }
  })
}

// 获取AQI趋势数据
export function getTrend(stationCode, type = 'daily', days) {
  return request.get('/v1/aqi/trend', {
    params: { stationCode, type, days }
  })
}

// 获取站点AQI详情
export function getStationAqi(code) {
  return request.get(`/v1/aqi/station/${code}`)
}

// 获取AQI统计信息
export function getStatistics(date) {
  return request.get('/v1/aqi/statistics', {
    params: { date }
  })
}
