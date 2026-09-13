import request from './request'

export const getRepairTypes = () => request.get('/repair-types')
export const getRepairLocations = () => request.get('/repair-locations')
export const getMyOrders = () => request.get('/repair-orders/my')
export const getOrderProgress = (orderId) => request.get(`/repair-orders/${orderId}`)
export const updateOrder = (orderId, payload) => request.put(`/repair-orders/${orderId}`, payload)
export const getUsers = (params = {}) => request.get('/users', { params })

export const createOrder = (payload, files = []) => {
  const data = new FormData()
  data.append('typeId', payload.typeId)
  data.append('locationId', payload.locationId)
  data.append('title', payload.title)
  data.append('description', payload.description)
  files.forEach((file) => data.append('files', file))
  return request.post('/repair-orders', data)
}

export const getAdminPendingOrders = () => request.get('/admin/repair-orders/pending')
export const auditOrder = (orderId, payload) => request.post(`/admin/repair-orders/${orderId}/audit`, payload)
export const dispatchOrder = (orderId, payload) => request.post(`/admin/repair-orders/${orderId}/dispatch`, payload)
export const getAdminOrderHistory = (orderId) => request.get(`/admin/repair-orders/${orderId}/history`)

export const getMaintenanceTasks = () => request.get('/maintenance/tasks')
export const acceptMaintenanceTask = (orderId) => request.post(`/maintenance/tasks/${orderId}/accept`)
export const continueMaintenanceTask = (orderId) => request.post(`/maintenance/tasks/${orderId}/continue`)
export const processMaintenanceTask = (orderId, payload) => request.post(`/maintenance/tasks/${orderId}/process`, payload)
export const getMaintenanceHistory = (orderId) => request.get(`/maintenance/tasks/${orderId}/history`)

export const acceptRepairOrder = (orderId, payload) => request.post(`/repair-orders/${orderId}/acceptance`, payload)
export const requestRework = (orderId, payload) => request.post(`/repair-orders/${orderId}/rework`, payload)
export const evaluateOrder = (orderId, payload) => request.post(`/repair-orders/${orderId}/evaluation`, payload)
export const getAcceptanceHistory = (orderId) => request.get(`/repair-orders/${orderId}/acceptance-history`)
export const getEvaluation = (orderId) => request.get(`/repair-orders/${orderId}/evaluation`)

export const getStatisticsByStatus = () => request.get('/admin/statistics/status')
export const getStatisticsByType = () => request.get('/admin/statistics/type')
export const getStatisticsByPeriod = (start, end) => request.get('/admin/statistics/period', { params: { start, end } })
export const getStatisticsDuration = () => request.get('/admin/statistics/repair-duration')
