// Axios实例配置与拦截器

import axios from 'axios'
import router from '@/router'
import { tokenManager } from '@/utils/token'
import type { Result } from '@/types/api'

// API基础URL - 使用相对路径走Vite代理，避免浏览器代理干扰
const baseURL = import.meta.env.VITE_API_BASE_URL || ''

// 创建Axios实例
const api = axios.create({
  baseURL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 白名单路径（无需认证）
const whiteList = ['/api/users/login', '/api/users/register', '/api/users/refresh']

// 标记是否正在刷新Token
let isRefreshing = false
// 等待Token刷新的请求队列
let refreshSubscribers: ((token: string) => void)[] = []
// 标记是否已经处理401跳转（防止多次跳转）
let hasRedirectedToLogin = false

// 订阅Token刷新
function subscribeTokenRefresh(callback: (token: string) => void) {
  refreshSubscribers.push(callback)
}

// 通知所有订阅者Token已刷新
function onTokenRefreshed(token: string) {
  refreshSubscribers.forEach(callback => callback(token))
  refreshSubscribers = []
}

// 请求拦截器
api.interceptors.request.use(
  async (config) => {
    // 白名单路径跳过认证（精确匹配）
    const url = config.url || ''
    const isWhiteListed = whiteList.some(path => url === path || url.startsWith(path + '?'))
    if (isWhiteListed) {
      return config
    }

    // 获取Token信息
    const tokenInfo = tokenManager.getTokenInfo()

    if (!tokenInfo) {
      // 没有token信息，返回请求（让响应拦截器处理401）
      return config
    }

    const now = Date.now()
    let accessToken = tokenInfo.accessToken

    // Token即将过期（提前5分钟），尝试刷新
    if (now > tokenInfo.expiresAt - 5 * 60 * 1000 && now < tokenInfo.refreshExpiresAt) {
      const refreshToken = tokenManager.getRefreshToken()
      if (refreshToken) {
        // 如果已经在刷新，等待刷新完成
        if (isRefreshing) {
          return new Promise((resolve) => {
            subscribeTokenRefresh((token) => {
              config.headers.Authorization = `Bearer ${token}`
              resolve(config)
            })
          })
        }

        isRefreshing = true
        try {
          const response = await axios.post(`${baseURL}/api/users/refresh`, null, {
            params: { refreshToken }
          })
          const result = response.data as Result<string>
          if (result.code === 200) {
            tokenManager.updateAccessToken(result.data)
            accessToken = result.data
            onTokenRefreshed(result.data)
          }
        } catch (error) {
          // 刷新失败，清除token并跳转登录页
          console.warn('Token刷新失败，清除认证信息')
          tokenManager.clearTokens()
          onTokenRefreshed('') // 通知等待的请求（token为空表示失败）
          // 跳转登录页
          const currentPath = window.location.pathname
          router.push({ path: '/login', query: { redirect: currentPath !== '/login' ? currentPath : '/dashboard' } })
          return Promise.reject(error)
        } finally {
          isRefreshing = false
        }
      }
    }

    // 设置Authorization头
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`
    }

    // 注意：不再设置X-User-Id头，网关会从JWT解析并设置该头

    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    // 统一处理Result响应
    const result = response.data as Result<any>

    if (result.code === 200) {
      // 直接返回data，简化调用
      return result.data as any
    }

    // 业务错误，返回错误信息（不自动跳转）
    return Promise.reject({ code: result.code, message: result.message })
  },
  (error) => {
    // HTTP错误处理
    if (error.response?.status === 401) {
      // 清除token
      tokenManager.clearTokens()

      // 防止多次跳转（使用防抖标记）
      if (!hasRedirectedToLogin) {
        hasRedirectedToLogin = true
        // 获取当前路径作为重定向目标
        const currentPath = window.location.pathname
        const redirectPath = currentPath !== '/login' ? currentPath : '/dashboard'

        // 跳转登录页，携带redirect参数
        if (currentPath !== '/login') {
          router.push({ path: '/login', query: { redirect: redirectPath } })
        }
        // 延迟重置标记，防止在跳转过程中重复触发
        setTimeout(() => {
          hasRedirectedToLogin = false
        }, 1000)
      }
    }
    return Promise.reject(error)
  }
)

export default api