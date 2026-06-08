<template>
  <div class="page" v-loading="loading">
    <!-- 顶部操作栏 -->
    <div class="header">
      <el-button @click="$router.push('/destinations')" :icon="ArrowLeft">返回列表</el-button>
      <h2>{{ destination.destinationName || '目的地详情' }}</h2>
      <el-tag v-if="destination.destinationType" size="large">
        {{ destination.destinationType }}
      </el-tag>
    </div>

    <template v-if="destination.destinationCode">
      <el-row :gutter="20">
        <!-- 基本信息 -->
        <el-col :span="14">
          <el-card shadow="hover">
            <template #header>
              <span style="font-weight: 600">基本信息</span>
            </template>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="目的地编码">{{ destination.destinationCode }}</el-descriptions-item>
              <el-descriptions-item label="目的地名称">{{ destination.destinationName }}</el-descriptions-item>
              <el-descriptions-item label="国家">{{ destination.country || '--' }}</el-descriptions-item>
              <el-descriptions-item label="省份">{{ destination.province || '--' }}</el-descriptions-item>
              <el-descriptions-item label="城市">{{ destination.city || '--' }}</el-descriptions-item>
              <el-descriptions-item label="类型">
                <el-tag size="small">{{ destination.destinationType }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="经度">{{ destination.longitude || '--' }}</el-descriptions-item>
              <el-descriptions-item label="纬度">{{ destination.latitude || '--' }}</el-descriptions-item>
              <el-descriptions-item label="简介" :span="2">
                {{ destination.description || '暂无描述' }}
              </el-descriptions-item>
            </el-descriptions>
          </el-card>
        </el-col>

        <!-- 评分雷达图 -->
        <el-col :span="10">
          <ScoreRadarChart :data="destination" title="综合评分" />
        </el-col>
      </el-row>

      <!-- 各维度评分 -->
      <el-card shadow="hover" style="margin-top: 20px">
        <template #header>
          <span style="font-weight: 600">各维度评分</span>
        </template>
        <el-row :gutter="24">
          <el-col :span="8">
            <div class="score-item">
              <div class="score-label">
                <span>景观评分</span>
                <span class="score-value" style="color: #67C23A">{{ destination.sceneryScore }}</span>
              </div>
              <el-progress :percentage="Number(destination.sceneryScore) || 0" :stroke-width="12" color="#67C23A" />
            </div>
          </el-col>
          <el-col :span="8">
            <div class="score-item">
              <div class="score-label">
                <span>人气评分</span>
                <span class="score-value" style="color: #E6A23C">{{ destination.popularityScore }}</span>
              </div>
              <el-progress :percentage="Number(destination.popularityScore) || 0" :stroke-width="12" color="#E6A23C" />
            </div>
          </el-col>
          <el-col :span="8">
            <div class="score-item">
              <div class="score-label">
                <span>配套评分</span>
                <span class="score-value" style="color: #409EFF">{{ destination.facilityScore }}</span>
              </div>
              <el-progress :percentage="Number(destination.facilityScore) || 0" :stroke-width="12" color="#409EFF" />
            </div>
          </el-col>
        </el-row>
      </el-card>
    </template>

    <el-empty v-if="!loading && !destination.destinationCode" description="未找到目的地信息" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import ScoreRadarChart from '@/components/ScoreRadarChart.vue'
import { getDestinationByCode } from '@/api/destination'

const route = useRoute()
const loading = ref(false)
const destination = ref({})

onMounted(async () => {
  const code = route.params.code
  if (!code) return

  loading.value = true
  try {
    const res = await getDestinationByCode(code)
    destination.value = res || {}
  } catch (e) {
    ElMessage.error('获取目的地详情失败')
    console.error(e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page {
  max-width: 1200px;
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

.score-item {
  margin-bottom: 20px;
}

.score-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
  color: #606266;
}

.score-value {
  font-size: 18px;
  font-weight: 700;
}
</style>
