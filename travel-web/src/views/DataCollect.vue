<template>
  <div class="page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h2 style="margin: 0">数据采集</h2>
      <p class="page-desc">
        天气和路线数据均来自高德 Web 服务 API。天气按目的地 adcode 查询，路线按经纬度查询。
      </p>
    </div>

    <!-- 采集卡片 -->
    <el-row :gutter="20">
      <!-- 天气采集 -->
      <el-col :span="12">
        <el-card shadow="hover" class="collect-card">
          <div class="card-header">
            <el-icon :size="28" color="#409EFF"><Sunny /></el-icon>
            <div>
              <div class="card-title">采集高德天气</div>
              <div class="card-desc">按目的地 adcode 查询未来天气预报，写入 dwd_weather_detail</div>
            </div>
          </div>
          <el-button
            type="primary"
            :loading="weatherLoading"
            @click="handleCollectWeather"
            style="width: 100%; margin-top: 16px"
          >
            采集天气数据
          </el-button>
          <div v-if="weatherResult" class="result-box" :class="weatherResult.success ? 'result-success' : 'result-error'">
            <span>{{ weatherResult.message }}</span>
          </div>
        </el-card>
      </el-col>

      <!-- 路线采集 -->
      <el-col :span="12">
        <el-card shadow="hover" class="collect-card">
          <div class="card-header">
            <el-icon :size="28" color="#67C23A"><Guide /></el-icon>
            <div>
              <div class="card-title">采集高德路线</div>
              <div class="card-desc">按经纬度查询公交路线规划，写入 dwd_route_detail</div>
            </div>
          </div>
          <el-form
            :model="routeForm"
            label-width="72px"
            size="small"
            style="margin-top: 16px"
          >
            <el-form-item label="出发城市">
              <el-input v-model="routeForm.originCity" placeholder="如：北京" />
            </el-form-item>
            <el-form-item label="出发经度">
              <el-input v-model.number="routeForm.originLongitude" placeholder="如：116.4074" />
            </el-form-item>
            <el-form-item label="出发纬度">
              <el-input v-model.number="routeForm.originLatitude" placeholder="如：39.9042" />
            </el-form-item>
            <el-form-item label="旅行日期">
              <el-date-picker
                v-model="routeForm.travelDate"
                type="date"
                placeholder="选择日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-form>
          <el-button
            type="success"
            :loading="routeLoading"
            @click="handleCollectRoute"
            style="width: 100%"
          >
            采集路线数据
          </el-button>
          <div v-if="routeResult" class="result-box" :class="routeResult.success ? 'result-success' : 'result-error'">
            <span>{{ routeResult.message }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <!-- 酒店价格生成 -->
      <el-col :span="12">
        <el-card shadow="hover" class="collect-card">
          <div class="card-header">
            <el-icon :size="28" color="#E6A23C"><OfficeBuilding /></el-icon>
            <div>
              <div class="card-title">生成酒店价格</div>
              <div class="card-desc">根据目的地生成参考价格数据，写入 dwd_hotel_price_detail</div>
            </div>
          </div>
          <el-button
            type="warning"
            :loading="hotelLoading"
            @click="handleGenerateHotel"
            style="width: 100%; margin-top: 16px"
          >
            生成价格数据
          </el-button>
          <div v-if="hotelResult" class="result-box" :class="hotelResult.success ? 'result-success' : 'result-error'">
            <span>{{ hotelResult.message }}</span>
          </div>
        </el-card>
      </el-col>

      <!-- 推荐结果生成 -->
      <el-col :span="12">
        <el-card shadow="hover" class="collect-card">
          <div class="card-header">
            <el-icon :size="28" color="#F56C6C"><Promotion /></el-icon>
            <div>
              <div class="card-title">生成推荐结果</div>
              <div class="card-desc">综合天气、路线、酒店数据，计算目的地推荐评分</div>
            </div>
          </div>
          <el-button
            type="danger"
            :loading="recommendLoading"
            @click="handleGenerateRecommend"
            style="width: 100%; margin-top: 16px"
          >
            生成推荐结果
          </el-button>
          <div v-if="recommendResult" class="result-box" :class="recommendResult.success ? 'result-success' : 'result-error'">
            <span>{{ recommendResult.message }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 操作日志 -->
    <el-card shadow="never" style="margin-top: 20px">
      <template #header>
        <span style="font-weight: 600">操作日志</span>
      </template>
      <el-timeline>
        <el-timeline-item
          v-for="(log, index) in logs"
          :key="index"
          :timestamp="log.time"
          :type="log.type"
          placement="top"
        >
          {{ log.message }}
        </el-timeline-item>
      </el-timeline>
      <el-empty v-if="logs.length === 0" description="暂无操作记录" :image-size="60" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { Sunny, Guide, OfficeBuilding, Promotion } from '@element-plus/icons-vue'
import { collectWeather, collectRoute, generateHotelPrice, calculateRecommend } from '@/api/collect'

// ---- Loading states ----
const weatherLoading = ref(false)
const routeLoading = ref(false)
const hotelLoading = ref(false)
const recommendLoading = ref(false)

// ---- Result states ----
const weatherResult = ref(null)
const routeResult = ref(null)
const hotelResult = ref(null)
const recommendResult = ref(null)

// ---- Operation logs ----
const logs = ref([])

function addLog(message, type = 'success') {
  const now = new Date()
  const time = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
  logs.value.unshift({ message, type, time })
}

// ---- Route form ----
const routeForm = reactive({
  originCity: '北京',
  originLongitude: 116.4074,
  originLatitude: 39.9042,
  travelDate: '2026-07-01'
})

// ---- Weather ----
async function handleCollectWeather() {
  weatherLoading.value = true
  weatherResult.value = null
  try {
    const res = await collectWeather()
    const msg = res.message || `采集完成，共处理 ${res.collectedCount || 0} 个目的地`
    weatherResult.value = { success: true, message: msg }
    ElMessage.success(msg)
    addLog(`天气采集成功: ${msg}`, 'success')
  } catch (e) {
    const msg = e?.response?.data?.message || '天气采集失败，请检查后端服务'
    weatherResult.value = { success: false, message: msg }
    ElMessage.error(msg)
    addLog(`天气采集失败: ${msg}`, 'danger')
    console.error(e)
  } finally {
    weatherLoading.value = false
  }
}

// ---- Route ----
async function handleCollectRoute() {
  if (!routeForm.originCity) {
    ElMessage.warning('请输入出发城市')
    return
  }
  routeLoading.value = true
  routeResult.value = null
  try {
    const res = await collectRoute(routeForm)
    const msg = res.message || `采集完成，共写入 ${res.collectedCount || 0} 条路线`
    routeResult.value = { success: true, message: msg }
    ElMessage.success(msg)
    addLog(`路线采集成功: ${msg}`, 'success')
  } catch (e) {
    const msg = e?.response?.data?.message || '路线采集失败，请检查后端服务'
    routeResult.value = { success: false, message: msg }
    ElMessage.error(msg)
    addLog(`路线采集失败: ${msg}`, 'danger')
    console.error(e)
  } finally {
    routeLoading.value = false
  }
}

// ---- Hotel ----
async function handleGenerateHotel() {
  hotelLoading.value = true
  hotelResult.value = null
  try {
    const res = await generateHotelPrice()
    const msg = res.message || '酒店价格数据生成完成'
    hotelResult.value = { success: true, message: msg }
    ElMessage.success(msg)
    addLog(`酒店价格生成: ${msg}`, 'success')
  } catch (e) {
    const msg = e?.response?.data?.message || '酒店价格生成失败，请检查后端服务'
    hotelResult.value = { success: false, message: msg }
    ElMessage.error(msg)
    addLog(`酒店价格生成失败: ${msg}`, 'danger')
    console.error(e)
  } finally {
    hotelLoading.value = false
  }
}

// ---- Recommend ----
async function handleGenerateRecommend() {
  recommendLoading.value = true
  recommendResult.value = null
  try {
    const res = await calculateRecommend({
      originCity: routeForm.originCity,
      travelStartDate: routeForm.travelDate,
      travelEndDate: routeForm.travelDate,
      userBudget: 5000
    })
    const msg = typeof res === 'string' ? res : (res.message || '推荐结果生成完成')
    recommendResult.value = { success: true, message: msg }
    ElMessage.success(msg)
    addLog(`推荐结果生成: ${msg}`, 'success')
  } catch (e) {
    const msg = e?.response?.data?.message || '推荐结果生成失败，请检查后端服务'
    recommendResult.value = { success: false, message: msg }
    ElMessage.error(msg)
    addLog(`推荐结果生成失败: ${msg}`, 'danger')
    console.error(e)
  } finally {
    recommendLoading.value = false
  }
}
</script>

<style scoped>
.page {
  max-width: 1100px;
}

.page-header {
  margin-bottom: 24px;
}

.page-desc {
  color: #909399;
  font-size: 14px;
  margin: 8px 0 0;
}

.collect-card {
  height: 100%;
}

.card-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.card-desc {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}

.result-box {
  margin-top: 12px;
  padding: 8px 12px;
  border-radius: 4px;
  font-size: 13px;
}

.result-success {
  background: #f0f9eb;
  color: #67c23a;
}

.result-error {
  background: #fef0f0;
  color: #f56c6c;
}
</style>