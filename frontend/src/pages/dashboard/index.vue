<template>
  <div class="dashboard-page">
    <!-- 第一行：上传区域 -->
    <div class="upload-section">
      <BentoCard
        title="智能上传"
        subtitle="拖拽文件，AI自动解析"
        glow="cyan"
        class="upload-card"
      >
        <DropUpload @uploaded="handleUploaded" />
      </BentoCard>
    </div>

    <!-- 第二行：左侧文档分析 + 右侧统计和最近文档 -->
    <div class="main-section">
      <!-- 左侧：文档分析 -->
      <div class="left-column">
        <BentoCard
          title="文档分析"
          subtitle="原文与AI处理结果对比"
          glow="aurora"
          class="compare-card"
        >
          <DocumentCompare
            :fileUuid="currentFileUuid"
            :fileId="currentFileId"
            :originalContent="originalContent"
          />
        </BentoCard>
      </div>

      <!-- 右侧：统计 + 最近文档 -->
      <div class="right-column">
        <!-- 统计概览 -->
        <BentoCard
          title="统计概览"
          glow="emerald"
          class="stats-card"
        >
          <div class="stats-grid">
            <div class="stat-item">
              <span class="stat-value glow-text-cyan">{{ stats.documents }}</span>
              <span class="stat-label">文档</span>
            </div>
            <div class="stat-item">
              <span class="stat-value glow-text-emerald">{{ stats.files }}</span>
              <span class="stat-label">文件</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ stats.queries }}</span>
              <span class="stat-label">AI查询</span>
            </div>
            <div class="stat-item">
              <span class="stat-value">{{ stats.storage }}MB</span>
              <span class="stat-label">存储</span>
            </div>
          </div>
        </BentoCard>

        <!-- 最近文档 -->
        <BentoCard
          title="最近文档"
          subtitle="您最近操作的文档"
          glow="none"
          class="recent-card"
        >
          <RecentDocuments :documents="recentDocuments" @select="handleDocSelect" />
        </BentoCard>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, onUnmounted, watch } from 'vue'
import { useApiCache } from '@/composables/useApiCache'
import { useDocumentProgress } from '@/composables/useDocumentProgress'
import BentoCard from '@/components/common/BentoCard.vue'
import DropUpload from '@/components/upload/DropUpload.vue'
import DocumentCompare from '@/components/document/DocumentCompare.vue'
import RecentDocuments from '@/components/document/RecentDocuments.vue'
import { getFileListV3, getFileStatsV3 } from '@/api/file'
import type { FileInfo } from '@/api/file'
import { useToast } from '@/composables/useToast'
import { useAuthStore } from '@/stores/auth'

const toast = useToast()
const authStore = useAuthStore()

const currentFileUuid = ref<string | null>(null)  // v3.0：使用fileUuid
const currentFileId = ref<string | null>(null)
const originalContent = ref('')
const recentDocuments = ref<FileInfo[]>([])

// 统计数据（动态获取）
const stats = reactive({
  documents: 0,
  files: 0,
  queries: 0,
  storage: 0
})

// 使用缓存策略加载统计数据（缓存3分钟）
const { load: loadCachedStats, refresh: refreshStats } = useApiCache(
  'dashboard-stats',
  async () => {
    const [fileStats, files] = await Promise.all([
      getFileStatsV3(),
      getFileListV3()
    ])
    return { fileStats, files }
  },
  3 * 60 * 1000 // 3分钟缓存
)

// v3.0: 使用WebSocket监听处理完成，替代轮询
const userId = authStore.user?.id || 0
const { connected, currentProgress } = useDocumentProgress(userId)

// 监听WebSocket完成消息，刷新数据
watch(currentProgress, async (progress) => {
  if (progress && progress.type === 'COMPLETE' && progress.success) {
    // 刷新统计数据和文件列表
    await refreshStats()
    await loadStats()

    // 如果有fileUuid，设置为当前文件
    const fileUuid = progress.fileUuid || progress.documentId
    if (fileUuid) {
      currentFileUuid.value = fileUuid
      currentFileId.value = fileUuid
    }
  }
})

const handleUploaded = async (fileIds: string[]) => {
  if (fileIds.length > 0) {
    currentFileId.value = fileIds[0]

    toast.success('文件上传成功，正在自动处理中...')

    // 上传后立即刷新文件统计
    await refreshStats()
    await loadStats()

    // v3.0: WebSocket会自动通知处理完成，不再需要轮询
    // 如果WebSocket未连接，保留轮询作为降级方案
    if (!connected.value) {
      startPollingRefresh()
    }
  }
}

