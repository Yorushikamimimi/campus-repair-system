<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  acceptMaintenanceTask,
  continueMaintenanceTask,
  getMaintenanceHistory,
  getMaintenanceTasks,
  processMaintenanceTask,
} from '../api/repair'

const tasks = ref([])
const loading = ref(false)
const submitting = ref(false)
const statusNames = { SUBMITTED: '待审核', PENDING_PROCESS: '待处理', PROCESSING: '处理中', PENDING_ACCEPTANCE: '待验收', REWORK: '待返修', COMPLETED: '已完成' }
const activeTaskId = ref(null)
const history = ref([])
const showHistory = ref(false)
const formVisible = ref(false)
const form = reactive({
  processDesc: '',
  completed: true,
  repairResult: '',
  unfinishedReason: '',
})

const currentForm = () => activeTaskId.value

async function loadTasks() {
  loading.value = true
  try {
    const response = await getMaintenanceTasks()
    tasks.value = response.data.data || []
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '维修任务加载失败')
  } finally {
    loading.value = false
  }
}

async function acceptTask(orderId) {
  try {
    await acceptMaintenanceTask(orderId)
    ElMessage.success('已接单')
    await loadTasks()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '接单失败')
  }
}

async function reopenTask(orderId) {
  try {
    await continueMaintenanceTask(orderId)
    ElMessage.success('已进入重新维修流程')
    await loadTasks()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '重新开始失败')
  }
}

function openProcess(orderId) {
  activeTaskId.value = orderId
  form.processDesc = ''
  form.completed = true
  form.repairResult = ''
  form.unfinishedReason = ''
  formVisible.value = true
}

async function submitProcess() {
  if (!form.processDesc.trim()) {
    ElMessage.error('维修过程不能为空')
    return
  }
  if (form.completed && !form.repairResult.trim()) {
    ElMessage.error('完成维修时维修结果不能为空')
    return
  }
  if (!form.completed && !form.unfinishedReason.trim()) {
    ElMessage.error('未完成原因不能为空')
    return
  }
  if (!currentForm()) {
    return
  }
  submitting.value = true
  try {
    await processMaintenanceTask(currentForm(), {
      processDesc: form.processDesc,
      completed: form.completed,
      repairResult: form.repairResult || '',
      unfinishedReason: form.unfinishedReason || '',
    })
    ElMessage.success('维修结果已提交')
    formVisible.value = false
    await loadTasks()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '提交处理结果失败')
  } finally {
    submitting.value = false
  }
}

async function openHistory(orderId) {
  try {
    const response = await getMaintenanceHistory(orderId)
    history.value = response.data.data || []
    showHistory.value = true
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '加载维修记录失败')
  }
}

onMounted(loadTasks)
</script>

<template>
  <section>
    <div class="title-row">
      <div><h2>维修人员 - 我的任务</h2><p>接单后处理工单并提交维修结果</p></div>
      <el-button type="primary" :loading="loading" @click="loadTasks">刷新</el-button>
    </div>
    <el-card v-loading="loading">
      <el-empty v-if="!loading && tasks.length === 0" description="当前没有分配给你的任务" />
      <el-table v-else :data="tasks" stripe>
        <el-table-column prop="orderId" label="工单号" width="100" />
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column label="状态" width="110">
          <template #default="scope"><el-tag>{{ statusNames[scope.row.status] || scope.row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="dispatchTime" label="派单时间" min-width="180" />
        <el-table-column prop="dispatchNote" label="派单说明" min-width="180" />
        <el-table-column label="操作" width="300">
          <template #default="scope">
            <el-space>
              <el-button
                v-if="scope.row.status === 'PENDING_PROCESS'"
                type="primary"
                @click="acceptTask(scope.row.orderId)"
              >
                接单
              </el-button>
              <el-button
                v-if="scope.row.status === 'REWORK'"
                type="warning"
                @click="reopenTask(scope.row.orderId)"
              >
                继续维修
              </el-button>
              <el-button
                v-if="scope.row.status === 'PROCESSING'"
                type="success"
                @click="openProcess(scope.row.orderId)"
              >
                提交结果
              </el-button>
              <el-button link type="primary" @click="openHistory(scope.row.orderId)">查看历史</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="formVisible" title="提交维修结果" width="560px">
      <el-form>
        <el-form-item label="维修过程"><el-input v-model="form.processDesc" type="textarea" :rows="4" placeholder="输入处理过程" /></el-form-item>
        <el-form-item label="是否完成"><el-radio-group v-model="form.completed"><el-radio :label="true">完成</el-radio><el-radio :label="false">未完成</el-radio></el-radio-group></el-form-item>
        <el-form-item v-if="form.completed" label="维修结果"><el-input v-model="form.repairResult" placeholder="填写维修结果" /></el-form-item>
        <el-form-item v-else label="未完成原因"><el-input v-model="form.unfinishedReason" placeholder="填写未完成原因" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitProcess">提交</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="showHistory" title="维修历史">
      <el-empty v-if="history.length === 0" description="暂无维修记录" />
      <el-timeline v-else>
        <el-timeline-item v-for="item in history" :key="item.repairId">
          <div><strong>记录ID：</strong>{{ item.repairId }}</div>
          <div><strong>开始：</strong>{{ item.startTime || '-' }}</div>
          <div><strong>结束：</strong>{{ item.endTime || '-' }}</div>
          <div><strong>过程：</strong>{{ item.processDesc }}</div>
          <div><strong>结果：</strong>{{ item.repairResult || '-' }}</div>
          <div><strong>未完成原因：</strong>{{ item.unfinishedReason || '-' }}</div>
        </el-timeline-item>
      </el-timeline>
    </el-drawer>
  </section>
</template>

<style scoped>
.title-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
h2 { margin: 0 0 8px; color: #303133; }
p { margin: 0 0 18px; color: #909399; }
:deep(.el-space) { display: flex; align-items: center; }
</style>
