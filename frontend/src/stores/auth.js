import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getMyRoles } from '../api/auth'
import { login as loginRequest } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  function loadRoles() {
    const raw = localStorage.getItem('campus_repair_roles')
    if (!raw) return []
    try {
      return JSON.parse(raw).filter((value) => typeof value === 'string')
    } catch {
      return []
    }
  }

  const token = ref(localStorage.getItem('campus_repair_token') || '')
  const roles = ref(loadRoles())
  const isLoggedIn = computed(() => Boolean(token.value))
  const isAdmin = computed(() => roles.value.includes('ADMIN'))
  const isMaintainer = computed(() => roles.value.includes('MAINTAINER'))
  const isReporter = computed(() => roles.value.includes('REPORTER'))

  async function login(username, password) {
    const response = await loginRequest({ username, password })
    const newToken = response.data.data
    token.value = newToken
    localStorage.setItem('campus_repair_token', newToken)
    try {
      const rolesResponse = await getMyRoles()
      roles.value = Array.isArray(rolesResponse.data.data)
        ? rolesResponse.data.data.map((item) => item.roleCode).filter(Boolean)
        : []
      localStorage.setItem('campus_repair_roles', JSON.stringify(roles.value))
    } catch (error) {
      logout()
      throw error
    }
  }

  function logout() {
    token.value = ''
    localStorage.removeItem('campus_repair_token')
    roles.value = []
    localStorage.removeItem('campus_repair_roles')
  }

  return {
    token,
    roles,
    isLoggedIn,
    isAdmin,
    isMaintainer,
    isReporter,
    login,
    logout,
  }
})
