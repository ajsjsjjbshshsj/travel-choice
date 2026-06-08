<template>
  <div class="page">
    <div class="header">
      <el-button @click="$router.push('/recommend')" :icon="ArrowLeft">返回</el-button>
      <h2>推荐结果</h2>
      <el-tag type="info" v-if="requestId">RequestID: {{ requestId }}</el-tag>
      <el-tag type="success" v-if="results.length">共 {{ results.length }} 个目的地</el-tag>
    </div>

    <el-card shadow="hover" v-loading="loading">
      <RecommendTable :data="results" />
      <el-empty v-if="!loading && !results.length" description="暂无推荐结果" />
    </el-card>

    <div class="chart-section" v-if="results.length">
      <ScoreBarChart :data="topResults" title="Top 5 目的地评分对比" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import RecommendTable from '@/components/RecommendTable.vue'
import ScoreBarChart from '@/components/ScoreBarChart.vue'
import { getRecommendResultsByRequestId } from '@/api/recommend'

const route = useRoute()
const loading = ref(false)
const results = ref([])
const requestId = ref('')

const topResults = computed(() => results.value.slice(0, 5))

onMounted(async () => {
  requestId.value = route.query.requestId || ''
  if (!requestId.value) {
    ElMessage.warning('缺少 requestId 参数')
    return
  }

  loading.value = true
  try {
    const res = await getRecommendResultsByRequestId(requestId.value)
    results.value = Array.isArray(res) ? res : (res.data || [])
  } catch (e) {
    ElMessage.error('查询推荐结果失败')
    console.error(e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page {
  max-width: 1100px;
}
.header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.header h2 {
  margin: 0;
}
.chart-section {
  margin-top: 24px;
}
</style>
