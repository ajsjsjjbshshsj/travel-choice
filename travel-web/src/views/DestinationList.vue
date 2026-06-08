<template>
  <div class="page">
    <h2>目的地列表</h2>

    <el-table
      :data="list"
      stripe
      v-loading="loading"
      style="width: 100%; cursor: pointer"
      @row-click="handleRowClick"
    >
      <el-table-column prop="destinationName" label="目的地名称" width="140" />
      <el-table-column prop="city" label="城市" width="100" />
      <el-table-column prop="destinationType" label="类型" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ row.destinationType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="景色评分" width="100" align="center">
        <template #default="{ row }">
          <span style="color: #67C23A; font-weight: 600">{{ row.sceneryScore }}</span>
        </template>
      </el-table-column>
      <el-table-column label="热度评分" width="100" align="center">
        <template #default="{ row }">
          <span style="color: #E6A23C; font-weight: 600">{{ row.popularityScore }}</span>
        </template>
      </el-table-column>
      <el-table-column label="配套评分" width="100" align="center">
        <template #default="{ row }">
          <span style="color: #409EFF; font-weight: 600">{{ row.facilityScore }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="简介" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="80" align="center">
        <template #default="{ row }">
          <el-button type="primary" link size="small">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDestinations } from '@/api/destination'

const router = useRouter()
const loading = ref(false)
const list = ref([])

function handleRowClick(row) {
  router.push(`/destinations/${row.destinationCode}`)
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await getDestinations()
    list.value = Array.isArray(res) ? res : (res.data || [])
  } catch (e) {
    ElMessage.error('获取目的地列表失败')
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
h2 {
  margin-bottom: 20px;
}
</style>
