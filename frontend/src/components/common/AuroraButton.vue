<template>
  <button
    class="aurora-button"
    :class="[sizeClass, { loading: loading, glow: glow }]"
    :disabled="disabled || loading"
    @click="handleClick"
  >
    <!-- 加载状态 -->
    <span v-if="loading" class="loading-spinner">
      <svg viewBox="0 0 24 24" width="18" height="18" fill="currentColor" class="spin">
        <path d="M12 4V1L8 5l4 4V6c3.31 0 6 2.69 6 6 0 1.01-.25 1.97-.7 2.8l1.46 1.46C19.54 15.35 20 13.7 20 12c0-4.42-3.58-8-8-8zm0 14c-3.31 0-6-2.69-6-6 0-1.01.25-1.97.7-2.8L5.24 7.74C4.46 8.65 4 10.3 4 12c0 4.42 3.58 8 8 8v3l4-4-4-4v3c3.31 0 6-2.69 6-6 0-1.01-.25-1.97-.7-2.8l1.46 1.46C19.54 15.35 20 13.7 20 12c0-4.42-3.58-8-8-8z"/>
      </svg>
    </span>

    <!-- 正常状态 -->
    <span v-else class="button-content">
      <span v-if="icon" class="button-icon">{{ icon }}</span>
      <span class="button-text">{{ text }}</span>
    </span>

    <!-- 涟漪效果容器 -->
    <span v-if="ripple.show" class="ripple-effect" :style="rippleStyle"></span>
  </button>
</template>

<script setup lang="ts">
import { computed, reactive } from 'vue'

interface Props {
  text: string
  icon?: string
  size?: 'sm' | 'md' | 'lg'
  glow?: boolean
  loading?: boolean
  disabled?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  size: 'md',
  glow: true,
  loading: false,
  disabled: false
})

const emit = defineEmits<{
  click: []
}>()

const sizeClass = computed(() => `btn-${props.size}`)

// 涟漪效果
const ripple = reactive({
  show: false,
  x: 0,
  y: 0
})

const rippleStyle = computed(() => ({
  left: ripple.x + 'px',
  top: ripple.y + 'px'
}))

function handleClick(e: MouseEvent) {
  // 触发涟漪效果
  const target = e.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  ripple.x = e.clientX - rect.left
  ripple.y = e.clientY - rect.top
  ripple.show = true

  // 动画结束后隐藏涟漪
  setTimeout(() => {
    ripple.show = false
  }, 600)

  emit('click')
}
</script>

<style scoped>
.aurora-button {
  position: relative;
  overflow: hidden;
  border: none;
  cursor: pointer;
  font-weight: 600;
  transition: all var(--transition-base);
  background: var(--gradient-aurora);
  color: white;
  box-shadow: var(--glow-aurora);
}

/* 尺寸 */
.btn-sm {
  padding: 8px 16px;
  font-size: var(--font-size-sm);
  border-radius: var(--radius-sm);
}

.btn-md {
  padding: 12px 24px;
  font-size: var(--font-size-base);
  border-radius: var(--radius-md);
}

.btn-lg {
  padding: 16px 32px;
  font-size: var(--font-size-lg);
  border-radius: var(--radius-lg);
}

/* 悬停效果 */
.aurora-button:hover:not(:disabled) {
  transform: translateY(-2px) scale(1.02);
  box-shadow: 0 0 40px rgba(8, 145, 178, 0.6), 0 0 60px rgba(59, 130, 246, 0.4);
}

/* 点击效果 */
.aurora-button:active:not(:disabled) {
  transform: scale(0.95);
}

/* 禁用状态 */
.aurora-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* 发光效果 */
.aurora-button.glow {
  animation: glow-pulse 2s ease-in-out infinite;
}

.aurora-button.glow:hover {
  animation: none;
}

/* 按钮内容 */
.button-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.button-icon {
  font-size: 1.1em;
}

.button-text {
  white-space: nowrap;
}

/* 加载状态 */
.loading-spinner {
  display: flex;
  align-items: center;
  justify-content: center;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes glow-pulse {
  0%, 100% {
    box-shadow: var(--glow-cyan-soft), var(--glow-blue-soft);
  }
  50% {
    box-shadow: var(--glow-cyan), var(--glow-blue);
  }
}

/* 涟漪效果 */
.ripple-effect {
  position: absolute;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.8), rgba(8, 145, 178, 0.4), transparent);
  animation: ripple-expand 0.6s ease-out forwards;
  pointer-events: none;
  transform: translate(-50%, -50%);
}

@keyframes ripple-expand {
  0% {
    width: 10px;
    height: 10px;
    opacity: 1;
  }
  100% {
    width: 200px;
    height: 200px;
    opacity: 0;
  }
}
</style>