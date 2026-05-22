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
        <component :is="tab.iconComponent" class="tab-icon" :size="24" />
        <div class="tab-info">
          <span class="tab-label">{{ tab.label }}</span>
          <span class="tab-desc">{{ tab.desc }}</span>
        </div>
      </button>
    </div>

    <!-- RAG问答 -->
    <div v-if="activeTab === 'rag'" class="rag-section">
      <div class="rag-panel glass-card">
        <h3><Search class="inline-icon" :size="20" /> RAG 知识库问答</h3>
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
            <h4><PenLine class="inline-icon" :size="16" /> 答案</h4>
            <div class="answer-box">
              {{ ragResult.answer }}
              <span v-if="ragStreaming" class="typing-cursor">▌</span>
            </div>
          </div>

          <div v-if="ragResult.context" class="result-section">
            <h4><Library class="inline-icon" :size="16" /> 相关上下文</h4>
            <div class="context-box">{{ ragResult.context }}</div>
          </div>

          <div v-if="ragResult.sources?.length" class="result-section">
            <h4><BookOpen class="inline-icon" :size="16" /> 来源文档</h4>
            <div class="sources-list">
              <div v-for="(source, idx) in ragResult.sources" :key="idx" class="source-item">
                <FileText class="inline-icon-sm" :size="14" /> {{ source.title || source.id || source }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 混合搜索 -->
    <div v-if="activeTab === 'search'" class="search-section">
      <div class="search-panel glass-card">
        <h3><Search class="inline-icon" :size="20" /> 智能文档搜索</h3>
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
              <div class="result-score">{{ (result.similarity || result.finalScore || result.score || 0).toFixed(2) }}</div>
              <div class="result-content">
                <div class="result-title">{{ result.title || '文档 ' + (result.id || index) }}</div>
                <p class="result-text">{{ (result.content || '').substring(0, 200) || '(无内容预览)' }}{{ (result.content || '').length > 200 ? '...' : '' }}</p>
                <div class="result-meta">
                  <span v-if="result.id" class="result-doc-id">ID: {{ result.id }}</span>
                  <span v-if="result.source" class="result-source">{{ result.source === 'vector' ? '向量检索' : '关键词检索' }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 文档处理 -->
    <div v-if="activeTab === 'process'" class="process-section">
      <div class="process-panel glass-card">
        <h3><Zap class="inline-icon" :size="20" /> AI 文档处理</h3>
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
          <div v-if="processResult.error" class="result-card error-card">
            <h4><XCircle class="inline-icon" :size="16" /> 处理失败</h4>
            <p class="error-message">{{ processResult.error }}</p>
            <p class="error-hint">请检查AI服务配置或稍后重试</p>
          </div>

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
        <h3><Library class="inline-icon" :size="20" /> 知识库管理</h3>
        <p class="description">管理知识库中的文档索引，选择已创建的文档加入知识库</p>

        <div class="kb-actions">
          <button class="index-from-docs-btn" @click="showDocSelectDialog">
            <Download class="inline-icon-sm" :size="16" /> 从文档索引
          </button>
          <button class="refresh-btn" @click="loadDocuments">
            <RefreshCw class="inline-icon-sm" :size="16" /> 刷新列表
          </button>
        </div>

        <!-- 索引状态显示 -->
        <div v-if="indexingStatus.active" class="indexing-status">
          <RippleLoader />
          <p>{{ indexingStatus.message }}</p>
          <div class="index-progress">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: `${indexingStatus.progress}%` }"></div>
            </div>
            <span>{{ indexingStatus.current }}/{{ indexingStatus.total }}</span>
          </div>
        </div>

        <div v-if="kbLoading" class="kb-loading">
          <RippleLoader />
        </div>

        <div v-else-if="kbDocuments.length === 0" class="kb-empty">
          <span class="kb-empty-icon"><Inbox :size="48" /></span>
          <p>知识库暂无文档</p>
          <p class="hint">点击"从文档索引"添加已创建的文档</p>
        </div>

        <div v-else class="kb-list">
          <div class="kb-header">
            <span class="col-title">文档标题</span>
            <span class="col-status">状态</span>
            <span class="col-actions">操作</span>
          </div>
          <div v-for="doc in kbDocuments" :key="doc.id" class="kb-item">
            <span class="kb-col-title">{{ doc.title || '文档 ' + doc.id }}</span>
            <span class="kb-col-status indexed-badge"><CheckCircle class="inline-icon-sm" :size="14" /> 已索引</span>
            <span class="kb-col-actions">
              <button class="delete-btn" @click="deleteDocument(doc.id)"><Trash2 class="inline-icon-sm" :size="14" /></button>
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- 文档选择对话框 -->
    <div v-if="docSelectDialog.visible" class="dialog-overlay" @click.self="closeDocSelectDialog">
      <div class="doc-select-dialog glass-card">
        <h3>选择要索引的文档</h3>
        <div v-if="docSelectDialog.loading" class="dialog-loading">
          <RippleLoader />
          <p>加载文档列表...</p>
        </div>
        <div v-else-if="docSelectDialog.documents.length === 0" class="dialog-empty">
          <span class="dialog-empty-icon"><Inbox :size="48" /></span>
          <p>暂无可用文档</p>
          <p class="hint">请先在文档管理页面创建文档</p>
        </div>
        <div v-else class="doc-list">
          <div class="doc-list-header">
            <input type="checkbox" @change="toggleSelectAll" :checked="isAllSelected" />
            <span>标题</span>
            <span>分类</span>
            <span>更新时间</span>
          </div>
          <div v-for="doc in docSelectDialog.documents" :key="doc.id" class="doc-item"
               :class="{ selected: docSelectDialog.selectedIds.includes(doc.id) }"
               @click="toggleDocSelect(doc.id)">
            <input type="checkbox" :checked="docSelectDialog.selectedIds.includes(doc.id)" @click.stop />
            <span class="doc-title">{{ doc.title }}</span>
            <span class="doc-category">{{ doc.category || '未分类' }}</span>
            <span class="doc-time">{{ formatDate(doc.updateTime) }}</span>
          </div>
        </div>
        <div class="dialog-footer">
          <span class="selected-count">已选择 {{ docSelectDialog.selectedIds.length }} 个文档</span>
          <button class="cancel-btn" @click="closeDocSelectDialog">取消</button>
          <button class="index-btn" @click="indexSelectedDocs"
                  :disabled="docSelectDialog.selectedIds.length === 0 || indexingStatus.active">
            <Download class="inline-icon-sm" :size="16" /> 开始索引
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import {
  ragHybridSearch, summarize, extractKeywords, analyzeText, streamRagQuery
} from '@/api/ai'
import { getFileListV3, getFileInfoV3 } from '@/api/file'
import type { FileInfo } from '@/api/file'
import api from '@/api/index'
import RippleLoader from '@/components/common/RippleLoader.vue'
import { useToast } from '@/composables/useToast'
import {
  Search, PenLine, Library, BookOpen, FileText, Zap, XCircle,
  Download, RefreshCw, Inbox, CheckCircle, Trash2, Bot
} from '@lucide/vue'

const toast = useToast()

// Tabs
const tabs = [
  { key: 'rag', iconComponent: Bot, label: '智能问答', desc: '基于知识库的AI问答' },
  { key: 'search', iconComponent: Search, label: '文档搜索', desc: '混合检索文档内容' },
  { key: 'process', iconComponent: Zap, label: '内容处理', desc: '摘要/关键词提取' },
  { key: 'kb', iconComponent: Library, label: '知识库', desc: '管理索引文档' }
]
const activeTab = ref('rag')

// RAG问答
const ragQuestion = ref('')
const ragStrategy = ref('')
const ragResult = ref<any>(null)
const ragLoading = ref(false)
const ragStreaming = ref(false) // 流式输出状态

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
const kbDocuments = ref<{id: string, title: string}[]>([])
const kbLoading = ref(false)

// 索引状态
const indexingStatus = ref({
  active: false,
  message: '',
  progress: 0,
  current: 0,
  total: 0
})

// 文档选择对话框
const docSelectDialog = ref({
  visible: false,
  loading: false,
  documents: [] as Array<{ id: string; title: string; category: string; updateTime: string }>,
  selectedIds: [] as string[]
})

const isAllSelected = computed(() => {
  return docSelectDialog.value.documents.length > 0 &&
    docSelectDialog.value.selectedIds.length === docSelectDialog.value.documents.length
})

// RAG问答（流式）
async function handleRagQuery() {
  ragLoading.value = true
  ragStreaming.value = true
  ragResult.value = { answer: '' } // 初始化空答案

  try {
    await streamRagQuery(
      ragQuestion.value,
      ragStrategy.value || 'HYBRID',
      (chunk) => {
        // 实时追加文本（打字机效果）
        if (ragResult.value) {
          ragResult.value.answer += chunk
        }
      },
      () => {
        // 完成
        ragLoading.value = false
        ragStreaming.value = false
        toast.success('问答完成')
      },
      (error) => {
        // 错误
        ragLoading.value = false
        ragStreaming.value = false
        ragResult.value = { answer: '查询失败: ' + error }
        toast.error('查询失败')
      }
    )
  } catch (error: any) {
    console.error('RAG查询失败:', error.message)
    ragLoading.value = false
    ragStreaming.value = false
    ragResult.value = { answer: '查询失败: ' + error.message }
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
      const summaryResult = await summarize({ content: processContent.value, maxLength: 200 })
      result.summary = summaryResult.summary
      result.originalLength = summaryResult.originalLength
      result.summaryLength = summaryResult.summaryLength
      result.compressionRatio = summaryResult.compressionRatio
    }

    if (doKeywords.value) {
      const keywordResult = await extractKeywords({ content: processContent.value, count: 10 })
      result.keywords = keywordResult.keywords
    }

    if (doAnalyze.value) {
      result.analysis = await analyzeText(processContent.value)
    }

    processResult.value = result
    toast.success('文档处理完成')
  } catch (error: any) {
    console.error('处理失败:', error.message)
    toast.error('处理失败: ' + error.message)
    processResult.value = { error: error.message }
  } finally {
    processLoading.value = false
  }
}

