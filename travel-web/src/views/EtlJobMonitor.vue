<template>
  <div class="page">
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="mini-stat">
          <div class="mini-stat-value">{{ jobs.length }}</div>
          <div class="mini-stat-label">Total Jobs</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="mini-stat">
          <div class="mini-stat-value success">{{ successCount }}</div>
          <div class="mini-stat-label">Success</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="mini-stat">
          <div class="mini-stat-value danger">{{ failedCount }}</div>
          <div class="mini-stat-label">Failed</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="mini-stat">
          <div class="mini-stat-value running">{{ runningCount }}</div>
          <div class="mini-stat-label">Running</div>
        </el-card>
      </el-col>
    </el-row>

    <div class="toolbar">
      <el-select v-model="statusFilter" placeholder="Status" clearable style="width: 160px">
        <el-option label="All" value="" />
        <el-option label="Success" value="SUCCESS" />
        <el-option label="Failed" value="FAILED" />
        <el-option label="Running" value="RUNNING" />
      </el-select>
      <div class="toolbar-right">
        <el-switch v-model="autoRefresh" active-text="Auto refresh" />
        <el-button type="primary" :icon="Refresh" @click="fetchData" :loading="loading">Refresh</el-button>
      </div>
    </div>

    <el-table :data="filteredJobs" stripe v-loading="loading" style="width: 100%">
      <el-table-column prop="jobName" label="Job Name" width="190" />
      <el-table-column prop="jobType" label="Type" width="110" />
      <el-table-column prop="requestId" label="Request ID" min-width="180" show-overflow-tooltip />
      <el-table-column prop="jobDesc" label="Description" min-width="180" show-overflow-tooltip />
      <el-table-column label="Status" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Start Time" width="170">
        <template #default="{ row }">{{ formatTime(row.startTime) }}</template>
      </el-table-column>
      <el-table-column label="End Time" width="170">
        <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
      </el-table-column>
      <el-table-column label="Duration" width="110" align="center">
        <template #default="{ row }">{{ formatDuration(row) }}</template>
      </el-table-column>
      <el-table-column prop="rowCount" label="Rows" width="90" align="center" />
      <el-table-column label="Trigger" width="100" align="center">
        <template #default="{ row }">
          <el-tag type="info" size="small">{{ row.triggerType || '--' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="Error" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <span v-if="row.errorMessage" class="danger">{{ row.errorMessage }}</span>
          <span v-else class="muted">--</span>
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
  if (status === 'RUNNING') return 'warning'
  return 'info'
}

function formatTime(value) {
  if (!value) return '--'
  if (Array.isArray(value)) {
    const [y, m, d, h = 0, mi = 0, s = 0] = value
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(mi).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }
  return String(value).replace('T', ' ').slice(0, 19)
}

function calcDuration(start, end) {
  const startDate = new Date(Array.isArray(start) ? formatTime(start) : start)
  const endDate = new Date(Array.isArray(end) ? formatTime(end) : end)
  return Math.max(0, Math.round((endDate - startDate) / 1000))
}

function formatDuration(row) {
  if (row.durationSeconds != null) return `${row.durationSeconds}s`
  if (row.startTime && row.endTime) return `${calcDuration(row.startTime, row.endTime)}s`
  return '--'
}

async function fetchData() {
  loading.value = true
  try {
    jobs.value = await getEtlJobs()
  } catch (e) {
    ElMessage.error('Failed to load ETL jobs')
    console.error(e)
  } finally {
    loading.value = false
  }
}

watch(autoRefresh, (enabled) => {
  if (enabled) {
    refreshTimer = setInterval(fetchData, 30000)
  } else if (refreshTimer) {
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
  font-size: 30px;
  font-weight: 700;
  color: #303133;
}

.mini-stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.success {
  color: #67C23A;
}

.danger {
  color: #F56C6C;
}

.running {
  color: #409EFF;
}

.muted {
  color: #C0C4CC;
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
  gap: 16px;
}
</style>
