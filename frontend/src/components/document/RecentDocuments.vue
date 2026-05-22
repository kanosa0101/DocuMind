<template>
  <div class="recent-documents">
    <div v-if="documents.length === 0" class="empty">
      <FolderOpen class="empty-icon" :size="36" />
      <p>暂无文档</p>
    </div>
    <div v-else class="doc-list">
      <div v-for="doc in documents" :key="doc.fileUuid" class="doc-item" @click="$emit('select', doc)">
        <FileText class="doc-icon" :size="24" />
        <div class="doc-info">
          <span class="doc-title">{{ doc.originalName || doc.fileName }}</span>
          <span class="doc-time">{{ formatDate(doc.createTime) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { FileInfo } from '@/api/file'
import { FolderOpen, FileText } from '@lucide/vue'

defineProps<{
  documents: FileInfo[]
}>()

defineEmits<{
  select: [doc: FileInfo]
}>()

const formatDate = (date: string) => {
  return new Date(date).toLocaleDateString()
}
</script>

<style scoped>
.recent-documents {
  height: 100%;
  overflow-y: auto;
  padding: 16px;
}

.empty {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 8px;
  color: var(--color-text-muted);
  height: 100%;
}

.empty-icon {
  color: var(--color-text-muted);
}

.doc-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.doc-item {
  padding: 12px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

[data-theme="dark"] .doc-item {
  background: rgba(26, 26, 26, 0.3);
  border: 1px solid var(--glass-dark-border);
}

.doc-item:hover {
  background: rgba(8, 145, 178, 0.1);
  border-color: rgba(8, 145, 178, 0.3);
}

[data-theme="dark"] .doc-item:hover {
  background: rgba(8, 145, 178, 0.15);
  box-shadow: var(--glow-cyan-soft);
}

.doc-icon {
  color: var(--aurora-cyan);
}

[data-theme="dark"] .doc-icon {
  color: var(--aurora-cyan-light);
}

.doc-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-width: 0;
}

.doc-title {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-time {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

/* 滚动条 */
.recent-documents::-webkit-scrollbar {
  width: 4px;
}

.recent-documents::-webkit-scrollbar-track {
  background: transparent;
}

[data-theme="dark"] .recent-documents::-webkit-scrollbar-track {
  background: rgba(26, 26, 26, 0.2);
}

.recent-documents::-webkit-scrollbar-thumb {
  background: rgba(8, 145, 178, 0.3);
  border-radius: 2px;
}

[data-theme="dark"] .recent-documents::-webkit-scrollbar-thumb {
  background: rgba(8, 145, 178, 0.4);
}
</style>