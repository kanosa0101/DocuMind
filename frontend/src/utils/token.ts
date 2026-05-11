// Token管理工具

interface TokenInfo {
  accessToken: string
  refreshToken: string
  expiresAt: number
  refreshExpiresAt: number
}

const ACCESS_TOKEN_KEY = 'docai_access_token'
const REFRESH_TOKEN_KEY = 'docai_refresh_token'
const USER_KEY = 'docai_user'

// Token有效期常量（与后端一致）
const ACCESS_TOKEN_EXPIRES = 30 * 60 * 1000 // 30分钟
const REFRESH_TOKEN_EXPIRES = 7 * 24 * 60 * 60 * 1000 // 7天

export class TokenManager {
  // 存储Token
  setTokens(accessToken: string, refreshToken: string, user: any): void {
    const now = Date.now()
    const tokenInfo: TokenInfo = {
      accessToken,
      refreshToken,
      expiresAt: now + ACCESS_TOKEN_EXPIRES,
      refreshExpiresAt: now + REFRESH_TOKEN_EXPIRES
    }
    localStorage.setItem(ACCESS_TOKEN_KEY, JSON.stringify(tokenInfo))
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  }

  // 获取Token信息
  getTokenInfo(): TokenInfo | null {
    const tokenStr = localStorage.getItem(ACCESS_TOKEN_KEY)
    if (!tokenStr) return null
    try {
      return JSON.parse(tokenStr)
    } catch {
      return null
    }
  }

  // 获取有效Token（检查过期）
  getValidToken(): string | null {
    const tokenInfo = this.getTokenInfo()
    if (!tokenInfo) return null

    const now = Date.now()
    // 提前5分钟判断即将过期
    if (now > tokenInfo.expiresAt - 5 * 60 * 1000) {
      return null // 需要刷新
    }

    return tokenInfo.accessToken
  }

  // 获取RefreshToken
  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY)
  }

  // 更新AccessToken
  updateAccessToken(accessToken: string): void {
    const tokenInfo = this.getTokenInfo()
    if (tokenInfo) {
      tokenInfo.accessToken = accessToken
      tokenInfo.expiresAt = Date.now() + ACCESS_TOKEN_EXPIRES
      localStorage.setItem(ACCESS_TOKEN_KEY, JSON.stringify(tokenInfo))
    }
  }

  // 获取用户信息
  getUser(): any | null {
    const userStr = localStorage.getItem(USER_KEY)
    if (!userStr) return null
    try {
      return JSON.parse(userStr)
    } catch {
      return null
    }
  }

  // 检查是否已登录
  isAuthenticated(): boolean {
    const tokenInfo = this.getTokenInfo()
    if (!tokenInfo) return false

    // 检查refreshToken是否过期
    return Date.now() < tokenInfo.refreshExpiresAt
  }

  // 清除所有Token和用户信息
  clearTokens(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }
}

export const tokenManager = new TokenManager()