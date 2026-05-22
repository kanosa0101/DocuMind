<template>
  <aside
    class="sidebar glass-card"
    :class="{ collapsed: layoutStore.sidebarCollapsed, expanded: !layoutStore.sidebarCollapsed }"
    :style="{ width: layoutStore.sidebarWidth + 'px' }"
  >
    <!-- Logo + 切换按钮 -->
    <div class="sidebar-header">
      <div class="sidebar-logo" @click="layoutStore.toggleSidebar">
        <div class="logo-icon">
          <BookOpen :size="32" />
        </div>
        <span v-show="!layoutStore.sidebarCollapsed" class="logo-text">DocuMind</span>
      </div>
      <!-- 展开/收起按钮 -->
      <button class="toggle-btn" @click="layoutStore.toggleSidebar" :title="layoutStore.sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'">
        <ChevronRight v-if="layoutStore.sidebarCollapsed" :size="20" />
        <ChevronLeft v-else :size="20" />
      </button>
    </div>

    <!-- 导航菜单 -->
    <nav class="sidebar-nav">
      <router-link
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="nav-item"
        :class="{ active: isActive(item.path) }"
        :title="layoutStore.sidebarCollapsed ? item.label : ''"
      >
        <component :is="iconComponents[item.icon]" class="nav-icon" :size="20" />
        <span v-show="!layoutStore.sidebarCollapsed" class="nav-label">{{ item.label }}</span>
        <!-- Tooltip for collapsed mode -->
        <span v-if="layoutStore.sidebarCollapsed" class="nav-tooltip">{{ item.label }}</span>
      </router-link>
    </nav>

    <!-- 底部用户信息 -->
    <div class="sidebar-footer" v-show="!layoutStore.sidebarCollapsed">
      <!-- 存储进度 -->
      <div class="storage-section">
        <div class="storage-header">
          <span>存储空间</span>
          <span class="storage-percent">{{ storagePercent }}%</span>
        </div>
        <div class="storage-bar">
          <div class="storage-fill" :style="{ width: storagePercent + '%' }"></div>
        </div>
        <div class="storage-info">
          <span>{{ totalSizeMB }}MB / {{ totalSpaceMB }}MB</span>
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

    <!-- 收起模式下的用户头像 -->
    <div class="sidebar-footer-collapsed" v-show="layoutStore.sidebarCollapsed">
      <div class="user-avatar-small" :title="userName">{{ userInitial }}</div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useLayoutStore } from '@/stores/layout'
import { getFileStats } from '@/api/file'
import {
  LayoutDashboard,
  FolderOpen,
  FlaskConical,
  Settings,
  ChevronRight,
  ChevronLeft,
  BookOpen
} from '@lucide/vue'

const route = useRoute()
const authStore = useAuthStore()
const layoutStore = useLayoutStore()

const navItems: Array<{ icon: string; label: string; path: string }> = [
  { icon: 'LayoutDashboard', label: '工作台', path: '/dashboard' },
  { icon: 'FolderOpen', label: '文件中心', path: '/files' },
  { icon: 'FlaskConical', label: 'AI实验室', path: '/ai' },
  { icon: 'Settings', label: '系统设置', path: '/settings' }
]

const iconComponents: Record<string, any> = {
  LayoutDashboard,
  FolderOpen,
  FlaskConical,
  Settings
}

const isActive = (path: string) => route.path === path || route.path.startsWith(path + '/')

const userName = computed(() => authStore.user?.username || '用户')
const userRole = computed(() => authStore.user?.role || 'user')
const userInitial = computed(() => userName.value.charAt(0).toUpperCase())

// 存储空间数据
const storagePercent = ref(0)
const fileCount = ref(0)
const totalSizeMB = ref(0)
const totalSpaceMB = 1024 // 默认总空间 1GB

async function loadStorageStats() {
  try {
    const stats = await getFileStats()
    fileCount.value = stats.fileCount
    totalSizeMB.value = stats.totalSizeMB
    storagePercent.value = Math.min(Math.round((stats.totalSizeMB / totalSpaceMB) * 100), 100)
  } catch (e) {
    console.error('获取存储统计失败:', e)
  }
}

onMounted(() => {
  loadStorageStats()
})
</script>

<style scoped>
.sidebar {
  height: 100vh;
  position: sticky;
  left: 0;
  top: 0;
  display: flex;
  flex-direction: column;
  background: var(--gradient-sidebar);
  border-right: 1px solid var(--glass-border);
  z-index: 100;
  transition: width var(--transition-base);
  overflow: hidden;
  align-self: start;
}

