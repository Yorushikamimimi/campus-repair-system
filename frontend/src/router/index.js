import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('../views/Login.vue'), meta: { public: true } },
    {
      path: '/',
      component: () => import('../layouts/MainLayout.vue'),
      children: [
        { path: '', redirect: '/orders' },
        { path: 'orders', component: () => import('../views/OrderList.vue') },
        { path: 'orders/create', component: () => import('../views/OrderCreate.vue') },
        { path: 'orders/:orderId', component: () => import('../views/OrderDetail.vue') },
        { path: 'admin/dispatch', component: () => import('../views/AdminDispatch.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'admin/statistics', component: () => import('../views/AdminStatistics.vue'), meta: { roles: ['ADMIN'] } },
        { path: 'maintenance/tasks', component: () => import('../views/MaintenanceTasks.vue'), meta: { roles: ['MAINTAINER'] } },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem('campus_repair_token')
  const auth = useAuthStore()
  if (!to.meta.public && !token) return '/login'
  if (to.meta.roles && to.meta.roles.length > 0) {
    const roles = auth.roles ?? []
    const matched = to.meta.roles.some((role) => roles.includes(role))
    if (!matched) return '/orders'
  }
  if (to.path === '/login' && token) return '/orders'
  return true
})

export default router
