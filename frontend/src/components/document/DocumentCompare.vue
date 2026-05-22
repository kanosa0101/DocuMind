<template>
  <div class="document-compare">
    <!-- 左侧：原文档 -->
    <div class="original-panel">
      <div class="panel-header">
        <h3><FileText class="inline-icon" :size="20" /> 原文档</h3>
        <span class="file-name">{{ fileName || '等待上传' }}</span>
      </div>
      <div class="panel-content">
        <div v-if="!displayContent" class="empty-placeholder">
          <FileText class="empty-icon" :size="36" />
          <p>上传文件后显示原文</p>
        </div>
        <!-- PDF预览 -->
        <div v-else-if="isPDF" class="pdf-preview-container">
          <VuePdfEmbed
            :source="pdfUrl"
            class="pdf-embed"
          />
        </div>
        <!-- 其他二进制文件 -->
        <div v-else-if="isBinaryFile" class="binary-placeholder">
          <FileText class="binary-icon" :size="48" />
          <p class="binary-hint">{{ fileName }}</p>
          <p class="binary-desc">PDF/图片等二进制文件</p>
          <p class="binary-desc">请在AI处理结果中查看解析内容</p>
        </div>
        <!-- 文本内容 -->
        <pre v-else class="content-text">{{ truncatedContent }}</pre>
      </div>
    </div>

    <!-- 右侧：AI结果 -->
    <div class="ai-panel">
      <div class="panel-header">
        <h3><Bot class="inline-icon" :size="20" /> AI处理结果</h3>
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
        <!-- AI处理中 -->
        <div v-if="isProcessing" class="processing-indicator">
          <RippleLoader />
          <p class="processing-text">{{ processingStep }}</p>
        </div>

        <!-- 摘要 -->
        <div v-else-if="activeTab === 'summary'" class="result-area">
          <AiSummary
            :summary="aiResult.summary"
            :ratio="aiResult.compressionRatio"
          />
        </div>

        <!-- 关键词 -->
        <div v-else-if="activeTab === 'keywords'" class="result-area">
          <KeywordTags :keywords="aiResult.keywords" />
        </div>

        <!-- 分析 -->
        <div v-else-if="activeTab === 'analyze'" class="result-area">
          <AnalysisChart :analysis="aiResult.analysis" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import VuePdfEmbed from 'vue-pdf-embed'
import AiSummary from '@/components/ai/AiSummary.vue'
import KeywordTags from '@/components/ai/KeywordTags.vue'
import AnalysisChart from '@/components/ai/AnalysisChart.vue'
import RippleLoader from '@/components/common/RippleLoader.vue'
import { summarize, extractKeywords, analyzeText } from '@/api/ai'
import { getFileContent, getFileInfoV3 } from '@/api/file'
import { useToast } from '@/composables/useToast'
import type { TextAnalyzeVO } from '@/types/api'
import { FileText, Bot } from '@lucide/vue'

const toast = useToast()

const props = defineProps<{
  fileId: string | null
  fileUuid: string | null  // v3.0：文件UUID（等同于旧版docId）
  originalContent: string
}>()

const fileName = ref('')
const displayContent = ref('')
const activeTab = ref('summary')
const tabs = [
  { key: 'summary', label: '摘要' },
  { key: 'keywords', label: '关键词' },
  { key: 'analyze', label: '分析' }
]

const isProcessing = ref(false)
const processingStep = ref('')
const contentLoaded = ref(false)
const aiResult = ref({
  summary: '',
  compressionRatio: 0,
  keywords: [] as { word: string; score: number }[],
  analysis: null as TextAnalyzeVO | null
})

// PDF预览URL（使用后端预览接口）
const pdfUrl = computed(() => {
  if (!props.fileId) return ''
  return `${import.meta.env.VITE_API_BASE_URL || '/api'}/files/preview/${props.fileId}`
})

// 检测是否为PDF文件
const isPDF = computed(() => {
  if (!fileName.value) return false
  return fileName.value.toLowerCase().endsWith('.pdf')
})