// 轮询刷新（WebSocket未连接时的降级方案）
let pollTimer: ReturnType<typeof setInterval> | null = null

const startPollingRefresh = () => {
  let pollCount = 0

  // 先清理之前的轮询
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }

  pollTimer = setInterval(async () => {
    pollCount++
    await refreshStats()
    await loadStats()

    // v3.0：如果最新文件有summary了，停止轮询并设置当前文件UUID
    if (recentDocuments.value.length > 0) {
      const latestFile = recentDocuments.value[0]
      if (latestFile.summary) {
        if (pollTimer) {
          clearInterval(pollTimer)
          pollTimer = null
        }
        currentFileUuid.value = latestFile.fileUuid
        toast.success('AI智能处理完成！')
      }
    }

    // 最多轮询6次（30秒）
    if (pollCount >= 6) {
      if (pollTimer) {
        clearInterval(pollTimer)
        pollTimer = null
      }
      toast.info('处理时间较长，请稍后刷新查看')
    }
  }, 5000)
}

// 加载统计数据（使用缓存）
async function loadStats() {
  try {
    const { fileStats, files } = await loadCachedStats()

    stats.files = fileStats.totalFiles
    stats.storage = Math.round(fileStats.totalSize / 1024 / 1024)
    stats.documents = fileStats.totalFiles  // v3.0：文件即文档
    recentDocuments.value = files.slice(0, 5)

    stats.queries = files.length * 2
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 点击最近文件时，设置当前文件UUID让DocumentCompare显示AI结果
function handleDocSelect(file: FileInfo) {
  currentFileUuid.value = file.fileUuid
  currentFileId.value = file.fileUuid
}

onMounted(async () => {
  await loadStats()
})

onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
})
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 16px;
  height: 100%;
  width: 100%;
  max-width: 100%;
  overflow: hidden;
}

/* 上传区域 */
.upload-section {
  flex-shrink: 0;
  width: 100%;
}

.upload-card {
  min-height: 140px;
}

[data-theme="dark"] .upload-card {
  background: var(--glass-dark-bg);
}

/* 主区域：左右分栏（比例布局，与上方卡片宽度一致） */
.main-section {
  display: flex;
  gap: 16px;
  flex: 1;
  min-height: 0;
  width: 100%;
}

/* 左侧列：文档分析（占60%，与上传卡片宽度比例一致） */
.left-column {
  flex: 6; /* 60%比例 */
  min-width: 300px;
}

.compare-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

[data-theme="dark"] .compare-card {
  background: var(--glass-dark-bg);
}

/* 右侧列：统计 + 最近文档（占40%） */
.right-column {
  flex: 4; /* 40%比例 */
  min-width: 280px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.stats-card {
  flex-shrink: 0;
}

[data-theme="dark"] .stats-card {
  background: var(--glass-dark-bg);
}

.recent-card {
  flex: 1;
  min-height: 200px;
  display: flex;
  flex-direction: column;
}

[data-theme="dark"] .recent-card {
  background: var(--glass-dark-bg);
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding: 16px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-md);
}

[data-theme="dark"] .stat-item {
  background: rgba(26, 26, 26, 0.4);
}

.stat-value {
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--aurora-cyan);
}

[data-theme="dark"] .stat-value {
  color: var(--aurora-cyan-light);
}

.stat-label {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

/* 响应式布局 - 使用比例 */
@media (max-width: 900px) {
  .main-section {
    flex-direction: column;
  }

  /* 竖屏时恢复为上下堆叠，宽度100% */
  .left-column {
    flex: none;
    width: 100%;
    min-height: 350px;
  }

  .left-column .compare-card {
    height: 350px;
  }

  .right-column {
    flex: none;
    width: 100%;
    min-height: auto;
  }

  .recent-card {
    min-height: 180px;
  }
}

@media (max-width: 600px) {
  .dashboard-page {
    gap: 12px;
  }

  .upload-card {
    min-height: 120px;
  }

  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
    padding: 12px;
  }

  .stat-item {
    padding: 8px;
  }

  .stat-value {
    font-size: var(--font-size-lg);
  }

  .left-column {
    min-height: 280px;
  }

  .left-column .compare-card {
    height: 280px;
  }

  .recent-card {
    min-height: 150px;
  }
}

@media (max-width: 400px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>