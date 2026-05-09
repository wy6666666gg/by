import request from '../utils/request'

// 获取当前活跃预警
export function getActiveAlerts() {
  return request.get('/v1/alerts/active')
}

// 获取历史预警
export function getAlertHistory(days) {
  return request.get('/v1/alerts/history', {
    params: { days }
  })
}

// 获取预警规则列表
export function getAlertRules() {
  return request.get('/v1/alerts/rules')
}

// 保存预警规则
export function saveAlertRule(rule) {
  return request.post('/v1/alerts/rules', rule)
}

// 更新预警规则
export function updateAlertRule(id, rule) {
  return request.put(`/v1/alerts/rules/${id}`, rule)
}

// 删除预警规则
export function deleteAlertRule(id) {
  return request.delete(`/v1/alerts/rules/${id}`)
}

// 标记预警为已处理
export function resolveAlert(id) {
  return request.post(`/v1/alerts/${id}/resolve`)
}

// 获取预警统计
export function getAlertStats() {
  return request.get('/v1/alerts/stats')
}

// 手动触发预警检查
export function triggerAlertCheck() {
  return request.post('/v1/alerts/trigger')
}
