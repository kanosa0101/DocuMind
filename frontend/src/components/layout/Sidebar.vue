<template>
  <aside class="sidebar glass-card">
    <!-- Logo -->
    <div class="sidebar-logo">
      <div class="logo-icon">
        <svg viewBox="0 0 24 24" fill="currentColor">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8l-6-6zm-1 2l5 5h-5V4zm-3 9v6H9v-6H7v-2h6v2h-2z"/>
        </svg>
      </div>
      <span class="logo-text">DocuMind</span>
    </div>

    <!-- 导航菜单 -->
    <nav class="sidebar-nav">
      <router-link
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="nav-item"
        :class="{ active: isActive(item.path) }"
      >
        <span class="nav-icon">{{ item.icon }}</span>
        <span class="nav-label">{{ item.label }}</span>
      </router-link>
    </nav>

    <!-- 底部用户信息 -->
    <div class="sidebar-footer">
      <!-- 存储进度 -->
      <div class="storage-section">
        <div class="storage-header">
          <span>存储空间</span>
          <span class="storage-percent">65%</span>
        </div>
        <div class="storage-bar">
          <div class="storage-fill" style="width: 65%"></div>
        </div>
      </div>

      <!-- 用户信息 -->
      <div class="user-info">
        <div class="user-avatar">{{ userInitial }}</div>
        <div class="user-details">
          <span class="user-name">{{ userName }}</span>
          <span class="user-role">{{ userRole }}</span>
        </div>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const authStore = useAuthStore()

const navItems = [
  { icon: '📊', label: '工作台', path: '/dashboard' },
  { icon: '📁', label: '文件中心', path: '/files' },
  { icon: '🧪', label: 'AI实验室', path: '/ai' },
  { icon: '📜', label: '文档版本', path: '/documents' },
  { icon: '⚙️', label: '系统设置', path: '/settings' }
]

const isActive = (path: string) => route.path === path || route.path.startsWith(path + '/')

const userName = computed(() => authStore.user?.username || '用户')
const userRole = computed(() => authStore.user?.role || 'user')
const userInitial = computed(() => userName.value.charAt(0).toUpperCase())
</script>

<style scoped>
.sidebar {
  width: var(--sidebar-width);
  height: 100vh;
  position: fixed;
  left: 0;
  top: 0;
  display: flex;
  flex-direction: column;
  background: var(--gradient-sidebar);
  border-right: 1px solid var(--glass-border);
  z-index: 100;
}

.sidebar-logo {
  padding: 24px 20px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 32px;
  height: 32px;
  color: var(--color-primary);
}

.logo-icon svg {
  width: 100%;
  height: 100%;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: white;
}

.sidebar-nav {
  flex: 1;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  color: var(--color-text-light);
  text-decoration: none;
  transition: all var(--transition-base);
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

.nav-item.active {
  background: var(--gradient-primary);
  color: white;
}

.nav-icon {
  font-size: 20px;
}

.nav-label {
  font-size: var(--font-size-sm);
  font-weight: 500;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.storage-section {
  margin-bottom: 16px;
}

.storage-header {
  display: flex;
  justify-content: space-between;
  color: var(--color-text-light);
  font-size: var(--font-size-xs);
  margin-bottom: 8px;
}

.storage-percent {
  color: var(--color-success);
}

.storage-bar {
  height: 6px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-full);
}

.storage-fill {
  height: 100%;
  background: var(--gradient-success);
  border-radius: var(--radius-full);
  transition: width var(--transition-base);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
  background: var(--gradient-primary);
  color: white;
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: 600;
}

.user-details {
  display: flex;
  flex-direction: column;
}

.user-name {
  color: white;
  font-weight: 500;
  font-size: var(--font-size-sm);
}

.user-role {
  color: var(--color-text-light);
  font-size: var(--font-size-xs);
}
</style>