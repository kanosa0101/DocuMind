<template>
  <Teleport to="body">
    <div v-if="visible" class="modal-overlay" @click.self="handleCancel">
      <div class="modal-content glass-card">
        <div class="modal-header">
          <h3>📄 上传文件: {{ fileName }}</h3>
          <button class="close-btn" @click="handleCancel">✕</button>
        </div>

      <div class="modal-body">
        <!-- 检测到相似文件 -->
        <div v-if="similarityResult?.similarityDetected" class="similarity-alert">
          <div class="alert-icon">⚠️</div>
          <div class="alert-text">
            <p class="alert-title">检测到相似文件</p>
            <p class="alert-detail">
              相似文件: <strong>{{ similarityResult.similarFileName }}</strong>
            </p>
            <p class="alert-score">
              文件名相似度: <span class="score">{{ Math.round(similarityResult.similarityScore * 100) }}%</span>
            </p>
            <p v-if="similarityResult.currentVersion" class="alert-version">
              当前版本: v{{ similarityResult.currentVersion }}
            </p>
          </div>
        </div>

        <!-- 选择处理方式 -->
        <div class="choice-section">
          <p class="choice-label">请选择处理方式：</p>

          <div class="choice-options">
            <!-- 创建新文档 -->
            <label class="choice-option" :class="{ selected: choice === 'NEW' }">
              <input type="radio" v-model="choice" value="NEW" />
              <div class="option-content">
                <span class="option-title">创建新文档</span>
                <span class="option-desc">作为独立的文档，不关联已有文档</span>
                <input
                  v-if="choice === 'NEW'"
                  v-model="newDocTitle"
                  type="text"
                  class="title-input"
                  placeholder="输入文档标题"
                />
              </div>
            </label>

            <!-- 更新现有文档版本 -->
            <label
              v-if="similarityResult?.similarFileUuid"
              class="choice-option"
              :class="{ selected: choice === 'UPDATE_VERSION', recommended: similarityResult?.recommendation === 'UPDATE_VERSION' }"
            >
              <input type="radio" v-model="choice" value="UPDATE_VERSION" />
              <div class="option-content">
                <span class="option-title">
                  更新现有文档版本
                  <span v-if="similarityResult?.recommendation === 'UPDATE_VERSION'" class="recommended-badge">推荐</span>
                </span>
                <span class="option-desc">更新相似文档内容，旧版本将保留在历史记录中</span>
                <div v-if="choice === 'UPDATE_VERSION'" class="version-inputs">
                  <input
                    v-model="changeSummary"
                    type="text"
                    class="summary-input"
                    placeholder="输入变更说明（可选）"
                  />
                </div>
              </div>
            </label>

            <!-- 无similarFileUuid时的提示 -->
            <div v-else-if="similarityResult?.similarityDetected && !similarityResult?.similarFileUuid" class="no-doc-warning">
              <span class="warning-text">⚠️ 相似文件信息不完整，无法更新版本</span>
              <span class="warning-desc">请选择"创建新文档"或稍后重试</span>
            </div>
          </div>
        </div>

        <!-- 提示信息 -->
        <div class="hint-section">
          <p class="hint-text">
            💡 提示: 选择"更新文档"后，旧文件将保留在历史记录中
          </p>
        </div>
      </div>

      <div class="modal-footer">
        <button class="cancel-btn" @click="handleCancel">取消</button>
        <button class="confirm-btn" @click="handleConfirm" :disabled="!choice">
          确认上传
        </button>
      </div>
    </div>
  </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import type { SimilarityResult } from '@/api/file'

interface Props {
  visible: boolean
  fileName: string
  fileType?: string
  similarityResult: SimilarityResult | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'cancel'): void
  (e: 'confirm', choice: 'NEW' | 'UPDATE_VERSION', data: { fileUuid?: string; changeSummary?: string; title?: string }): void
}>()

// 状态
const choice = ref<'NEW' | 'UPDATE_VERSION'>('NEW')
const newDocTitle = ref('')
const changeSummary = ref('')

