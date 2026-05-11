<template>
  <div class="ai-lab-page">
    <!-- 功能选择 -->
    <div class="feature-tabs glass-card">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        <span>{{ tab.icon }}</span> {{ tab.label }}
      </button>
    </div>

    <!-- RAG问答 -->
    <div v-if="activeTab === 'rag'" class="rag-section">
      <div class="rag-panel glass-card">
        <h3>🔍 RAG 知识库问答</h3>
        <p class="description">基于文档知识库进行智能问答，系统会检索相关文档并生成答案</p>

        <div class="query-form">
          <textarea
            v-model="ragQuestion"
            placeholder="输入您的问题..."
            rows="4"
          ></textarea>

          <div class="strategy-select">
            <label>检索策略:</label>
            <select v-model="ragStrategy">
              <option value="">默认</option>
              <option value="BM25_FUSION">BM25 融合</option>
              <option value="CROSS_ENCODER">交叉编码器</option>
              <option value="HYBRID">混合策略</option>
            </select>
          </div>

          <button class="query-btn" @click="handleRagQuery" :disabled="ragLoading || !ragQuestion.trim()">
            {{ ragLoading ? '查询中...' : '开始查询' }}
          </button>
        </div>

        <!-- 查询结果 -->
        <div v-if="ragResult" class="rag-result">
          <div class="result-section">
            <h4>📝 答案</h4>
            <div class="answer-box">{{ ragResult.answer }}</div>
          </div>

          <div v-if="ragResult.context" class="result-section">
            <h4>📚 相关上下文</h4>
            <div class="context-box">{{ ragResult.context }}</div>
          </div>

          <div v-if="ragResult.sources?.length" class="result-section">
            <h4>📖 来源文档</h4>
            <div class="sources-list">
              <div v-for="source in ragResult.sources" :key="source" class="source-item">
                {{ source }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 混合搜索 -->
    <div v-if="activeTab === 'search'" class="search-section">
      <div class="search-panel glass-card">
        <h3>🔎 智能文档搜索</h3>
        <p class="description">使用向量检索与BM25混合搜索，找到最相关的文档内容</p>

        <div class="search-form">
          <input
            v-model="searchQuery"
            type="text"
            placeholder="输入搜索关键词..."
          />

          <div class="topk-select">
            <label>返回数量:</label>
            <select v-model="searchTopK">
              <option :value="5">5条</option>
              <option :value="10">10条</option>
              <option :value="20">20条</option>
            </select>
          </div>

          <button class="search-btn" @click="handleSearch" :disabled="searchLoading || !searchQuery.trim()">
            {{ searchLoading ? '搜索中...' : '混合搜索' }}
          </button>
        </div>

        <!-- 搜索结果 -->
        <div v-if="searchResults.length > 0" class="search-results">
          <h4>搜索结果 ({{ searchResults.length }}条)</h4>
          <div class="results-list">
            <div v-for="(result, index) in searchResults" :key="index" class="result-item">
              <div class="result-score">{{ result.score?.toFixed(2) || '-' }}</div>
              <div class="result-content">
                <p>{{ result.content || result.text || result }}</p>
                <span v-if="result.documentId" class="result-doc-id">文档ID: {{ result.documentId }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 文档处理 -->
    <div v-if="activeTab === 'process'" class="process-section">
      <div class="process-panel glass-card">
        <h3>⚡ AI 文档处理</h3>
        <p class="description">对文档内容进行摘要、关键词提取、文本分析</p>

        <div class="process-form">
          <textarea
            v-model="processContent"
            placeholder="输入或粘贴文档内容..."
            rows="8"
          ></textarea>

          <div class="process-options">
            <label class="checkbox">
              <input type="checkbox" v-model="doSummarize" />
              <span>生成摘要</span>
            </label>
            <label class="checkbox">
              <input type="checkbox" v-model="doKeywords" />
              <span>提取关键词</span>
            </label>
            <label class="checkbox">
              <input type="checkbox" v-model="doAnalyze" />
              <span>文本分析</span>
            </label>
          </div>

          <button class="process-btn" @click="handleProcess" :disabled="processLoading || !processContent.trim()">
            {{ processLoading ? '处理中...' : '开始处理' }}
          </button>
        </div>

        <!-- 处理结果 -->
        <div v-if="processResult" class="process-results">
          <div v-if="processResult.summary" class="result-card">
            <h4>摘要</h4>
            <p>{{ processResult.summary }}</p>
            <div class="stats">
              原文 {{ processResult.originalLength }} 字 → 摘要 {{ processResult.summaryLength }} 字
              (压缩率: {{ ((1 - processResult.compressionRatio) * 100).toFixed(1) }}%)
            </div>
          </div>

          <div v-if="processResult.keywords?.length" class="result-card">
            <h4>关键词</h4>
            <div class="keywords-list">
              <span v-for="kw in processResult.keywords" :key="kw.word" class="keyword-tag">
                {{ kw.word }} <small>{{ kw.score.toFixed(2) }}</small>
              </span>
            </div>
          </div>

          <div v-if="processResult.analysis" class="result-card">
            <h4>文本分析</h4>
            <div class="analysis-grid">
              <div class="stat-item">
                <span class="stat-label">总字符</span>
                <span class="stat-value">{{ processResult.analysis.totalCharacters }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">中文字符</span>
                <span class="stat-value">{{ processResult.analysis.chineseCharacters }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">英文字符</span>
                <span class="stat-value">{{ processResult.analysis.englishCharacters }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">数字</span>
                <span class="stat-value">{{ processResult.analysis.digits }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">句子数</span>
                <span class="stat-value">{{ processResult.analysis.sentences }}</span>
              </div>
              <div class="stat-item">
                <span class="stat-label">行数</span>
                <span class="stat-value">{{ processResult.analysis.lines }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 知识库管理 -->
    <div v-if="activeTab === 'kb'" class="kb-section">
      <div class="kb-panel glass-card">
        <h3>📚 知识库管理</h3>
        <p class="description">管理知识库中的文档索引</p>

        <div class="kb-actions">
          <button class="refresh-btn" @click="loadDocuments">
            🔄 刷新列表
          </button>
        </div>

        <div v-if="kbLoading" class="kb-loading">
          <RippleLoader />
        </div>

        <div v-else-if="kbDocuments.length === 0" class="kb-empty">
          <span>📭</span>
          <p>知识库暂无文档</p>
        </div>

        <div v-else class="kb-list">
          <div v-for="doc in kbDocuments" :key="doc" class="kb-item">
            <span class="doc-id">{{ doc }}</span>
            <button class="delete-btn" @click="deleteDocument(doc)">🗑️</button>
          </div>
        </div>

        <!-- 文档索引 -->
        <div class="index-section">
          <h4>添加文档索引</h4>
          <input
            v-model="indexDocId"
            type="text"
            placeholder="文档ID"
          />
          <textarea
            v-model="indexContent"
            placeholder="文档内容..."
            rows="4"
          ></textarea>
          <button @click="indexDocument" :disabled="!indexDocId.trim() || !indexContent.trim()">
            📥 索引文档
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import {
  ragQuery, ragHybridSearch, summarize, extractKeywords, analyzeText
} from '@/api/ai'
import api from '@/api/index'
import RippleLoader from '@/components/common/RippleLoader.vue'

// Tabs
const tabs = [
  { key: 'rag', icon: '🤖', label: 'RAG问答' },
  { key: 'search', icon: '🔍', label: '智能搜索' },
  { key: 'process', icon: '⚡', label: '文档处理' },
  { key: 'kb', icon: '📚', label: '知识库' }
]
const activeTab = ref('rag')

// RAG问答
const ragQuestion = ref('')
const ragStrategy = ref('')
const ragResult = ref<any>(null)
const ragLoading = ref(false)

// 搜索
const searchQuery = ref('')
const searchTopK = ref(5)
const searchResults = ref<any[]>([])
const searchLoading = ref(false)

// 文档处理
const processContent = ref('')
const doSummarize = ref(true)
const doKeywords = ref(true)
const doAnalyze = ref(true)
const processResult = ref<any>(null)
const processLoading = ref(false)

// 知识库
const kbDocuments = ref<string[]>([])
const kbLoading = ref(false)
const indexDocId = ref('')
const indexContent = ref('')

// RAG问答
async function handleRagQuery() {
  ragLoading.value = true
  ragResult.value = null
  try {
    ragResult.value = await ragQuery(ragQuestion.value, ragStrategy.value)
  } catch (error: any) {
    console.error('RAG查询失败:', error.message)
    ragResult.value = { answer: '查询失败: ' + error.message }
  } finally {
    ragLoading.value = false
  }
}

// 搜索
async function handleSearch() {
  searchLoading.value = true
  searchResults.value = []
  try {
    searchResults.value = await ragHybridSearch(searchQuery.value, searchTopK.value)
  } catch (error: any) {
    console.error('搜索失败:', error.message)
  } finally {
    searchLoading.value = false
  }
}

// 文档处理
async function handleProcess() {
  processLoading.value = true
  processResult.value = null
  try {
    const result: any = {}

    if (doSummarize.value) {
      result.summary = await summarize({ content: processContent.value })
    }

    if (doKeywords.value) {
      result.keywords = await extractKeywords({ content: processContent.value, count: 10 })
    }

    if (doAnalyze.value) {
      result.analysis = await analyzeText(processContent.value)
    }

    processResult.value = result
  } catch (error: any) {
    console.error('处理失败:', error.message)
  } finally {
    processLoading.value = false
  }
}

// 知识库管理
async function loadDocuments() {
  kbLoading.value = true
  try {
    const res = await api.get<string[]>('/api/ai/rag/documents')
    kbDocuments.value = res
  } catch (error: any) {
    console.error('加载文档列表失败:', error.message)
  } finally {
    kbLoading.value = false
  }
}

async function indexDocument() {
  try {
    await api.post('/api/ai/rag/index', indexContent.value, {
      params: { documentId: indexDocId.value }
    })
    indexDocId.value = ''
    indexContent.value = ''
    loadDocuments()
  } catch (error: any) {
    console.error('索引失败:', error.message)
  }
}

async function deleteDocument(docId: string) {
  if (!confirm(`确认删除文档 ${docId}?`)) return
  try {
    await api.delete(`/api/ai/rag/document/${docId}`)
    loadDocuments()
  } catch (error: any) {
    console.error('删除失败:', error.message)
  }
}

onMounted(() => {
  loadDocuments()
})
</script>

<style scoped>
.ai-lab-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feature-tabs {
  display: flex;
  gap: 12px;
  padding: 16px;
}

.feature-tabs button {
  padding: 12px 24px;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all var(--transition-base);
}

.feature-tabs button:hover {
  background: rgba(255, 255, 255, 0.5);
}

.feature-tabs button.active {
  background: var(--gradient-primary);
  color: white;
}

.rag-panel, .search-panel, .process-panel, .kb-panel {
  padding: 24px;
}

.description {
  color: var(--color-text-muted);
  margin-bottom: 24px;
}

.query-form, .search-form, .process-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

textarea, input[type="text"] {
  padding: 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--glass-border);
  background: rgba(255, 255, 255, 0.5);
  outline: none;
  resize: vertical;
}

textarea:focus, input:focus {
  border-color: var(--color-primary);
}

.strategy-select, .topk-select {
  display: flex;
  align-items: center;
  gap: 12px;
}

.strategy-select select, .topk-select select {
  padding: 8px 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--glass-border);
  background: rgba(255, 255, 255, 0.5);
}

.query-btn, .search-btn, .process-btn {
  padding: 12px 24px;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  background: var(--gradient-primary);
  color: white;
  transition: all var(--transition-base);
}

.query-btn:disabled, .search-btn:disabled, .process-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.rag-result, .search-results, .process-results {
  margin-top: 24px;
}

.result-section {
  margin-bottom: 16px;
}

.result-section h4 {
  margin-bottom: 8px;
}

.answer-box, .context-box {
  padding: 16px;
  background: rgba(16, 185, 129, 0.1);
  border-radius: var(--radius-md);
  border-left: 4px solid var(--color-success);
}

.sources-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.source-item {
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
}

.results-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.result-item {
  display: flex;
  gap: 16px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: var(--radius-md);
}

.result-score {
  width: 60px;
  height: 60px;
  display: flex;
  justify-content: center;
  align-items: center;
  background: var(--gradient-primary);
  color: white;
  border-radius: var(--radius-md);
  font-weight: 600;
}

.result-content {
  flex: 1;
}

.result-doc-id {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.process-options {
  display: flex;
  gap: 24px;
}

.checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.result-card {
  padding: 16px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: var(--radius-md);
  margin-bottom: 16px;
}

.result-card h4 {
  margin-bottom: 12px;
}

.result-card .stats {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.keywords-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.keyword-tag {
  padding: 6px 12px;
  background: var(--gradient-primary);
  color: white;
  border-radius: var(--radius-full);
  font-size: var(--font-size-sm);
}

.keyword-tag small {
  opacity: 0.7;
}

.analysis-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: var(--radius-md);
}

.stat-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.stat-value {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-primary);
}

.kb-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.refresh-btn {
  padding: 8px 16px;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.5);
}

.kb-loading, .kb-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  gap: 12px;
}

.kb-empty span {
  font-size: 48px;
}

.kb-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 24px;
}

.kb-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: var(--radius-md);
}

.doc-id {
  font-size: var(--font-size-sm);
}

.delete-btn {
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  border: none;
  cursor: pointer;
  background: rgba(239, 68, 68, 0.2);
}

.delete-btn:hover {
  background: var(--color-error);
}

.index-section {
  padding-top: 16px;
  border-top: 1px solid var(--glass-border);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.index-section h4 {
  margin-bottom: 8px;
}

.index-section button {
  padding: 12px 24px;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  background: var(--gradient-success);
  color: white;
}

.index-section button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>