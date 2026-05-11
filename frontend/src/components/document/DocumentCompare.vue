<template>
  <div class="document-compare">
    <!-- 左侧：原文档 -->
    <div class="original-panel glass-card">
      <div class="panel-header">
        <h3>原文档</h3>
        <span class="file-name">{{ fileName || '等待上传' }}</span>
      </div>
      <div class="panel-content">
        <div v-if="!originalContent" class="empty-placeholder">
          <span class="empty-icon">📄</span>
          <p>上传文件后显示原文</p>
        </div>
        <pre v-else class="content-text">{{ originalContent }}</pre>
      </div>
    </div>

    <!-- 右侧：AI结果 -->
    <div class="ai-panel glass-card">
      <div class="panel-header">
        <h3>AI处理结果</h3>
        <div class="tab-buttons">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
          </button>
        </div>
      </div>

      <div class="panel-content">
        <!-- 摘要 -->
        <div v-if="activeTab === 'summary'" class="result-area">
          <AiSummary
            :summary="aiResult.summary"
            :ratio="aiResult.compressionRatio"
          />
        </div>

        <!-- 关键词 -->
        <div v-if="activeTab === 'keywords'" class="result-area">
          <KeywordTags :keywords="aiResult.keywords" />
        </div>

        <!-- 分析 -->
        <div v-if="activeTab === 'analyze'" class="result-area">
          <AnalysisChart :analysis="aiResult.analysis" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import AiSummary from '@/components/ai/AiSummary.vue'
import KeywordTags from '@/components/ai/KeywordTags.vue'
import AnalysisChart from '@/components/ai/AnalysisChart.vue'
import { summarize, extractKeywords, analyzeText } from '@/api/ai'
import type { TextSummarizeVO, KeywordExtractVO, TextAnalyzeVO } from '@/types/api'

const props = defineProps<{
  fileId: string | null
  originalContent: string
}>()

const fileName = ref('')
const activeTab = ref('summary')
const tabs = [
  { key: 'summary', label: '摘要' },
  { key: 'keywords', label: '关键词' },
  { key: 'analyze', label: '分析' }
]

const aiResult = ref({
  summary: '',
  compressionRatio: 0,
  keywords: [] as { word: string; score: number }[],
  analysis: null as TextAnalyzeVO | null
})

// 监听内容变化，触发AI处理
watch(() => props.originalContent, async (content) => {
  if (!content) return

  try {
    // 摘要
    const summaryVO = await summarize({ content })
    aiResult.value.summary = summaryVO.summary
    aiResult.value.compressionRatio = summaryVO.compressionRatio

    // 关键词
    const keywordsVO = await extractKeywords({ content, count: 10 })
    aiResult.value.keywords = keywordsVO.keywords

    // 分析
    aiResult.value.analysis = await analyzeText(content)
  } catch (error) {
    console.error('AI处理失败:', error)
  }
})
</script>

<style scoped>
.document-compare {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  height: 100%;
}

.original-panel, .ai-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--glass-border);
}

.panel-header h3 {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-text);
}

.file-name {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.tab-buttons {
  display: flex;
  gap: 8px;
}

.tab-buttons button {
  padding: 8px 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--glass-border);
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all var(--transition-base);
}

.tab-buttons button.active {
  background: var(--gradient-primary);
  color: white;
  border-color: transparent;
}

.panel-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.empty-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: var(--color-text-muted);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.content-text {
  font-size: var(--font-size-sm);
  line-height: 1.8;
  white-space: pre-wrap;
}

.result-area {
  animation: fade-in 0.3s ease;
}
</style>