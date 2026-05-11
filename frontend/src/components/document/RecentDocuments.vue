<template>
  <div class="recent-documents">
    <div v-if="documents.length === 0" class="empty">
      <span class="empty-icon">📂</span>
      <p>暂无文档</p>
    </div>
    <div v-else class="doc-grid">
      <div v-for="doc in documents" :key="doc.id" class="doc-item">
        <span class="doc-icon">📄</span>
        <span class="doc-title">{{ doc.title }}</span>
        <span class="doc-time">{{ formatDate(doc.createTime) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { DocumentVO } from '@/types/api'

defineProps<{
  documents: DocumentVO[]
}>()

const formatDate = (date: string) => {
  return new Date(date).toLocaleDateString()
}
</script>

<style scoped>
.recent-documents {
  min-height: 200px;
}

.empty {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 12px;
  color: var(--color-text-muted);
}

.empty-icon {
  font-size: 48px;
}

.doc-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
}

.doc-item {
  padding: 16px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.5);
  display: flex;
  flex-direction: column;
  gap: 8px;
  cursor: pointer;
  transition: all var(--transition-base);
}

.doc-item:hover {
  background: rgba(255, 255, 255, 0.8);
  transform: translateY(-2px);
}

.doc-icon {
  font-size: 32px;
}

.doc-title {
  font-weight: 500;
  color: var(--color-text);
}

.doc-time {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}
</style>