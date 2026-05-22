<template>
  <div class="ai-summary">
    <div class="summary-header">
      <CheckCircle class="success-icon" :size="24" />
      <span class="summary-label">AI 摘要</span>
      <span class="compression-badge">{{ ratio }}% 压缩</span>
    </div>
    <div class="summary-content">
      <p v-if="summary">{{ summary }}</p>
      <p v-else class="empty">等待AI处理...</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { CheckCircle } from '@lucide/vue'

defineProps<{
  summary: string
  ratio: number
}>()
</script>

<style scoped>
.ai-summary {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.success-icon {
  color: var(--aurora-emerald);
}

[data-theme="dark"] .success-icon {
  color: var(--aurora-emerald-light);
}

.summary-label {
  font-weight: 600;
  color: var(--color-text);
}

.compression-badge {
  padding: 4px 12px;
  border-radius: var(--radius-full);
  background: rgba(16, 185, 129, 0.15);
  color: var(--aurora-emerald);
  font-size: var(--font-size-xs);
  font-weight: 500;
}

[data-theme="dark"] .compression-badge {
  background: rgba(16, 185, 129, 0.25);
  text-shadow: var(--glow-text-emerald);
}

.summary-content {
  padding: 16px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.3);
}

[data-theme="dark"] .summary-content {
  background: rgba(26, 26, 26, 0.4);
  border: 1px solid var(--glass-dark-border);
}

.summary-content p {
  font-size: var(--font-size-sm);
  line-height: 1.8;
  color: var(--color-text);
}

.summary-content .empty {
  color: var(--color-text-muted);
  text-align: center;
}
</style>