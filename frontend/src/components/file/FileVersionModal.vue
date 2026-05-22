<template>
  <Teleport to="body">
    <div v-if="visible" class="modal-overlay" @click.self="handleClose">
      <div class="modal-content glass-card">
        <div class="modal-header">
          <h3>📄 文件版本历史</h3>
          <button class="close-btn" @click="handleClose">✕</button>
        </div>

        <div class="modal-body">
          <!-- 当前文件信息 -->
          <div v-if="fileInfo" class="current-file-info">
            <div class="file-header">
              <FileText class="file-icon" :size="24" />
              <div class="file-details">
                <h4 class="file-title">{{ fileInfo.originalName || fileInfo.fileName }}</h4>
                <p class="file-meta">
                  <span>版本: v{{ fileInfo.version }}</span>
                  <span>{{ formatFileSize(fileInfo.fileSize) }}</span>
                  <span>{{ formatDate(fileInfo.createTime) }}</span>
                </p>
              </div>
            </div>

            <!-- AI分析结果 -->
            <div v-if="fileInfo.summary || hasKeywords" class="ai-results">
              <h5 class="section-title"><Bot :size="16" /> AI分析结果</h5>
              <div v-if="fileInfo.summary" class="summary-section">
                <p class="summary-label">摘要</p>
                <p class="summary-content">{{ fileInfo.summary }}</p>
              </div>
              <div v-if="hasKeywords" class="keywords-section">
                <p class="keywords-label">关键词</p>
                <div class="keywords-list">
                  <span v-for="kw in keywordsArray" :key="kw" class="keyword-tag">{{ kw }}</span>
                </div>
              </div>
              <div v-if="fileInfo.category" class="category-section">
                <p class="category-label">分类</p>
                <span class="category-tag">{{ fileInfo.category }}</span>
              </div>
            </div>
            <div v-else class="no-ai-results">
              <p>暂无AI分析结果，文件正在处理中...</p>
            </div>
          </div>

          <!-- 版本历史列表 -->
          <div class="version-history">
            <h5 class="section-title"><History :size="16" /> 版本历史</h5>
            <div v-if="loadingVersions" class="loading-versions">
              <RippleLoader />
              <p>加载版本历史...</p>
            </div>
            <div v-else-if="versions.length === 0" class="no-versions">
              <p>当前文件没有历史版本</p>
            </div>
            <div v-else class="versions-list">
              <div
                v-for="ver in versions"
                :key="ver.version"
                class="version-item"
                :class="{ current: ver.version === fileInfo?.version }"
              >
                <div class="version-info">
                  <span class="version-number">v{{ ver.version }}</span>
                  <span class="version-date">{{ formatDate(ver.createTime) }}</span>
                  <span class="version-size">{{ formatFileSize(ver.fileSize) }}</span>
                </div>
                <div v-if="ver.changeSummary" class="version-summary">
                  {{ ver.changeSummary }}
                </div>
                <div v-if="ver.summary" class="version-ai-summary">
                  <span class="ai-label">AI摘要:</span> {{ ver.summary }}
                </div>
                <div class="version-actions">
                  <button
                    class="view-btn"
                    @click="handleViewVersion(ver)"
                    title="查看此版本AI分析"
                  >
                    查看
                  </button>
                  <button
                    class="restore-btn"
                    @click="handleRestoreVersion(ver.version)"
                    title="恢复到此版本（创建新版本）"
                  >
                    恢复
                  </button>
                  <button class="download-btn" @click="handleDownloadVersion(ver.version)" title="下载此版本">
                    <Download :size="14" />
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button v-if="versions.length >= 1" class="compare-btn" @click="showCompare = true">
            🔍 对比版本
          </button>
          <button class="close-action-btn" @click="handleClose">关闭</button>
        </div>
      </div>
    </div>

    <!-- 版本对比弹窗 -->
    <VersionCompareModal
      :visible="showCompare"
      :fileUuid="fileUuid"
      :currentVersion="fileInfo?.version || 1"
      @close="showCompare = false"
    />

    <!-- 查看历史版本弹窗 -->
    <div v-if="viewVersionData" class="view-version-overlay" @click.self="closeViewVersion">
      <div class="view-version-card glass-card">
        <div class="view-header">
          <h4>📋 版本 v{{ viewVersionData.version }} 详情</h4>
          <button class="close-view-btn" @click="closeViewVersion">✕</button>
        </div>
        <div class="view-body">
          <div class="view-section">
            <p class="view-label">文件名</p>
            <p class="view-content">{{ viewVersionData.originalName }}</p>
          </div>
          <div class="view-section">
            <p class="view-label">文件大小</p>
            <p class="view-content">{{ formatFileSize(viewVersionData.fileSize) }}</p>
          </div>
          <div class="view-section">
            <p class="view-label">上传时间</p>
            <p class="view-content">{{ formatDate(viewVersionData.createTime) }}</p>
          </div>
          <div v-if="viewVersionData.changeSummary" class="view-section">
            <p class="view-label">变更说明</p>
            <p class="view-content">{{ viewVersionData.changeSummary }}</p>
          </div>
          <div v-if="viewVersionData.summary" class="view-section">
            <p class="view-label"><Bot :size="14" /> AI摘要</p>
            <p class="view-content">{{ viewVersionData.summary }}</p>
          </div>
          <div v-if="viewKeywordsArray.length > 0" class="view-section">
            <p class="view-label"><Tags :size="14" /> 关键词</p>
            <div class="keywords-list">
              <span v-for="kw in viewKeywordsArray" :key="kw" class="keyword-tag">{{ kw }}</span>
            </div>
          </div>
          <div v-if="viewVersionData.category" class="view-section">
            <p class="view-label">分类</p>
            <span class="category-tag">{{ viewVersionData.category }}</span>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { getFileInfoV3, getVersionHistoryV3, downloadVersionV3, switchVersionV3 } from '@/api/file'