// 知识库管理
async function loadDocuments() {
  kbLoading.value = true
  try {
    const res = await api.get<{id: string, title: string}[]>('/api/ai/rag/documents')
    kbDocuments.value = res
  } catch (error: any) {
    console.error('加载文档列表失败:', error.message)
  } finally {
    kbLoading.value = false
  }
}

async function deleteDocument(docId: string) {
  if (!confirm(`确认删除文档 ${docId}?`)) return
  try {
    await api.delete(`/api/ai/rag/document/${docId}`)
    toast.success('删除成功')
    loadDocuments()
  } catch (error: any) {
    console.error('删除失败:', error.message)
    toast.error('删除失败: ' + error.message)
  }
}

// 文档选择对话框
async function showDocSelectDialog() {
  docSelectDialog.value.visible = true
  docSelectDialog.value.loading = true
  docSelectDialog.value.selectedIds = []

  try {
    const files: FileInfo[] = await getFileListV3()
    // v3.0：将FileInfo转换为简化对象供对话框使用
    docSelectDialog.value.documents = files.map(f => ({
      id: f.fileUuid,
      title: f.fileName || f.originalName,
      category: f.category || '未分类',
      updateTime: f.updateTime || f.createTime
    }))
  } catch (error: any) {
    console.error('加载文件列表失败:', error.message)
    toast.error('加载文件列表失败')
    docSelectDialog.value.documents = []
  } finally {
    docSelectDialog.value.loading = false
  }
}

