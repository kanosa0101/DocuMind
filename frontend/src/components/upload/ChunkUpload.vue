<template>
  <div class="chunk-upload">
    <!-- 上传区域 -->
    <div
      class="upload-zone"
      :class="{ dragging: isDragging, uploading: isUploading }"
      @dragover.prevent="isDragging = true"
      @dragleave.prevent="isDragging = false"
      @drop.prevent="handleDrop"
      @click="triggerUpload"
    >
      <input
        ref="fileInput"
        type="file"
        hidden
        :accept="acceptTypes"
        @change="handleFileSelect"
      />

      <!-- 上传状态显示 -->
      <div v-if="!isUploading" class="upload-content">
        <Package class="upload-icon" :size="48" />
        <p class="upload-hint">拖拽大文件到此处或点击上传</p>
        <p class="upload-limit">支持大文件分片上传（最大 {{ maxSizeGB }}GB）</p>
      </div>

      <!-- 上传进度 -->
      <div v-else class="upload-progress">
        <div class="progress-info">
          <span class="file-name">{{ currentFile?.name }}</span>
          <span class="progress-percent">{{ progress }}%</span>
        </div>
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: `${progress}%` }"></div>
        </div>
        <p class="chunk-info">
          已上传 {{ uploadedChunks }}/{{ totalChunks }} 分片
          <span v-if="speed"> · {{ speed }}MB/s</span>
        </p>
        <button class="cancel-btn" @click.stop="cancelUpload">取消上传</button>
      </div>
    </div>

    <!-- 断点续传提示 -->
    <div v-if="pendingChunks.length > 0" class="pending-notice">
      <AlertTriangle class="notice-icon" :size="18" />
      <span>有 {{ pendingChunks.length }} 个文件等待续传</span>
      <button @click="resumePendingUploads">继续上传</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useToast } from '@/composables/useToast'
import { uploadChunk, mergeChunk } from '@/api/file'
import { Package, AlertTriangle } from '@lucide/vue'

const props = withDefaults(defineProps<{
  maxSizeGB?: number
  chunkSizeMB?: number
  acceptTypes?: string
}>(), {
  maxSizeGB: 2,
  chunkSizeMB: 5,
  acceptTypes: '.pdf,.doc,.docx,.txt,.md,.xlsx,.pptx,.zip,.rar'
})

const emit = defineEmits<{
  uploaded: [fileId: string]
  error: [error: string]
}>()

const toast = useToast()

const fileInput = ref<HTMLInputElement | null>(null)
const isDragging = ref(false)
const isUploading = ref(false)
const currentFile = ref<File | null>(null)
const progress = ref(0)
const uploadedChunks = ref(0)
const totalChunks = ref(0)
const speed = ref(0)
const pendingChunks = ref<string[]>([])

// 分片上传状态存储（localStorage）
const CHUNK_STORAGE_KEY = 'documind_chunks'

const triggerUpload = () => {
  if (!isUploading.value) {
    fileInput.value?.click()
  }
}

const handleDrop = (e: DragEvent) => {
  isDragging.value = false
  const files = e.dataTransfer?.files
  if (files?.length) {
    handleFiles(files)
  }
}

const handleFileSelect = (e: Event) => {
  const files = (e.target as HTMLInputElement).files
  if (files?.length) {
    handleFiles(files)
  }
}

const handleFiles = async (files: FileList) => {
  const file = files[0] // 大文件上传只处理第一个

  // 验证文件大小
  const maxSizeBytes = props.maxSizeGB * 1024 * 1024 * 1024
  if (file.size > maxSizeBytes) {
    toast.error(`文件超过 ${props.maxSizeGB}GB 限制`)
    emit('error', '文件大小超限')
    return
  }

  currentFile.value = file
  isUploading.value = true
  progress.value = 0
  uploadedChunks.value = 0

  // 计算分片数量
  const chunkSize = props.chunkSizeMB * 1024 * 1024
  totalChunks.value = Math.ceil(file.size / chunkSize)

  // 生成文件唯一标识（基于文件名+大小+修改时间）
  const fileId = generateFileId(file)

  // 保存上传状态到localStorage（支持断点续传）
  saveUploadState(fileId, file.name, file.size, totalChunks.value)

  try {
    await uploadFileChunks(file, fileId, chunkSize)

    // 合并分片
    const result = await mergeChunk(fileId, file.name)

    toast.success('文件上传成功')
    emit('uploaded', result.fileId)

    // 清理上传状态
    clearUploadState(fileId)
  } catch (error: any) {
    toast.error(`上传失败: ${error.message || '未知错误'}`)
    emit('error', error.message || '上传失败')
  } finally {
    isUploading.value = false
    currentFile.value = null
    if (fileInput.value) {
      fileInput.value.value = ''
    }
  }
}

