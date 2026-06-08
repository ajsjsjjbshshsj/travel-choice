<template>
  <el-card shadow="hover">
    <template #header>
      <span style="font-weight: 600; font-size: 1.1rem">智能旅行推荐</span>
    </template>
    <el-form :model="form" label-width="110px">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="出发城市">
            <el-select
              v-model="form.originCity"
              placeholder="选择出发城市"
              filterable
              style="width: 100%"
            >
              <el-option
                v-for="city in cities"
                :key="city.cityCode"
                :label="city.cityName"
                :value="city.cityName"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="预算 (元)">
            <el-input-number v-model="form.userBudget" :min="1000" :max="200000" :step="1000" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="开始日期">
            <el-date-picker v-model="form.travelStartDate" type="date" placeholder="选择开始日期" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="结束日期">
            <el-date-picker v-model="form.travelEndDate" type="date" placeholder="选择结束日期" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="候选目的地">
            <el-select
              v-model="form.destinationCodes"
              multiple
              collapse-tags
              collapse-tags-tooltip
              clearable
              placeholder="不选则系统自动推荐全部目的地"
              style="width: 100%"
            >
              <el-option
                v-for="dest in destinations"
                :key="dest.destinationCode"
                :label="`${dest.destinationName} (${dest.destinationType || ''})`"
                :value="dest.destinationCode"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item>
        <el-button type="primary" size="large" @click="handleSubmit" :loading="loading">
          生成推荐
        </el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { getCities } from '@/api/recommend'
import { getDestinations } from '@/api/destination'

const props = defineProps({
  loading: { type: Boolean, default: false }
})

const emit = defineEmits(['search'])

const cities = ref([])
const destinations = ref([])

const form = reactive({
  originCity: '',
  travelStartDate: '',
  travelEndDate: '',
  userBudget: 5000,
  destinationCodes: []
})

onMounted(async () => {
  try {
    const [cityData, destData] = await Promise.all([
      getCities(),
      getDestinations()
    ])
    cities.value = Array.isArray(cityData) ? cityData : []
    destinations.value = Array.isArray(destData) ? destData : []
  } catch (e) {
    console.error('加载城市/目的地列表失败', e)
  }
})

function handleSubmit() {
  if (!form.originCity) return ElMessage.warning('请选择出发城市')
  if (!form.travelStartDate) return ElMessage.warning('请选择开始日期')
  if (!form.travelEndDate) return ElMessage.warning('请选择结束日期')
  if (!form.userBudget) return ElMessage.warning('请输入预算')

  emit('search', { ...form })
}
</script>
