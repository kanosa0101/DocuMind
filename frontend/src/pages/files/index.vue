<template>
  <div class="files-page">
    <!-- 顶部搜索和操作栏 -->
    <div class="toolbar glass-card">
      <div class="search-section">
        <div class="search-input-wrapper">
          <Search class="search-icon" :size="18" />
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索文件名..."
            @keydown.enter="handleSearch"
          />
          <button v-if="searchKeyword" class="clear-btn" @click="clearSearch">✕</button>
        </div>
        <button class="search-btn" @click="handleSearch">搜索</button>
      </div>
      <div class="action-section">
        <!-- Tab切换 -->
        <div class="tab-buttons">
          <button :class="{ active: activeView === 'files' }" @click="activeView = 'files'">
            <FolderOpen class="tab-icon" :size="16" /> 文件列表
          </button>
          <button :class="{ active: activeView === 'recycle' }" @click="activeView = 'recycle'">
            <Trash2 class="tab-icon" :size="16" /> 回收站
          </button>
        </div>
      </div>
    </div>

    <!-- 文件列表视图 -->
    <div v-if="activeView === 'files'" class="files-list glass-card">
      <div v-if="loading" class="loading-state">
        <RippleLoader />
        <p>加载文件列表...</p>
      </div>

      <div v-else-if="files.length === 0" class="empty-state">
        <Folder class="empty-icon" :size="48" />
        <p>暂无文件，请在工作台上传文件</p>
      </div>

      <div v-else class="files-grid">
        <div v-for="file in files" :key="file.fileId" class="file-card">
          <div class="file-icon">
            <component :is="getFileIconComponent(file)" :size="32" />
          </div>
          <div class="file-info">
            <h4 class="file-name" :title="file.originalFileName">{{ file.originalFileName }}</h4>
            <p class="file-meta">
              <span>{{ formatFileSize(file.fileSize) }}</span>
              <span>{{ formatDate(file.createTime) }}</span>
            </p>
          </div>
          <div class="file-actions">
            <button class="action-btn" @click="showFileVersion(file)" title="查看版本和AI分析"><History :size="16" /></button>
            <button class="action-btn" @click="handlePreview(file)" title="预览"><Eye :size="16" /></button>
            <button class="action-btn" @click="handleDownload(file)" title="下载"><Download :size="16" /></button>
            <button class="action-btn" @click="showRenameDialog(file)" title="重命名"><Pencil :size="16" /></button>
            <button class="action-btn delete" @click="handleDelete(file)" title="删除"><Trash2 :size="16" /></button>
          </div>
        </div>
      </div>

      // 分页 - 使用总文件数计算总页数
      <div v-if="files.length > 0 || currentPage > 1" class="pagination">
        <button :disabled="currentPage === 1" @click="changePage(currentPage - 1)">上一页</button>
        <span class="page-info">第 {{ currentPage }} 页 / 共 {{ Math.ceil(totalFiles / pageSize) || 1 }} 页 ({{ totalFiles }} 个文件)</span>
        <button :disabled="currentPage >= Math.ceil(totalFiles / pageSize)" @click="changePage(currentPage + 1)">下一页</button>
      </div>
    </div>

    <!-- 重命名对话框 -->
    <div v-if="renameDialog.visible" class="dialog-overlay" @click.self="closeRenameDialog">
      <div class="dialog glass-card">
        <h3>重命名文件</h3>
        <input
          v-model="renameDialog.newName"
          type="text"
          placeholder="输入新文件名"
        />
        <div class="dialog-actions">
          <button class="cancel-btn" @click="closeRenameDialog">取消</button>
          <button class="confirm-btn" @click="confirmRename">确认</button>
        </div>
      </div>
    </div>

    <!-- 文件版本弹窗 -->
    <FileVersionModal
      :visible="versionModal.visible"
      :fileUuid="versionModal.fileUuid"
      @close="closeVersionModal"
      @refresh="handleVersionModalRefresh"
    />

    <!-- 回收站视图 -->
    <div v-if="activeView === 'recycle'" class="recycle-bin glass-card">
      <div v-if="recycleLoading" class="loading-state">
        <RippleLoader />
        <p>加载回收站...</p>
      </div>

      <div v-else-if="recycleFiles.length === 0" class="empty-state">
        <Trash2 class="empty-icon" :size="48" />
        <p>回收站为空</p>
        <p class="hint">删除的文件将保留7天，之后自动永久删除</p>
      </div>

      <div v-else class="recycle-list">
        <div class="recycle-header">
          <span class="col-name">文件名</span>
          <span class="col-size">大小</span>
          <span class="col-delete-time">删除时间</span>
          <span class="col-expiry">保留期限</span>
          <span class="col-actions">操作</span>
        </div>
        <div v-for="file in recycleFiles" :key="file.recycleId" class="recycle-item">
          <span class="col-name">{{ file.originalObjectName }}</span>
          <span class="col-size">{{ formatFileSize(file.fileSize) }}</span>
          <span class="col-delete-time">{{ formatDateTime(file.deleteTime) }}</span>
          <span class="col-expiry">{{ formatDateTime(file.expiryTime) }}</span>
          <span class="col-actions">
            <button class="restore-btn" @click="handleRestore(file)" title="恢复"><RefreshCw :size="16" /></button>
            <button class="delete-btn" @click="handlePermanentDelete(file)" title="永久删除"><Trash2 :size="16" /></button>
          </span>
        </div>
        <div class="recycle-footer">
          <span>共 {{ recycleFiles.length }} 个文件待清理</span>
          <button class="clear-all-btn" @click="clearAllRecycle" :disabled="recycleFiles.length === 0">
            清空回收站
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getFileList, searchFiles, downloadFile, previewFile, deleteFile, renameFile, getRecycleBinList, restoreFile, permanentDeleteFile, getPreviewContentV3 } from '@/api/file'
import type { PreviewResult } from '@/api/file'
import type { FileMetadata, RecycleFile } from '@/types/api'
import RippleLoader from '@/components/common/RippleLoader.vue'
import { useToast } from '@/composables/useToast'
import {
  Search, FolderOpen, Trash2, Folder, FileText,
  Eye, Download, Pencil, RefreshCw, File, History
} from '@lucide/vue'
import FileVersionModal from '@/components/file/FileVersionModal.vue'

