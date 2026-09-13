import request from './request'

export const login = (payload) => request.post('/auth/login', payload)
export const getMyRoles = () => request.get('/users/me/roles')
