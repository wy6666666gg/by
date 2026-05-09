import request from '../utils/request'

// 获取站点预测数据
export function getPrediction(stationCode) {
  return request.get(`/v1/predict/${stationCode}`)
}

// 获取24小时预测
export function get24HourPrediction(stationCode) {
  return request.get('/v1/predict/24h', {
    params: { stationCode }
  })
}

// 获取72小时预测
export function get72HourPrediction(stationCode) {
  return request.get('/v1/predict/72h', {
    params: { stationCode }
  })
}

// 获取多站点预测对比
export function getComparison(stationCodes) {
  return request.get('/v1/predict/comparison', {
    params: { stationCodes }
  })
}

// 触发预测任务
export function triggerPrediction(stationCode) {
  return request.post('/v1/predict/trigger', null, {
    params: { stationCode }
  })
}
