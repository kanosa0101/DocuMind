<template>
  <div
    class="drop-zone glass-card"
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
      <span class="upload-icon">☁️</span>
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
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import RippleLoader from '@/components/common/RippleLoader.vue'
import { uploadFile } from '@/api/file'

const emit = defineEmits<{
  uploaded: [fileId: string]
}>()

const isDragging = ref(false)
const isProcessing = ref(false)
const uploadProgress = ref(0)
const fileInput = ref<HTMLInputElement | null>(null)

const onDragOver = () => isDragging.value = true
const onDragLeave = () => isDragging.value = false

const onDrop = (e: DragEvent) => {
  isDragging.value = false
  const files = e.dataTransfer?.files
  if (files?.length) handleFiles(files)
}

const triggerUpload = () => fileInput.value?.click()

const onFileSelect = (e: Event) => {
  const files = (e.target as HTMLInputElement).files
  if (files?.length) handleFiles(files)
}

const handleFiles = async (files: FileList) => {
  isProcessing.value = true
  uploadProgress.value = 0

  for (const file of files) {
    try {
      const fileId = await uploadFile(file, (percent) => {
        uploadProgress.value = percent
      })
      emit('uploaded', fileId)

      // 延迟重置
      setTimeout(() => {
        isProcessing.value = false
        uploadProgress.value = 0
      }, 1500)
    } catch (error: any) {
      console.error('上传失败:', error.message || error)
      isProcessing.value = false
    }
  }
}
</script>

<style scoped>
.drop-zone {
  padding: 48px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  border: 2px dashed rgba(26, 115, 232, 0.3);
  transition: all var(--transition-base);
  min-height: 200px;
}

.drop-zone.dragging {
  border-color: var(--color-primary);
  background: rgba(26, 115, 232, 0.1);
  box-shadow: var(--shadow-glow);
}

.drop-zone.processing {
  border-color: var(--color-success);
  background: rgba(16, 185, 129, 0.1);
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.upload-icon {
  font-size: 48px;
  animation: pulse-glow 2s ease-in-out infinite;
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
  height: 8px;
  background: rgba(26, 115, 232, 0.1);
  border-radius: var(--radius-full);
  margin-top: 24px;
  position: relative;
}

.progress-fill {
  height: 100%;
  background: var(--gradient-success);
  border-radius: var(--radius-full);
  transition: width var(--transition-base);
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: var(--font-size-xs);
  color: var(--color-text);
}

.hidden-input {
  display: none;
}
</style>