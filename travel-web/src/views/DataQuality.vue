<template>
  <div class="page">
    <!-- 操作栏 -->
    <div class="toolbar">
      <h2 style="margin: 0">数据质量检查</h2>
      <el-button type="primary" :icon="VideoPlay" @click="handleRunCheck" :loading="running">
        执行质量检查
      </el-button>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="8">
        <el-card shadow="hover" class="mini-stat" style="border-left: 4px solid #67C23A">
          <div class="mini-stat-value" style="color: #67C23A">{{ passCount }}</div>
          <div class="mini-stat-label">PASS 通过</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="mini-stat" style="border-left: 4px solid #E6A23C">
          <div class="mini-stat-value" style="color: #E6A23C">{{ warningCount }}</div>
          <div class="mini-stat-label">WARNING 警告</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="mini-stat" style="border-left: 4px solid #F56C6C">
          <div class="mini-stat-value" style="color: #F56C6C">{{ failedCount }}</div>
          <div class="mini-stat-label">FAILED 失败</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 数据表格 -->
    <el-table :data="results" stripe v-loading="loading" style="width: 100%">
      <el-table-column prop="tableName" label="表名" width="220" />
      <el-table-column prop="ruleName" label="规则名称" width="200" />
      <el-table-column prop="ruleDesc" label="规则描述" min-width="180" show-overflow-tooltip />
      <el-table-column label="检查结果" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="resultTagType(row.checkResult)" size="small">
            {{ row.checkResult }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="errorCount" label="错误数量" width="100" align="center">
        <template #default="{ row }">
          <span :style="{ color: row.errorCount > 0 ? '#F56C6C' : '#67C23A', fontWeight: 600 }">
            {{ row.errorCount }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="检查时间" width="170">
        <template #default="{ row }">{{ formatTime(row.checkTime) }}</template>
      </el-table-column>
      <el-table-column label="错误信息" min-width="200">
        <template #default="{ row }">
          <span v-if="row.errorMessage" style="color: #F56C6C">{{ row.errorMessage }}</span>
          <span v-else style="color: #C0C4CC">--</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { VideoPlay } from '@element-plus/icons-vue'
import { getQualityResults, runQualityChecks } from '@/api/quality'

const loading = ref(false)
const running = ref(false)
const results = ref([])

const passCount = computed(() => results.value.filter(r => r.checkResult === 'PASS').length)
const warningCount = computed(() => results.value.filter(r => r.checkResult === 'WARNING').length)
const failedCount = computed(() => results.value.filter(r => r.checkResult === 'FAILED').length)

function resultTagType(result) {
  if (result === 'PASS') return 'success'
  if (result === 'WARNING') return 'warning'
  if (result === 'FAILED') return 'danger'
  return 'info'
}

function formatTime(t) {
  if (!t) return '--'
  if (Array.isArray(t)) {
    const [y, m, d, h = 0, mi = 0, s = 0] = t
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(mi).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }
  return String(t).replace('T', ' ').slice(0, 19)
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getQualityResults()
    results.value = Array.isArray(res) ? res : (res.data || [])
  } catch (e) {
    ElMessage.error('获取质量检查结果失败')
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function handleRunCheck() {
  running.value = true
  try {
    await runQualityChecks()
    ElMessage.success('质量检查执行完成')
    await fetchData()
  } catch (e) {
    ElMessage.error('质量检查执行失败')
    console.error(e)
  } finally {
    running.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.page {
  max-width: 1400px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
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
}

.mini-stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}
</style>
