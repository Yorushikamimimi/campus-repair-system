<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getMyOrders } from '../api/repair'

const router = useRouter()
const orders = ref([])
const loading = ref(false)
const statusNames = { SUBMITTED: '待审核', PENDING_PROCESS: '待处理', PROCESSING: '处理中', PENDING_ACCEPTANCE: '待验收', REWORK: '待返修', COMPLETED: '已完成' }

async function loadOrders() {
  loading.value = true
  try {
    const response = await getMyOrders()
    orders.value = response.data.data || []
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '工单加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadOrders)
</script>

<template>
  <section>
    <div class="title-row"><div><h2>我的报修</h2><p>查看自己提交的工单及当前处理状态</p></div><el-button type="primary" @click="router.push('/orders/create')">新建报修</el-button></div>
    <el-card v-loading="loading">
      <el-empty v-if="!loading && orders.length === 0" description="还没有报修工单" />
      <el-table v-else :data="orders" stripe @row-click="(row) => router.push(`/orders/${row.orderId}`)">
        <el-table-column prop="orderId" label="工单号" width="100" />
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column prop="status" label="状态" width="130"><template #default="scope"><el-tag>{{ statusNames[scope.row.status] || scope.row.status }}</el-tag></template></el-table-column>
        <el-table-column prop="submitTime" label="提交时间" min-width="180" />
        <el-table-column label="操作" width="100"><template #default="scope"><el-button link type="primary" @click.stop="router.push(`/orders/${scope.row.orderId}`)">查看</el-button></template></el-table-column>
      </el-table>
    </el-card>
  </section>
</template>

<style scoped>
h2 { margin: 0 0 8px; color: #303133; }
.title-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
p { margin: 0 0 18px; color: #909399; }
:deep(.el-table__row) { cursor: pointer; }
</style>
