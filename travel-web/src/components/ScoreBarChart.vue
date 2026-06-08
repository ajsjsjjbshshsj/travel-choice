<template>
  <el-card shadow="hover">
    <template #header>
      <span style="font-weight: 600">{{ title || '评分对比' }}</span>
    </template>
    <div ref="chartRef" style="width: 100%; height: 350px"></div>
  </el-card>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  data: { type: Array, default: () => [] },
  title: { type: String, default: '' }
})

const chartRef = ref(null)
let chart = null

const DIMS = [
  { key: 'sceneryScore', name: '景观' },
  { key: 'trafficScore', name: '交通' },
  { key: 'hotelScore', name: '住宿' },
  { key: 'weatherScore', name: '天气' },
  { key: 'costScore', name: '性价比' },
  { key: 'crowdScore', name: '人流' }
]

function renderChart() {
  if (!chart || !props.data.length) return

  const categories = DIMS.map(d => d.name)
  const series = props.data.map(item => ({
    name: item.destinationName,
    type: 'bar',
    data: DIMS.map(d => Number(item[d.key]) || 0)
  }))

  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { top: 0, type: 'scroll' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: categories },
    yAxis: { type: 'value', max: 100 },
    series
  }, true)
}

onMounted(() => {
  nextTick(() => {
    if (chartRef.value) {
      chart = echarts.init(chartRef.value)
      renderChart()
    }
  })
  window.addEventListener('resize', () => chart?.resize())
})

onBeforeUnmount(() => {
  chart?.dispose()
})

watch(() => props.data, renderChart, { deep: true })
</script>
