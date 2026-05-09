<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="logo">
        <i class="fas fa-wind"></i>
        <span>空气质量监测</span>
      </div>
      <nav class="menu">
        <router-link to="/" :class="{ active: $route.path === '/' }">
          <el-icon><Monitor /></el-icon>
          <span>数据概览</span>
        </router-link>
        <router-link to="/realtime" :class="{ active: $route.path === '/realtime' }">
          <el-icon><Odometer /></el-icon>
          <span>实时监测</span>
        </router-link>
        <router-link to="/history" :class="{ active: $route.path === '/history' }">
          <el-icon><DataLine /></el-icon>
          <span>历史趋势</span>
        </router-link>
        <router-link to="/correlation" :class="{ active: $route.path === '/correlation' }">
          <el-icon><Share /></el-icon>
          <span>相关性分析</span>
        </router-link>
        <router-link to="/prediction" :class="{ active: $route.path === '/prediction' }">
          <el-icon><MagicStick /></el-icon>
          <span>预测分析</span>
        </router-link>
        <router-link to="/rf-prediction" :class="{ active: $route.path === '/rf-prediction' }">
          <el-icon><TrendCharts /></el-icon>
          <span>随机森林预测</span>
        </router-link>
        <router-link to="/query" :class="{ active: $route.path === '/query' }">
          <el-icon><Search /></el-icon>
          <span>数据查询</span>
        </router-link>
        <router-link to="/manage" :class="{ active: $route.path === '/manage' }">
          <el-icon><Setting /></el-icon>
          <span>数据管理</span>
        </router-link>
        <router-link to="/realtime-manage" :class="{ active: $route.path === '/realtime-manage' }">
          <el-icon><Edit /></el-icon>
          <span>实时数据修改</span>
        </router-link>
      </nav>
      <div class="sidebar-footer">
        <p>ZZU 大数据课程设计</p>
      </div>
    </aside>
    <main class="main-content">
      <header class="header">
        <h1>{{ pageTitle }}</h1>
        <div class="header-right">
          <span class="time">{{ currentTime }}</span>
        </div>
      </header>
      <div class="content">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { Search, Setting, TrendCharts, Edit } from '@element-plus/icons-vue'

const route = useRoute()
const currentTime = ref('')
let timer = null

const pageTitle = computed(() => {
  const titles = {
    '/': '数据概览',
    '/realtime': '实时监测大屏',
    '/history': '历史数据分析',
    '/correlation': '污染物相关性分析',
    '/prediction': 'AQI预测分析',
    '/rf-prediction': '随机森林智能预测',
    '/query': '空气质量数据查询',
    '/manage': '数据管理',
    '/realtime-manage': '实时监测数据修改'
  }
  return titles[route.path] || '空气质量监测'
})

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
})

onUnmounted(() => {
  clearInterval(timer)
})
</script>

<style scoped>
.layout {
  display: flex;
  min-height: 100vh;
}

.sidebar {
  width: 240px;
  background: linear-gradient(180deg, #1a2332 0%, #0f1419 100%);
  border-right: 1px solid #2a3441;
  display: flex;
  flex-direction: column;
  position: fixed;
  height: 100vh;
}

.logo {
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #2a3441;
}

.logo i {
  font-size: 28px;
  color: #00d4ff;
}

.logo span {
  font-size: 18px;
  font-weight: bold;
  background: linear-gradient(90deg, #00d4ff, #00ff88);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.menu {
  flex: 1;
  padding: 20px 0;
}

.menu a {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 15px 20px 15px 24px;
  color: #94a3b8;
  text-decoration: none;
  transition: all 0.3s;
  border-left: 3px solid transparent;
}

.menu a span {
  flex: 1;
}

.menu a:hover {
  color: #fff;
  background: rgba(0, 212, 255, 0.1);
}

.menu a.active {
  color: #00d4ff;
  background: rgba(0, 212, 255, 0.1);
  border-left-color: #00d4ff;
}

.sidebar-footer {
  padding: 20px;
  border-top: 1px solid #2a3441;
  text-align: center;
  color: #64748b;
  font-size: 12px;
}

.main-content {
  flex: 1;
  margin-left: 240px;
  display: flex;
  flex-direction: column;
}

.header {
  height: 64px;
  background: #1a2332;
  border-bottom: 1px solid #2a3441;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.header h1 {
  font-size: 20px;
  font-weight: 500;
}

.header-right .time {
  color: #94a3b8;
  font-family: monospace;
}

.content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}
</style>
