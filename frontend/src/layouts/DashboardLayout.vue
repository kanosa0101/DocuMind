<template>
  <div class="dashboard-layout" :data-theme="themeStore.mode">
    <!-- Aurora 背景 -->
    <AuroraBackground v-if="themeStore.mode === 'dark'" />

    <!-- 侧边栏 -->
    <Sidebar />

    <!-- 主内容区 -->
    <main class="main-content">
      <!-- 顶部搜索栏 -->
      <Header />

      <!-- 页面内容 -->
      <div class="page-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </div>
    </main>

    <!-- AI Agent 侧边栏 -->
    <ChatSidebar />
  </div>
</template>

<script setup lang="ts">
import Sidebar from '@/components/layout/Sidebar.vue'
import Header from '@/components/layout/Header.vue'
import ChatSidebar from '@/components/chat/ChatSidebar.vue'
import AuroraBackground from '@/components/common/AuroraBackground.vue'
import { useThemeStore } from '@/stores/theme'

const themeStore = useThemeStore()
</script>

<style scoped>
.dashboard-layout {
  display: grid;
  grid-template-columns: auto 1fr auto;
  min-height: 100vh;
  position: relative;
  transition: background-color var(--transition-base);
}

[data-theme="dark"] .dashboard-layout {
  background: var(--obsidian-base);
}

/* 侧边栏列：Sidebar 自动管理宽度 */
/* 主内容区：弹性填充 */
.main-content {
  display: flex;
  flex-direction: column;
  min-width: 0; /* 关键：防止内容溢出 */
  position: relative;
  z-index: 1;
  overflow: hidden;
}

.page-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  overflow-x: hidden;
}

/* 响应式布局 */
@media (max-width: 1200px) {
  /* 中屏幕：ChatSidebar 收起 */
  .dashboard-layout {
    grid-template-columns: auto 1fr auto;
  }
}

@media (max-width: 768px) {
  /* 小屏幕：单列布局 */
  .dashboard-layout {
    grid-template-columns: 1fr;
    grid-template-rows: auto 1fr auto;
  }

  .main-content {
    order: 2;
  }

  .page-content {
    padding: 12px;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>