// 检测是否为二进制文件（PDF、图片等）- PDF已有单独预览，这里排除
const isBinaryFile = computed(() => {
  if (!displayContent.value || !fileName.value) return false

  // 检查文件扩展名（排除PDF）
  const lowerName = fileName.value.toLowerCase()
  if (lowerName.endsWith('.pdf')) return false // PDF使用单独预览

  if (lowerName.endsWith('.jpg') ||
      lowerName.endsWith('.jpeg') || lowerName.endsWith('.png') ||
      lowerName.endsWith('.gif') || lowerName.endsWith('.bmp')) {
    return true
  }

  // 检查内容是否包含大量乱码字符（非可打印字符）
  const content = displayContent.value
  const nonPrintableCount = content.split('').filter(c => {
    const code = c.charCodeAt(0)
    return code < 32 && code !== 10 && code !== 13 // 排除换行符
  }).length

  // 如果非可打印字符超过5%，认为是二进制内容
  return nonPrintableCount > content.length * 0.05
})

// 截断显示内容（最多500字符）
const truncatedContent = computed(() => {
  if (!displayContent.value) return ''
  const maxLen = 500
  if (displayContent.value.length <= maxLen) return displayContent.value
  return displayContent.value.substring(0, maxLen) + '\n...(内容过长，已截断)'
})

// v3.0：监听fileUuid变化，从FileInfo获取AI处理结果
watch(() => props.fileUuid, async (fileUuid) => {
  if (!fileUuid) {
    // 如果没有fileUuid但有fileId，则获取文件内容并实时调用AI
    if (props.fileId) {
      await fetchFileAndProcessAI(props.fileId)
    }
    return
  }

  isProcessing.value = true
  processingStep.value = '正在获取文件信息...'

  try {
    // v3.0：从FileInfo获取已保存的AI结果
    const fileInfo = await getFileInfoV3(fileUuid)

    fileName.value = fileInfo.fileName || fileInfo.originalName || '未知文件'

    // v3.0：FileInfo已包含AI处理结果
    if (fileInfo.summary || fileInfo.keywords) {
      aiResult.value.summary = fileInfo.summary || ''
      // keywords可能是JSON字符串或数组，需要处理
      let keywordsArray: string[] = []
      if (fileInfo.keywords) {
        if (typeof fileInfo.keywords === 'string') {
          try {
            keywordsArray = JSON.parse(fileInfo.keywords)
          } catch {
            keywordsArray = []
          }
        } else if (Array.isArray(fileInfo.keywords)) {
          keywordsArray = fileInfo.keywords
        }
      }
      aiResult.value.keywords = keywordsArray.map(k => ({ word: k, score: 0 }))

      // FileInfo中没有content字段，需要单独获取文件内容
      if (props.fileId) {
        try {
          const fileContent = await getFileContent(props.fileId)
          displayContent.value = fileContent.content
        } catch (e) {
          console.warn('获取文件内容失败:', e)
        }
      }

      contentLoaded.value = true
      toast.success('已加载AI分析结果')

      // 如果还需要分析数据，实时调用
      if (!aiResult.value.analysis && fileInfo.summary) {
        try {
          aiResult.value.analysis = await analyzeText(fileInfo.summary)
        } catch (e) {
          console.warn('分析数据获取失败:', e)
        }
      }
    } else {
      // FileInfo无AI结果，获取文件内容并实时调用AI
      if (props.fileId) {
        await fetchFileAndProcessAI(props.fileId)
      }
    }
  } catch (error: any) {
    console.error('获取文件信息失败:', error)
    toast.error(error.message || '获取文件信息失败')

    // 降级：尝试从fileId获取
    if (props.fileId) {
      await fetchFileAndProcessAI(props.fileId)
    }
  } finally {
    isProcessing.value = false
    processingStep.value = ''
  }
}, { immediate: true })