// 监听相似度结果，自动设置推荐选项
watch(() => props.similarityResult, (result) => {
  if (result?.recommendation === 'UPDATE_VERSION') {
    choice.value = 'UPDATE_VERSION'
  } else {
    choice.value = 'NEW'
  }
  // 重置输入
  newDocTitle.value = props.fileName.replace(/\.[^/.]+$/, '') // 去掉扩展名
  changeSummary.value = ''
})

// 取消
function handleCancel() {
  emit('cancel')
}

// 确认
function handleConfirm() {
  if (!choice.value) return

  const data: { fileUuid?: string; changeSummary?: string; title?: string } = {}

  if (choice.value === 'UPDATE_VERSION' && props.similarityResult?.similarFileUuid) {
    data.fileUuid = props.similarityResult.similarFileUuid
    data.changeSummary = changeSummary.value
  } else {
    data.title = newDocTitle.value || props.fileName.replace(/\.[^/.]+$/, '')
  }

  emit('confirm', choice.value, data)
}
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
  width: 480px;
  max-width: 95vw;
  padding: 24px;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  position: relative;
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
  gap: 16px;
}

/* 相似度警告 */
.similarity-alert {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: rgba(8, 145, 178, 0.15);
  border-radius: var(--radius-md);
  border: 1px solid rgba(8, 145, 178, 0.3);
}

[data-theme="dark"] .similarity-alert {
  background: rgba(8, 145, 178, 0.2);
}

.alert-icon {
  font-size: 24px;
}

.alert-text {
  flex: 1;
}

.alert-title {
  font-weight: 600;
  color: var(--aurora-cyan);
  margin-bottom: 8px;
}

.alert-detail {
  color: var(--color-text);
  font-size: var(--font-size-sm);
}

.alert-score {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}

.alert-score .score {
  color: var(--aurora-emerald);
  font-weight: 600;
}

.alert-doc {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}

/* 选择区域 */
.choice-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.choice-label {
  color: var(--color-text);
  font-weight: 500;
}

.choice-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.choice-option {
  display: flex;
  gap: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition-base);
  border: 2px solid transparent;
}

[data-theme="dark"] .choice-option {
  background: rgba(26, 26, 26, 0.4);
}

.choice-option:hover {
  background: rgba(8, 145, 178, 0.1);
}

.choice-option.selected {
  border-color: var(--aurora-cyan);
  background: rgba(8, 145, 178, 0.15);
}

[data-theme="dark"] .choice-option.selected {
  border-color: rgba(8, 145, 178, 0.5);
}

.choice-option.recommended {
  border-color: var(--aurora-emerald);
}

.choice-option input[type="radio"] {
  accent-color: var(--aurora-cyan);
}

.option-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.option-title {
  color: var(--color-text);
  font-weight: 500;
}

.recommended-badge {
  background: var(--gradient-aurora);
  color: white;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-size: var(--font-size-xs);
  margin-left: 8px;
}

.option-desc {
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

/* 无docId时的警告提示 */
.no-doc-warning {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 16px;
  background: rgba(239, 68, 68, 0.1);
  border-radius: var(--radius-md);
  border: 1px solid rgba(239, 68, 68, 0.2);
}

[data-theme="dark"] .no-doc-warning {
  background: rgba(239, 68, 68, 0.15);
}

.warning-text {
  color: #ef4444;
  font-size: var(--font-size-sm);
  font-weight: 500;
}

.warning-desc {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}

.title-input,
.summary-input {
  margin-top: 8px;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--glass-border);
  background: rgba(255, 255, 255, 0.5);
  color: var(--color-text);
  font-size: var(--font-size-sm);
}

[data-theme="dark"] .title-input,
[data-theme="dark"] .summary-input {
  background: rgba(26, 26, 26, 0.4);
  border-color: var(--glass-dark-border);
}

.title-input:focus,
.summary-input:focus {
  border-color: var(--aurora-cyan);
  outline: none;
}

/* 提示 */
.hint-section {
  padding: 12px;
  background: rgba(16, 185, 129, 0.1);
  border-radius: var(--radius-sm);
}

.hint-text {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}

/* 按钮 */
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.cancel-btn,
.confirm-btn {
  padding: 10px 24px;
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

.confirm-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--glow-cyan);
}

.confirm-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>