import type { FileInfo, VersionHistoryItem } from '@/api/file'
import RippleLoader from '@/components/common/RippleLoader.vue'
import VersionCompareModal from './VersionCompareModal.vue'
import { useToast } from '@/composables/useToast'
import { FileText, Bot, History, Download, Tags } from '@lucide/vue'

interface Props {
  visible: boolean
  fileUuid: string | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'close'): void
  (e: 'refresh'): void
}>()

const toast = useToast()

const fileInfo = ref<FileInfo | null>(null)
const versions = ref<VersionHistoryItem[]>([])
const loading = ref(false)
const loadingVersions = ref(false)
const showCompare = ref(false)
const viewVersionData = ref<VersionHistoryItem | null>(null)

// 关键词数组处理
const keywordsArray = computed(() => {
  if (!fileInfo.value?.keywords) return []
  const kw = fileInfo.value.keywords
  if (typeof kw === 'string') {
    try {
      return JSON.parse(kw) as string[]
    } catch {
      return []
    }
  }
  return Array.isArray(kw) ? kw : []
})

const hasKeywords = computed(() => keywordsArray.value.length > 0)

// 查看版本关键词
const viewKeywordsArray = computed(() => {
  if (!viewVersionData.value?.keywords) return []
  const kw = viewVersionData.value.keywords
  if (typeof kw === 'string') {
    try { return JSON.parse(kw) as string[] } catch { return [] }
  }
  return Array.isArray(kw) ? kw : []
})

// 加载文件信息和版本历史
async function loadData() {
  if (!props.fileUuid) return

  loading.value = true
  loadingVersions.value = true

  try {
    // 并行加载文件信息和版本历史
    const [info, verHistory] = await Promise.all([
      getFileInfoV3(props.fileUuid),
      getVersionHistoryV3(props.fileUuid)
    ])

    fileInfo.value = info
    versions.value = verHistory
  } catch (error: any) {
    console.error('加载文件信息失败:', error)
    toast.error(`加载失败: ${error.message || '未知错误'}`)
  } finally {
    loading.value = false
    loadingVersions.value = false
  }
}

// 查看历史版本（只展示，不修改数据）
function handleViewVersion(ver: VersionHistoryItem) {
  viewVersionData.value = ver
}

// 关闭查看版本弹窗
function closeViewVersion() {
  viewVersionData.value = null
}

