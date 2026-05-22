<template>
  <div class="process-progress">
    <!-- 进度条 -->
    <div class="progress-bar-container">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: `${progress}%` }"></div>
      </div>
      <span class="progress-percent">{{ progress }}%</span>
    </div>

    <!-- 步骤状态 -->
    <div class="step-status">
      <component :is="statusIconComponent" class="step-icon" :size="20" />
      <span class="step-text">{{ statusText }}</span>
    </div>

    <!-- 步骤详情 -->
    <div class="step-details" v-if="showDetails">
      <div class="step-item" v-for="step in processedSteps" :key="step.name">
        <span class="step-check">
          <Check v-if="step.completed" :size="14" class="check-icon" />
          <Loader2 v-else-if="step.current" :size="14" class="loading-icon is-spinning" />
          <Circle v-else :size="14" class="pending-icon" />
        </span>
        <span class="step-name">{{ step.label }}</span>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="progress-actions" v-if="isCompleted || isFailed">
      <button class="open-btn" v-if="isCompleted" @click="$emit('openDocument')">
        打开文档
      </button>
      <button class="retry-btn" v-if="isFailed" @click="$emit('retry')">
        重新处理
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { Check, Loader2, Circle, CheckCircle, XCircle, Bot, Library } from '@lucide/vue'

const props = defineProps<{
  progress: number
  status: string
  currentStep?: string
}>()

defineEmits<{
  (e: 'openDocument'): void
  (e: 'retry'): void
}>()

const showDetails = ref(true)

const steps = [
  { name: 'PARSE', label: '解析内容' },
  { name: 'DOCUMENT_CREATE', label: '创建文档' },
  { name: 'AI_SUMMARIZE', label: '生成摘要' },
  { name: 'AI_KEYWORDS', label: '提取关键词' },
  { name: 'CLASSIFICATION', label: '智能分类' },
  { name: 'VECTOR_INDEX', label: '知识库索引' },
  { name: 'COMPLETE_NOTIFY', label: '完成通知' }
]

const currentStepIndex = computed(() => {
  if (!props.currentStep) return -1
  return steps.findIndex(s => s.name === props.currentStep)
})

const processedSteps = computed(() => {
  return steps.map((step, index) => ({
    ...step,
    completed: index < currentStepIndex.value,
    current: index === currentStepIndex.value
  }))
})

const isCompleted = computed(() => props.status === 'COMPLETED')
const isFailed = computed(() => props.status === 'FAILED')

const statusIconComponent = computed(() => {
  switch (props.status) {
    case 'COMPLETED': return CheckCircle
    case 'FAILED': return XCircle
    case 'AI_PROCESSING': return Bot
    case 'INDEXING': return Library
    default: return Loader2
  }
})

const statusText = computed(() => {
  switch (props.status) {
    case 'COMPLETED': return '智能整理完成'
    case 'FAILED': return '处理失败'
    case 'AI_PROCESSING': return 'AI正在理解内容...'
    case 'INDEXING': return '正在索引知识库...'
    case 'PARSE': return '正在解析文件...'
    default: return props.status || '处理中...'
  }
})
</script>

<style scoped>
.process-progress {
  padding: 16px;
  background: var(--glass-light-bg);
  border-radius: var(--radius-lg);
}

[data-theme="dark"] .process-progress {
  background: var(--glass-dark-bg);
}

.progress-bar-container {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.progress-bar {
  flex: 1;
  height: 8px;
  background: var(--bg-secondary);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: var(--accent-color);
  transition: width 0.3s ease;
}

.progress-percent {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.step-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.step-icon {
  color: var(--aurora-cyan);
}

[data-theme="dark"] .step-icon {
  color: var(--aurora-cyan-light);
}

.step-text {
  font-weight: 500;
  color: var(--text-primary);
}

.step-details {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  font-size: 12px;
}

.step-check {
  display: flex;
  align-items: center;
}

.check-icon {
  color: var(--aurora-emerald);
}

[data-theme="dark"] .check-icon {
  color: var(--aurora-emerald-light);
}

.loading-icon {
  color: var(--aurora-cyan);
}

[data-theme="dark"] .loading-icon {
  color: var(--aurora-cyan-light);
}

.is-spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.pending-icon {
  color: var(--color-text-muted);
}

.step-name {
  color: var(--text-secondary);
}

.progress-actions {
  display: flex;
  gap: 12px;
}

.open-btn, .retry-btn {
  padding: 8px 16px;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  font-weight: 500;
}

.open-btn {
  background: var(--accent-color);
  color: white;
}

.retry-btn {
  background: var(--bg-secondary);
  color: var(--text-primary);
}
</style>