import request from './request'
export const healthCheck = () => request.get('/health')
