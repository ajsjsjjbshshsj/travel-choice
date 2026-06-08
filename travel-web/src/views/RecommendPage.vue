<template>
  <div class="page">
    <SearchForm :loading="loading" @search="handleSearch" />

    <div class="tips">
      <el-alert
        title="使用说明"
        type="info"
        :closable="false"
        show-icon
      >
        选择出发城市、旅行日期和预算后，点击「生成推荐」。可选择候选目的地（不选则系统自动推荐全部）。系统会动态采集路线、天气、酒店数据后计算推荐评分。
      </el-alert>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import SearchForm from '@/components/SearchForm.vue'
import { calculateRecommend } from '@/api/recommend'

const router = useRouter()
const loading = ref(false)

async function handleSearch(formData) {
  loading.value = true
  try {
    // Step 1: 触发推荐计算 (Java 编排 → Python → MySQL ads 表)
    const res = await calculateRecommend(formData)
    const requestId = res.requestId

    // Step 2: 跳转到结果页，携带 requestId
    router.push({
      name: 'RecommendResult',
      query: { requestId }
    })
  } catch (e) {
    ElMessage.error('推荐计算失败，请检查后端服务')
    console.error(e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page {
  max-width: 700px;
}
.tips {
  margin-top: 24px;
}
</style>
