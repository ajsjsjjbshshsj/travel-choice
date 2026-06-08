<template>
  <el-card shadow="hover">
    <template #header>
      <span style="font-weight: 600">{{ title || '评分雷达图' }}</span>
    </template>
    <div ref="chartRef" style="width: 100%; height: 350px"></div>
  </el-card>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  data: { type: Object, default: () => ({}) },
  title: { type: String, default: '' }
})

const chartRef = ref(null)
let chart = null

const DIMS = [
  { key: 'sceneryScore', name: '景观', max: 100 },
  { key: 'popularityScore', name: '人气', max: 100 },
  { key: 'facilityScore', name: '配套', max: 100 }
]

function renderChart() {
  if (!chart || !props.data) return

  const values = DIMS.map(d => Number(props.data[d.key]) || 0)

  chart.setOption({
    tooltip: {},
    radar: {
      indicator: DIMS.map(d => ({ name: d.name, max: d.max })),
      shape: 'circle',
      splitArea: { areaStyle: { color: ['#fff', '#f5f5f5'] } }
    },
    series: [{
      type: 'radar',
      data: [{
        value: values,
        name: props.data.destinationName || '评分',
        areaStyle: { color: 'rgba(64, 158, 255, 0.2)' },
        lineStyle: { color: '#409EFF', width: 2 },
        itemStyle: { color: '#409EFF' }
      }]
    }]
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
