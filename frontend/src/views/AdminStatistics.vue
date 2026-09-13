<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getStatisticsByPeriod,
  getStatisticsByStatus,
  getStatisticsByType,
  getStatisticsDuration,
} from '../api/repair'

const loading = ref(false)
const statusStats = ref({})
const typeStats = ref([])
const repairDuration = ref(null)
const periodStart = ref(formatDate(new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)))
const periodEnd = ref(formatDate(new Date()))
const periodCount = ref(null)

const hasPeriodQuery = computed(() => periodStart.value && periodEnd.value)

function formatDate(value) {
  return value.toISOString().slice(0, 10)
}

async function loadStatus() {
  try {
    const response = await getStatisticsByStatus()
    statusStats.value = response.data.data || {}
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '加载状态统计失败')
  }
}

async function loadType() {
  try {
    const response = await getStatisticsByType()
    typeStats.value = response.data.data || []
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '加载类型统计失败')
  }
}

async function loadDuration() {
  try {
    const response = await getStatisticsDuration()
    repairDuration.value = response.data.data
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '加载平均工时失败')
  }
}

async function loadPeriod() {
  periodCount.value = null
  if (!hasPeriodQuery.value) return
  try {
    const response = await getStatisticsByPeriod(periodStart.value, periodEnd.value)
    periodCount.value = response.data.data
  } catch (error) {
    periodCount.value = null
    ElMessage.error(error.response?.data?.message || '加载时间范围统计失败')
  }
}

async function refreshAll() {
  loading.value = true
  try {
    await Promise.all([loadStatus(), loadType(), loadDuration(), loadPeriod()])
  } finally {
    loading.value = false
  }
}

onMounted(refreshAll)
</script>

<template>
  <section v-loading="loading">
    <div class="title-row">
      <h2>管理员 - 统计看板</h2>
      <el-button type="primary" @click="refreshAll">刷新</el-button>
    </div>

    <el-card class="margin-top">
      <template #header>按状态统计</template>
      <el-empty v-if="Object.keys(statusStats).length === 0" description="暂无状态统计数据" />
      <el-row v-else :gutter="16">
        <el-col :span="6" v-for="item in Object.keys(statusStats)" :key="item">
          <el-card shadow="hover">
            <template #header>{{ item }}</template>
            <div class="metric">{{ statusStats[item] || 0 }}</div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="margin-top">
      <template #header>按类型统计</template>
      <el-empty v-if="typeStats.length === 0" description="暂无类型统计数据" />
      <el-table v-else :data="typeStats" border>
        <el-table-column prop="typeId" label="类型ID" width="90" />
        <el-table-column prop="typeName" label="类型名称" />
        <el-table-column prop="count" label="工单数" width="120" />
      </el-table>
    </el-card>

    <el-card class="margin-top">
      <template #header>时间范围统计</template>
      <el-form inline>
        <el-form-item label="开始日期">
          <el-date-picker
            v-model="periodStart"
            type="date"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            placeholder="开始"
          />
        </el-form-item>
        <el-form-item label="结束日期">
          <el-date-picker
            v-model="periodEnd"
            type="date"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            placeholder="结束"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadPeriod">查询</el-button>
        </el-form-item>
      </el-form>
      <el-descriptions border>
        <el-descriptions-item label="范围内工单数">{{ periodCount === null ? '未查询' : periodCount }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="margin-top">
      <template #header>平均维修时长（分钟）</template>
      <el-empty v-if="repairDuration === null" description="暂无维修时长" />
      <div class="metric" v-else>{{ repairDuration }}</div>
    </el-card>
  </section>
</template>

<style scoped>
.title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

h2 { margin: 0; color: #303133; }
.margin-top { margin-top: 16px; }
.metric { font-size: 30px; text-align: center; color: #409eff; padding: 14px 0; }
</style>