// 恢复历史版本（创建新版本）
async function handleRestoreVersion(targetVersion: number) {
  if (!props.fileUuid) return

  const confirmed = window.confirm(`恢复到 v${targetVersion} 版本？\n这将创建一个新的版本，当前版本会保存到历史记录中。`)
  if (!confirmed) return

  try {
    toast.info('正在恢复版本...')
    // 调用恢复版本API（后端会创建新版本）
    await switchVersionV3(props.fileUuid, targetVersion)
    toast.success('版本恢复成功，已创建新版本')
    emit('refresh')
    // 重新加载数据
    await loadData()
  } catch (error: any) {
    console.error('恢复版本失败:', error)
    toast.error(`恢复失败: ${error.message || '未知错误'}`)
  }
}

// 下载指定版本
async function handleDownloadVersion(version: number) {
  if (!props.fileUuid) return

  try {
    toast.info('正在下载...')
    const { blob, fileName } = await downloadVersionV3(props.fileUuid, version)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = fileName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    toast.success('下载成功')
  } catch (error: any) {
    console.error('下载失败:', error)
    toast.error(`下载失败: ${error.message || '未知错误'}`)
  }
}

function handleClose() {
  emit('close')
}

function formatFileSize(size: number): string {
  if (!size) return '-'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  if (size < 1024 * 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`
  return `${(size / 1024 / 1024 / 1024).toFixed(1)} GB`
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 监听visible变化加载数据
watch(() => props.visible, (visible) => {
  if (visible && props.fileUuid) {
    loadData()
  }
})
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 9999;
  backdrop-filter: blur(4px);
}

[data-theme="dark"] .modal-overlay {
  background: rgba(13, 13, 13, 0.85);
}

.modal-content {
  width: 640px;
  max-width: 95vw;
  max-height: 85vh;
  overflow-y: auto;
  padding: 24px;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
}

[data-theme="dark"] .modal-content {
  background: var(--glass-dark-bg);
  border: 1px solid var(--glass-dark-border);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.modal-header h3 {
  color: var(--color-text);
  font-size: 18px;
}

.close-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-text-muted);
  font-size: 20px;
}

.modal-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 当前文件信息 */
.current-file-info {
  padding: 16px;
  background: rgba(8, 145, 178, 0.1);
  border-radius: var(--radius-md);
  border: 1px solid rgba(8, 145, 178, 0.2);
}

[data-theme="dark"] .current-file-info {
  background: rgba(8, 145, 178, 0.15);
}

.file-header {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.file-icon {
  color: var(--aurora-cyan);
}

[data-theme="dark"] .file-icon {
  color: var(--aurora-cyan-light);
}

.file-details {
  flex: 1;
}

.file-title {
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 4px 0;
}

.file-meta {
  display: flex;
  gap: 12px;
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
  margin: 0;
}

/* AI分析结果 */
.ai-results {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(8, 145, 178, 0.2);
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--aurora-cyan);
  margin: 0 0 12px 0;
}

[data-theme="dark"] .section-title {
  color: var(--aurora-cyan-light);
}

.summary-section,
.keywords-section,
.category-section {
  margin-bottom: 12px;
}

.summary-label,
.keywords-label,
.category-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  margin: 0 0 4px 0;
}

.summary-content {
  font-size: var(--font-size-sm);
  color: var(--color-text);
  line-height: 1.6;
  margin: 0;
}

.keywords-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.keyword-tag {
  padding: 4px 10px;
  background: rgba(16, 185, 129, 0.2);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  color: var(--aurora-emerald);
}

[data-theme="dark"] .keyword-tag {
  background: rgba(16, 185, 129, 0.25);
}

.category-tag {
  padding: 4px 10px;
  background: rgba(139, 92, 246, 0.2);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  color: #8b5cf6;
}

.no-ai-results {
  padding: 12px;
  text-align: center;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

/* 版本历史 */
.version-history {
  padding: 16px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: var(--radius-md);
}

[data-theme="dark"] .version-history {
  background: rgba(26, 26, 26, 0.4);
}

.loading-versions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 24px;
}

.no-versions {
  text-align: center;
  padding: 24px;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.versions-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.version-item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: var(--radius-sm);
  transition: all var(--transition-base);
}

[data-theme="dark"] .version-item {
  background: rgba(26, 26, 26, 0.5);
}

.version-item:hover {
  background: rgba(8, 145, 178, 0.1);
}

.version-item.current {
  border: 1px solid var(--aurora-cyan);
  background: rgba(8, 145, 178, 0.15);
}

.version-info {
  display: flex;
  gap: 12px;
  align-items: center;
}

.version-number {
  font-weight: 600;
  color: var(--aurora-cyan);
  min-width: 40px;
}

[data-theme="dark"] .version-number {
  color: var(--aurora-cyan-light);
}

.version-date,
.version-size {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.version-summary,
.version-ai-summary {
  flex: 1;
  min-width: 200px;
  font-size: var(--font-size-sm);
  color: var(--color-text);
}

.version-ai-summary {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.ai-label {
  color: var(--aurora-emerald);
}

.version-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.switch-btn,
.download-btn,
.view-btn,
.restore-btn {
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  border: none;
  cursor: pointer;
  font-size: var(--font-size-xs);
  transition: all var(--transition-base);
}

.switch-btn {
  background: var(--aurora-cyan);
  color: white;
}

[data-theme="dark"] .switch-btn {
  background: var(--gradient-aurora);
}

.switch-btn:hover {
  transform: translateY(-1px);
  box-shadow: var(--glow-cyan-soft);
}

.view-btn {
  background: rgba(8, 145, 178, 0.2);
  color: var(--aurora-cyan);
}

[data-theme="dark"] .view-btn {
  background: rgba(8, 145, 178, 0.25);
}

.view-btn:hover {
  background: rgba(8, 145, 178, 0.4);
}

.restore-btn {
  background: rgba(16, 185, 129, 0.2);
  color: var(--aurora-emerald);
}

[data-theme="dark"] .restore-btn {
  background: rgba(16, 185, 129, 0.25);
}

.restore-btn:hover {
  background: rgba(16, 185, 129, 0.4);
}

.download-btn {
  background: rgba(255, 255, 255, 0.5);
  color: var(--color-text);
  display: flex;
  align-items: center;
}

[data-theme="dark"] .download-btn {
  background: rgba(26, 26, 26, 0.6);
}

.download-btn:hover {
  background: var(--aurora-emerald);
  color: white;
}

/* 底部按钮 */
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.compare-btn {
  padding: 10px 24px;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  background: var(--gradient-aurora);
  color: white;
  transition: all var(--transition-base);
}

[data-theme="dark"] .compare-btn {
  box-shadow: var(--glow-cyan-soft);
}

.compare-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--glow-cyan);
}

.close-action-btn {
  padding: 10px 24px;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.5);
  color: var(--color-text);
  transition: all var(--transition-base);
}

[data-theme="dark"] .close-action-btn {
  background: rgba(26, 26, 26, 0.4);
  border: 1px solid var(--glass-dark-border);
}

.close-action-btn:hover {
  background: rgba(255, 255, 255, 0.7);
}

[data-theme="dark"] .close-action-btn:hover {
  background: rgba(26, 26, 26, 0.6);
}

/* 查看历史版本弹窗 */
.view-version-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 10000;
}

[data-theme="dark"] .view-version-overlay {
  background: rgba(13, 13, 13, 0.7);
}

.view-version-card {
  width: 480px;
  max-width: 90vw;
  padding: 24px;
  border-radius: var(--radius-lg);
}

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.view-header h4 {
  color: var(--color-text);
  font-size: var(--font-size-base);
  font-weight: 600;
}

.close-view-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-text-muted);
  font-size: 18px;
}

.view-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.view-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.view-label {
  font-size: var(--font-size-sm);
  color: var(--aurora-cyan);
  font-weight: 500;
  margin: 0;
}

[data-theme="dark"] .view-label {
  color: var(--aurora-cyan-light);
}

.view-content {
  font-size: var(--font-size-sm);
  color: var(--color-text);
  line-height: 1.6;
  margin: 0;
}
</style>