// 从文件获取内容并实时调用AI处理
async function fetchFileAndProcessAI(fileId: string) {
  if (!fileId) return

  isProcessing.value = true
  processingStep.value = '正在获取文件信息...'

  try {
    // v3.0 fix: 先获取FileInfo，检查是否有AI结果
    const fileInfo = await getFileInfoV3(fileId)
    fileName.value = fileInfo.originalName || fileInfo.fileName || '未知文件'

    // 检测是否为PDF文件 - PDF使用预览组件，不获取文本内容
    const isPdfFile = fileName.value.toLowerCase().endsWith('.pdf')

    if (fileInfo.summary && fileInfo.keywords) {
      // 已有AI结果，直接显示
      aiResult.value.summary = fileInfo.summary
      // keywords可能是JSON字符串或数组，需要处理
      let keywordsArray: string[] = []
      if (fileInfo.keywords) {
        if (typeof fileInfo.keywords === 'string') {
          try {
            keywordsArray = JSON.parse(fileInfo.keywords)
          } catch {
            keywordsArray = []
          }
        } else if (Array.isArray(fileInfo.keywords)) {
          keywordsArray = fileInfo.keywords
        }
      }
      aiResult.value.keywords = keywordsArray.map(k => ({ word: k, score: 0 }))
      aiResult.value.compressionRatio = fileInfo.summary.length / (fileInfo.fileSize || 1000)
      contentLoaded.value = true
      displayContent.value = fileInfo.summary
      toast.success('已加载AI分析结果')

      // 获取analysis数据
      if (!aiResult.value.analysis && fileInfo.summary) {
        try {
          aiResult.value.analysis = await analyzeText(fileInfo.summary)
        } catch (e) {
          console.warn('分析数据获取失败:', e)
        }
      }
    } else if (isPdfFile) {
      // PDF文件且无AI结果，显示提示
      displayContent.value = 'PDF文件正在智能处理中，请稍后刷新查看AI结果...'
      toast.info('PDF文件正在智能处理中，请稍后刷新查看')
    } else {
      // 非PDF且无AI结果，获取文本内容并处理
      processingStep.value = '正在获取文件内容...'
      const fileContent = await getFileContent(fileId)
      displayContent.value = fileContent.content

      if (fileContent.content) {
        // 检测内容是否为有效的文本（非二进制乱码）
        const isBinaryContent = checkIfBinaryContent(fileContent.content)
        if (isBinaryContent) {
          processingStep.value = '文件正在解析中，请稍后刷新查看AI结果...'
          toast.info('文件正在智能处理中，请稍后刷新查看')
          return
        }

        processingStep.value = '正在AI处理...'
        const [summaryVO, keywordsVO, analysisVO] = await Promise.all([
          summarize({ content: fileContent.content }),
          extractKeywords({ content: fileContent.content, count: 10 }),
          analyzeText(fileContent.content)
        ])

        aiResult.value.summary = summaryVO.summary
        aiResult.value.compressionRatio = summaryVO.compressionRatio
        aiResult.value.keywords = keywordsVO.keywords
        aiResult.value.analysis = analysisVO

        toast.success('AI处理完成')
        contentLoaded.value = true
      }
    }
  } catch (error: any) {
    console.error('获取文件内容失败:', error)
    toast.error(error.message || '获取文件内容失败')
  } finally {
    isProcessing.value = false
    processingStep.value = ''
  }
}

// 检测内容是否为二进制/乱码（不应进行AI分析）
function checkIfBinaryContent(content: string): boolean {
  if (!content || content.length < 50) return false

  // 检查是否包含PDF解析失败提示
  if (content.includes('PDF文件') && content.includes('无法提取文本内容')) {
    return true
  }

  // 检查是否包含大量乱码字符（非可打印字符比例过高）
  const nonPrintableCount = content.split('').filter(c => {
    const code = c.charCodeAt(0)
    // 排除常见可打印字符和换行符
    return code < 32 && code !== 10 && code !== 13
  }).length

  // 如果非可打印字符超过3%，认为是二进制内容
  if (nonPrintableCount > content.length * 0.03) {
    return true
  }

  // 检查是否为后端返回的提示文本（如"PDF文件（扫描版或图像版）"）
  if (content.startsWith('PDF文件') || content.includes('解析失败')) {
    return true
  }

  return false
}

// 监听fileId变化（备用方案）
watch(() => props.fileId, async (fileId) => {
  // 如果已有fileUuid且已加载结果，不再处理
  if (props.fileUuid && contentLoaded.value) return

  if (!fileId) {
    displayContent.value = ''
    contentLoaded.value = false
    return
  }

  await fetchFileAndProcessAI(fileId)
})

