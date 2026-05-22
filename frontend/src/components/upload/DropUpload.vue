<template>
  <div
    class="drop-zone"
    :class="{ 'dragging': isDragging, 'processing': isProcessing }"
    @dragover.prevent="onDragOver"
    @dragleave.prevent="onDragLeave"
    @drop.prevent="onDrop"
    @click="triggerUpload"
  >
    <!-- 波纹扫描动画 -->
    <RippleLoader v-if="isProcessing" />

    <!-- 上传图标 -->
    <div v-else class="upload-content">
      <CloudUpload class="upload-icon" :size="40" />
      <p class="upload-hint">拖拽文件到此处或点击上传</p>
      <p class="upload-types">支持 PDF、Word、TXT、Markdown</p>
    </div>

    <!-- 进度条 -->
    <div v-if="uploadProgress > 0 && !isProcessing" class="progress-bar">
      <div class="progress-fill" :style="{ width: `${uploadProgress}%` }"></div>
      <span class="progress-text">{{ uploadProgress }}%</span>
    </div>

    <!-- 隐藏的文件输入 -->
    <input
      ref="fileInput"
      type="file"
      multiple
      accept=".pdf,.doc,.docx,.txt,.md,.xlsx,.pptx"
      class="hidden-input"
      @change="onFileSelect"
    />

    <!-- 版本选择弹窗 -->
    <VersionSelectModal
      :visible="versionSelectModal.visible"
      :fileName="versionSelectModal.fileName"
      :fileType="versionSelectModal.fileType"
      :similarityResult="versionSelectModal.similarityResult"
      @cancel="handleVersionSelectCancel"
      @confirm="handleVersionSelectConfirm"
    />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import RippleLoader from '@/components/common/RippleLoader.vue'
import VersionSelectModal from '@/components/upload/VersionSelectModal.vue'
import { uploadFile, uploadFileWithVersion, checkFileSimilarity } from '@/api/file'
import type { SimilarityResult } from '@/api/file'
import { CloudUpload } from '@lucide/vue'
import { useToast } from '@/composables/useToast'

const toast = useToast()

const emit = defineEmits<{
  uploaded: [fileIds: string[]]
}>()

const isDragging = ref(false)
const isProcessing = ref(false)
const uploadProgress = ref(0)
const fileInput = ref<HTMLInputElement | null>(null)
const uploadedFileIds = ref<string[]>([])

// 版本选择弹窗状态
const versionSelectModal = ref({
  visible: false,
  fileName: '',
  fileType: '',
  similarityResult: null as SimilarityResult | null,
  pendingFile: null as File | null,
  pendingFiles: [] as File[]
})

const onDragOver = () => isDragging.value = true
const onDragLeave = () => isDragging.value = false

const onDrop = (e: DragEvent) => {
  isDragging.value = false
  const files = e.dataTransfer?.files
  if (files?.length) handleFiles(Array.from(files))
}

const triggerUpload = () => fileInput.value?.click()

const onFileSelect = (e: Event) => {
  const files = (e.target as HTMLInputElement).files
  if (files?.length) handleFiles(Array.from(files))
}

const handleFiles = async (files: File[]) => {
  isProcessing.value = true
  uploadProgress.value = 0
  uploadedFileIds.value = [] // 重置上传文件列表

  for (const file of files) {
    // 先检测相似度
    try {
      console.log('[版本识别] 检测文件:', file.name)
      const similarityResult = await checkFileSimilarity(file.name, file.type)
      console.log('[版本识别] 结果:', similarityResult)

      if (similarityResult && similarityResult.similarityScore >= 0.5) {
        // 找到相似文件，显示选择弹窗
        versionSelectModal.value = {
          visible: true,
          fileName: file.name,
          fileType: file.type || '',
          similarityResult: similarityResult,
          pendingFile: file,
          pendingFiles: files.slice(files.indexOf(file) + 1) // 剩余文件
        }
        return // 等待用户选择
      }

      // 无相似文件，直接上传
      const fileId = await doUploadFile(file, null, null)
      if (fileId) uploadedFileIds.value.push(fileId)
    } catch (error: any) {
      console.warn('[版本识别] 检测失败，直接上传:', error.message || error)
      const fileId = await doUploadFile(file, null, null)
      if (fileId) uploadedFileIds.value.push(fileId)
    }
  }

  // 完成
  finishUpload()
}

