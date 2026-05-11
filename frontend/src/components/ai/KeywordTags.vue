<template>
  <div class="keyword-tags">
    <div class="tags-header">
      <span class="tag-icon">🏷️</span>
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

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.keyword-tag {
  padding: 8px 16px;
  border-radius: var(--radius-full);
  background: var(--gradient-primary);
  color: white;
  font-size: var(--font-size-sm);
  cursor: pointer;
  transition: all var(--transition-base);
}

.keyword-tag:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.empty {
  color: var(--color-text-muted);
  text-align: center;
  padding: 24px;
}
</style>