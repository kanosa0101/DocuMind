<template>
  <div class="analysis-chart">
    <div class="chart-header">
      <span class="chart-icon">📊</span>
      <span>文档分析</span>
    </div>
    <div v-if="analysis" class="chart-content">
      <div class="stat-item">
        <span class="stat-label">总字符</span>
        <span class="stat-value">{{ analysis.totalCharacters }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">中文字符</span>
        <span class="stat-value">{{ analysis.chineseCharacters }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">英文字符</span>
        <span class="stat-value">{{ analysis.englishCharacters }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">句子数</span>
        <span class="stat-value">{{ analysis.sentences }}</span>
      </div>
      <div class="stat-item">
        <span class="stat-label">段落数</span>
        <span class="stat-value">{{ analysis.lines }}</span>
      </div>
    </div>
    <p v-else class="empty">等待AI处理...</p>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  analysis: any
}>()
</script>

<style scoped>
.analysis-chart {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.chart-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--color-text);
}

.chart-content {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.stat-item {
  padding: 12px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.5);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.stat-value {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-primary);
}

.empty {
  color: var(--color-text-muted);
  text-align: center;
  padding: 24px;
}
</style>