const doUploadFile = async (file: File, docId: string | null, changeSummary: string | null) => {
  try {
    let result
    if (docId) {
      result = await uploadFileWithVersion(file, docId, changeSummary || '', (percent) => {
        uploadProgress.value = percent
      })
    } else {
      result = await uploadFile(file, (percent) => {
        uploadProgress.value = percent
      })
    }
    uploadProgress.value = 100
    return result.fileId
  } catch (error: any) {
    toast.error(`上传失败: ${file.name}`)
    console.error(`上传失败: ${file.name}`, error.message || error)
    return null
  }
}

const handleVersionSelectCancel = () => {
  versionSelectModal.value.visible = false
  // 取消上传
  finishUpload()
}

const handleVersionSelectConfirm = async (choice: 'NEW' | 'UPDATE_VERSION', data: { fileUuid?: string; changeSummary?: string; title?: string }) => {
  const pendingFile = versionSelectModal.value.pendingFile
  versionSelectModal.value.visible = false

  if (!pendingFile) {
    finishUpload()
    return
  }

  // 根据用户选择上传
  // v3.0: fileUuid作为docId传递给后端（后端upload接口用docId参数名）
  if (choice === 'UPDATE_VERSION' && data.fileUuid) {
    const fileId = await doUploadFile(pendingFile, data.fileUuid, data.changeSummary || null)
    if (fileId) uploadedFileIds.value.push(fileId)
  } else {
    const fileId = await doUploadFile(pendingFile, null, null)
    if (fileId) uploadedFileIds.value.push(fileId)
  }

  // 继续处理剩余文件
  const remainingFiles = versionSelectModal.value.pendingFiles
  for (const file of remainingFiles) {
    const fileId = await doUploadFile(file, null, null)
    if (fileId) uploadedFileIds.value.push(fileId)
  }

  finishUpload()
}

const finishUpload = () => {
  // 触发uploaded事件，通知父组件上传完成
  if (uploadedFileIds.value.length > 0) {
    emit('uploaded', uploadedFileIds.value)
  }

  setTimeout(() => {
    isProcessing.value = false
    uploadProgress.value = 0
    uploadedFileIds.value = []
    if (fileInput.value) {
      fileInput.value.value = ''
    }
  }, 1500)
}
</script>

<style scoped>
.drop-zone {
  padding: 40px 20px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  border: 2px dashed rgba(8, 145, 178, 0.3);
  transition: all var(--transition-base);
  min-height: 140px;
  border-radius: var(--radius-md);
  background: transparent;
}

[data-theme="dark"] .drop-zone {
  border-color: rgba(8, 145, 178, 0.25);
}

.drop-zone:hover {
  border-color: var(--aurora-cyan);
  background: rgba(8, 145, 178, 0.05);
}

[data-theme="dark"] .drop-zone:hover {
  border-color: rgba(8, 145, 178, 0.5);
  background: rgba(8, 145, 178, 0.1);
}

.drop-zone.dragging {
  border-color: var(--aurora-cyan);
  background: rgba(8, 145, 178, 0.1);
  box-shadow: var(--glow-cyan);
}

[data-theme="dark"] .drop-zone.dragging {
  background: rgba(8, 145, 178, 0.15);
}

.drop-zone.processing {
  border-color: var(--aurora-emerald);
  background: rgba(16, 185, 129, 0.05);
}

[data-theme="dark"] .drop-zone.processing {
  background: rgba(16, 185, 129, 0.1);
  box-shadow: var(--glow-emerald);
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.upload-icon {
  color: var(--aurora-cyan);
  animation: pulse-glow 2s ease-in-out infinite;
}

[data-theme="dark"] .upload-icon {
  color: var(--aurora-cyan-light);
}

.upload-hint {
  font-size: var(--font-size-base);
  color: var(--color-text);
  font-weight: 500;
}

.upload-types {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.progress-bar {
  width: 80%;
  height: 6px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-full);
  margin-top: 16px;
  position: relative;
}

[data-theme="dark"] .progress-bar {
  background: rgba(26, 26, 26, 0.3);
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

.progress-text {
  position: absolute;
  top: -20px;
  left: 50%;
  transform: translateX(-50%);
  font-size: var(--font-size-xs);
  color: var(--aurora-emerald);
}

[data-theme="dark"] .progress-text {
  text-shadow: var(--glow-text-emerald);
}

.hidden-input {
  display: none;
}

@keyframes pulse-glow {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.85;
    transform: scale(1.05);
  }
}
</style>