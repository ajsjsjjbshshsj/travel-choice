<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="sidebar">
      <div class="logo-area">
        <span v-if="!isCollapse" class="logo-text">旅行决策平台</span>
        <span v-else class="logo-text-mini">旅</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :router="true"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
        class="sidebar-menu"
      >
        <el-menu-item index="/">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>数据看板</template>
        </el-menu-item>
        <el-menu-item index="/recommend">
          <el-icon><Promotion /></el-icon>
          <template #title>智能推荐</template>
        </el-menu-item>
        <el-menu-item index="/destinations">
          <el-icon><LocationFilled /></el-icon>
          <template #title>目的地列表</template>
        </el-menu-item>
        <el-menu-item index="/data-collect">
          <el-icon><UploadFilled /></el-icon>
          <template #title>数据采集</template>
        </el-menu-item>
        <el-menu-item index="/etl-monitor">
          <el-icon><Monitor /></el-icon>
          <template #title>ETL 监控</template>
        </el-menu-item>
        <el-menu-item index="/data-quality">
          <el-icon><Checked /></el-icon>
          <template #title>数据质量</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="main-container">
      <!-- 顶部 Header -->
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  DataAnalysis, Promotion, LocationFilled, Monitor, Checked,
  Fold, Expand, UploadFilled
} from '@element-plus/icons-vue'

const route = useRoute()
const isCollapse = ref(false)

const activeMenu = computed(() => {
  const path = route.path
  // 子页面（如 /result, /destinations/:code）高亮其父菜单
  if (path.startsWith('/destinations/') && path !== '/destinations') {
    return '/destinations'
  }
  if (path === '/result') {
    return '/recommend'
  }
  return path
})

const currentTitle = computed(() => route.meta?.title || '')
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  transition: width 0.3s;
  overflow: hidden;
}

.logo-area {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.logo-text {
  color: #fff;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 2px;
  white-space: nowrap;
}

.logo-text-mini {
  color: #fff;
  font-size: 20px;
  font-weight: 700;
}

.sidebar-menu {
  border-right: none;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 220px;
}

.main-container {
  display: flex;
  flex-direction: column;
}

.header {
  height: 60px;
  display: flex;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
  transition: color 0.2s;
}

.collapse-btn:hover {
  color: #409EFF;
}

.main-content {
  background: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>
