import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

// Mock tokenManager
vi.mock('@/utils/token', () => ({
  tokenManager: {
    getUser: vi.fn(),
    getTokenInfo: vi.fn(),
    isAuthenticated: vi.fn(),
    setTokens: vi.fn(),
    clearTokens: vi.fn(),
    getToken: vi.fn()
  }
}))

// Mock auth API
vi.mock('@/api/auth', () => ({
  login: vi.fn(),
  logout: vi.fn()
}))

import { useAuthStore } from '../auth'
import { tokenManager } from '@/utils/token'
import { login, logout } from '@/api/auth'

describe('auth store测试', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('初始状态应为未登录', () => {
    vi.mocked(tokenManager.getUser).mockReturnValue(null)
    vi.mocked(tokenManager.isAuthenticated).mockReturnValue(false)

    const store = useAuthStore()

    expect(store.user).toBeNull()
    expect(store.isAuthenticated).toBe(false)
    expect(store.loading).toBe(false)
  })

  it('登录成功应更新状态', async () => {
    const mockUser = { id: 1, username: 'testuser', email: 'test@example.com', role: 'USER', status: 1, createTime: '2026-05-18' }
    const mockLoginVO = {
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      user: mockUser
    }

    vi.mocked(tokenManager.getUser).mockReturnValue(null)
    vi.mocked(login).mockResolvedValue(mockLoginVO)

    const store = useAuthStore()
    const result = await store.handleLogin({
      username: 'testuser',
      password: 'password123'
    })

    expect(result).toBe(true)
    expect(store.user).toEqual(mockUser)
    expect(store.loading).toBe(false)
    expect(tokenManager.setTokens).toHaveBeenCalledWith('access-token', 'refresh-token', mockUser)
  })

  it('登录失败应返回false', async () => {
    vi.mocked(tokenManager.getUser).mockReturnValue(null)
    vi.mocked(login).mockRejectedValue(new Error('登录失败'))

    const store = useAuthStore()
    const result = await store.handleLogin({
      username: 'testuser',
      password: 'wrongpassword'
    })

    expect(result).toBe(false)
    expect(store.user).toBeNull()
    expect(store.loading).toBe(false)
  })

  it('logout应清理状态', async () => {
    const mockUser = { id: 1, username: 'testuser', email: 'test@example.com', role: 'USER', status: 1, createTime: '2026-05-18' }
    vi.mocked(tokenManager.getUser).mockReturnValue(mockUser)
    vi.mocked(logout).mockResolvedValue(undefined)

    const store = useAuthStore()
    store.user = mockUser  // 设置初始状态

    await store.handleLogout()

    expect(tokenManager.clearTokens).toHaveBeenCalled()
    expect(store.user).toBeNull()
  })

  it('checkAuth应检查认证状态', () => {
    const mockUser = { id: 1, username: 'testuser', email: 'test@example.com', role: 'USER', status: 1, createTime: '2026-05-18' }
    vi.mocked(tokenManager.getUser).mockReturnValue(mockUser)
    vi.mocked(tokenManager.isAuthenticated).mockReturnValue(true)

    const store = useAuthStore()
    const result = store.checkAuth()

    expect(result).toBe(true)
    expect(store.user).toEqual(mockUser)
  })

  it('checkAuth无效token应清理', () => {
    vi.mocked(tokenManager.getUser).mockReturnValue(null)
    vi.mocked(tokenManager.isAuthenticated).mockReturnValue(false)
    vi.mocked(tokenManager.getTokenInfo).mockReturnValue({
      accessToken: '',
      refreshToken: '',
      expiresAt: 0,
      refreshExpiresAt: 0
    })

    const store = useAuthStore()
    const result = store.checkAuth()

    expect(result).toBe(false)
    expect(tokenManager.clearTokens).toHaveBeenCalled()
  })

  it('isAuthenticated计算属性应正确判断', () => {
    const mockUser = { id: 1, username: 'testuser', email: 'test@example.com', role: 'USER' }
    vi.mocked(tokenManager.getUser).mockReturnValue(mockUser)
    vi.mocked(tokenManager.isAuthenticated).mockReturnValue(true)

    const store = useAuthStore()
    store.checkAuth()

    expect(store.isAuthenticated).toBe(true)
  })

  it('loading状态应在请求中正确更新', async () => {
    vi.mocked(tokenManager.getUser).mockReturnValue(null)
    vi.mocked(login).mockImplementation(() => new Promise(resolve => {
      setTimeout(() => resolve({
        accessToken: 'token',
        refreshToken: 'refresh',
        user: { id: 1, username: 'user', email: '', role: 'USER', status: 1, createTime: '2026-05-18' }
      }), 100)
    }))

    const store = useAuthStore()
    const loginPromise = store.handleLogin({ username: 'user', password: 'pass' })

    // 检查loading状态
    expect(store.loading).toBe(true)

    await loginPromise

    expect(store.loading).toBe(false)
  })
})