[data-theme="dark"] .sidebar {
  background: linear-gradient(180deg, var(--obsidian-surface), var(--obsidian-elevated));
  border-right: 1px solid var(--glass-dark-border);
}

.sidebar.collapsed {
  width: 72px;
}

.sidebar.expanded {
  width: 240px;
}

.sidebar-header {
  padding: 16px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

[data-theme="dark"] .sidebar-header {
  border-bottom: 1px solid var(--glass-dark-border);
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  flex: 1;
}

.logo-icon {
  width: 32px;
  height: 32px;
  color: var(--aurora-cyan);
  flex-shrink: 0;
}

[data-theme="dark"] .logo-icon {
  color: var(--aurora-cyan-light);
}

.logo-icon svg {
  width: 100%;
  height: 100%;
}

.logo-text {
  font-size: 18px;
  font-weight: 700;
  color: white;
  white-space: nowrap;
}

.toggle-btn {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.1);
  border: none;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  color: rgba(255, 255, 255, 0.6);
  transition: all var(--transition-fast);
  flex-shrink: 0;
}

.toggle-btn:hover {
  background: rgba(8, 145, 178, 0.2);
  color: var(--aurora-cyan);
}

[data-theme="dark"] .toggle-btn:hover {
  background: rgba(8, 145, 178, 0.3);
  box-shadow: var(--glow-cyan-soft);
}

.sidebar-nav {
  flex: 1;
  padding: 12px 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: var(--radius-md);
  color: var(--color-text-light);
  text-decoration: none;
  transition: all var(--transition-base);
  position: relative;
}

.sidebar.collapsed .nav-item {
  padding: 12px;
  justify-content: center;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.1);
  color: white;
}

[data-theme="dark"] .nav-item:hover {
  background: rgba(8, 145, 178, 0.15);
  box-shadow: var(--glow-cyan-soft);
}

.nav-item.active {
  background: var(--gradient-aurora);
  color: white;
  box-shadow: var(--glow-aurora);
}

[data-theme="dark"] .nav-item.active {
  background: var(--gradient-aurora);
}

.nav-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.nav-label {
  font-size: var(--font-size-sm);
  font-weight: 500;
  white-space: nowrap;
  opacity: 1;
  transition: opacity var(--transition-fast);
}

.sidebar.collapsed .nav-label {
  opacity: 0;
  width: 0;
}

.nav-tooltip {
  position: absolute;
  left: 100%;
  top: 50%;
  transform: translateY(-50%);
  background: var(--obsidian-elevated);
  color: white;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  white-space: nowrap;
  opacity: 0;
  visibility: hidden;
  transition: opacity var(--transition-fast), visibility var(--transition-fast);
  z-index: 10;
  margin-left: 8px;
  box-shadow: var(--shadow-md);
}

.sidebar.collapsed .nav-item:hover .nav-tooltip {
  opacity: 1;
  visibility: visible;
}

.sidebar-footer {
  padding: 16px 12px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  opacity: 1;
  transition: opacity var(--transition-base);
}

[data-theme="dark"] .sidebar-footer {
  border-top: 1px solid var(--glass-dark-border);
}

.sidebar-footer-collapsed {
  padding: 12px 8px;
  display: flex;
  justify-content: center;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

[data-theme="dark"] .sidebar-footer-collapsed {
  border-top: 1px solid var(--glass-dark-border);
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
  color: var(--aurora-emerald);
}

[data-theme="dark"] .storage-percent {
  color: var(--aurora-emerald-light);
  text-shadow: var(--glow-text-emerald);
}

.storage-bar {
  height: 6px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-full);
}

[data-theme="dark"] .storage-bar {
  background: rgba(255, 255, 255, 0.05);
}

.storage-info {
  margin-top: 4px;
  font-size: 10px;
  color: var(--color-text-light);
  opacity: 0.7;
}

.storage-fill {
  height: 100%;
  background: var(--gradient-aurora);
  border-radius: var(--radius-full);
  transition: width var(--transition-base);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  background: var(--gradient-aurora);
  color: white;
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: 600;
  font-size: var(--font-size-sm);
}

[data-theme="dark"] .user-avatar {
  box-shadow: var(--glow-aurora);
}

.user-avatar-small {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  background: var(--gradient-aurora);
  color: white;
  display: flex;
  justify-content: center;
  align-items: center;
  font-weight: 600;
  font-size: var(--font-size-sm);
  cursor: pointer;
}

[data-theme="dark"] .user-avatar-small {
  box-shadow: var(--glow-aurora);
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

/* 响应式：小屏幕时隐藏侧边栏 */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    transform: translateX(-100%);
    z-index: 200;
  }

  .sidebar.expanded {
    transform: translateX(0);
  }
}
</style>