const uploadFileChunks = async (file: File, fileId: string, chunkSize: number) => {
  const startTime = Date.now()
  let uploadedSize = 0

  for (let i = 0; i < totalChunks.value; i++) {
    const start = i * chunkSize
    const end = Math.min(start + chunkSize, file.size)
    const chunk = file.slice(start, end)

    await uploadChunk(chunk, fileId, i, totalChunks.value)

    uploadedChunks.value = i + 1
    uploadedSize += chunk.size

    // 计算进度和速度
    progress.value = Math.round((uploadedSize / file.size) * 100)

    const elapsedSeconds = (Date.now() - startTime) / 1000
    const uploadedMB = uploadedSize / 1024 / 1024
    speed.value = elapsedSeconds > 0 ? Math.round(uploadedMB / elapsedSeconds) : 0

    // 更新localStorage状态
    updateUploadProgress(fileId, i + 1)
  }
}

const cancelUpload = () => {
  isUploading.value = false
  currentFile.value = null
  progress.value = 0
  uploadedChunks.value = 0
  toast.warning('上传已取消')
}

const generateFileId = (file: File): string => {
  // 使用文件名+大小+修改时间生成唯一标识
  const hash = `${file.name}_${file.size}_${file.lastModified}`
  return btoa(hash).replace(/[^a-zA-Z0-9]/g, '').substring(0, 36)
}

const saveUploadState = (fileId: string, fileName: string, fileSize: number, totalChunks: number) => {
  const state = {
    fileId,
    fileName,
    fileSize,
    totalChunks,
    uploadedChunks: 0,
    timestamp: Date.now()
  }
  localStorage.setItem(`${CHUNK_STORAGE_KEY}_${fileId}`, JSON.stringify(state))

  // 更新待续传列表
  pendingChunks.value.push(fileId)
}

const updateUploadProgress = (fileId: string, uploadedChunks: number) => {
  const key = `${CHUNK_STORAGE_KEY}_${fileId}`
  const state = JSON.parse(localStorage.getItem(key) || '{}')
  state.uploadedChunks = uploadedChunks
  localStorage.setItem(key, JSON.stringify(state))
}

const clearUploadState = (fileId: string) => {
  localStorage.removeItem(`${CHUNK_STORAGE_KEY}_${fileId}`)
  pendingChunks.value = pendingChunks.value.filter(id => id !== fileId)
}

const resumePendingUploads = async () => {
  // TODO: 实现断点续传逻辑
  toast.info('断点续传功能开发中')
}

defineExpose({
  cancelUpload,
  resumePendingUploads
})
</script>

<style scoped>
.chunk-upload {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.upload-zone {
  padding: 40px 20px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  border: 2px dashed rgba(8, 145, 178, 0.3);
  border-radius: var(--radius-md);
  transition: all var(--transition-base);
  min-height: 180px;
  background: transparent;
}

[data-theme="dark"] .upload-zone {
  border-color: rgba(8, 145, 178, 0.25);
}

.upload-zone:hover {
  border-color: var(--aurora-cyan);
  background: rgba(8, 145, 178, 0.05);
}

.upload-zone.dragging {
  border-color: var(--aurora-cyan);
  background: rgba(8, 145, 178, 0.1);
  box-shadow: var(--glow-cyan);
}

.upload-zone.uploading {
  cursor: default;
  border-color: var(--aurora-emerald);
  background: rgba(16, 185, 129, 0.05);
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.upload-icon {
  color: var(--aurora-cyan);
}

[data-theme="dark"] .upload-icon {
  color: var(--aurora-cyan-light);
}

.upload-hint {
  font-size: var(--font-size-base);
  color: var(--color-text);
  font-weight: 500;
}

.upload-limit {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.upload-progress {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  width: 80%;
}

.progress-info {
  display: flex;
  justify-content: space-between;
  width: 100%;
}

.file-name {
  font-size: var(--font-size-sm);
  color: var(--color-text);
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 70%;
}

.progress-percent {
  font-size: var(--font-size-sm);
  color: var(--aurora-emerald);
  font-weight: 600;
}

[data-theme="dark"] .progress-percent {
  text-shadow: var(--glow-text-emerald);
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-full);
}

[data-theme="dark"] .progress-bar {
  background: rgba(26, 26, 26, 0.4);
}

.progress-fill {
  height: 100%;
  background: var(--gradient-aurora);
  border-radius: var(--radius-full);
  transition: width 0.3s ease;
}

[data-theme="dark"] .progress-fill {
  box-shadow: var(--glow-cyan-soft);
}

.chunk-info {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.cancel-btn {
  padding: 8px 16px;
  border-radius: var(--radius-sm);
  border: 1px solid rgba(239, 68, 68, 0.5);
  background: transparent;
  color: #ef4444;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.cancel-btn:hover {
  background: rgba(239, 68, 68, 0.1);
  border-color: #ef4444;
}

.pending-notice {
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: space-between;
  padding: 12px 16px;
  background: rgba(245, 158, 11, 0.1);
  border-radius: var(--radius-md);
  border: 1px solid rgba(245, 158, 11, 0.3);
}

.notice-icon {
  color: #f59e0b;
}

.pending-notice span {
  font-size: var(--font-size-sm);
  color: #f59e0b;
}

.pending-notice button {
  padding: 6px 12px;
  border-radius: var(--radius-sm);
  border: none;
  background: #f59e0b;
  color: white;
  cursor: pointer;
}
</style>