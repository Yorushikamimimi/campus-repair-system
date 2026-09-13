<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  auditOrder,
  dispatchOrder,
  getAdminPendingOrders,
} from '../api/repair'

const orders = ref([])
const loading = ref(false)
const submitting = ref(false)
const dispatchPayload = reactive({})
const statusNames = { SUBMITTED: '待审核', PENDING_PROCESS: '待处理', PROCESSING: '处理中', PENDING_ACCEPTANCE: '待验收', REWORK: '待返修', COMPLETED: '已完成' }

function normalizeNumber(value) {
  const id = Number(value)
  return Number.isFinite(id) ? id : null
}

function getInput(orderId) {
  return dispatchPayload[orderId] || { maintainerId: '', note: '' }
}

async function loadOrders() {
  loading.value = true
  try {
    const response = await getAdminPendingOrders()
    orders.value = response.data.data || []
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '待处理工单加载失败')
  } finally {
    loading.value = false
  }
}

async function audit(orderId, approved) {
  const comment = approved ? '审核通过' : '退回补充'
  await ElMessageBox.confirm(
    `确认要对工单 ${orderId} 执行${approved ? '通过' : '退回补充'}吗？`,
    '审核确认',
    { type: 'warning' },
  )
  submitting.value = true
  try {
    await auditOrder(orderId, { approved, comment })
    ElMessage.success(approved ? '审核通过' : '已退回')
    await loadOrders()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '审核操作失败')
  } finally {
    submitting.value = false
  }
}

async function dispatch(orderId) {
  const payload = getInput(orderId)
  const maintainerId = normalizeNumber(payload.maintainerId)
  if (!maintainerId) {
    ElMessage.error('请输入有效的维修人员ID')
    return
  }
  try {
    await dispatchOrder(orderId, { maintainerId, note: payload.note || '' })
    ElMessage.success('派单成功')
    dispatchPayload[orderId] = { maintainerId: '', note: '' }
    await loadOrders()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '派单失败')
  }
}

function payloadFor(orderId) {
  if (!dispatchPayload[orderId]) {
    dispatchPayload[orderId] = { maintainerId: '', note: '' }
  }
  return dispatchPayload[orderId]
}

onMounted(loadOrders)
</script>

<template>
  <section>
    <div class="title-row">
      <div><h2>管理员 - 审核与派单</h2><p>处理“待审核/待派单”工单</p></div>
      <el-button type="primary" :loading="loading" @click="loadOrders">刷新</el-button>
    </div>
    <el-card v-loading="loading">
      <el-empty v-if="!loading && orders.length === 0" description="当前没有可处理的工单" />
      <el-table v-else :data="orders" stripe>
        <el-table-column prop="orderId" label="工单号" width="100" />
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column prop="status" label="状态" width="130">
          <template #default="scope">
            <el-tag>{{ statusNames[scope.row.status] || scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" min-width="180" />
        <el-table-column label="操作" width="380">
          <template #default="scope">
            <el-space>
              <el-button
                v-if="scope.row.status === 'SUBMITTED'"
                type="success"
                :loading="submitting"
                @click="audit(scope.row.orderId, true)"
              >
                审核通过
              </el-button>
              <el-button
                v-if="scope.row.status === 'SUBMITTED'"
                type="warning"
                :loading="submitting"
                @click="audit(scope.row.orderId, false)"
              >
                退回补充
              </el-button>
              <template v-if="scope.row.status === 'PENDING_PROCESS'">
                <el-input-number
                  v-model.number="payloadFor(scope.row.orderId).maintainerId"
                  :min="1"
                  :controls="false"
                  controls-position="right"
                  style="width: 150px"
                  placeholder="维修人员ID"
                />
                <el-input
                  v-model="payloadFor(scope.row.orderId).note"
                  placeholder="派单说明"
                  style="width: 180px"
                />
                <el-button type="primary" @click="dispatch(scope.row.orderId)">派单</el-button>
              </template>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </section>
</template>

<style scoped>
.title-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
h2 { margin: 0 0 8px; color: #303133; }
p { margin: 0 0 18px; color: #909399; }
:deep(.el-table .el-space) { display: flex; align-items: center; }
</style>
