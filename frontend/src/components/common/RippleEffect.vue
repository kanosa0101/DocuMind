<template>
  <div
    class="ripple-container"
    @click="triggerRipple"
  >
    <slot />

    <!-- 涟漪效果层 -->
    <span
      v-for="(ripple, index) in ripples"
      :key="index"
      class="ripple"
      :style="getRippleStyle(ripple)"
    />
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'

interface Ripple {
  x: number
  y: number
  id: number
}

const ripples = reactive<Ripple[]>([])

function triggerRipple(e: MouseEvent) {
  const target = e.currentTarget as HTMLElement
  const rect = target.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top

  const ripple: Ripple = {
    x,
    y,
    id: Date.now()
  }

  ripples.push(ripple)

  // 动画结束后移除
  setTimeout(() => {
    const index = ripples.findIndex(r => r.id === ripple.id)
    if (index > -1) {
      ripples.splice(index, 1)
    }
  }, 600)
}

function getRippleStyle(ripple: Ripple) {
  return {
    left: ripple.x + 'px',
    top: ripple.y + 'px'
  }
}
</script>

<style scoped>
.ripple-container {
  position: relative;
  overflow: hidden;
  cursor: pointer;
}

.ripple {
  position: absolute;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.6), rgba(8, 145, 178, 0.3), transparent);
  animation: ripple-aurora 0.6s ease-out forwards;
  pointer-events: none;
  transform: translate(-50%, -50%);
}

@keyframes ripple-aurora {
  0% {
    width: 10px;
    height: 10px;
    opacity: 1;
    background: radial-gradient(circle, rgba(8, 145, 178, 0.8), rgba(16, 185, 129, 0.4), transparent);
  }
  50% {
    background: radial-gradient(circle, rgba(59, 130, 246, 0.4), transparent);
  }
  100% {
    width: 200px;
    height: 200px;
    opacity: 0;
    background: transparent;
  }
}
</style>