function closeDocSelectDialog() {
  docSelectDialog.value.visible = false
}

function toggleDocSelect(docId: string) {
  const idx = docSelectDialog.value.selectedIds.indexOf(docId)
  if (idx === -1) {
    docSelectDialog.value.selectedIds.push(docId)
  } else {
    docSelectDialog.value.selectedIds.splice(idx, 1)
  }
}

function toggleSelectAll() {
  if (isAllSelected.value) {
    docSelectDialog.value.selectedIds = []
  } else {
    docSelectDialog.value.selectedIds = docSelectDialog.value.documents.map(d => d.id)
  }
}

// 批量索引选中的文档
async function indexSelectedDocs() {
  const selectedIds = [...docSelectDialog.value.selectedIds]
  if (selectedIds.length === 0) return

  indexingStatus.value = {
    active: true,
    message: '开始索引...',
    progress: 0,
    current: 0,
    total: selectedIds.length
  }

  let successCount = 0
  let failCount = 0

  for (let i = 0; i < selectedIds.length; i++) {
    const docId = selectedIds[i]
    indexingStatus.value.current = i + 1
    indexingStatus.value.progress = Math.round((i + 1) / selectedIds.length * 100)

    try {
      // v3.0：获取FileInfo
      const fileInfo = await getFileInfoV3(docId)
      const contentForIndex = fileInfo.summary || fileInfo.fileName || ''

      if (contentForIndex) {
        // 索引到知识库 - 使用text/plain Content-Type
        await api.post('/api/ai/rag/index', contentForIndex, {
          params: { documentId: docId, title: fileInfo.fileName || fileInfo.originalName },
          headers: { 'Content-Type': 'text/plain' }
        })
        successCount++
        indexingStatus.value.message = `正在索引: ${fileInfo.fileName}`
      } else {
        failCount++
        console.warn(`文件 ${docId} 无摘要内容`)
      }
    } catch (error: any) {
      failCount++
      console.error(`索引文件 ${docId} 失败:`, error.message)
    }
  }

  indexingStatus.value.active = false
  indexingStatus.value.message = ''

  closeDocSelectDialog()
  loadDocuments()

  if (successCount > 0) {
    toast.success(`成功索引 ${successCount} 个文档`)
  }
  if (failCount > 0) {
    toast.warning(`${failCount} 个文档索引失败`)
  }
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
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

[data-theme="dark"] .feature-tabs {
  background: var(--glass-dark-bg);
  border-color: var(--glass-dark-border);
}

.feature-tabs button {
  padding: 12px 24px;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.3);
  display: flex;
  align-items: center;
  gap: 12px;
  transition: all var(--transition-base);
  color: var(--color-text);
}

