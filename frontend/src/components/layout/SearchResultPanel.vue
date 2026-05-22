<template>
  <div class="search-result-panel">
    <!-- 问答模式显示 -->
    <template v-if="isQAMode">
      <div class="qa-answer" v-if="searchResults?.answer">
        <div class="answer-header">
          <Lightbulb class="answer-icon" :size="18" />
          <span class="answer-title">AI答案</span>
        </div>
        <div class="answer-content">{{ searchResults.answer }}</div>
      </div>
      <div class="sources-list" v-if="searchResults?.sources?.length">
        <div class="sources-header">
          <Library class="sources-icon" :size="18" />
          <span class="sources-title">来源文档</span>
        </div>
        <div class="source-item" v-for="source in searchResults.sources" :key="source.id">
          <span class="source-title">{{ source.title }}</span>
          <span class="source-similarity">相似度: {{ Math.round(source.similarity * 100) }}%</span>
          <button class="source-open-btn" @click="$emit('openDocument', source.id)">打开</button>
        </div>
      </div>
    </template>

    <!-- 关键词模式显示 -->
    <template v-else>
      <div class="documents-list" v-if="searchResults?.documents?.length">
        <div class="documents-header">
          <FileText class="documents-icon" :size="18" />
          <span class="documents-title">相关文档</span>
          <span class="documents-count">{{ searchResults.documents.length }}个</span>
        </div>
        <div class="document-item" v-for="doc in searchResults.documents" :key="doc.id">
          <span class="document-title">{{ doc.title }}</span>
          <span class="document-similarity">相似度: {{ Math.round(doc.similarity * 100) }}%</span>
          <button class="document-open-btn" @click="$emit('openDocument', doc.id)">打开</button>
        </div>
      </div>
      <div class="no-results" v-else>
        <SearchX class="no-results-icon" :size="18" />
        <span class="no-results-text">未找到相关文档</span>
      </div>
    </template>

    <!-- 加载状态 -->
    <div class="loading" v-if="loading">
      <Loader2 class="loading-icon is-spinning" :size="18" />
      <span class="loading-text">正在搜索...</span>
    </div>

    <!-- 错误状态 -->
    <div class="error" v-if="error">
      <CircleX class="error-icon" :size="18" />
      <span class="error-text">{{ error }}</span>
    </div>

    <!-- 推荐问题 -->
    <div class="recommended-questions" v-if="isQAMode && recommendedQuestions.length">
      <div class="recommended-header">
        <Lightbulb class="recommended-icon" :size="18" />
        <span class="recommended-title">您可能还想问</span>
      </div>
      <div class="recommended-item" v-for="q in recommendedQuestions" :key="q">
        <button class="recommended-btn" @click="$emit('searchQuestion', q)">{{ q }}</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { GlobalSearchResult } from '@/api/globalSearch'
import { Lightbulb, Library, FileText, SearchX, Loader2, CircleX } from '@lucide/vue'

const props = defineProps<{
  searchResults: GlobalSearchResult | null
  loading: boolean
  error: string | null
}>()

defineEmits<{
  (e: 'openDocument', id: string): void
  (e: 'searchQuestion', question: string): void
}>()

const isQAMode = computed(() => props.searchResults?.strategy === 'QA')

const recommendedQuestions = ref<string[]>([])
</script>

<style scoped>
.search-result-panel {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin-top: 8px;
  background: var(--glass-light-bg);
  border-radius: var(--radius-lg);
  padding: 16px;
  max-height: 400px;
  overflow-y: auto;
  z-index: 100;
}

[data-theme="dark"] .search-result-panel {
  background: var(--glass-dark-bg);
}

.qa-answer {
  margin-bottom: 16px;
}

.answer-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.answer-icon {
  color: var(--aurora-emerald);
}

[data-theme="dark"] .answer-icon {
  color: var(--aurora-emerald-light);
}

.answer-title {
  font-weight: 600;
  color: var(--text-primary);
}

.answer-content {
  padding: 12px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  line-height: 1.6;
}

.sources-list {
  margin-top: 16px;
}

.sources-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.sources-icon {
  color: var(--aurora-cyan);
}

[data-theme="dark"] .sources-icon {
  color: var(--aurora-cyan-light);
}

.sources-title {
  font-weight: 600;
}

.source-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  margin-bottom: 8px;
}

.source-title {
  flex: 1;
  font-weight: 500;
}

.source-similarity {
  color: var(--text-secondary);
  font-size: 12px;
}

.source-open-btn {
  padding: 4px 12px;
  background: var(--accent-color);
  color: white;
  border-radius: var(--radius-sm);
  border: none;
  cursor: pointer;
}

.documents-list {
  margin-top: 8px;
}

.documents-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.documents-icon {
  color: var(--aurora-cyan);
}

[data-theme="dark"] .documents-icon {
  color: var(--aurora-cyan-light);
}

.documents-title {
  font-weight: 600;
}

.documents-count {
  color: var(--text-secondary);
  font-size: 12px;
}

.document-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  margin-bottom: 8px;
}

.document-title {
  flex: 1;
  font-weight: 500;
}

.document-similarity {
  color: var(--text-secondary);
  font-size: 12px;
}

.document-open-btn {
  padding: 4px 12px;
  background: var(--accent-color);
  color: white;
  border-radius: var(--radius-sm);
  border: none;
  cursor: pointer;
}

.no-results {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
  color: var(--text-secondary);
}

.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
}

.loading-icon {
  color: var(--aurora-cyan);
}

[data-theme="dark"] .loading-icon {
  color: var(--aurora-cyan-light);
}

.is-spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.error {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  color: var(--error-color);
}

.error-icon {
  color: var(--color-error);
}

.recommended-questions {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--border-color);
}

.recommended-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.recommended-icon {
  color: var(--aurora-emerald);
}

[data-theme="dark"] .recommended-icon {
  color: var(--aurora-emerald-light);
}

.recommended-title {
  font-weight: 600;
}

.recommended-item {
  margin-bottom: 8px;
}

.recommended-btn {
  width: 100%;
  padding: 8px 12px;
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  border: none;
  text-align: left;
  cursor: pointer;
  color: var(--text-primary);
}
</style>