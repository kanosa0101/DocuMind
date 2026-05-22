import { computed } from 'vue'
import { useThemeStore } from '@/stores/theme'

/**
 * 主题工具 composable
 * 提供主题状态和切换方法
 */
export function useTheme() {
  const themeStore = useThemeStore()

  const isDark = computed(() => themeStore.mode === 'dark')
  const isLight = computed(() => themeStore.mode === 'light')

  // 动态玻璃卡片类名
  const glassClass = computed(() =>
    isDark.value ? 'glass-card' : 'glass-card'
  )

  // 切换主题
  const toggleTheme = () => {
    themeStore.toggleTheme()
  }

  // 设置暗色模式
  const setDarkMode = () => {
    themeStore.setTheme('dark')
  }

  // 设置亮色模式
  const setLightMode = () => {
    themeStore.setTheme('light')
  }

  return {
    isDark,
    isLight,
    glassClass,
    toggleTheme,
    setDarkMode,
    setLightMode,
    themeMode: computed(() => themeStore.mode)
  }
}