[data-theme="dark"] .feature-tabs button {
  background: rgba(26, 26, 26, 0.4);
  border: 1px solid var(--glass-dark-border);
}

.feature-tabs button:hover {
  background: rgba(8, 145, 178, 0.2);
  box-shadow: var(--glow-cyan-soft);
}

[data-theme="dark"] .feature-tabs button:hover {
  background: rgba(8, 145, 178, 0.25);
  border-color: rgba(8, 145, 178, 0.3);
}

.feature-tabs button.active {
  background: var(--gradient-aurora);
  color: white;
  box-shadow: var(--glow-cyan);
}

.tab-icon {
  color: var(--aurora-cyan);
}

[data-theme="dark"] .tab-icon {
  color: var(--aurora-cyan-light);
}

.tab-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.tab-label {
  font-weight: 600;
  font-size: var(--font-size-sm);
}

.tab-desc {
  font-size: var(--font-size-xs);
  opacity: 0.8;
}

.rag-panel, .search-panel, .process-panel, .kb-panel {
  padding: 24px;
}

[data-theme="dark"] .rag-panel,
[data-theme="dark"] .search-panel,
[data-theme="dark"] .process-panel,
[data-theme="dark"] .kb-panel {
  background: var(--glass-dark-bg);
  border-color: var(--glass-dark-border);
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
  background: rgba(255, 255, 255, 0.3);
  outline: none;
  resize: vertical;
  color: var(--color-text);
  transition: all var(--transition-base);
}

[data-theme="dark"] textarea,
[data-theme="dark"] input[type="text"] {
  background: rgba(26, 26, 26, 0.4);
  border-color: var(--glass-dark-border);
}

textarea:focus, input:focus {
  border-color: var(--aurora-cyan);
  box-shadow: var(--glow-cyan-soft);
}

[data-theme="dark"] textarea:focus,
[data-theme="dark"] input:focus {
  border-color: rgba(8, 145, 178, 0.5);
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
  background: rgba(255, 255, 255, 0.3);
  color: var(--color-text);
  transition: all var(--transition-base);
}

[data-theme="dark"] .strategy-select select,
[data-theme="dark"] .topk-select select {
  background: rgba(26, 26, 26, 0.4);
  border-color: var(--glass-dark-border);
}

.strategy-select select:focus, .topk-select select:focus {
  border-color: var(--aurora-cyan);
}

.query-btn, .search-btn, .process-btn {
  padding: 12px 24px;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  background: var(--gradient-aurora);
  color: white;
  transition: all var(--transition-base);
  box-shadow: var(--glow-cyan-soft);
}

[data-theme="dark"] .query-btn,
[data-theme="dark"] .search-btn,
[data-theme="dark"] .process-btn {
  box-shadow: var(--glow-cyan);
}

.query-btn:hover:not(:disabled), .search-btn:hover:not(:disabled), .process-btn:hover:not(:disabled) {
  transform: translateY(-2px);
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
  color: var(--color-text);
}

