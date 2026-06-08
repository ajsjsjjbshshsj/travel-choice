<template>
  <div class="page" v-loading="loading">
    <!-- 统计卡片 — 第一行 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background: #e8f5e9">
              <el-icon :size="28" color="#67C23A"><LocationFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.destinationCount }}</div>
              <div class="stat-label">目的地数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background: #fff3e0">
              <el-icon :size="28" color="#E6A23C"><Promotion /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stats.recommendTotalCount }}</div>
              <div class="stat-label">推荐结果数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background: #e3f2fd">
              <el-icon :size="28" color="#409EFF"><Monitor /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value" style="color: #67C23A">{{ stats.etlSuccessCount }}</div>
              <div class="stat-label">成功任务数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 统计卡片 — 第二行 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background: #fde8e8">
              <el-icon :size="28" color="#F56C6C"><WarningFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value" style="color: #F56C6C">{{ stats.etlFailedCount }}</div>
              <div class="stat-label">失败任务数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background: #f3e5f5">
              <el-icon :size="28" color="#9C27B0"><TrendCharts /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatScore(stats.avgFinalScore) }}</div>
              <div class="stat-label">平均推荐分</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-item">
            <div class="stat-icon" style="background: #e0f7fa">
              <el-icon :size="28" color="#00BCD4"><UserFilled /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatScore(stats.avgCrowdIndex) }}</div>
              <div class="stat-label">平均拥挤指数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="14">
        <el-card shadow="hover">
          <template #header>
            <span style="font-weight: 600">Top 目的地评分对比</span>
          </template>
          <div ref="barChartRef" style="width: 100%; height: 350px"></div>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="hover">
          <template #header>
            <span style="font-weight: 600">数据质量检查分布</span>
          </template>
          <div ref="pieChartRef" style="width: 100%; height: 350px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { LocationFilled, Monitor, Promotion, WarningFilled, TrendCharts, UserFilled } from '@element-plus/icons-vue'
import { getDashboardStats } from '@/api/dashboard'

const loading = ref(false)
const stats = ref({
  destinationCount: 0,
  etlTotalCount: 0,
  etlSuccessCount: 0,
  etlFailedCount: 0,
  etlRunningCount: 0,
  qualityTotalCount: 0,
  qualityPassCount: 0,
  qualityWarningCount: 0,
  qualityFailedCount: 0,
  recommendTotalCount: 0,
  avgFinalScore: null,
  avgCrowdIndex: null,
  topDestinations: [],
  latestQualityResults: []
})

function formatScore(val) {
  if (val == null) return '--'
  return Number(val).toFixed(1)
}

// Charts
const barChartRef = ref(null)
const pieChartRef = ref(null)
let barChart = null
let pieChart = null

function renderBarChart() {
  if (!barChart || !stats.value.topDestinations?.length) return
  const dests = stats.value.topDestinations
  const names = dests.map(d => d.destinationName)
  barChart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { top: 0 },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: names },
    yAxis: { type: 'value', max: 100 },
    series: [
      { name: '景观', type: 'bar', data: dests.map(d => Number(d.sceneryScore) || 0) },
      { name: '人气', type: 'bar', data: dests.map(d => Number(d.popularityScore) || 0) },
      { name: '配套', type: 'bar', data: dests.map(d => Number(d.facilityScore) || 0) }
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
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: true, formatter: '{b}\n{c}' },
      data: [
        { value: s.qualityPassCount, name: 'PASS', itemStyle: { color: '#67C23A' } },
        { value: s.qualityWarningCount, name: 'WARNING', itemStyle: { color: '#E6A23C' } },
        { value: s.qualityFailedCount, name: 'FAILED', itemStyle: { color: '#F56C6C' } }
      ]
    }]
  }, true)
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getDashboardStats()
    stats.value = res || stats.value
  } catch (e) {
    ElMessage.error('获取看板数据失败')
    console.error(e)
  } finally {
    loading.value = false
  }

  nextTick(() => {
    if (barChartRef.value) {
      barChart = echarts.init(barChartRef.value)
      renderBarChart()
    }
    if (pieChartRef.value) {
      pieChart = echarts.init(pieChartRef.value)
      renderPieChart()
    }
  })

  window.addEventListener('resize', handleResize)
})

function handleResize() {
  barChart?.resize()
  pieChart?.resize()
}

watch(() => stats.value, () => {
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
  margin-bottom: 20px;
}

.stat-card {
  cursor: default;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}
</style>
