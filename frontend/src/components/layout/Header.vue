<template>
  <header class="header glass-card">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <span class="search-icon">🔍</span>
      <input
        v-model="searchQuery"
        type="text"
        placeholder="跨文档内容搜索..."
        class="search-input"
        @input="handleSearch"
        @keydown.enter="executeSearch"
      />
      <kbd class="search-hint">Ctrl+K</kbd>
    </div>

    <!-- 右侧操作区 -->
    <div class="header-actions">
      <span class="user-name">{{ userName }}</span>
      <button class="logout-btn" @click="handleLogout">
        <span>退出登录</span>
      </button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const searchQuery = ref('')

const userName = computed(() => authStore.user?.username || '用户')

const handleSearch = () => {
  // 搜索逻辑待实现
}

const executeSearch = () => {
  if (searchQuery.value.trim()) {
    router.push({ path: '/search', query: { q: searchQuery.value } })
  }
}

const handleLogout = async () => {
  await authStore.handleLogout()
  router.push('/login')
}

// 全局快捷键
const handleKeydown = (e: KeyboardEvent) => {
  if (e.ctrlKey && e.key === 'k') {
    document.querySelector('.search-input')?.focus()
  }
}

document.addEventListener('keydown', handleKeydown)
</script>

<style scoped>
.header {
  height: var(--header-height);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  margin: 16px;
  border-radius: var(--radius-lg);
  background: var(--glass-bg);
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: var(--radius-md);
  width: 400px;
}

.search-icon {
  color: var(--color-text-muted);
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  font-size: var(--font-size-sm);
  color: var(--color-text);
}

.search-input::placeholder {
  color: var(--color-text-light);
}

.search-hint {
  padding: 4px 8px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-name {
  color: var(--color-text);
  font-weight: 500;
}

.logout-btn {
  padding: 8px 16px;
  border-radius: var(--radius-md);
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all var(--transition-base);
  color: var(--color-error);
}

.logout-btn:hover {
  background: var(--color-error);
  color: white;
}
</style>