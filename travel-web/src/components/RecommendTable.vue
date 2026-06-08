01           98                                                                                                               <template>
  <el-table :data="data" stripe highlight-current-row style="width: 100%">
    <el-table-column label="排名" width="70" align="center">
      <template #default="{ row }">
        <el-tag :type="row.recommendRank <= 3 ? 'danger' : 'info'" effect="dark" round size="small">
          {{ row.recommendRank }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="destinationName" label="目的地" width="110" />
    <el-table-column label="综合评分" width="100" align="center">
      <template #default="{ row }">
        <span style="font-weight: 600; color: #409EFF">{{ row.finalScore }}</span>
      </template>
    </el-table-column>
    <el-table-column label="预计花费" width="110" align="right">
      <template #default="{ row }">¥{{ Number(row.estimatedTotalCost).toLocaleString() }}</template>
    </el-table-column>
    <el-table-column label="交通费用" width="100" align="right">
      <template #default="{ row }">¥{{ Number(row.trafficCost).toLocaleString() }}</template>
    </el-table-column>
    <el-table-column label="酒店费用" width="100" align="right">
      <template #default="{ row }">¥{{ Number(row.hotelCost).toLocaleString() }}</template>
    </el-table-column>
    <el-table-column label="天气评分" width="90" align="center">
      <template #default="{ row }">
        <el-tag :type="row.weatherScore >= 80 ? 'success' : row.weatherScore >= 60 ? 'warning' : 'danger'" size="small">
          {{ row.weatherScore }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="拥挤指数" width="90" align="center">
      <template #default="{ row }">
        <el-tag :type="row.crowdIndex <= 40 ? 'success' : row.crowdIndex <= 70 ? 'warning' : 'danger'" size="small" effect="plain">
          {{ row.crowdIndex }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="recommendReason" label="推荐理由" min-width="200" show-overflow-tooltip />
  </el-table>
</template>

<script setup>
defineProps({
  data: { type: Array, default: () => [] }
})
</script>
