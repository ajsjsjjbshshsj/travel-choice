<template>
  <div class="page">
    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="mini-stat">
          <div class="mini-stat-value">{{ jobs.length }}</div>
          <div class="mini-stat-label">总任务数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="mini-stat">
          <div class="mini-stat-value" style="color: #67C23A">{{ successCount }}</div>
          <div class="mini-stat-label">成功</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="mini-stat">
          <div class="mini-stat-value" style="color: #F56C6C">{{ failedCount }}</div>
          <div class="mini-stat-label">失败</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="mini-stat">
          <div class="mini-stat-value" style="color: #409EFF">{{ runningCount }}</div>
          <div class="mini-stat-label">运行中</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 操作栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 160px">
          <el-option label="全部" value="" />
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
          <el-option label="运行中" value="RUNNING" />
        </el-select>
      </div>
      <div class="toolbar-right">
        <el-switch v-model="autoRefresh" active-text="自动刷新" style="margin-right: 16px" />
        <el-button type="primary" :icon="Refresh" @click="fetchData" :loading="loading">刷新</el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <el-table :data="filteredJobs" stripe v-loading="loading" style="width: 100%">
      <el-table-column prop="jobName" label="任务名称" width="180" />
      <el-table-column prop="jobDesc" label="描述" min-width="160" show-overflow-tooltip />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" width="170">
        <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
      </el-table-column>
      <el-table-column label="结束时间" width="170">
        <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
      </el-table-column>
      <el-table-column label="耗时" width="90" align="center">
        <template #default="{ row }">
          <span v-if="row.startTime && row.endTime">{{ calcDuration(row.startTime, row.endTime) }}s</span>
          <span v-else style="color: #409EFF">--</span>
        </template>
      </el-table-column>
      <el-table-column prop="rowCount" label="处理行数" width="100" align="center" />
      <el-table-column label="触发方式" width="100" align="center">
        <template #default="{ row }">
          <el-tag type="info" size="small">{{ row.triggerType || '--' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="错误信息" min-width="180">
        <template #default="{ row }">
          <span v-if="row.errorMessage" style="color: #F56C6C">{{ row.errorMessage }}</span>
          <span v-else style="color: #C0C4CC">--</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getEtlJobs } from '@/api/job'

const loading = ref(false)
const jobs = ref([])
const statusFilter = ref('')
const autoRefresh = ref(false)
let refreshTimer = null

const successCount = computed(() => jobs.value.filter(j => j.status === 'SUCCESS').length)
const failedCount = computed(() => jobs.value.filter(j => j.status === 'FAILED').length)
const runningCount = computed(() => jobs.value.filter(j => j.status === 'RUNNING').length)

const filteredJobs = computed(() => {
  if (!statusFilter.value) return jobs.value
  return jobs.value.filter(j => j.status === statusFilter.value)
})

function statusTagType(status) {
  if (status === 'SUCCESS') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'RUNNING') return ''
  return 'info'
}

function formatTime(t) {
  if (!t) return '--'
  // Handle ISO or array format
  if (Array.isArray(t)) {
    const [y, m, d, h = 0, mi = 0, s = 0] = t
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(mi).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }
  return String(t).replace('T', ' ').slice(0, 19)
}

function calcDuration(start, end) {
  try {
    const s = new Date(Array.isArray(start) ? formatTime(start) : start)
    const e = new Date(Array.isArray(end) ? formatTime(end) : end)
    return Math.round((e - s) / 1000)
  } catch {
    return '--'
  }
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getEtlJobs()
    jobs.value = Array.isArray(res) ? res : (res.data || [])
  } catch (e) {
    ElMessage.error('获取 ETL 任务列表失败')
    console.error(e)
  } finally {
    loading.value = false
  }
}

watch(autoRefresh, (val) => {
  if (val) {
    refreshTimer = setInterval(fetchData, 30000)
  } else {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})

onMounted(fetchData)

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped>
.page {
  max-width: 1400px;
}

.stat-row {
  margin-bottom: 20px;
}

.mini-stat {
  text-align: center;
  padding: 8px 0;
}

.mini-stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #303133;
}

.mini-stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.toolbar-right {
  display: flex;
  align-items: center;
}
</style>
