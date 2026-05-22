// Axios实例配置与拦截器

import axios, { type AxiosRequestConfig } from 'axios'
import router from '@/router'
import { tokenManager } from '@/utils/token'
import { useToast } from '@/composables/useToast'
import type { Result } from '@/types/api'

const toast = useToast()

// API基础URL - 使用相对路径走Vite代理，避免浏览器代理干扰
const baseURL = import.meta.env.VITE_API_BASE_URL || ''

// 自定义API实例类型，因为响应拦截器直接返回data而非AxiosResponse
interface ApiInstance {
  get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
  post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T>
  delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T>
}

// 创建Axios实例
const axiosInstance = axios.create({
  baseURL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json; charset=UTF-8'
  },
  // 禁用HTTP代理，避免系统http_proxy环境变量干扰本地请求
  proxy: false
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
axiosInstance.interceptors.request.use(
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
          toast.warning('登录状态已过期，请重新登录')
          tokenManager.clearTokens()
          onTokenRefreshed('') // 通知等待的请求（token为空表示失败）
          // 跳转登录页
          const currentPath = window.location.pathname
          router.push({ path: '/login', query: { redirect: currentPath !== '/login' ? currentPath : '/dashboard', reason: 'session_expired' } })
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
axiosInstance.interceptors.response.use(
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
    const status = error.response?.status
    let message = '操作失败'

    if (!error.response) {
      message = '网络连接失败，请检查网络'
      toast.error(message)
    } else if (status === 401) {
      // 清除token
      tokenManager.clearTokens()

      message = '登录已过期，请重新登录'
      toast.warning(message)

      // 防止多次跳转（使用防抖标记）
      if (!hasRedirectedToLogin) {
        hasRedirectedToLogin = true
        // 获取当前路径作为重定向目标
        const currentPath = window.location.pathname
        const redirectPath = currentPath !== '/login' ? currentPath : '/dashboard'

        // 跳转登录页，携带redirect参数
        if (currentPath !== '/login') {
          router.push({ path: '/login', query: { redirect: redirectPath, reason: 'session_expired' } })
        }
        // 延迟重置标记，防止在跳转过程中重复触发
        setTimeout(() => {
          hasRedirectedToLogin = false
        }, 1000)
      }
    } else if (status === 403) {
      message = '无权限访问此资源'
      toast.error(message)
    } else if (status === 404) {
      message = '请求的资源不存在'
      toast.warning(message)
    } else if (status === 500) {
      message = '服务器内部错误，请稍后重试'
      toast.error(message)
    } else if (status === 502 || status === 503) {
      message = '服务暂时不可用，请稍后重试'
      toast.error(message)
    } else {
      message = error.response?.data?.message || `请求失败（${status}）`
      toast.error(message)
    }

    return Promise.reject({ status, message, error })
  }
)

// 创建符合自定义类型的API实例
const api: ApiInstance = {
  get: (url, config) => axiosInstance.get(url, config) as Promise<any>,
  post: (url, data, config) => axiosInstance.post(url, data, config) as Promise<any>,
  put: (url, data, config) => axiosInstance.put(url, data, config) as Promise<any>,
  delete: (url, config) => axiosInstance.delete(url, config) as Promise<any>
}

export default api