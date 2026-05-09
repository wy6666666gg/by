import request from '../utils/request'

// 获取污染分布热力图数据
export function getHeatmapData(time, pollutant, method) {
  return request.get('/v1/spatial/heatmap', {
    params: { time, pollutant, method }
  })
}

// 获取站点空间分布
export function getStationDistribution() {
  return request.get('/v1/spatial/stations')
}

// 获取插值分析结果
export function getInterpolationResult(time, pollutant, method) {
  return request.get('/v1/spatial/interpolation', {
    params: { time, pollutant, method }
  })
}

// 获取多站点空间对比
export function getSpatialComparison(stationCodes, time) {
  return request.get('/v1/spatial/comparison', {
    params: { stationCodes, time }
  })
}

// 获取时空演变数据
export function getTimelineData(startTime, endTime, pollutant) {
  return request.get('/v1/spatial/timeline', {
    params: { startTime, endTime, pollutant }
  })
}
