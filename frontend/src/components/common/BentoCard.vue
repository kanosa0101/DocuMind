<template>
  <div
    class="bento-card glass-card"
    :class="glowClass"
  >
    <!-- 卡片头部 -->
    <div v-if="title" class="card-header">
      <h3 class="card-title">{{ title }}</h3>
      <span v-if="subtitle" class="card-subtitle">{{ subtitle }}</span>
    </div>

    <!-- 卡片内容 -->
    <div class="card-content">
      <slot />
    </div>

    <!-- 发光效果层 -->
    <div v-if="glow !== 'none'" class="glow-layer"></div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  title?: string
  subtitle?: string
  glow?: 'cyan' | 'emerald' | 'blue' | 'aurora' | 'none'
}

const props = withDefaults(defineProps<Props>(), {
  glow: 'none'
})

// 发光类名
const glowClass = computed(() => `glow-${props.glow}`)
</script>

<style scoped>
.bento-card {
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-lg);
}

[data-theme="dark"] .bento-card {
  background: var(--glass-dark-bg);
  border: 1px solid var(--glass-dark-border);
}

[data-theme="dark"] .bento-card:hover {
  background: rgba(26, 26, 26, 0.9);
  box-shadow: var(--shadow-lg), var(--glow-cyan-soft);
}

/* 卡片头部 */
.card-header {
  padding: 16px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  flex-shrink: 0;
}

[data-theme="dark"] .card-header {
  border-bottom: 1px solid var(--glass-dark-border);
}

.card-title {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
}

[data-theme="dark"] .card-title {
  color: var(--color-text);
}

.card-subtitle {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
  margin-top: 4px;
}

[data-theme="dark"] .card-subtitle {
  color: rgba(255, 255, 255, 0.5);
}

/* 卡片内容 */
.card-content {
  flex: 1;
  padding: 0;
  overflow: hidden;
  min-height: 0;
}

/* 发光效果 */
.glow-layer {
  position: absolute;
  inset: -2px;
  border-radius: inherit;
  pointer-events: none;
  opacity: 0;
  transition: opacity var(--transition-base);
}

.bento-card:hover .glow-layer {
  opacity: 1;
}

.glow-cyan .glow-layer {
  box-shadow: inset 0 0 20px rgba(8, 145, 178, 0.2), var(--glow-cyan-soft);
}

[data-theme="dark"] .glow-cyan .glow-layer {
  box-shadow: inset 0 0 20px rgba(8, 145, 178, 0.3), var(--glow-cyan);
}

.glow-emerald .glow-layer {
  box-shadow: inset 0 0 20px rgba(16, 185, 129, 0.2), var(--glow-emerald-soft);
}

[data-theme="dark"] .glow-emerald .glow-layer {
  box-shadow: inset 0 0 20px rgba(16, 185, 129, 0.3), var(--glow-emerald);
}

.glow-blue .glow-layer {
  box-shadow: inset 0 0 20px rgba(59, 130, 246, 0.2), var(--glow-blue-soft);
}

[data-theme="dark"] .glow-blue .glow-layer {
  box-shadow: inset 0 0 20px rgba(59, 130, 246, 0.3), var(--glow-blue);
}

.glow-aurora .glow-layer {
  box-shadow: inset 0 0 30px rgba(8, 145, 178, 0.15), var(--glow-aurora);
}

[data-theme="dark"] .glow-aurora .glow-layer {
  box-shadow: inset 0 0 30px rgba(8, 145, 178, 0.25), var(--glow-aurora);
}
</style>