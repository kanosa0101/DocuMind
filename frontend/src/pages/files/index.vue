<template>
  <div class="files-page">
    <!-- 顶部搜索和操作栏 -->
    <div class="toolbar glass-card">
      <div class="search-section">
        <div class="search-input-wrapper">
          <span class="search-icon">🔍</span>
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
        <button class="upload-btn" @click="triggerUpload">
          <span>📤</span> 上传文件
        </button>
      </div>
    </div>

    <!-- 上传区域 -->
    <div
      v-if="showUploadZone"
      class="upload-zone glass-card"
      :class="{ dragging: isDragging }"
      @dragover.prevent="isDragging = true"
      @dragleave.prevent="isDragging = false"
      @drop.prevent="handleDrop"
    >
      <input
        ref="fileInput"
        type="file"
        multiple
        hidden
        @change="handleFileSelect"
      />
      <div class="upload-content">
        <span class="upload-icon">📁</span>
        <p>拖拽文件到此处，或 <a @click="triggerUpload">点击选择</a></p>
      </div>
      <!-- 上传进度 -->
      <div v-if="uploadingFiles.length > 0" class="upload-progress">
        <div v-for="file in uploadingFiles" :key="file.name" class="progress-item">
          <span class="file-name">{{ file.name }}</span>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: file.progress + '%' }"></div>
          </div>
          <span class="progress-text">{{ file.progress }}%</span>
        </div>
      </div>
    </div>

    <!-- 文件列表 -->
    <div class="files-list glass-card">
      <div v-if="loading" class="loading-state">
        <RippleLoader />
        <p>加载文件列表...</p>
      </div>

      <div v-else-if="files.length === 0" class="empty-state">
        <span class="empty-icon">📂</span>
        <p>暂无文件，点击上方上传按钮添加</p>
      </div>

      <div v-else class="files-grid">
        <div v-for="file in files" :key="file.fileId" class="file-card">
          <div class="file-icon">{{ getFileIcon(file) }}</div>
          <div class="file-info">
            <h4 class="file-name" :title="file.originalFileName">{{ file.originalFileName }}</h4>
            <p class="file-meta">
              <span>{{ formatFileSize(file.fileSize) }}</span>
              <span>{{ formatDate(file.createTime) }}</span>
            </p>
          </div>
          <div class="file-actions">
            <button class="action-btn" @click="handlePreview(file)" title="预览">👁️</button>
            <button class="action-btn" @click="handleDownload(file)" title="下载">⬇️</button>
            <button class="action-btn" @click="showRenameDialog(file)" title="重命名">✏️</button>
            <button class="action-btn delete" @click="handleDelete(file)" title="删除">🗑️</button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getFileList, searchFiles, uploadFile, downloadFile, previewFile, deleteFile, renameFile } from '@/api/file'
import type { FileMetadata } from '@/types/api'
import RippleLoader from '@/components/common/RippleLoader.vue'

// 状态
const files = ref<FileMetadata[]>([])
const loading = ref(false)
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(12)
const totalFiles = ref(0) // 总文件数，用于更准确的分页判断
const showUploadZone = ref(false)
const isDragging = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const uploadingFiles = ref<{ name: string; progress: number }[]>([])
const uploadErrors = ref<{ name: string; error: string }[]>([]) // 上传错误列表

// 重命名对话框
const renameDialog = ref({
  visible: false,
  fileId: '',
  newName: ''
})