const route = useRoute()
const toast = useToast()

// 视图切换
const activeView = ref<'files' | 'recycle'>('files')

// 状态
const files = ref<FileMetadata[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(12)
const totalFiles = ref(0) // 总文件数，用于更准确的分页判断

// 回收站状态
const recycleFiles = ref<RecycleFile[]>([])
const recycleLoading = ref(false)

// 重命名对话框
const renameDialog = ref({
  visible: false,
  fileId: '',
  newName: ''
})

// 文件版本弹窗
const versionModal = ref({
  visible: false,
  fileUuid: ''
})

// 加载文件列表
async function loadFiles() {
  loading.value = true
  try {
    if (searchKeyword.value.trim()) {
      const result = await searchFiles(searchKeyword.value.trim(), currentPage.value, pageSize.value)
      // 映射字段名: fileUuid -> fileId, originalName -> originalFileName
      files.value = result.files.map((f: any) => ({
        ...f,
        fileId: f.fileUuid,
        originalFileName: f.originalName || f.fileName
      }))
      totalFiles.value = result.total
    } else {
      const result = await getFileList(currentPage.value, pageSize.value)
      // 映射字段名: fileUuid -> fileId, originalName -> originalFileName
      files.value = result.files.map((f: any) => ({
        ...f,
        fileId: f.fileUuid,
        originalFileName: f.originalName || f.fileName
      }))
      totalFiles.value = result.total
    }
  } catch (error) {
    const err = error as { message?: string }
    console.error('加载文件失败:', err?.message || '未知错误')
    files.value = []
    totalFiles.value = 0
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  currentPage.value = 1
  loadFiles()
}

function clearSearch() {
  searchKeyword.value = ''
  currentPage.value = 1
  loadFiles()
}

// 分页
function changePage(page: number) {
  currentPage.value = page
  loadFiles()
}

// 预览 - v3.0: 使用preview-content API获取解析后的内容
async function handlePreview(file: FileMetadata) {
  try {
    // 先尝试使用v3 preview-content API
    try {
      const result: PreviewResult = await getPreviewContentV3(file.fileId)

      if (result.contentType === 'pdf') {
        // PDF: 显示分页内容
        const previewContent = result.pages?.map(p =>
          `<div class="pdf-page"><h3>第 ${p.pageNumber} 页</h3><pre>${p.content}</pre></div>`
        ).join('\n') || '<p>无内容</p>'

        showPreviewWindow(result.fileName, previewContent, 'pdf', result.totalPages)
      } else if (result.contentType === 'html') {
        // Word: 显示HTML内容
        showPreviewWindow(result.fileName, result.htmlContent || '<p>无内容</p>', 'html')
      } else if (result.contentType === 'text') {
        // 文本: 直接显示
        showPreviewWindow(result.fileName, `<pre>${result.textContent}</pre>`, 'text')
      } else if (result.contentType === 'unsupported' || result.contentType === 'error') {
        // 不支持或错误: 显示提示，提供下载选项
        toast.warning(result.message || '该文件类型不支持预览')
        // 提供下载选项
        const shouldDownload = window.confirm('该文件类型不支持在线预览，是否下载查看？')
        if (shouldDownload) {
          await handleDownload(file)
        }
      }
    } catch (v3Error) {
      // v3 API失败，尝试旧的v2预览
      console.warn('v3预览失败，尝试v2预览:', v3Error)
      const blob = await previewFile(file.fileId)
      const url = URL.createObjectURL(blob)
      const previewWindow = window.open(url, '_blank')
      if (!previewWindow) {
        toast.error('无法打开预览窗口，请检查浏览器设置')
      }
    }
  } catch (error) {
    const err = error as { message?: string }
    console.error('预览失败:', err?.message || '未知错误')
    toast.error(`预览失败: ${err?.message || '未知错误'}`)
  }
}

// 显示预览窗口
function showPreviewWindow(fileName: string, content: string, _type: string, totalPages?: number) {
  const title = totalPages ? `${fileName} (${totalPages}页)` : fileName
  const style = `
    <style>
      body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; padding: 20px; background: #f5f5f5; }
      .pdf-page { margin-bottom: 20px; padding: 15px; background: white; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
      .pdf-page h3 { color: #0891b2; margin-bottom: 10px; font-size: 14px; }
      pre { white-space: pre-wrap; word-wrap: break-word; font-size: 13px; line-height: 1.6; }
      h1 { color: #0891b2; margin-bottom: 20px; }
      .doc-preview { background: white; padding: 20px; border-radius: 8px; }
      .doc-preview h3 { color: #10b981; margin: 15px 0 10px; }
      .doc-preview p { margin: 10px 0; line-height: 1.8; }
      .doc-table { border-collapse: collapse; width: 100%; margin: 15px 0; }
      .doc-table td { border: 1px solid #ddd; padding: 8px; }
    </style>
  `

  const previewWindow = window.open('', '_blank')
  if (previewWindow) {
    previewWindow.document.write(`
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="UTF-8">
        <title>${title}</title>
        ${style}
      </head>
      <body>
        <h1>${title}</h1>
        ${content}
      </body>
      </html>
    `)
    previewWindow.document.close()
  } else {
    toast.error('无法打开预览窗口，请检查浏览器设置')
  }
}

// 下载
async function handleDownload(file: FileMetadata) {
  try {
    const { blob, fileName } = await downloadFile(file.fileId)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    // 优先使用后端返回的文件名，其次使用前端元数据中的文件名
    let downloadName = fileName || file.originalFileName || file.fileName || 'download'
    // 如果没有扩展名，尝试从fileType推断
    if (!downloadName.includes('.') && file.fileType) {
      const extMap: Record<string, string> = {
        'application/pdf': '.pdf',
        'application/msword': '.doc',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document': '.docx',
        'application/vnd.ms-excel': '.xls',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': '.xlsx',
        'image/jpeg': '.jpg',
        'image/png': '.png',
        'text/plain': '.txt'
      }
      const ext = extMap[file.fileType] || ''
      downloadName += ext
    }
    a.download = downloadName
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (error) {
    const err = error as { message?: string }
    console.error('下载失败:', err?.message || '未知错误')
    alert(`下载失败: ${err?.message || '未知错误'}`)
  }
}

// 删除
async function handleDelete(file: FileMetadata) {
  // 使用自定义确认对话框代替原生confirm
  const confirmed = window.confirm(`确认删除文件 "${file.originalFileName}"？删除后将无法恢复。`)
  if (!confirmed) return

  try {
    await deleteFile(file.fileId)
    // 从列表中直接移除，避免重新请求
    files.value = files.value.filter(f => f.fileId !== file.fileId)
    // 如果当前页没有文件了，回到上一页
    if (files.value.length === 0 && currentPage.value > 1) {
      currentPage.value--
      loadFiles()
    }
  } catch (error) {
    const err = error as { message?: string }
    console.error('删除失败:', err?.message || '未知错误')
    alert(`删除失败: ${err?.message || '未知错误'}`)
  }
}

// 重命名
function showRenameDialog(file: FileMetadata) {
  renameDialog.value = {
    visible: true,
    fileId: file.fileId,
    newName: file.originalFileName
  }
}

function closeRenameDialog() {
  renameDialog.value.visible = false
}

async function confirmRename() {
  if (!renameDialog.value.newName.trim()) {
    alert('文件名不能为空')
    return
  }

  try {
    await renameFile(renameDialog.value.fileId, renameDialog.value.newName.trim())
    closeRenameDialog()
    // 直接更新列表中的文件名，避免重新请求
    const file = files.value.find(f => f.fileId === renameDialog.value.fileId)
    if (file) {
      file.originalFileName = renameDialog.value.newName.trim()
      file.fileName = renameDialog.value.newName.trim()
    }
  } catch (error) {
    const err = error as { message?: string }
    console.error('重命名失败:', err?.message || '未知错误')
    alert(`重命名失败: ${err?.message || '未知错误'}`)
  }
}

// 查看文件版本和AI分析
function showFileVersion(file: FileMetadata) {
  versionModal.value = {
    visible: true,
    fileUuid: file.fileId
  }
}

function closeVersionModal() {
  versionModal.value.visible = false
}

function handleVersionModalRefresh() {
  loadFiles()
}

// ============ 回收站功能 ============

// 加载回收站文件
async function loadRecycleBin() {
  recycleLoading.value = true
  try {
    const result = await getRecycleBinList()
    recycleFiles.value = result.files || []
  } catch (error: any) {
    console.error('加载回收站失败:', error.message)
    recycleFiles.value = []
  } finally {
    recycleLoading.value = false
  }
}

// 恢复文件
async function handleRestore(file: RecycleFile) {
  try {
    await restoreFile(file.recycleId)
    toast.success(`文件 "${file.originalObjectName}" 已恢复`)
    loadRecycleBin()
    if (activeView.value === 'files') {
      loadFiles()
    }
  } catch (error: any) {
    console.error('恢复失败:', error.message)
    toast.error(`恢复失败: ${error.message}`)
  }
}

// 永久删除
async function handlePermanentDelete(file: RecycleFile) {
  const confirmed = window.confirm(`确认永久删除文件 "${file.originalObjectName}"？此操作不可恢复。`)
  if (!confirmed) return

  try {
    await permanentDeleteFile(file.recycleId)
    toast.success(`文件已永久删除`)
    loadRecycleBin()
  } catch (error: any) {
    console.error('永久删除失败:', error.message)
    toast.error(`永久删除失败: ${error.message}`)
  }
}

// 清空回收站
async function clearAllRecycle() {
  const confirmed = window.confirm('确认清空回收站？所有文件将被永久删除，不可恢复。')
  if (!confirmed) return

  try {
    for (const file of recycleFiles.value) {
      await permanentDeleteFile(file.recycleId)
    }
    toast.success('回收站已清空')
    recycleFiles.value = []
  } catch (error: any) {
    console.error('清空失败:', error.message)
    toast.error(`清空失败: ${error.message}`)
    loadRecycleBin()
  }
}

// 格式化日期时间
function formatDateTime(timestamp: number): string {
  if (!timestamp) return '-'
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 监听视图切换
watch(activeView, (view) => {
  if (view === 'recycle') {
    loadRecycleBin()
  } else if (view === 'files') {
    loadFiles()
  }
})

// 工具函数 - 返回Lucide图标组件名
function getFileIconComponent(file: FileMetadata): any {
  const fileName = file.originalFileName || file.fileName || ''
  const ext = fileName.split('.').pop()?.toLowerCase() || ''

  const iconMap: Record<string, any> = {
    'pdf': FileText,
    'doc': FileText,
    'docx': FileText,
    'xls': File,
    'xlsx': File,
    'ppt': File,
    'pptx': File,
    'txt': FileText,
    'jpg': File,
    'jpeg': File,
    'png': File,
    'gif': File,
    'zip': Folder,
    'rar': Folder,
    'mp4': File,
    'mp3': File
  }
  return iconMap[ext] || FileText
}

function formatFileSize(size: number): string {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  if (size < 1024 * 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)} MB`
  return `${(size / 1024 / 1024 / 1024).toFixed(1)} GB`
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

// 初始化
onMounted(() => {
  // 检查是否有搜索参数（从Header跳转）
  const searchParam = route.query.search as string
  if (searchParam) {
    searchKeyword.value = searchParam
  }
  loadFiles()
})
</script>

<style scoped>
.files-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
}

[data-theme="dark"] .toolbar {
  background: var(--glass-dark-bg);
  border-color: var(--glass-dark-border);
}

.search-section {
  display: flex;
  gap: 12px;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: var(--radius-md);
  width: 300px;
  transition: all var(--transition-base);
}

[data-theme="dark"] .search-input-wrapper {
  background: rgba(26, 26, 26, 0.4);
  border: 1px solid var(--glass-dark-border);
}

.search-input-wrapper:focus-within {
  border-color: var(--aurora-cyan);
  box-shadow: var(--glow-cyan-soft);
}

[data-theme="dark"] .search-input-wrapper:focus-within {
  border-color: rgba(8, 145, 178, 0.5);
}

.search-input-wrapper input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  color: var(--color-text);
}

[data-theme="dark"] .search-input-wrapper input {
  color: var(--color-text);
}

.clear-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: var(--color-text-muted);
}

.search-btn, .upload-btn {
  padding: 8px 16px;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all var(--transition-base);
}

.search-btn {
  background: var(--aurora-cyan);
  color: white;
}

[data-theme="dark"] .search-btn {
  background: var(--gradient-aurora);
  box-shadow: var(--glow-cyan-soft);
}

.search-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--glow-cyan);
}

.upload-btn {
  background: var(--gradient-aurora);
  color: white;
  box-shadow: var(--glow-emerald-soft);
}

[data-theme="dark"] .upload-btn {
  box-shadow: var(--glow-emerald);
}

.upload-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 0 20px rgba(16, 185, 129, 0.5);
}

.upload-zone {
  padding: 40px;
  text-align: center;
  transition: all var(--transition-base);
}

[data-theme="dark"] .upload-zone {
  background: var(--glass-dark-bg);
  border-color: var(--glass-dark-border);
}

.upload-zone.dragging {
  background: rgba(8, 145, 178, 0.15);
  border: 2px dashed var(--aurora-cyan);
}

[data-theme="dark"] .upload-zone.dragging {
  background: rgba(8, 145, 178, 0.2);
  box-shadow: var(--glow-cyan-soft);
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.upload-icon {
  color: var(--aurora-cyan);
}

[data-theme="dark"] .upload-icon {
  color: var(--aurora-cyan-light);
}

.upload-content a {
  color: var(--aurora-cyan);
  cursor: pointer;
  text-decoration: underline;
}

[data-theme="dark"] .upload-content a {
  text-shadow: var(--glow-text-cyan);
}

.upload-progress {
  margin-top: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.progress-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-item .file-name {
  flex: 1;
  font-size: var(--font-size-sm);
  color: var(--color-text);
}

.progress-bar {
  width: 200px;
  height: 8px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: var(--radius-full);
}

[data-theme="dark"] .progress-bar {
  background: rgba(26, 26, 26, 0.4);
}

.progress-fill {
  height: 100%;
  background: var(--gradient-aurora);
  border-radius: var(--radius-full);
  transition: width 0.3s;
}

[data-theme="dark"] .progress-fill {
  box-shadow: var(--glow-cyan-soft);
}

.progress-text {
  font-size: var(--font-size-xs);
  color: var(--aurora-emerald);
}

[data-theme="dark"] .progress-text {
  text-shadow: var(--glow-text-emerald);
}

/* 文档处理进度样式 */
.process-progress {
  margin-top: 24px;
  padding: 16px;
  background: rgba(8, 145, 178, 0.1);
  border-radius: var(--radius-md);
  border: 1px solid rgba(8, 145, 178, 0.2);
}

[data-theme="dark"] .process-progress {
  background: rgba(8, 145, 178, 0.15);
  box-shadow: var(--glow-cyan-soft);
}

.process-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: var(--aurora-cyan);
}

[data-theme="dark"] .process-header {
  color: var(--aurora-cyan-light);
}

.process-icon.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.process-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.step-name {
  flex: 1;
  font-size: var(--font-size-sm);
  color: var(--color-text);
}

.progress-fill.aurora {
  background: linear-gradient(90deg, var(--aurora-cyan), var(--aurora-emerald));
  box-shadow: 0 0 10px rgba(8, 145, 178, 0.4);
}

[data-theme="dark"] .progress-fill.aurora {
  box-shadow: var(--glow-cyan);
}

.files-list {
  padding: 24px;
}

[data-theme="dark"] .files-list {
  background: var(--glass-dark-bg);
  border-color: var(--glass-dark-border);
}

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  gap: 16px;
}

.empty-icon {
  color: var(--color-text-muted);
}

.tab-icon {
  color: inherit;
}

.search-icon {
  color: var(--color-text-muted);
}

.files-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.file-card {
  padding: 16px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: all var(--transition-base);
}

[data-theme="dark"] .file-card {
  background: rgba(26, 26, 26, 0.4);
  border: 1px solid var(--glass-dark-border);
}

.file-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--glow-cyan-soft);
}

[data-theme="dark"] .file-card:hover {
  background: rgba(26, 26, 26, 0.6);
  border-color: rgba(8, 145, 178, 0.3);
}

.file-icon {
  display: flex;
  justify-content: center;
  align-items: center;
  color: var(--aurora-cyan);
}

[data-theme="dark"] .file-icon {
  color: var(--aurora-cyan-light);
}

.file-info {
  text-align: center;
}

.file-name {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.file-meta {
  display: flex;
  justify-content: center;
  gap: 8px;
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.file-actions {
  display: flex;
  justify-content: center;
  gap: 8px;
}

.action-btn {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.5);
  border: none;
  cursor: pointer;
  transition: all var(--transition-base);
}

[data-theme="dark"] .action-btn {
  background: rgba(8, 145, 178, 0.2);
  border: 1px solid var(--glass-dark-border);
}

.action-btn:hover {
  background: var(--aurora-cyan);
  color: white;
  box-shadow: var(--glow-cyan-soft);
}

.action-btn.delete:hover {
  background: rgba(239, 68, 68, 0.8);
  box-shadow: 0 0 10px rgba(239, 68, 68, 0.4);
}

[data-theme="dark"] .action-btn.delete:hover {
  background: rgba(239, 68, 68, 0.6);
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
}

.pagination button {
  padding: 8px 16px;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  background: var(--gradient-aurora);
  color: white;
  transition: all var(--transition-base);
}

[data-theme="dark"] .pagination button {
  box-shadow: var(--glow-cyan-soft);
}

.pagination button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--glow-cyan);
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  color: var(--color-text);
}

[data-theme="dark"] .page-info {
  color: var(--color-text);
}

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

.dialog {
  padding: 24px;
  width: 400px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

[data-theme="dark"] .dialog {
  background: var(--glass-dark-bg);
  border: 1px solid var(--glass-dark-border);
}

.dialog h3 {
  text-align: center;
  color: var(--color-text);
}

.dialog input {
  padding: 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--glass-border);
  background: rgba(255, 255, 255, 0.3);
  outline: none;
  color: var(--color-text);
}

[data-theme="dark"] .dialog input {
  background: rgba(26, 26, 26, 0.4);
  border-color: var(--glass-dark-border);
}

.dialog input:focus {
  border-color: var(--aurora-cyan);
  box-shadow: var(--glow-cyan-soft);
}

.dialog-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.cancel-btn, .confirm-btn {
  padding: 8px 24px;
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

.confirm-btn {
  background: var(--gradient-aurora);
  color: white;
  box-shadow: var(--glow-cyan-soft);
}

[data-theme="dark"] .confirm-btn {
  box-shadow: var(--glow-cyan);
}

.confirm-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--glow-cyan);
}

.action-section {
  display: flex;
  align-items: center;
  gap: 16px;
}

/* Tab切换按钮 */
.tab-buttons {
  display: flex;
  gap: 8px;
}

.tab-buttons button {
  padding: 8px 16px;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.3);
  color: var(--color-text);
  transition: all var(--transition-base);
}

[data-theme="dark"] .tab-buttons button {
  background: rgba(26, 26, 26, 0.4);
  border: 1px solid var(--glass-dark-border);
}

.tab-buttons button:hover {
  background: rgba(8, 145, 178, 0.2);
}

.tab-buttons button.active {
  background: var(--gradient-aurora);
  color: white;
  box-shadow: var(--glow-cyan-soft);
}

[data-theme="dark"] .tab-buttons button.active {
  box-shadow: var(--glow-cyan);
}

/* 回收站样式 */
.recycle-bin {
  padding: 24px;
}

[data-theme="dark"] .recycle-bin {
  background: var(--glass-dark-bg);
  border-color: var(--glass-dark-border);
}

.recycle-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.recycle-header, .recycle-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  color: var(--color-text);
}

.recycle-header {
  background: rgba(255, 255, 255, 0.3);
  font-weight: 600;
  border-radius: var(--radius-md);
}

[data-theme="dark"] .recycle-header {
  background: rgba(26, 26, 26, 0.4);
}

.recycle-item {
  background: rgba(255, 255, 255, 0.15);
  border-bottom: 1px solid var(--glass-border);
  transition: all var(--transition-base);
}

[data-theme="dark"] .recycle-item {
  background: rgba(26, 26, 26, 0.25);
  border-bottom: 1px solid var(--glass-dark-border);
}

.recycle-item:hover {
  background: rgba(8, 145, 178, 0.1);
  box-shadow: var(--glow-cyan-soft);
}

[data-theme="dark"] .recycle-item:hover {
  background: rgba(8, 145, 178, 0.15);
}

.recycle-header .col-name, .recycle-item .col-name { flex: 2; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.recycle-header .col-size, .recycle-item .col-size { flex: 1; text-align: center; }
.recycle-header .col-delete-time, .recycle-item .col-delete-time { flex: 1.5; text-align: center; }
.recycle-header .col-expiry, .recycle-item .col-expiry { flex: 1.5; text-align: center; }
.recycle-header .col-actions, .recycle-item .col-actions { flex: 1; display: flex; justify-content: flex-end; gap: 8px; }

.recycle-item .col-size,
.recycle-item .col-delete-time,
.recycle-item .col-expiry {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.restore-btn, .delete-btn {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  border: none;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.5);
  transition: all var(--transition-base);
}

[data-theme="dark"] .restore-btn,
[data-theme="dark"] .delete-btn {
  background: rgba(8, 145, 178, 0.2);
  border: 1px solid var(--glass-dark-border);
}

.restore-btn:hover {
  background: var(--aurora-emerald);
  color: white;
  box-shadow: 0 0 10px rgba(16, 185, 129, 0.4);
}

.delete-btn:hover {
  background: rgba(239, 68, 68, 0.8);
  box-shadow: 0 0 10px rgba(239, 68, 68, 0.4);
}

.recycle-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-top: 1px solid var(--glass-border);
  margin-top: 16px;
}

[data-theme="dark"] .recycle-footer {
  border-top: 1px solid var(--glass-dark-border);
}

.recycle-footer span {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.clear-all-btn {
  padding: 8px 16px;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
  transition: all var(--transition-base);
}

[data-theme="dark"] .clear-all-btn {
  background: rgba(239, 68, 68, 0.25);
}

.clear-all-btn:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.8);
  color: white;
  box-shadow: 0 0 15px rgba(239, 68, 68, 0.5);
}

.clear-all-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.hint {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  margin-top: 8px;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    gap: 16px;
    padding: 16px;
  }

  .search-section {
    width: 100%;
    flex-wrap: wrap;
  }

  .search-input-wrapper {
    flex: 1;
    min-width: 200px;
  }

  .action-section {
    width: 100%;
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 12px;
  }

  .tab-buttons {
    display: flex;
    gap: 8px;
  }

  .files-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
    gap: 12px;
  }

  .file-card {
    padding: 12px;
  }

  .file-actions {
    flex-wrap: wrap;
    gap: 4px;
  }

  .action-btn {
    padding: 6px 8px;
    font-size: var(--font-size-sm);
  }

  .dialog, .recycle-bin {
    width: 95%;
    max-height: 85vh;
  }

  .recycle-item {
    flex-wrap: wrap;
    gap: 8px;
    padding: 12px;
  }
}

@media (max-width: 480px) {
  .files-grid {
    grid-template-columns: 1fr;
  }

  .file-info {
    flex: 1;
    min-width: 0;
  }

  .file-actions {
    width: 100%;
    justify-content: flex-end;
    margin-top: 8px;
  }

  .pagination {
    flex-wrap: wrap;
    justify-content: center;
    gap: 8px;
  }

  .pagination button {
    padding: 8px 12px;
    font-size: var(--font-size-sm);
  }
}
</style>