<template>
  <div class="dashboard-page">
    <!-- 智能上传区 -->
    <section class="upload-section glass-card">
      <DropUpload @uploaded="handleUploaded" />
    </section>

    <!-- 双栏对比视图 -->
    <section class="compare-section">
      <DocumentCompare
        :fileId="currentFileId"
        :originalContent="originalContent"
      />
    </section>

    <!-- 最近文档 -->
    <section class="recent-section glass-card">
      <h3 class="section-title">最近文档</h3>
      <RecentDocuments :documents="recentDocuments" />
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import DropUpload from '@/components/upload/DropUpload.vue'
import DocumentCompare from '@/components/document/DocumentCompare.vue'
import RecentDocuments from '@/components/document/RecentDocuments.vue'
import { getUserDocuments } from '@/api/document'
import type { DocumentVO } from '@/types/api'

const authStore = useAuthStore()
const currentFileId = ref<string | null>(null)
const originalContent = ref('')
const recentDocuments = ref<DocumentVO[]>([])

const handleUploaded = async (fileId: string) => {
  currentFileId.value = fileId
}

onMounted(async () => {
  if (authStore.user) {
    recentDocuments.value = await getUserDocuments(authStore.user.id)
  }
})
</script>

<style scoped>
.dashboard-page {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: auto 1fr auto;
  gap: 24px;
}

.upload-section {
  grid-column: 1 / -1;
}

.compare-section {
  grid-column: 1 / -1;
  min-height: 500px;
}

.recent-section {
  grid-column: 1 / -1;
  padding: 24px;
}

.section-title {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 16px;
}
</style>