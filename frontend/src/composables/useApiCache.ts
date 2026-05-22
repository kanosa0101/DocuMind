import { ref, computed } from 'vue'

interface CacheEntry<T> {
  data: T
  timestamp: number
  expiresIn: number
}

const cache = new Map<string, CacheEntry<any>>()
const pendingRequests = new Map<string, Promise<any>>()

/**
 * API缓存策略
 * 支持内存缓存、请求合并、过期自动清理
 */
export function useApiCache<T>(
  key: string,
  fetcher: () => Promise<T>,
  expiresIn: number = 5 * 60 * 1000 // 默认5分钟缓存
) {
  const loading = ref(false)
  const error = ref<Error | null>(null)

  // 检查缓存是否有效
  const isValid = (entry: CacheEntry<T>): boolean => {
    return Date.now() - entry.timestamp < entry.expiresIn
  }

  // 获取缓存数据
  const getCached = (): T | null => {
    const entry = cache.get(key)
    if (entry && isValid(entry)) {
      return entry.data
    }
    return null
  }

  // 设置缓存
  const setCache = (data: T): void => {
    cache.set(key, {
      data,
      timestamp: Date.now(),
      expiresIn
    })
  }

  // 计算属性返回缓存数据或null
  const cachedData = computed<T | null>(() => getCached())

  // 惰性加载 - 先返回缓存，无缓存时加载
  const load = async (): Promise<T> => {
    // 先检查缓存
    const cached = getCached()
    if (cached) {
      return cached
    }

    // 检查是否有相同请求正在进行（请求合并）
    const pending = pendingRequests.get(key)
    if (pending) {
      return pending as Promise<T>
    }

    loading.value = true
    error.value = null

    // 创建新请求
    const request = fetcher()
    pendingRequests.set(key, request)

    try {
      const data = await request
      setCache(data)
      return data
    } catch (e) {
      error.value = e instanceof Error ? e : new Error(String(e))
      throw e
    } finally {
      loading.value = false
      pendingRequests.delete(key)
    }
  }

  // 强制刷新 - 忽略缓存重新加载
  const refresh = async (): Promise<T> => {
    cache.delete(key)
    return load()
  }

  // 清除缓存
  const clear = (): void => {
    cache.delete(key)
  }

  return {
    cachedData,
    loading,
    error,
    load,
    refresh,
    clear
  }
}

/**
 * 批量缓存管理
 */
export function useCacheManager() {
  // 清除所有缓存
  const clearAll = (): void => {
    cache.clear()
  }

  // 清除指定前缀的缓存
  const clearByPrefix = (prefix: string): void => {
    for (const key of cache.keys()) {
      if (key.startsWith(prefix)) {
        cache.delete(key)
      }
    }
  }

  // 获取缓存大小
  const size = computed(() => cache.size)

  // 清理过期缓存
  const cleanup = (): void => {
    for (const [key, entry] of cache.entries()) {
      if (Date.now() - entry.timestamp >= entry.expiresIn) {
        cache.delete(key)
      }
    }
  }

  return {
    clearAll,
    clearByPrefix,
    size,
    cleanup
  }
}

// 自动定时清理过期缓存（每10分钟）
if (typeof window !== 'undefined') {
  setInterval(() => {
    for (const [key, entry] of cache.entries()) {
      if (Date.now() - entry.timestamp >= entry.expiresIn) {
        cache.delete(key)
      }
    }
  }, 10 * 60 * 1000)
}