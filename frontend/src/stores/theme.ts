import { defineStore } from 'pinia'
import { ref } from 'vue'

export type ThemeMode = 'light' | 'dark'

export const useThemeStore = defineStore('theme', () => {
  // 从localStorage读取或默认dark模式
  const mode = ref<ThemeMode>(
    (localStorage.getItem('documind-theme') as ThemeMode) || 'dark'
  )

  // 应用主题到DOM
  function applyTheme(theme: ThemeMode) {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem('documind-theme', theme)
    mode.value = theme

    // 更新meta theme-color (移动端浏览器标签颜色)
    const metaThemeColor = document.querySelector('meta[name="theme-color"]')
    if (metaThemeColor) {
      metaThemeColor.setAttribute('content', theme === 'dark' ? '#0D0D0D' : '#F8FAFC')
    }
  }

  // 切换主题
  function toggleTheme() {
    const next = mode.value === 'dark' ? 'light' : 'dark'
    applyTheme(next)
  }

  // 设置特定主题
  function setTheme(theme: ThemeMode) {
    applyTheme(theme)
  }

  // 监听系统主题变化
  function listenSystemTheme() {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')

    // 如果用户没有手动设置过主题，跟随系统
    if (!localStorage.getItem('documind-theme')) {
      applyTheme(mediaQuery.matches ? 'dark' : 'light')
    }

    mediaQuery.addEventListener('change', (e) => {
      // 仅在用户未手动设置时跟随系统
      if (!localStorage.getItem('documind-theme-manual')) {
        applyTheme(e.matches ? 'dark' : 'light')
      }
    })
  }

  // 初始化时应用主题
  applyTheme(mode.value)

  return {
    mode,
    isDark: () => mode.value === 'dark',
    isLight: () => mode.value === 'light',
    toggleTheme,
    setTheme,
    listenSystemTheme
  }
})