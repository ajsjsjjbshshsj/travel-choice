<template>
  <div class="page" v-loading="loading">
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <el-icon :size="26" color="#67C23A"><LocationFilled /></el-icon>
            <div>
              <div class="stat-value">{{ stats.destinationCount }}</div>
              <div class="stat-label">Destinations</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <el-icon :size="26" color="#E6A23C"><Promotion /></el-icon>
            <div>
              <div class="stat-value">{{ stats.recommendTotalCount }}</div>
              <div class="stat-label">Recommend Results</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <el-icon :size="26" color="#409EFF"><Monitor /></el-icon>
            <div>
              <div class="stat-value success">{{ stats.etlSuccessCount }}</div>
              <div class="stat-label">ETL Success</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <el-icon :size="26" color="#F56C6C"><WarningFilled /></el-icon>
            <div>
              <div class="stat-value danger">{{ stats.etlFailedCount }}</div>
              <div class="stat-label">ETL Failed</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ formatScore(stats.avgFinalScore) }}</div>
          <div class="stat-label">Average Final Score</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ formatScore(stats.avgCrowdIndex) }}</div>
          <div class="stat-label">Average Crowd Index</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-tag :type="stats.redisStatus === 'UP' ? 'success' : 'danger'">{{ stats.redisStatus || 'UNKNOWN' }}</el-tag>
          <div class="stat-label status-label">Redis Status</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="stat-card">
          <el-tag type="info">{{ stats.schedulerStatus || 'UNKNOWN' }}</el-tag>
          <div class="stat-label status-label">Scheduler Status</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>
            <span class="card-title">Top Destination Scores</span>
          </template>
          <div ref="barChartRef" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header>
            <span class="card-title">Data Quality Distribution</span>
          </template>
          <div ref="pieChartRef" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="hover" style="margin-top: 20px">
      <template #header>
        <span class="card-title">Realtime Hot Destination TopN</span>
      </template>
      <el-table :data="hotDestinations" stripe style="width: 100%">
        <el-table-column prop="rankNo" label="#" width="70" align="center" />
        <el-table-column prop="destinationName" label="Destination" min-width="160" />
        <el-table-column prop="destinationCode" label="Code" width="120" />
        <el-table-column prop="requestCount" label="Requests" width="120" align="center" />
        <el-table-column prop="windowEnd" label="Window End" min-width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { LocationFilled, Monitor, Promotion, WarningFilled } from '@element-plus/icons-vue'
import { getDashboardStats } from '@/api/dashboard'
import { getHotDestinationTopN } from '@/api/hotDestination'

const loading = ref(false)
const stats = ref({
  destinationCount: 0,
  etlTotalCount: 0,
  etlSuccessCount: 0,
  etlFailedCount: 0,
  etlRunningCount: 0,
  qualityPassCount: 0,
  qualityWarningCount: 0,
  qualityFailedCount: 0,
  recommendTotalCount: 0,
  avgFinalScore: null,
  avgCrowdIndex: null,
  topDestinations: [],
  latestQualityResults: [],
  redisStatus: 'UNKNOWN',
  schedulerStatus: 'UNKNOWN'
})

const barChartRef = ref(null)
const pieChartRef = ref(null)
const hotDestinations = ref([])
let barChart = null
let pieChart = null

function formatScore(val) {
  if (val == null) return '--'
  return Number(val).toFixed(1)
}

function renderBarChart() {
  if (!barChart) return
  const dests = stats.value.topDestinations || []
  barChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { top: 0 },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: dests.map(d => d.destinationName) },
    yAxis: { type: 'value', max: 100 },
    series: [
      { name: 'Scenery', type: 'bar', data: dests.map(d => Number(d.sceneryScore) || 0) },
      { name: 'Popularity', type: 'bar', data: dests.map(d => Number(d.popularityScore) || 0) },
      { name: 'Facility', type: 'bar', data: dests.map(d => Number(d.facilityScore) || 0) }
    ]
  }, true)
}

function renderPieChart() {
  if (!pieChart) return
  const s = stats.value
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      label: { show: true, formatter: '{b}\n{c}' },
      data: [
        { value: s.qualityPassCount, name: 'PASS', itemStyle: { color: '#67C23A' } },
        { value: s.qualityWarningCount, name: 'WARNING', itemStyle: { color: '#E6A23C' } },
        { value: s.qualityFailedCount, name: 'FAILED', itemStyle: { color: '#F56C6C' } }
      ]
    }]
  }, true)
}

async function fetchStats() {
  loading.value = true
  try {
    const [dashboardStats, hotTopN] = await Promise.all([
      getDashboardStats(),
      getHotDestinationTopN(10)
    ])
    stats.value = dashboardStats
    hotDestinations.value = Array.isArray(hotTopN) ? hotTopN : []
  } catch (e) {
    ElMessage.error('Failed to load dashboard stats')
    console.error(e)
  } finally {
    loading.value = false
  }
}

function handleResize() {
  barChart?.resize()
  pieChart?.resize()
}

onMounted(async () => {
  await fetchStats()
  await nextTick()
  if (barChartRef.value) barChart = echarts.init(barChartRef.value)
  if (pieChartRef.value) pieChart = echarts.init(pieChartRef.value)
  renderBarChart()
  renderPieChart()
  window.addEventListener('resize', handleResize)
})

watch(stats, () => {
  renderBarChart()
  renderPieChart()
}, { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.page {
  max-width: 1200px;
}

.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  min-height: 92px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 14px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  color: #303133;
}

.success {
  color: #67C23A;
}

.danger {
  color: #F56C6C;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.status-label {
  margin-top: 10px;
}

.card-title {
  font-weight: 600;
}

.chart {
  width: 100%;
  height: 350px;
}
</style>
