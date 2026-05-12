// 认证状态管理

import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { tokenManager } from '@/utils/token'
import { login, logout } from '@/api/auth'
import type { LoginDTO, UserVO } from '@/types/api'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<UserVO | null>(tokenManager.getUser())
  const loading = ref(false)

  const isAuthenticated = computed(() => !!user.value && tokenManager.isAuthenticated())

  // 登录
  async function handleLogin(loginDTO: LoginDTO): Promise<boolean> {
    loading.value = true
    try {
      const loginVO = await login(loginDTO)
      tokenManager.setTokens(loginVO.accessToken, loginVO.refreshToken, loginVO.user)
      user.value = loginVO.user
      return true
    } catch (error: any) {
      console.error('登录失败:', error.message || error)
      return false
    } finally {
      loading.value = false
    }
  }

  // 登出
  async function handleLogout(): Promise<void> {
    try {
      await logout()
    } finally {
      tokenManager.clearTokens()
      user.value = null
    }
  }

  // 检查认证状态
  function checkAuth(): boolean {
    const savedUser = tokenManager.getUser()
    if (savedUser && tokenManager.isAuthenticated()) {
      user.value = savedUser
      return true
    }
    // 认证信息无效，清除token
    if (savedUser || tokenManager.getTokenInfo()) {
      tokenManager.clearTokens()
    }
    return false
  }

  return {
    user,
    loading,
    isAuthenticated,
    handleLogin,
    handleLogout,
    checkAuth
  }
})