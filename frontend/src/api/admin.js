import request from './request'

export function getLogs(params) {
  return request.get('/admin/logs', { params })
}

export function exportLogs(params) {
  return request.get('/admin/logs/export', {
    params,
    responseType: 'blob'
  })
}

export function getConfigs() {
  return request.get('/admin/configs')
}

export function updateConfig(data) {
  return request.put('/admin/config', data)
}

export function getMonitor() {
  return request.get('/admin/monitor')
}
