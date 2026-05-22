import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useLayoutStore = defineStore('layout', () => {
  // 侧边栏折叠状态
  const sidebarCollapsed = ref(
    localStorage.getItem('documind-sidebar-collapsed') === 'true'
  )

  // Copilot折叠状态
  const copilotCollapsed = ref(
    localStorage.getItem('documind-copilot-collapsed') === 'true'
  )

  // 计算侧边栏宽度
  const sidebarWidth = computed(() =>
    sidebarCollapsed.value ? 72 : 240
  )

  // 计算Copilot宽度
  const copilotWidth = computed(() =>
    copilotCollapsed.value ? 48 : 320
  )

  // 切换侧边栏折叠
  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
    localStorage.setItem('documind-sidebar-collapsed', String(sidebarCollapsed.value))
  }

  // 设置侧边栏状态
  function setSidebarCollapsed(collapsed: boolean) {
    sidebarCollapsed.value = collapsed
    localStorage.setItem('documind-sidebar-collapsed', String(collapsed))
  }

  // 切换Copilot折叠
  function toggleCopilot() {
    copilotCollapsed.value = !copilotCollapsed.value
    localStorage.setItem('documind-copilot-collapsed', String(copilotCollapsed.value))
  }

  // 设置Copilot状态
  function setCopilotCollapsed(collapsed: boolean) {
    copilotCollapsed.value = collapsed
    localStorage.setItem('documind-copilot-collapsed', String(collapsed))
  }

  // 展开/收起全部（快捷操作）
  function expandAll() {
    setSidebarCollapsed(false)
    setCopilotCollapsed(false)
  }

  function collapseAll() {
    setSidebarCollapsed(true)
    setCopilotCollapsed(true)
  }

  return {
    sidebarCollapsed,
    copilotCollapsed,
    sidebarWidth,
    copilotWidth,
    toggleSidebar,
    setSidebarCollapsed,
    toggleCopilot,
    setCopilotCollapsed,
    expandAll,
    collapseAll
  }
})