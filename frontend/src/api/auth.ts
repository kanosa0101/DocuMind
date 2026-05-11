// 认证API

import api from './index'
import type { LoginDTO, LoginVO, RegisterDTO, UserVO } from '@/types/api'

// 登录
export async function login(data: LoginDTO): Promise<LoginVO> {
  return api.post<LoginVO>('/api/users/login', data)
}

// 注册
export async function register(data: RegisterDTO): Promise<UserVO> {
  return api.post<UserVO>('/api/users/register', data)
}

// 刷新Token
export async function refreshToken(refreshToken: string): Promise<string> {
  return api.post<string>('/api/users/refresh', null, {
    params: { refreshToken }
  })
}

// 获取用户信息
export async function getUserInfo(id: number): Promise<UserVO> {
  return api.get<UserVO>(`/api/users/${id}`)
}

// 登出
export async function logout(): Promise<void> {
  return api.post('/api/users/logout')
}