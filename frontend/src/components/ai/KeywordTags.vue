<template>
  <div class="keyword-tags">
    <div class="tags-header">
      <Tag class="tag-icon" :size="20" />
      <span>关键词提取</span>
    </div>
    <div class="tags-container">
      <span
        v-for="keyword in keywords"
        :key="keyword.word"
        class="keyword-tag"
        :style="{ opacity: 0.5 + keyword.score * 0.5 }"
      >
        {{ keyword.word }}
      </span>
      <p v-if="keywords.length === 0" class="empty">等待AI处理...</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Tag } from '@lucide/vue'

defineProps<{
  keywords: { word: string; score: number }[]
}>()
</script>

<style scoped>
.keyword-tags {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.tags-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--color-text);
}

.tag-icon {
  color: var(--aurora-cyan);
}

[data-theme="dark"] .tag-icon {
  color: var(--aurora-cyan-light);
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.keyword-tag {
  padding: 8px 16px;
  border-radius: var(--radius-full);
  background: var(--gradient-aurora);
  color: white;
  font-size: var(--font-size-sm);
  cursor: pointer;
  transition: all var(--transition-base);
}

[data-theme="dark"] .keyword-tag {
  box-shadow: var(--glow-cyan-soft);
}

.keyword-tag:hover {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 0 20px rgba(8, 145, 178, 0.4), 0 0 30px rgba(59, 130, 246, 0.3);
}

[data-theme="dark"] .keyword-tag:hover {
  box-shadow: var(--glow-cyan), var(--glow-blue);
}

.empty {
  color: var(--color-text-muted);
  text-align: center;
  padding: 24px;
}
</style>