<template>
  <header class="header glass-card">
    <!-- 搜索栏 -->
    <div class="search-bar">
      <Search class="search-icon" :size="18" />
      <input
        v-model="searchQuery"
        type="text"
        placeholder="输入问题或关键词搜索..."
        class="search-input"
        @focus="showResults = true"
        @keydown.enter="executeGlobalSearch"
      />
      <kbd class="search-hint">Ctrl+K</kbd>

      <!-- 搜索结果面板 -->
      <SearchResultPanel
        v-if="showResults && (searchResults || loading || error)"
        :search-results="searchResults"
        :loading="loading"
        :error="error"
        @openDocument="openDocument"
        @searchQuestion="searchQuestion"
      />
    </div>

    <!-- 右侧操作区 -->
    <div class="header-actions">
      <!-- 主题切换按钮 -->
      <button class="theme-btn" @click="toggleTheme" :title="isDark ? '切换亮色模式' : '切换暗色模式'">
        <Sun v-if="isDark" :size="18" />
        <Moon v-else :size="18" />
      </button>
      <span class="user-name">{{ userName }}</span>
      <button class="logout-btn" @click="handleLogout">
        <span>退出登录</span>
      </button>
    </div>
  </header>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useTheme } from '@/composables/useTheme'
import { useSearch } from '@/composables/useSearch'
import SearchResultPanel from './SearchResultPanel.vue'
import { Search, Sun, Moon } from '@lucide/vue'

const router = useRouter()
const authStore = useAuthStore()
const { isDark, toggleTheme } = useTheme()
const { searchQuery, searchResults, loading, error, executeSearch } = useSearch()

const showResults = ref(false)

const userName = computed(() => authStore.user?.username || '用户')

const executeGlobalSearch = () => {
  if (searchQuery.value.trim()) {
    executeSearch(searchQuery.value, authStore.user?.id)
    showResults.value = true
  }
}

const openDocument = (documentId: string) => {
  showResults.value = false
  router.push(`/documents/${documentId}`)
}

const searchQuestion = (question: string) => {
  searchQuery.value = question
  executeSearch(question, authStore.user?.id)
}

const handleLogout = async () => {
  await authStore.handleLogout()
  router.push('/login')
}

// 全局快捷键
const handleKeydown = (e: KeyboardEvent) => {
  if (e.ctrlKey && e.key === 'k') {
    e.preventDefault()
    document.querySelector<HTMLInputElement>('.search-input')?.focus()
  }
  // ESC关闭搜索结果
  if (e.key === 'Escape') {
    showResults.value = false
  }
}

document.addEventListener('keydown', handleKeydown)

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
})
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
}

[data-theme="dark"] .header {
  background: var(--glass-dark-bg);
  border: 1px solid var(--glass-dark-border);
  margin: 16px;
  margin-bottom: 0;
}

.search-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: var(--radius-md);
  width: 400px;
  transition: all var(--transition-base);
}

[data-theme="dark"] .search-bar {
  background: rgba(26, 26, 26, 0.6);
  border: 1px solid var(--glass-dark-border);
}

.search-bar:focus-within {
  border-color: var(--aurora-cyan);
  box-shadow: 0 0 0 3px rgba(8, 145, 178, 0.2);
}

[data-theme="dark"] .search-bar:focus-within {
  box-shadow: 0 0 0 3px rgba(8, 145, 178, 0.3), var(--glow-cyan-soft);
}

.search-icon {
  color: var(--color-text-muted);
}

[data-theme="dark"] .search-icon {
  color: var(--aurora-cyan);
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  font-size: var(--font-size-sm);
  color: var(--color-text);
}

[data-theme="dark"] .search-input {
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

[data-theme="dark"] .search-hint {
  background: rgba(8, 145, 178, 0.2);
  color: var(--aurora-cyan);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.theme-btn {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.1);
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 18px;
  transition: all var(--transition-base);
}

[data-theme="dark"] .theme-btn {
  background: rgba(8, 145, 178, 0.2);
  border-color: var(--glass-dark-border);
}

.theme-btn:hover {
  background: rgba(8, 145, 178, 0.3);
  transform: scale(1.1);
}

[data-theme="dark"] .theme-btn:hover {
  background: rgba(8, 145, 178, 0.4);
  box-shadow: var(--glow-cyan-soft);
}

.user-name {
  color: var(--color-text);
  font-weight: 500;
}

[data-theme="dark"] .user-name {
  color: var(--color-text);
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

[data-theme="dark"] .logout-btn {
  background: rgba(239, 68, 68, 0.15);
  border-color: rgba(239, 68, 68, 0.4);
}

.logout-btn:hover {
  background: var(--color-error);
  color: white;
}

[data-theme="dark"] .logout-btn:hover {
  box-shadow: 0 0 20px rgba(239, 68, 68, 0.4);
}

/* 响应式布局 */
@media (max-width: 900px) {
  .search-bar {
    width: 300px;
  }

  .search-hint {
    display: none;
  }
}

@media (max-width: 700px) {
  .header {
    padding: 12px 16px;
    margin: 8px;
  }

  .search-bar {
    width: 200px;
    padding: 6px 12px;
  }

  .user-name {
    display: none;
  }

  .logout-btn span {
    display: none;
  }

  .logout-btn {
    width: 36px;
    height: 36px;
    padding: 0;
    display: flex;
    justify-content: center;
    align-items: center;
  }

  /* 使用图标替代文字 */
  .logout-btn::before {
    content: "⏻";
    font-size: 16px;
  }
}

@media (max-width: 500px) {
  .search-bar {
    width: 150px;
    min-width: 120px;
  }

  .header-actions {
    gap: 8px;
  }

  .theme-btn {
    width: 32px;
    height: 32px;
  }
}
</style>