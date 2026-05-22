import { ref, computed } from 'vue'
import type { GlobalSearchResult } from '../api/globalSearch'
import { globalSearch, detectSearchStrategy } from '../api/globalSearch'

/**
 * 搜索逻辑Hook
 * 智能判断搜索策略并执行搜索
 */

export function useSearch() {
  const searchQuery = ref('')
  const searchResults = ref<GlobalSearchResult | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)
  const searchStrategy = ref<string | null>(null)

  // 判断是否为问答模式
  const isQAMode = computed(() => searchResults.value?.strategy === 'QA')

  // 执行搜索
  const executeSearch = async (query: string, userId?: number) => {
    if (!query.trim()) {
      error.value = '请输入搜索内容'
      return
    }

    loading.value = true
    error.value = null
    searchQuery.value = query

    try {
      // 先检测策略
      const strategyResult = await detectSearchStrategy(query)
      if (strategyResult) {
        searchStrategy.value = strategyResult
      }

      // 执行搜索 - API直接返回GlobalSearchResult（响应拦截器已处理）
      const result = await globalSearch(query, userId)
      searchResults.value = result
    } catch (e: any) {
      error.value = e.message || '搜索异常'
    } finally {
      loading.value = false
    }
  }

  // 清空搜索
  const clearSearch = () => {
    searchQuery.value = ''
    searchResults.value = null
    error.value = null
    searchStrategy.value = null
  }

  return {
    searchQuery,
    searchResults,
    loading,
    error,
    searchStrategy,
    isQAMode,
    executeSearch,
    clearSearch
  }
}