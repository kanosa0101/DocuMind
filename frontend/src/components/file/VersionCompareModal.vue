<template>
  <Teleport to="body">
    <div v-if="visible" class="modal-overlay" @click.self="handleClose">
      <div class="modal-content glass-card">
        <div class="modal-header">
          <h3>🔍 版本对比</h3>
          <button class="close-btn" @click="handleClose">✕</button>
        </div>

        <div class="modal-body">
          <!-- 版本选择 -->
          <div class="version-select-row">
            <div class="version-select">
              <label>版本 A</label>
              <select v-model="versionA" @change="loadVersionData">
                <option v-for="v in availableVersions" :key="'a-' + v" :value="v">v{{ v }}</option>
              </select>
            </div>
            <div class="vs-badge">VS</div>
            <div class="version-select">
              <label>版本 B</label>
              <select v-model="versionB" @change="loadVersionData">
                <option v-for="v in availableVersions" :key="'b-' + v" :value="v">v{{ v }}</option>
              </select>
            </div>
          </div>

          <!-- 对比内容 -->
          <div v-if="loading" class="loading-state">
            <RippleLoader />
            <p>加载对比数据...</p>
          </div>

          <div v-else-if="dataA && dataB" class="compare-content">
            <!-- 文件名对比 -->
            <div class="compare-section">
              <h5 class="section-title"><FileText :size="16" /> 文件名</h5>
              <div class="compare-row">
                <div class="compare-cell" :class="{ changed: dataA.originalName !== dataB.originalName }">
                  <span class="version-label">v{{ versionA }}</span>
                  <span class="cell-content">{{ dataA.originalName }}</span>
                </div>
                <div class="compare-cell" :class="{ changed: dataA.originalName !== dataB.originalName }">
                  <span class="version-label">v{{ versionB }}</span>
                  <span class="cell-content">{{ dataB.originalName }}</span>
                </div>
              </div>
            </div>

            <!-- 文件大小对比 -->
            <div class="compare-section">
              <h5 class="section-title"><HardDrive :size="16" /> 文件大小</h5>
              <div class="compare-row">
                <div class="compare-cell" :class="{ changed: dataA.fileSize !== dataB.fileSize }">
                  <span class="version-label">v{{ versionA }}</span>
                  <span class="cell-content">{{ formatFileSize(dataA.fileSize) }}</span>
                </div>
                <div class="compare-cell" :class="{ changed: dataA.fileSize !== dataB.fileSize }">
                  <span class="version-label">v{{ versionB }}</span>
                  <span class="cell-content">{{ formatFileSize(dataB.fileSize) }}</span>
                </div>
              </div>
            </div>

            <!-- 摘要对比 -->
            <div class="compare-section">
              <h5 class="section-title"><Bot :size="16" /> AI摘要</h5>
              <div class="compare-row summary-row">
                <div class="compare-cell" :class="{ changed: dataA.summary !== dataB.summary }">
                  <span class="version-label">v{{ versionA }}</span>
                  <p class="cell-content summary-text">{{ dataA.summary || '无摘要' }}</p>
                </div>
                <div class="compare-cell" :class="{ changed: dataA.summary !== dataB.summary }">
                  <span class="version-label">v{{ versionB }}</span>
                  <p class="cell-content summary-text">{{ dataB.summary || '无摘要' }}</p>
                </div>
              </div>
              <!-- 摘要差异高亮 -->
              <div v-if="summaryDiff" class="diff-display">
                <p class="diff-label">摘要差异</p>
                <div class="diff-content">
                  <span v-for="(part, idx) in summaryDiff" :key="idx" :class="part.type">{{ part.text }}</span>
                </div>
              </div>
            </div>

            <!-- 关键词对比 -->
            <div class="compare-section">
              <h5 class="section-title"><Tags :size="16" /> 关键词</h5>
              <div class="compare-row">
                <div class="compare-cell keywords-cell">
                  <span class="version-label">v{{ versionA }}</span>
                  <div class="keywords-list">
                    <span v-for="kw in keywordsA" :key="'a-kw-' + kw"
                          class="keyword-tag"
                          :class="{ removed: !keywordsB.includes(kw), unchanged: keywordsB.includes(kw) }">
                      {{ kw }}
                    </span>
                  </div>
                </div>
                <div class="compare-cell keywords-cell">
                  <span class="version-label">v{{ versionB }}</span>
                  <div class="keywords-list">
                    <span v-for="kw in keywordsB" :key="'b-kw-' + kw"
                          class="keyword-tag"
                          :class="{ added: !keywordsA.includes(kw), unchanged: keywordsA.includes(kw) }">
                      {{ kw }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 分类对比 -->
            <div class="compare-section">
              <h5 class="section-title"><Folder :size="16" /> 分类</h5>
              <div class="compare-row">
                <div class="compare-cell" :class="{ changed: dataA.category !== dataB.category }">
                  <span class="version-label">v{{ versionA }}</span>
                  <span class="cell-content category-badge">{{ dataA.category || '未分类' }}</span>
                </div>
                <div class="compare-cell" :class="{ changed: dataA.category !== dataB.category }">
                  <span class="version-label">v{{ versionB }}</span>
                  <span class="cell-content category-badge">{{ dataB.category || '未分类' }}</span>
                </div>
              </div>
            </div>

            <!-- 变更说明对比 -->
            <div class="compare-section">
              <h5 class="section-title"><MessageSquare :size="16" /> 变更说明</h5>
              <div class="compare-row">
                <div class="compare-cell">
                  <span class="version-label">v{{ versionA }}</span>
                  <span class="cell-content">{{ getVersionChangeSummary(dataA) || '无' }}</span>
                </div>
                <div class="compare-cell">
                  <span class="version-label">v{{ versionB }}</span>
                  <span class="cell-content">{{ getVersionChangeSummary(dataB) || '无' }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button class="close-action-btn" @click="handleClose">关闭</button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { getFileInfoV3, getVersionHistoryV3 } from '@/api/file'
import type { FileInfo, VersionHistoryItem } from '@/api/file'
import RippleLoader from '@/components/common/RippleLoader.vue'
import { FileText, Bot, Tags, Folder, HardDrive, MessageSquare } from '@lucide/vue'

interface Props {
  visible: boolean
  fileUuid: string | null
  currentVersion: number
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'close'): void
}>()

const versionA = ref<number>(1)
const versionB = ref<number>(1)
const dataA = ref<VersionHistoryItem | FileInfo | null>(null)
const dataB = ref<VersionHistoryItem | FileInfo | null>(null)
const loading = ref(false)
const allVersions = ref<VersionHistoryItem[]>([])
const fileInfo = ref<FileInfo | null>(null)

// 可用的版本列表
const availableVersions = computed(() => {
  const versions: number[] = []
  // 包含当前版本
  if (fileInfo.value) {
    versions.push(fileInfo.value.version)
  }
  // 包含历史版本
  for (const v of allVersions.value) {
    if (!versions.includes(v.version)) {
      versions.push(v.version)
    }
  }
  return versions.sort((a, b) => a - b)
})

// 关键词数组处理
const keywordsA = computed(() => {
  if (!dataA.value) return []
  const kw = (dataA.value as any).keywords
  if (typeof kw === 'string') {
    try { return JSON.parse(kw) as string[] } catch { return [] }
  }
  return Array.isArray(kw) ? kw : []
})

const keywordsB = computed(() => {
  if (!dataB.value) return []
  const kw = (dataB.value as any).keywords
  if (typeof kw === 'string') {
    try { return JSON.parse(kw) as string[] } catch { return [] }
  }
  return Array.isArray(kw) ? kw : []
})

// 摘要差异计算
const summaryDiff = computed(() => {
  if (!dataA.value?.summary || !dataB.value?.summary) return null

  const textA = dataA.value.summary
  const textB = dataB.value.summary

  // 简单的逐字符对比
  const result: Array<{ type: string; text: string }> = []
  const maxLen = Math.max(textA.length, textB.length)

  for (let i = 0; i < maxLen; i++) {
    const charA = textA[i] || ''
    const charB = textB[i] || ''

    if (charA === charB) {
      result.push({ type: 'unchanged', text: charA })
    } else if (!charA) {
      result.push({ type: 'added', text: charB })
    } else if (!charB) {
      result.push({ type: 'removed', text: charA })
    } else {
      result.push({ type: 'changed', text: charB })
    }
  }

  // 合并相邻相同类型的字符
  const merged: Array<{ type: string; text: string }> = []
  for (const part of result) {
    if (merged.length > 0 && merged[merged.length - 1].type === part.type) {
      merged[merged.length - 1].text += part.text
    } else {
      merged.push(part)
    }
  }

  return merged
})

// 加载版本数据
async function loadVersionData() {
  if (!props.fileUuid) return

  loading.value = true

  try {
    // 加载文件信息和版本历史
    fileInfo.value = await getFileInfoV3(props.fileUuid)
    allVersions.value = await getVersionHistoryV3(props.fileUuid)

    // 获取版本A数据
    if (versionA.value === fileInfo.value.version) {
      dataA.value = fileInfo.value
    } else {
      dataA.value = allVersions.value.find(v => v.version === versionA.value) || null
    }

    // 获取版本B数据
    if (versionB.value === fileInfo.value.version) {
      dataB.value = fileInfo.value
    } else {
      dataB.value = allVersions.value.find(v => v.version === versionB.value) || null
    }
  } catch (error) {
    console.error('加载版本数据失败:', error)
  } finally {
    loading.value = false
  }
}

function handleClose() {
  emit('close')
}

// 获取版本变更说明（类型安全处理）
function getVersionChangeSummary(data: VersionHistoryItem | FileInfo | null): string {
  if (!data) return ''
  // VersionHistoryItem有changeSummary属性
  if ('changeSummary' in data) {
    return data.changeSummary || ''
  }
  return ''
}

function formatFileSize(size: number): string {
  if (!size) return '-'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

// 监听visible变化
watch(() => props.visible, (visible) => {
  if (visible && props.fileUuid) {
    // 初始化版本选择
    versionA.value = Math.max(1, props.currentVersion - 1)
    versionB.value = props.currentVersion
    loadVersionData()
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
  width: 800px;
  max-width: 95vw;
  max-height: 85vh;
  overflow-y: auto;
  padding: 24px;
  background: var(--glass-bg);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius-lg);
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

/* 版本选择行 */
.version-select-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: rgba(8, 145, 178, 0.1);
  border-radius: var(--radius-md);
}

[data-theme="dark"] .version-select-row {
  background: rgba(8, 145, 178, 0.15);
}

.version-select {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.version-select label {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.version-select select {
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--glass-border);
  background: rgba(255, 255, 255, 0.3);
  color: var(--color-text);
  cursor: pointer;
}

[data-theme="dark"] .version-select select {
  background: rgba(26, 26, 26, 0.4);
  border-color: var(--glass-dark-border);
}

.vs-badge {
  padding: 8px 16px;
  background: var(--gradient-aurora);
  color: white;
  border-radius: var(--radius-md);
  font-weight: 600;
}

/* 对比内容 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px;
}

.compare-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.compare-section {
  padding: 16px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: var(--radius-md);
}

[data-theme="dark"] .compare-section {
  background: rgba(26, 26, 26, 0.4);
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

.compare-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.compare-cell {
  padding: 12px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: var(--radius-sm);
  border: 1px solid transparent;
  transition: all var(--transition-fast);
}

[data-theme="dark"] .compare-cell {
  background: rgba(26, 26, 26, 0.5);
}

.compare-cell.changed {
  border-color: rgba(8, 145, 178, 0.5);
  background: rgba(8, 145, 178, 0.15);
}

[data-theme="dark"] .compare-cell.changed {
  background: rgba(8, 145, 178, 0.2);
}

.version-label {
  display: block;
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  margin-bottom: 8px;
}

.cell-content {
  font-size: var(--font-size-sm);
  color: var(--color-text);
}

/* 摘要对比 */
.summary-row .compare-cell {
  min-height: 100px;
}

.summary-text {
  margin: 0;
  line-height: 1.6;
}

/* 摘要差异显示 */
.diff-display {
  margin-top: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: var(--radius-sm);
}

[data-theme="dark"] .diff-display {
  background: rgba(26, 26, 26, 0.3);
}

.diff-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
  margin: 0 0 8px 0;
}

.diff-content {
  font-size: var(--font-size-sm);
  line-height: 1.8;
}

.diff-content .unchanged {
  color: var(--color-text);
}

.diff-content .added {
  background: rgba(16, 185, 129, 0.3);
  color: var(--aurora-emerald);
  padding: 1px 2px;
  border-radius: 2px;
}

.diff-content .removed {
  background: rgba(239, 68, 68, 0.3);
  color: #ef4444;
  padding: 1px 2px;
  border-radius: 2px;
}

.diff-content .changed {
  background: rgba(139, 92, 246, 0.3);
  color: #8b5cf6;
  padding: 1px 2px;
  border-radius: 2px;
}

/* 关键词对比 */
.keywords-cell {
  min-height: 60px;
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
  transition: all var(--transition-fast);
}

[data-theme="dark"] .keyword-tag {
  background: rgba(16, 185, 129, 0.25);
}

.keyword-tag.unchanged {
  background: rgba(16, 185, 129, 0.2);
}

.keyword-tag.added {
  background: rgba(16, 185, 129, 0.4);
  box-shadow: 0 0 4px rgba(16, 185, 129, 0.3);
}

.keyword-tag.removed {
  background: rgba(239, 68, 68, 0.3);
  color: #ef4444;
  opacity: 0.6;
}

.category-badge {
  padding: 4px 12px;
  background: rgba(139, 92, 246, 0.2);
  border-radius: var(--radius-sm);
  color: #8b5cf6;
}

/* 底部按钮 */
.modal-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
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
</style>