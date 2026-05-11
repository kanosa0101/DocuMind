// Axios实例配置与拦截器

import axios from 'axios'
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
const whiteList = ['/api/users/login', '/api/users/register', '/api/users/refresh', '/api/users/logout']

// 请求拦截器
api.interceptors.request.use(
  async (config) => {
    // 白名单路径跳过认证
    const url = config.url || ''
    if (whiteList.some(path => url.includes(path))) {
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
        try {
          const response = await axios.post(`${baseURL}/api/users/refresh`, null, {
            params: { refreshToken }
          })
          const result = response.data as Result<string>
          if (result.code === 200) {
            tokenManager.updateAccessToken(result.data)
            accessToken = result.data
          }
        } catch (error) {
          // 刷新失败，继续使用旧token（让响应拦截器处理401）
          console.warn('Token刷新失败，继续使用旧token')
        }
      }
    }

    // 设置Authorization头
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`
    }

    // 添加用户ID头（从Token解析）
    const user = tokenManager.getUser()
    if (user) {
      config.headers['X-User-Id'] = user.id
    }

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
      // 只在明确401时清除token并跳转
      tokenManager.clearTokens()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api