import request from '../utils/request'

// 获取相关性矩阵
export function getCorrelationMatrix(startDate, endDate, method, stationCode) {
  return request.get('/v1/correlation/matrix', {
    params: { startDate, endDate, method, stationCode }
  })
}

// 获取随机森林特征重要性
export function getFeatureImportance(startDate, endDate, stationCode) {
  return request.get('/v1/correlation/feature-importance', {
    params: { startDate, endDate, stationCode }
  })
}

// 获取时滞相关性
export function getLagCorrelation(factor1, factor2, maxLag, startDate, endDate) {
  return request.get('/v1/correlation/lag', {
    params: { factor1, factor2, maxLag, startDate, endDate }
  })
}

// 获取散点图数据
export function getScatterData(factorX, factorY, startDate, endDate, stationCode) {
  return request.get('/v1/correlation/scatter', {
    params: { factorX, factorY, startDate, endDate, stationCode }
  })
}
