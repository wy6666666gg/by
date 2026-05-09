import request from '../utils/request'

// 获取系统运行状态
export function getSystemStatus() {
  return request.get('/v1/system/status')
}

// 获取系统监控指标
export function getMonitorMetrics() {
  return request.get('/v1/system/monitor')
}

// 获取系统日志
export function getSystemLogs(page, size, level) {
  return request.get('/v1/system/logs', {
    params: { page, size, level }
  })
}

// 获取数据质量报告
export function getDataQualityReport() {
  return request.get('/v1/system/data-quality')
}

// 清除缓存
export function clearCache() {
  return request.post('/v1/system/cache/clear')
}

// 获取系统版本信息
export function getVersionInfo() {
  return request.get('/v1/system/version')
}
