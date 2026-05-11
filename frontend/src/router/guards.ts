// 路由守卫

import router from './index'
import { tokenManager } from '@/utils/token'

const whiteList = ['/login', '/register', '/404']

router.beforeEach(async (to, from, next) => {
  // 白名单直接放行
  if (whiteList.includes(to.path)) {
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

  // 有token，放行（过期由API拦截器处理）
  next()
})

export function setupRouterGuards(app: any) {
  app.use(router)
}