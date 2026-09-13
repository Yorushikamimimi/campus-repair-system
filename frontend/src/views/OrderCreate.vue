<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createOrder, getRepairLocations, getRepairTypes } from '../api/repair'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const types = ref([])
const locations = ref([])
const files = ref([])
const form = reactive({ typeId: '', locationId: '', title: '', description: '' })
const rules = {
  typeId: [{ required: true, message: '请选择报修类型', trigger: 'change' }],
  locationId: [{ required: true, message: '请选择报修地点', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }, { max: 128, message: '标题不能超过128个字符', trigger: 'blur' }],
  description: [{ required: true, message: '请输入问题描述', trigger: 'blur' }],
}

function selectFiles(event) { files.value = Array.from(event.target.files || []) }

async function loadOptions() {
  const [typeResponse, locationResponse] = await Promise.all([getRepairTypes(), getRepairLocations()])
  types.value = typeResponse.data.data || []
  locations.value = locationResponse.data.data || []
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await createOrder(form, files.value)
    ElMessage.success('报修提交成功')
    router.push('/orders')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '报修提交失败')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try { await loadOptions() } catch { ElMessage.error('报修类型或地点加载失败') }
})
</script>

<template>
  <section class="form-page">
    <div class="title-row"><div><h2>新建报修</h2><p>请填写准确的问题和位置，方便后续处理</p></div><el-button @click="router.back()">返回</el-button></div>
    <el-card>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="grid">
          <el-form-item label="报修类型" prop="typeId"><el-select v-model="form.typeId" placeholder="请选择类型" class="full"><el-option v-for="item in types" :key="item.typeId" :label="item.typeName" :value="item.typeId" /></el-select></el-form-item>
          <el-form-item label="报修地点" prop="locationId"><el-select v-model="form.locationId" placeholder="请选择地点" class="full"><el-option v-for="item in locations" :key="item.locationId" :label="`${item.buildingName} ${item.areaName || ''} ${item.roomNo || ''}`" :value="item.locationId" /></el-select></el-form-item>
        </div>
        <el-form-item label="标题" prop="title"><el-input v-model="form.title" maxlength="128" show-word-limit placeholder="例如：教学楼一楼水龙头漏水" /></el-form-item>
        <el-form-item label="问题描述" prop="description"><el-input v-model="form.description" type="textarea" :rows="6" placeholder="请描述故障现象、发生时间等信息" /></el-form-item>
        <el-form-item label="现场图片"><input type="file" accept="image/jpeg,image/png,image/gif,image/webp" multiple @change="selectFiles" /><small v-if="files.length">已选择 {{ files.length }} 张图片</small></el-form-item>
        <el-button type="primary" :loading="loading" @click="submit">提交报修</el-button>
      </el-form>
    </el-card>
  </section>
</template>

<style scoped>
h2 { margin: 0 0 8px; color: #303133; }
p { margin: 0 0 18px; color: #909399; }
.title-row { display: flex; justify-content: space-between; align-items: center; }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.full { width: 100%; }
small { margin-left: 12px; color: #909399; }
@media (max-width: 640px) { .grid { grid-template-columns: 1fr; gap: 0; } }
</style>
