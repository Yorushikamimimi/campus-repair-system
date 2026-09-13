<script setup>
import { useAuthStore } from '../stores/auth'
import { computed } from 'vue'

const auth = useAuthStore()
const hasAdminRoute = computed(() => Boolean(auth.isAdmin))
const hasMaintainerRoute = computed(() => Boolean(auth.isMaintainer))

function logout() {
  auth.logout()
  window.location.href = '/login'
}
</script>

<template>
  <el-container class="shell">
    <el-header class="header">
      <div class="brand">校园报修</div>
      <nav>
        <RouterLink to="/orders">我的工单</RouterLink>
        <RouterLink to="/orders/create">新建报修</RouterLink>
        <RouterLink v-if="hasAdminRoute" to="/admin/dispatch">审核与派单</RouterLink>
      <RouterLink v-if="hasAdminRoute" to="/admin/statistics">统计</RouterLink>
        <RouterLink v-if="hasMaintainerRoute" to="/maintenance/tasks">我的维修任务</RouterLink>
        <el-button text type="primary" @click="logout">退出登录</el-button>
      </nav>
    </el-header>
    <el-main class="content"><RouterView /></el-main>
  </el-container>
</template>

<style scoped>
.shell { min-height: 100vh; background: #f5f7fa; }
.header { display: flex; align-items: center; justify-content: space-between; background: #fff; border-bottom: 1px solid #ebeef5; }
.brand { color: #1f5f8b; font-size: 20px; font-weight: 700; }
nav { display: flex; align-items: center; gap: 22px; }
nav a { color: #606266; text-decoration: none; }
nav a.router-link-active { color: #1f5f8b; font-weight: 600; }
.content { width: min(1060px, 100%); margin: 0 auto; padding: 28px 20px; }
</style>