.answer-box, .context-box {
  padding: 16px;
  background: rgba(8, 145, 178, 0.15);
  border-radius: var(--radius-md);
  border-left: 4px solid var(--aurora-cyan);
  color: var(--color-text);
}

[data-theme="dark"] .answer-box,
[data-theme="dark"] .context-box {
  background: rgba(8, 145, 178, 0.2);
  border-left: 4px solid var(--aurora-cyan);
}

/* 打字机光标效果 */
.typing-cursor {
  display: inline-block;
  animation: blink 1s infinite;
  font-weight: bold;
  color: var(--aurora-cyan);
  margin-left: 2px;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
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
  color: var(--color-text);
}

[data-theme="dark"] .source-item {
  background: rgba(26, 26, 26, 0.4);
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
  transition: all var(--transition-base);
}

[data-theme="dark"] .result-item {
  background: rgba(26, 26, 26, 0.4);
  border: 1px solid var(--glass-dark-border);
}

.result-item:hover {
  box-shadow: var(--glow-cyan-soft);
}

[data-theme="dark"] .result-item:hover {
  border-color: rgba(8, 145, 178, 0.3);
}

.result-score {
  width: 60px;
  height: 60px;
  display: flex;
  justify-content: center;
  align-items: center;
  background: var(--gradient-aurora);
  color: white;
  border-radius: var(--radius-md);
  font-weight: 600;
  box-shadow: var(--glow-cyan);
}

.result-content {
  flex: 1;
  color: var(--color-text);
}

.result-title {
  font-weight: 600;
  color: var(--aurora-cyan);
  margin-bottom: 4px;
}

[data-theme="dark"] .result-title {
  text-shadow: var(--glow-text-cyan);
}

.result-text {
  font-size: var(--font-size-sm);
  color: var(--color-text);
  margin-bottom: 8px;
}

