<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import {
  acceptRepairOrder,
  evaluateOrder,
  getAcceptanceHistory,
  getEvaluation,
  getOrderProgress,
  requestRework,
} from '../api/repair'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const loading = ref(false)
const acceptanceHistory = ref([])
const evaluation = ref(null)
const evaluationEditing = ref(false)
const evaluateForm = ref({ score: 5, comment: '' })
const reworkReason = ref('')
const auth = useAuthStore()
const isReporter = computed(() => auth.isReporter)

const statusNames = { SUBSIGNED: '待审核', SUBMITTED: '待审核', PENDING_PROCESS: '待处理', PROCESSING: '处理中', PENDING_ACCEPTANCE: '待验收', REWORK: '待返修', COMPLETED: '已完成' }

async function loadOrder() {
  loading.value = true
  try {
    const response = await getOrderProgress(route.params.orderId)
    order.value = response.data.data
    await Promise.all([loadAcceptanceHistory(), loadEvaluation()])
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '工单加载失败')
  } finally { loading.value = false }
}

async function loadAcceptanceHistory() {
  try {
    const response = await getAcceptanceHistory(route.params.orderId)
    acceptanceHistory.value = response.data.data || []
  } catch {
    acceptanceHistory.value = []
  }
}

async function loadEvaluation() {
  try {
    const response = await getEvaluation(route.params.orderId)
    evaluation.value = response.data.data
  } catch {
    evaluation.value = null
  }
}

async function passAcceptance() {
  try {
    await acceptRepairOrder(route.params.orderId, { passed: true })
    ElMessage.success('验收通过')
    await loadOrder()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '验收操作失败')
  }
}

async function failAcceptance() {
  if (!reworkReason.value.trim()) {
    ElMessage.error('请输入返修原因')
    return
  }
  try {
    await requestRework(route.params.orderId, { returnReason: reworkReason.value.trim() })
    ElMessage.success('验收不通过，已进入待返修')
    reworkReason.value = ''
    await loadOrder()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '验收操作失败')
  }
}

async function submitEvaluation() {
  if (!evaluateForm.value.score || evaluateForm.value.score < 1 || evaluateForm.value.score > 5) {
    ElMessage.error('评分必须在1到5之间')
    return
  }
  try {
    await evaluateOrder(route.params.orderId, {
      score: evaluateForm.value.score,
      comment: evaluateForm.value.comment,
    })
    ElMessage.success('感谢评价')
    evaluationEditing.value = false
    await loadEvaluation()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '评价提交失败')
  }
}

onMounted(loadOrder)
</script>

<template>
  <section v-loading="loading">
    <div class="title-row"><h2>工单详情</h2><el-button @click="router.back()">返回</el-button></div>
    <el-card v-if="order">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="工单号">{{ order.orderId }}</el-descriptions-item>
        <el-descriptions-item label="标题">{{ order.title }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ order.description }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag>{{ statusNames[order.status] || order.status }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ order.submitTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ order.updateTime }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="order && isReporter && order.status === 'PENDING_ACCEPTANCE'" class="margin-top">
      <template #header><span>验收处理</span></template>
      <el-space direction="vertical" size="large" style="width: 100%;">
        <el-space>
          <el-button type="primary" @click="passAcceptance">验收通过</el-button>
          <el-input v-model="reworkReason" placeholder="验收失败原因" clearable style="width: 320px" />
          <el-button type="warning" @click="failAcceptance">验收不通过</el-button>
        </el-space>
      </el-space>
    </el-card>

    <el-card v-if="order && order.status === 'COMPLETED' && isReporter" class="margin-top">
      <template #header><span>评价</span></template>
      <el-empty v-if="evaluation" :description="`评分：${evaluation.score}，评价：${evaluation.comment || '-'}`" />
      <template v-else>
        <el-form v-if="!evaluationEditing">
          <el-button type="primary" @click="evaluationEditing = true">我要评价</el-button>
        </el-form>
        <el-form v-else>
          <el-form-item label="评分">
            <el-input-number v-model="evaluateForm.score" :min="1" :max="5" />
          </el-form-item>
          <el-form-item label="评价文字">
            <el-input v-model="evaluateForm.comment" type="textarea" maxlength="500" show-word-limit placeholder="选填" />
          </el-form-item>
          <el-space>
            <el-button @click="evaluationEditing = false">取消</el-button>
            <el-button type="primary" @click="submitEvaluation">提交</el-button>
          </el-space>
        </el-form>
      </template>
    </el-card>

    <el-card v-if="acceptanceHistory.length > 0" class="margin-top">
      <template #header><span>验收历史</span></template>
      <el-timeline>
        <el-timeline-item
          v-for="record in acceptanceHistory"
          :key="record.acceptanceId"
          :type="record.acceptResult === 'PASSED' ? 'success' : 'warning'"
        >
          <div><strong>结果：</strong>{{ record.acceptResult }}</div>
          <div v-if="record.returnReason"><strong>原因：</strong>{{ record.returnReason }}</div>
          <div><strong>时间：</strong>{{ record.acceptTime }}</div>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </section>
</template>

<style scoped>
.title-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 18px; }
h2 { margin: 0; color: #303133; }
.margin-top { margin-top: 16px; }
</style>
