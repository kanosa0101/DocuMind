// 路由守卫

import router from './index'
import { tokenManager } from '@/utils/token'
import { useAuthStore } from '@/stores/auth'

const whiteList = ['/login', '/register', '/404']

router.beforeEach(async (to, _from, next) => {
  // 白名单直接放行
  if (whiteList.includes(to.path)) {
    // 已登录用户访问登录/注册页，重定向到首页
    if (to.path === '/login' || to.path === '/register') {
      const tokenInfo = tokenManager.getTokenInfo()
      if (tokenInfo && tokenManager.isAuthenticated()) {
        next('/dashboard')
        return
      }
    }
    next()
    return
  }

  // 检查是否有token信息（不检查过期，让API拦截器处理）
  const tokenInfo = tokenManager.getTokenInfo()

  if (!tokenInfo) {
    // 没有token，跳转登录
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 有token，检查是否已过期（refreshToken过期）
  if (!tokenManager.isAuthenticated()) {
    // refreshToken已过期，清除token并跳转登录
    tokenManager.clearTokens()
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  // 有有效token，初始化用户状态
  const authStore = useAuthStore()
  if (!authStore.user) {
    authStore.checkAuth()
  }

  // 放行（过期由API拦截器处理）
  next()
})

export function setupRouterGuards(app: any) {
  app.use(router)
}