// 加载文件列表
async function loadFiles() {
  loading.value = true
  try {
    if (searchKeyword.value.trim()) {
      const result = await searchFiles(searchKeyword.value.trim(), currentPage.value, pageSize.value)
      files.value = result.files
      totalFiles.value = result.total
    } else {
      const result = await getFileList(currentPage.value, pageSize.value)
      files.value = result.files
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

// 上传
function triggerUpload() {
  showUploadZone.value = true
  fileInput.value?.click()
}

async function handleFileSelect(e: Event) {
  const target = e.target as HTMLInputElement
  const selectedFiles = target.files
  if (selectedFiles) {
    await uploadFiles(Array.from(selectedFiles))
  }
}

async function handleDrop(e: DragEvent) {
  isDragging.value = false
  const droppedFiles = e.dataTransfer?.files
  if (droppedFiles) {
    await uploadFiles(Array.from(droppedFiles))
  }
}

async function uploadFiles(fileList: File[]) {
  uploadErrors.value = [] // 清空之前的错误

  for (const file of fileList) {
    const uploadingFile = { name: file.name, progress: 0 }
    uploadingFiles.value.push(uploadingFile)

    try {
      await uploadFile(file, (percent) => {
        uploadingFile.progress = percent
      })
      uploadingFile.progress = 100
    } catch (error) {
      const err = error as { message?: string }
      const errorMsg = err?.message || '上传失败'
      console.error(`上传 ${file.name} 失败:`, errorMsg)
      uploadErrors.value.push({ name: file.name, error: errorMsg })
      // 移除上传进度项
      uploadingFiles.value = uploadingFiles.value.filter(f => f.name !== file.name)
    }
  }

  // 上传完成后刷新列表
  if (uploadingFiles.value.filter(f => f.progress === 100).length > 0) {
    loadFiles()
  }

  // 显示错误信息
  if (uploadErrors.value.length > 0) {
    const errorNames = uploadErrors.value.map(e => e.name).join(', ')
    alert(`以下文件上传失败: ${errorNames}`)
  }

  // 延迟清空进度列表
  setTimeout(() => {
    uploadingFiles.value = []
    showUploadZone.value = false
  }, 500)
}

// 预览
async function handlePreview(file: FileMetadata) {
  try {
    const blob = await previewFile(file.fileId)
    const url = URL.createObjectURL(blob)
    // 使用更可靠的预览方式
    const previewWindow = window.open(url, '_blank')
    if (!previewWindow) {
      alert('无法打开预览窗口，请检查浏览器设置')
    }
  } catch (error) {
    const err = error as { message?: string }
    console.error('预览失败:', err?.message || '未知错误')
    alert(`预览失败: ${err?.message || '未知错误'}`)
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

// 工具函数
function getFileIcon(file: FileMetadata): string {
  // 从文件名提取扩展名
  const fileName = file.originalFileName || file.fileName || ''
  const ext = fileName.split('.').pop()?.toLowerCase() || ''

  const icons: Record<string, string> = {
    'pdf': '📕',
    'doc': '📘',
    'docx': '📘',
    'xls': '📗',
    'xlsx': '📗',
    'ppt': '📙',
    'pptx': '📙',
    'txt': '📄',
    'jpg': '🖼️',
    'jpeg': '🖼️',
    'png': '🖼️',
    'gif': '🖼️',
    'zip': '📦',
    'rar': '📦',
    'mp4': '🎬',
    'mp3': '🎵'
  }
  return icons[ext] || '📄'
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

.search-section {
  display: flex;
  gap: 12px;
}

.search-input-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: var(--radius-md);
  width: 300px;
}

.search-icon {
  color: var(--color-text-muted);
}

.search-input-wrapper input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
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
  background: var(--color-primary);
  color: white;
}

.upload-btn {
  background: var(--gradient-success);
  color: white;
}

.upload-zone {
  padding: 40px;
  text-align: center;
  transition: all var(--transition-base);
}

.upload-zone.dragging {
  background: rgba(16, 185, 129, 0.1);
  border: 2px dashed var(--color-success);
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.upload-icon {
  font-size: 48px;
}

.upload-content a {
  color: var(--color-primary);
  cursor: pointer;
  text-decoration: underline;
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
}

.progress-bar {
  width: 200px;
  height: 8px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: var(--radius-full);
}

.progress-fill {
  height: 100%;
  background: var(--gradient-success);
  border-radius: var(--radius-full);
  transition: width 0.3s;
}

.progress-text {
  font-size: var(--font-size-xs);
  color: var(--color-success);
}

.files-list {
  padding: 24px;
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
  font-size: 48px;
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

.file-card:hover {
  transform: translateY(-4px);
  background: rgba(255, 255, 255, 0.5);
}

.file-icon {
  font-size: 32px;
  text-align: center;
}

.file-info {
  text-align: center;
}

.file-name {
  font-size: var(--font-size-sm);
  font-weight: 500;
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

.action-btn:hover {
  background: var(--color-primary);
  color: white;
}

.action-btn.delete:hover {
  background: var(--color-error);
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
  background: var(--color-primary);
  color: white;
  transition: all var(--transition-base);
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-info {
  color: var(--color-text);
}

.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 200;
}

.dialog {
  padding: 24px;
  width: 400px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dialog h3 {
  text-align: center;
}

.dialog input {
  padding: 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--glass-border);
  outline: none;
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
}

.cancel-btn {
  background: rgba(255, 255, 255, 0.5);
}

.confirm-btn {
  background: var(--color-primary);
  color: white;
}
</style>