// 监听originalContent变化（手动传入内容的情况）
watch(() => props.originalContent, async (content) => {
  if (!content || contentLoaded.value) return

  displayContent.value = content
  isProcessing.value = true
  processingStep.value = '正在AI处理...'

  try {
    const [summaryVO, keywordsVO, analysisVO] = await Promise.all([
      summarize({ content }),
      extractKeywords({ content, count: 10 }),
      analyzeText(content)
    ])

    aiResult.value.summary = summaryVO.summary
    aiResult.value.compressionRatio = summaryVO.compressionRatio
    aiResult.value.keywords = keywordsVO.keywords
    aiResult.value.analysis = analysisVO

    toast.success('AI处理完成')
  } catch (error: any) {
    console.error('AI处理失败:', error)
    toast.error(error.message || 'AI处理失败')
  } finally {
    isProcessing.value = false
    processingStep.value = ''
  }
})
</script>

<style scoped>
.document-compare {
  display: flex;
  gap: 16px;
  height: 100%;
  padding: 16px;
}

.original-panel, .ai-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  min-height: 0;
}

[data-theme="dark"] .original-panel,
[data-theme="dark"] .ai-panel {
  background: rgba(26, 26, 26, 0.3);
  border: 1px solid var(--glass-dark-border);
}

.panel-header {
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  flex-shrink: 0;
}

[data-theme="dark"] .panel-header {
  border-bottom: 1px solid var(--glass-dark-border);
}

.panel-header h3 {
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--color-text);
  display: flex;
  align-items: center;
  gap: 8px;
}

.inline-icon {
  color: var(--aurora-cyan);
}

[data-theme="dark"] .inline-icon {
  color: var(--aurora-cyan-light);
}

.file-name {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.tab-buttons {
  display: flex;
  gap: 6px;
}

.tab-buttons button {
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: transparent;
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-size: var(--font-size-sm);
}

[data-theme="dark"] .tab-buttons button {
  border-color: var(--glass-dark-border);
}

.tab-buttons button:hover {
  border-color: var(--aurora-cyan);
  color: var(--aurora-cyan);
}

.tab-buttons button.active {
  background: var(--gradient-aurora);
  color: white;
  border-color: transparent;
}

[data-theme="dark"] .tab-buttons button.active {
  box-shadow: var(--glow-cyan-soft);
}

.panel-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  min-height: 0;
}

.empty-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: var(--color-text-muted);
}

/* PDF预览样式 */
.pdf-preview-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
}

.pdf-embed {
  width: 100%;
}

[data-theme="dark"] .pdf-embed {
  background: transparent;
}

.binary-placeholder {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: var(--color-text-muted);
  gap: 8px;
}

.binary-icon {
  color: var(--aurora-cyan);
  margin-bottom: 16px;
}

[data-theme="dark"] .binary-icon {
  color: var(--aurora-cyan-light);
}

.binary-hint {
  font-size: var(--font-size-base);
  color: var(--color-text);
  font-weight: 500;
}

.binary-desc {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.empty-icon {
  color: var(--color-text-muted);
  margin-bottom: 8px;
}

.content-text {
  font-size: var(--font-size-sm);
  line-height: 1.7;
  white-space: pre-wrap;
  color: var(--color-text);
  margin: 0;
}

.result-area {
  animation: fade-in 0.2s ease;
}

.processing-indicator {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 16px;
}

.processing-text {
  font-size: var(--font-size-sm);
  color: var(--aurora-cyan);
}

[data-theme="dark"] .processing-text {
  text-shadow: var(--glow-text-cyan);
}

/* 滚动条 */
.panel-content::-webkit-scrollbar {
  width: 4px;
}

.panel-content::-webkit-scrollbar-track {
  background: transparent;
}

[data-theme="dark"] .panel-content::-webkit-scrollbar-track {
  background: rgba(26, 26, 26, 0.2);
}

.panel-content::-webkit-scrollbar-thumb {
  background: rgba(8, 145, 178, 0.3);
  border-radius: 2px;
}

[data-theme="dark"] .panel-content::-webkit-scrollbar-thumb {
  background: rgba(8, 145, 178, 0.4);
}

@keyframes fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>