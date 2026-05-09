<template>
  <div class="user-layout">
    <header class="user-header">
      <div class="header-content">
        <div class="logo">
          <i class="fas fa-wind"></i>
          <span>空气质量查询平台</span>
        </div>
        <nav class="nav-menu">
          <router-link to="/" class="nav-item" :class="{ active: $route.path === '/' }">
            <i class="fas fa-home"></i> 首页
          </router-link>
          <router-link to="/query" class="nav-item" :class="{ active: $route.path === '/query' }">
            <i class="fas fa-search"></i> 空气质量查询
          </router-link>
        <router-link to="/prediction" class="nav-item" :class="{ active: $route.path === '/prediction' }">
          <i class="fas fa-magic"></i> 智能预测
        </router-link>
        <router-link to="/compare" class="nav-item" :class="{ active: $route.path === '/compare' }">
          <i class="fas fa-balance-scale"></i> 历年对比
        </router-link>
      </nav>
        <div class="header-actions">
          <a href="/" class="admin-link">
            <i class="fas fa-cog"></i> 管理后台
          </a>
        </div>
      </div>
    </header>
    
    <main class="user-main">
      <router-view />
    </main>
    
    <footer class="user-footer">
      <p>© 2026 城市空气质量分析与预测系统 | 数据仅供参考</p>
    </footer>
  </div>
</template>

<script>
export default {
  name: 'UserLayout'
}
</script>

<style scoped>
.user-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.user-header {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  box-shadow: 0 2px 20px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.3rem;
  font-weight: bold;
  color: #1a2980;
}

.logo i {
  font-size: 1.5rem;
  color: #26d0ce;
}

.nav-menu {
  display: flex;
  gap: 10px;
}

.nav-item {
  padding: 8px 16px;
  text-decoration: none;
  color: #666;
  border-radius: 20px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 6px;
}

.nav-item:hover {
  background: #f0f0f0;
  color: #1a2980;
}

.nav-item.active {
  background: linear-gradient(135deg, #1a2980 0%, #26d0ce 100%);
  color: white;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.admin-link {
  padding: 8px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  text-decoration: none;
  border-radius: 20px;
  font-size: 0.9rem;
  transition: transform 0.3s ease;
}

.admin-link:hover {
  transform: translateY(-2px);
}

.user-main {
  flex: 1;
  padding: 20px;
}

.user-footer {
  background: rgba(0, 0, 0, 0.3);
  color: white;
  text-align: center;
  padding: 20px;
  font-size: 0.9rem;
}

@media (max-width: 768px) {
  .header-content {
    flex-wrap: wrap;
    height: auto;
    padding: 10px;
  }
  
  .nav-menu {
    order: 3;
    width: 100%;
    justify-content: center;
    margin-top: 10px;
  }
}
</style>