.result-meta {
  display: flex;
  gap: 12px;
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.result-source {
  background: rgba(8, 145, 178, 0.1);
  padding: 2px 8px;
  border-radius: var(--radius-sm);
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
  color: var(--color-text);
}

.result-card {
  padding: 16px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: var(--radius-md);
  margin-bottom: 16px;
  transition: all var(--transition-base);
}

[data-theme="dark"] .result-card {
  background: rgba(26, 26, 26, 0.4);
  border: 1px solid var(--glass-dark-border);
}

.result-card:hover {
  box-shadow: var(--glow-cyan-soft);
}

.result-card h4 {
  margin-bottom: 12px;
  color: var(--aurora-cyan);
}

.error-card {
  background: rgba(239, 68, 68, 0.15);
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.error-card h4 {
  color: #ef4444;
}

.error-message {
  color: var(--color-text);
  margin-bottom: 8px;
}

.error-hint {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

[data-theme="dark"] .result-card h4 {
  text-shadow: var(--glow-text-cyan);
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
  background: var(--gradient-aurora);
  color: white;
  border-radius: var(--radius-full);
  font-size: var(--font-size-sm);
  box-shadow: var(--glow-cyan-soft);
  transition: all var(--transition-base);
}

[data-theme="dark"] .keyword-tag {
  box-shadow: var(--glow-cyan);
}

.keyword-tag:hover {
  transform: translateY(-2px) scale(1.05);
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
  background: rgba(255, 255, 255, 0.3);
  border-radius: var(--radius-md);
  transition: all var(--transition-base);
}

[data-theme="dark"] .stat-item {
  background: rgba(26, 26, 26, 0.4);
  border: 1px solid var(--glass-dark-border);
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: var(--glow-cyan-soft);
}

[data-theme="dark"] .stat-item:hover {
  border-color: rgba(8, 145, 178, 0.3);
}

.stat-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.stat-value {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--aurora-cyan);
}

[data-theme="dark"] .stat-value {
  text-shadow: var(--glow-text-cyan);
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
  color: var(--color-text);
  transition: all var(--transition-base);
}

[data-theme="dark"] .refresh-btn {
  background: rgba(8, 145, 178, 0.2);
  border: 1px solid var(--glass-dark-border);
}

.refresh-btn:hover {
  background: rgba(8, 145, 178, 0.3);
  box-shadow: var(--glow-cyan-soft);
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
  transition: all var(--transition-base);
}

[data-theme="dark"] .kb-item {
  background: rgba(26, 26, 26, 0.4);
  border: 1px solid var(--glass-dark-border);
}

.kb-item:hover {
  box-shadow: var(--glow-cyan-soft);
}

.doc-title {
  font-size: var(--font-size-sm);
  color: var(--color-text);
  font-weight: 500;
}

.delete-btn {
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  border: none;
  cursor: pointer;
  background: rgba(239, 68, 68, 0.2);
  transition: all var(--transition-base);
}

[data-theme="dark"] .delete-btn {
  background: rgba(239, 68, 68, 0.25);
}

.delete-btn:hover {
  background: rgba(239, 68, 68, 0.8);
  box-shadow: 0 0 10px rgba(239, 68, 68, 0.4);
}

.index-section {
  padding-top: 16px;
  border-top: 1px solid var(--glass-border);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

[data-theme="dark"] .index-section {
  border-top: 1px solid var(--glass-dark-border);
}

.index-section h4 {
  margin-bottom: 8px;
  color: var(--color-text);
}

.index-section button {
  padding: 12px 24px;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  background: var(--gradient-aurora);
  color: white;
  box-shadow: var(--glow-emerald);
  transition: all var(--transition-base);
}

.index-section button:hover:not(:disabled) {
  transform: translateY(-2px);
}

.index-section button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 新增样式：索引按钮 */
.index-from-docs-btn {
  padding: 10px 20px;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  background: var(--gradient-aurora);
  color: white;
  box-shadow: var(--glow-emerald);
  transition: all var(--transition-base);
}

[data-theme="dark"] .index-from-docs-btn {
  box-shadow: var(--glow-emerald);
}

.index-from-docs-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 0 20px rgba(16, 185, 129, 0.5);
}

/* 索引状态 */
.indexing-status {
  padding: 20px;
  background: rgba(8, 145, 178, 0.15);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

[data-theme="dark"] .indexing-status {
  background: rgba(8, 145, 178, 0.2);
}

.indexing-status p {
  color: var(--aurora-cyan);
  font-size: var(--font-size-sm);
}

[data-theme="dark"] .indexing-status p {
  text-shadow: var(--glow-text-cyan);
}

.index-progress {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.index-progress .progress-bar {
  flex: 1;
  height: 8px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-full);
}

[data-theme="dark"] .index-progress .progress-bar {
  background: rgba(26, 26, 26, 0.3);
}

.index-progress .progress-fill {
  height: 100%;
  background: var(--gradient-aurora);
  border-radius: var(--radius-full);
  transition: width 0.3s;
}

[data-theme="dark"] .index-progress .progress-fill {
  box-shadow: var(--glow-cyan-soft);
}

.index-progress span {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

/* 知识库列表表头 */
.kb-header {
  display: flex;
  padding: 12px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: var(--radius-md);
  font-weight: 600;
  color: var(--color-text);
}

[data-theme="dark"] .kb-header {
  background: rgba(26, 26, 26, 0.4);
}

.col-title, .col-status, .col-actions {
  flex: 1;
  text-align: left;
}

.col-actions {
  text-align: right;
}

/* 知识库列表项列样式 */
.kb-col-title, .kb-col-status, .kb-col-actions {
  flex: 1;
  text-align: left;
  font-size: var(--font-size-sm);
  color: var(--color-text);
}

.kb-col-title {
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.kb-col-status {
  font-size: var(--font-size-sm);
}

.kb-col-actions {
  text-align: right;
}

.indexed-badge {
  color: var(--aurora-emerald);
  font-size: var(--font-size-sm);
  display: flex;
  align-items: center;
  gap: 4px;
}

[data-theme="dark"] .indexed-badge {
  text-shadow: var(--glow-text-emerald);
}

.inline-icon {
  color: var(--aurora-cyan);
  vertical-align: middle;
  margin-right: 6px;
}

[data-theme="dark"] .inline-icon {
  color: var(--aurora-cyan-light);
}

.inline-icon-sm {
  color: inherit;
  vertical-align: middle;
}

.kb-empty-icon, .dialog-empty-icon {
  color: var(--color-text-muted);
}

.kb-empty .hint {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  margin-top: 8px;
}

/* 文档选择对话框 */
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 200;
}

[data-theme="dark"] .dialog-overlay {
  background: rgba(13, 13, 13, 0.85);
}

.doc-select-dialog {
  padding: 24px;
  max-width: 600px;
  width: 90%;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

[data-theme="dark"] .doc-select-dialog {
  background: var(--glass-dark-bg);
  border: 1px solid var(--glass-dark-border);
}

.doc-select-dialog h3 {
  text-align: center;
  color: var(--color-text);
}

.dialog-loading, .dialog-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  gap: 12px;
}

.dialog-empty span {
  font-size: 48px;
}

.dialog-empty .hint {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.doc-list {
  flex: 1;
  overflow-y: auto;
  min-height: 200px;
  max-height: 400px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.doc-list-header {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: var(--radius-md);
  font-weight: 600;
  color: var(--color-text);
}

[data-theme="dark"] .doc-list-header {
  background: rgba(26, 26, 26, 0.4);
}

.doc-list-header input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.doc-list-header span:nth-child(2) { flex: 2; }
.doc-list-header span:nth-child(3) { flex: 1; }
.doc-list-header span:nth-child(4) { flex: 1; }

.doc-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-base);
  align-items: center;
}

[data-theme="dark"] .doc-item {
  background: rgba(26, 26, 26, 0.4);
  border: 1px solid var(--glass-dark-border);
}

.doc-item:hover {
  background: rgba(8, 145, 178, 0.1);
  box-shadow: var(--glow-cyan-soft);
}

.doc-item.selected {
  background: rgba(8, 145, 178, 0.2);
  border: 2px solid var(--aurora-cyan);
}

[data-theme="dark"] .doc-item.selected {
  background: rgba(8, 145, 178, 0.25);
}

.doc-item input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.doc-title {
  flex: 2;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.doc-category {
  flex: 1;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.doc-time {
  flex: 1;
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid var(--glass-border);
}

[data-theme="dark"] .dialog-footer {
  border-top: 1px solid var(--glass-dark-border);
}

.selected-count {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.cancel-btn, .index-btn {
  padding: 10px 20px;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  transition: all var(--transition-base);
}

.cancel-btn {
  background: rgba(255, 255, 255, 0.5);
  color: var(--color-text);
}

[data-theme="dark"] .cancel-btn {
  background: rgba(26, 26, 26, 0.4);
  border: 1px solid var(--glass-dark-border);
}

.cancel-btn:hover {
  background: rgba(255, 255, 255, 0.7);
}

[data-theme="dark"] .cancel-btn:hover {
  background: rgba(26, 26, 26, 0.6);
}

.index-btn {
  background: var(--gradient-aurora);
  color: white;
  box-shadow: var(--glow-emerald);
}

[data-theme="dark"] .index-btn {
  box-shadow: var(--glow-emerald);
}

.index-btn:hover:not(:disabled) {
  transform: translateY(-2px);
}

.index-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .ai-page {
    padding: 16px;
    gap: 16px;
  }

  .tabs {
    flex-wrap: wrap;
    gap: 8px;
  }

  .tab {
    padding: 10px 16px;
    font-size: var(--font-size-sm);
  }

  .rag-panel, .search-panel, .process-panel, .kb-panel {
    padding: 16px;
  }

  .query-form, .search-form, .process-form {
    gap: 12px;
  }

  textarea {
    min-height: 100px;
  }

  .strategy-select, .topk-select {
    flex-direction: column;
    gap: 8px;
  }

  .process-options {
    flex-direction: column;
    gap: 12px;
  }

  .analysis-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }

  .doc-select-dialog {
    max-height: 90vh;
    padding: 16px;
  }

  .doc-list {
    max-height: 300px;
  }

  .doc-list-header, .doc-item {
    flex-wrap: wrap;
    gap: 8px;
    font-size: var(--font-size-sm);
  }

  .dialog-footer {
    flex-direction: column;
    gap: 12px;
  }

  .cancel-btn, .index-btn {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .tabs {
    justify-content: center;
  }

  .tab {
    padding: 8px 12px;
    font-size: var(--font-size-xs);
  }

  .analysis-grid {
    grid-template-columns: 1fr;
  }

  .stat-item {
    padding: 8px;
  }

  .stat-value {
    font-size: var(--font-size-base);
  }

  .result-item {
    flex-direction: column;
    gap: 12px;
  }

  .result-score {
    width: 50px;
    height: 50px;
  }

  .doc-title, .doc-category, .doc-time {
    flex: 1 1 100%;
    text-align: left;
  }
}
</style>