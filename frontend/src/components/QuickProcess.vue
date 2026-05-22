<template>
  <div class="quick-process">
    <h3>
      <Zap :size="20" class="title-icon" />
      快速处理
    </h3>
    <div class="process-steps">
      <div class="step" :class="{ active: currentStep >= 1, completed: currentStep > 1 }">
        <Upload class="step-icon" :size="24" />
        <span class="step-label">上传文件</span>
      </div>
      <div class="step" :class="{ active: currentStep >= 2, completed: currentStep > 2 }">
        <FileText class="step-icon" :size="24" />
        <span class="step-label">创建文档</span>
      </div>
      <div class="step" :class="{ active: currentStep >= 3, completed: currentStep > 3 }">
        <Bot class="step-icon" :size="24" />
        <span class="step-label">AI摘要</span>
      </div>
      <div class="step" :class="{ active: currentStep >= 4, completed: currentStep > 4 }">
        <Library class="step-icon" :size="24" />
        <span class="step-label">索引知识库</span>
      </div>
    </div>

    <!-- 上传区域 -->
    <div v-if="currentStep === 1" class="upload-area">
      <DropUpload @uploaded="handleUploaded" />
    </div>

    <!-- 处理进度 -->
    <div v-if="processing" class="processing">
      <RippleLoader />
      <p>{{ processingMessage }}</p>
    </div>

    <!-- 完成 -->
    <div v-if="completed" class="completed">
      <CheckCircle class="success-icon" :size="48" />
      <p>文档已创建并完成AI处理</p>
      <button class="view-btn" @click="viewDocument">查看文档</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from '@/composables/useToast'
import DropUpload from '@/components/upload/DropUpload.vue'
import RippleLoader from '@/components/common/RippleLoader.vue'
import { Zap, Upload, FileText, Bot, Library, CheckCircle } from '@lucide/vue'

const router = useRouter()
const toast = useToast()

const currentStep = ref(1)
const processing = ref(false)
const processingMessage = ref('')
const completed = ref(false)

const handleUploaded = async (fileIds: string[]) => {
  if (fileIds.length === 0) return

  // v3.0：文件即一切架构
  // 上传文件后，file-service发送MQ消息
  // AI服务自动处理文件并更新FileInfo的summary、keywords、category字段
  // 用户可以在文件中心看到AI处理结果

  currentStep.value = 2
  processing.value = true

  try {
    processingMessage.value = '文件正在处理，请稍候...'

    // TODO: 使用WebSocket监听处理进度
    currentStep.value = 3
    processingMessage.value = '文件已上传，正在进行AI处理'

    completed.value = true
    toast.success('文件上传成功，AI正在自动处理，请刷新文件列表查看')
  } catch (error: any) {
    toast.error(`处理失败: ${error.message || '未知错误'}`)
    currentStep.value = 1
  } finally {
    processing.value = false
  }
}

const viewDocument = () => {
  // v3.0：跳转到文件中心查看处理结果
  router.push('/files')
}
</script>

<style scoped>
.quick-process {
  padding: 24px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: var(--radius-md);
}

[data-theme="dark"] .quick-process {
  background: rgba(26, 26, 26, 0.3);
}

.quick-process h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--font-size-lg);
  color: var(--color-text);
  margin-bottom: 20px;
}

.title-icon {
  color: var(--aurora-cyan);
}

[data-theme="dark"] .title-icon {
  color: var(--aurora-cyan-light);
}

.process-steps {
  display: flex;
  justify-content: space-between;
  margin-bottom: 24px;
}

.step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  opacity: 0.4;
  transition: all var(--transition-base);
}

.step.active {
  opacity: 1;
}

.step.completed {
  opacity: 1;
}

.step-icon {
  color: var(--color-text-muted);
  transition: all var(--transition-base);
}

.step.active .step-icon,
.step.completed .step-icon {
  color: var(--aurora-cyan);
}

[data-theme="dark"] .step.active .step-icon,
[data-theme="dark"] .step.completed .step-icon {
  color: var(--aurora-cyan-light);
}

.step.active .step-icon,
.step.completed .step-icon {
  filter: drop-shadow(0 0 8px rgba(8, 145, 178, 0.5));
}

.step-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.step.active .step-label,
.step.completed .step-label {
  color: var(--aurora-cyan);
}

.upload-area {
  margin-bottom: 24px;
}

.processing {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 40px;
}

.processing p {
  font-size: var(--font-size-sm);
  color: var(--aurora-cyan);
}

.completed {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 40px;
}

.success-icon {
  color: var(--aurora-emerald);
}

[data-theme="dark"] .success-icon {
  color: var(--aurora-emerald-light);
}

.completed p {
  font-size: var(--font-size-base);
  color: var(--aurora-emerald);
}

.view-btn {
  padding: 12px 24px;
  border-radius: var(--radius-md);
  border: none;
  background: var(--gradient-aurora);
  color: white;
  cursor: pointer;
}
</style>