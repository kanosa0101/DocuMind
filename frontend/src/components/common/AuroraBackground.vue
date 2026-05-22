<template>
  <div class="aurora-background">
    <!-- SVG Aurora Waves -->
    <svg class="aurora-waves" viewBox="0 0 1440 400" preserveAspectRatio="none">
      <!-- Wave 1 - Cyan -->
      <path
        class="wave wave-1"
        d="M0,100 C360,150 720,50 1080,100 C1260,120 1440,80 1440,80 L1440,400 L0,400 Z"
        fill="rgba(8, 145, 178, 0.15)"
      />
      <!-- Wave 2 - Emerald -->
      <path
        class="wave wave-2"
        d="M0,120 C300,80 600,140 900,100 C1200,60 1440,120 1440,120 L1440,400 L0,400 Z"
        fill="rgba(16, 185, 129, 0.1)"
      />
      <!-- Wave 3 - Blue -->
      <path
        class="wave wave-3"
        d="M0,150 C400,100 800,180 1200,130 C1320,110 1440,150 1440,150 L1440,400 L0,400 Z"
        fill="rgba(59, 130, 246, 0.08)"
      />
    </svg>

    <!-- Canvas Particles -->
    <canvas ref="particleCanvas" class="aurora-particles"></canvas>

    <!-- Gradient Overlay -->
    <div class="gradient-overlay"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const particleCanvas = ref<HTMLCanvasElement | null>(null)
let animationId: number | null = null
let particles: Particle[] = []
let resizeHandler: (() => void) | null = null // 保存resize函数引用

interface Particle {
  x: number
  y: number
  size: number
  speedY: number
  speedX: number
  opacity: number
  color: string
  fadeSpeed: number
}

// Aurora 颜色池
const colors = [
  'rgba(8, 145, 178, 0.6)',   // cyan
  'rgba(16, 185, 129, 0.5)',  // emerald
  'rgba(59, 130, 246, 0.5)',  // blue
  'rgba(139, 92, 246, 0.4)'   // purple
]

// 创建粒子
function createParticle(width: number, height: number): Particle {
  return {
    x: Math.random() * width,
    y: height + Math.random() * 100,
    size: 2 + Math.random() * 4,
    speedY: 0.3 + Math.random() * 0.5,
    speedX: (Math.random() - 0.5) * 0.2,
    opacity: 0.3 + Math.random() * 0.5,
    color: colors[Math.floor(Math.random() * colors.length)],
    fadeSpeed: 0.002 + Math.random() * 0.003
  }
}

// 初始化粒子
function initParticles(width: number, height: number, count: number = 40) {
  particles = []
  for (let i = 0; i < count; i++) {
    const p = createParticle(width, height)
    p.y = Math.random() * height // 初始分布在整个画布
    particles.push(p)
  }
}

// 动画循环
function animate(canvas: HTMLCanvasElement) {
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const width = canvas.width
  const height = canvas.height

  // 清空画布
  ctx.clearRect(0, 0, width, height)

  // 更新并绘制粒子
  particles.forEach((p, index) => {
    // 更新位置
    p.y -= p.speedY
    p.x += p.speedX
    p.opacity -= p.fadeSpeed

    // 绘制粒子
    if (p.opacity > 0) {
      ctx.beginPath()
      ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2)
      ctx.fillStyle = p.color.replace(/[\d.]+\)$/, `${p.opacity})`)
      ctx.fill()
    }

    // 重生粒子
    if (p.opacity <= 0 || p.y < -10) {
      particles[index] = createParticle(width, height)
    }
  })
}

onMounted(() => {
  const canvas = particleCanvas.value
  if (!canvas) return

  // 设置画布尺寸
  resizeHandler = () => {
    canvas.width = window.innerWidth
    canvas.height = window.innerHeight
    initParticles(canvas.width, canvas.height)
  }

  resizeHandler()
  window.addEventListener('resize', resizeHandler)

  // 开始动画 (限制30fps以优化性能)
  let lastTime = 0
  const fps = 30
  const interval = 1000 / fps

  const animateWithLimit = (time: number) => {
    if (time - lastTime >= interval) {
      lastTime = time
      animate(canvas)
    }
    animationId = requestAnimationFrame(animateWithLimit)
  }

  animationId = requestAnimationFrame(animateWithLimit)
})

onUnmounted(() => {
  // 正确清理动画
  if (animationId) {
    cancelAnimationFrame(animationId)
    animationId = null
  }
  // 正确清理resize监听器
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
    resizeHandler = null
  }
  // 清空粒子数组
  particles = []
})
</script>

<style scoped>
.aurora-background {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background: var(--obsidian-base);
}

/* SVG Aurora Waves */
.aurora-waves {
  position: absolute;
  width: 100%;
  height: 40%;
  top: 0;
  left: 0;
  opacity: 0.8;
}

.wave {
  animation: wave-float 20s ease-in-out infinite;
  transform-origin: bottom;
}

.wave-1 {
  animation-delay: 0s;
}

.wave-2 {
  animation-delay: -5s;
}

.wave-3 {
  animation-delay: -10s;
}

@keyframes wave-float {
  0%, 100% {
    transform: translateY(0) scaleY(1);
  }
  50% {
    transform: translateY(-20px) scaleY(1.1);
  }
}

/* Canvas Particles */
.aurora-particles {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0.6;
}

/* Gradient Overlay */
.gradient-overlay {
  position: absolute;
  inset: 0;
  background: radial-gradient(
    ellipse at 50% 0%,
    rgba(8, 145, 178, 0.1) 0%,
    rgba(16, 185, 129, 0.05) 30%,
    transparent 60%
  );
  animation: aurora-pulse 15s ease-in-out infinite;
}
</style>