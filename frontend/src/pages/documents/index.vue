<template>
  <div class="documents-page">
    <!-- 工具栏 -->
    <div class="toolbar glass-card">
      <div class="search-section">
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="搜索文档..."
          @keydown.enter="handleSearch"
        />
        <button @click="handleSearch">搜索</button>
      </div>
      <div class="action-section">
        <button class="create-btn" @click="showCreateDialog">
          ✏️ 新建文档
        </button>
      </div>
    </div>

    <!-- 文档列表 -->
    <div class="documents-list glass-card">
      <div v-if="loading" class="loading-state">
        <RippleLoader />
      </div>

      <div v-else-if="documents.length === 0" class="empty-state">
        <span>📭</span>
        <p>暂无文档</p>
      </div>

      <div v-else class="documents-table">
        <div class="table-header">
          <span class="col-title">标题</span>
          <span class="col-category">分类</span>
          <span class="col-version">版本</span>
          <span class="col-time">更新时间</span>
          <span class="col-actions">操作</span>
        </div>
        <div v-for="doc in documents" :key="doc.id" class="table-row">
          <span class="col-title">
            <a @click="openDocument(doc)">{{ doc.title }}</a>
          </span>
          <span class="col-category">{{ doc.category || '未分类' }}</span>
          <span class="col-version">v{{ doc.version }}</span>
          <span class="col-time">{{ formatDate(doc.updateTime) }}</span>
          <span class="col-actions">
            <button @click="openDocument(doc)" title="查看">👁️</button>
            <button @click="editDocument(doc)" title="编辑">✏️</button>
            <button @click="showVersions(doc)" title="版本历史">📜</button>
            <button @click="deleteDocument(doc)" title="删除">🗑️</button>
          </span>
        </div>
      </div>
    </div>

    <!-- 创建/编辑文档对话框 -->
    <div v-if="docDialog.visible" class="dialog-overlay" @click.self="closeDocDialog">
      <div class="doc-dialog glass-card">
        <h3>{{ docDialog.isEdit ? '编辑文档' : '新建文档' }}</h3>
        <div class="form-group">
          <label>标题</label>
          <input v-model="docDialog.title" type="text" placeholder="文档标题" />
        </div>
        <div class="form-group">
          <label>分类</label>
          <input v-model="docDialog.category" type="text" placeholder="文档分类" />
        </div>
        <div class="form-group">
          <label>标签</label>
          <input v-model="docDialog.tagsInput" type="text" placeholder="标签（逗号分隔）" />
        </div>
        <div class="form-group">
          <label>内容</label>
          <textarea v-model="docDialog.content" rows="10" placeholder="文档内容..."></textarea>
        </div>
        <div class="dialog-actions">
          <button class="cancel-btn" @click="closeDocDialog">取消</button>
          <button class="save-btn" @click="saveDocument" :disabled="!docDialog.title.trim()">
            {{ docDialog.isEdit ? '保存版本' : '创建' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 版本历史对话框 -->
    <div v-if="versionDialog.visible" class="dialog-overlay" @click.self="closeVersionDialog">
      <div class="version-dialog glass-card">
        <h3>版本历史 - {{ versionDialog.title }}</h3>
        <div v-if="versionLoading" class="version-loading">
          <RippleLoader />
        </div>
        <div v-else-if="versionDialog.versions.length === 0" class="version-empty">
          <p>暂无版本历史</p>
        </div>
        <div v-else class="version-list">
          <div v-for="ver in versionDialog.versions" :key="ver.id" class="version-item">
            <div class="version-info">
              <span class="version-number">v{{ ver.versionNumber }}</span>
              <span class="version-time">{{ formatDate(ver.createTime) }}</span>
              <span v-if="ver.changeLog" class="version-summary">{{ ver.changeLog }}</span>
            </div>
            <div class="version-actions">
              <button @click="previewVersion(ver)" title="预览">👁️</button>
              <button @click="restoreVersion(ver.versionNumber)" title="恢复">🔄</button>
            </div>
          </div>
        </div>
        <div class="dialog-footer">
          <button @click="closeVersionDialog">关闭</button>
        </div>
      </div>
    </div>

    <!-- 版本预览对话框 -->
    <div v-if="previewDialog.visible" class="dialog-overlay" @click.self="closePreviewDialog">
      <div class="preview-dialog glass-card">
        <h3>版本预览 - v{{ previewDialog.version }}</h3>
        <div class="preview-content">
          {{ previewDialog.content }}
        </div>
        <div class="dialog-footer">
          <button @click="closePreviewDialog">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import {
  getUserDocuments, searchDocuments, createDocument,
  updateDocument, deleteDocument as deleteDocApi,
  getDocumentVersions, restoreDocumentVersion, getDocument
} from '@/api/document'
import type { DocumentVO, DocumentVersion } from '@/types/api'
import RippleLoader from '@/components/common/RippleLoader.vue'

const authStore = useAuthStore()

// 状态
const documents = ref<DocumentVO[]>([])
const loading = ref(false)
const searchKeyword = ref('')

// 文档对话框
const docDialog = ref({
  visible: false,
  isEdit: false,
  id: '',
  title: '',
  category: '',
  tagsInput: '',
  content: ''
})

// 版本对话框
const versionDialog = ref({
  visible: false,
  docId: '',
  title: '',
  versions: [] as DocumentVersion[]
})
const versionLoading = ref(false)

// 预览对话框
const previewDialog = ref({
  visible: false,
  version: 0,
  content: ''
})

// 加载文档列表
async function loadDocuments() {
  loading.value = true
  try {
    if (searchKeyword.value.trim()) {
      documents.value = await searchDocuments(searchKeyword.value.trim())
    } else if (authStore.user?.id) {
      documents.value = await getUserDocuments(authStore.user.id)
    }
  } catch (error: any) {
    console.error('加载文档失败:', error.message)
  } finally {
    loading.value = false
  }
}

// 搜索
function handleSearch() {
  loadDocuments()
}

// 打开文档查看
async function openDocument(doc: DocumentVO) {
  try {
    const fullDoc = await getDocument(doc.id)
    previewDialog.value = {
      visible: true,
      version: fullDoc.version,
      content: fullDoc.content || '(无内容)'
    }
  } catch (error: any) {
    console.error('获取文档失败:', error.message)
  }
}

// 编辑文档
function editDocument(doc: DocumentVO) {
  docDialog.value = {
    visible: true,
    isEdit: true,
    id: doc.id,
    title: doc.title,
    category: doc.category || '',
    tagsInput: doc.tags?.join(', ') || '',
    content: doc.content || ''
  }
}

// 显示创建对话框
function showCreateDialog() {
  docDialog.value = {
    visible: true,
    isEdit: false,
    id: '',
    title: '',
    category: '',
    tagsInput: '',
    content: ''
  }
}

// 关闭文档对话框
function closeDocDialog() {
  docDialog.value.visible = false
}

// 保存文档
async function saveDocument() {
  const tags = docDialog.value.tagsInput.split(',').map(t => t.trim()).filter(t => t)

  try {
    if (docDialog.value.isEdit) {
      await updateDocument(docDialog.value.id, {
        title: docDialog.value.title,
        content: docDialog.value.content,
        category: docDialog.value.category,
        keywords: tags,
        changeLog: '用户编辑'
      })
    } else {
      await createDocument({
        title: docDialog.value.title,
        content: docDialog.value.content,
        category: docDialog.value.category,
        tags
      })
    }
    closeDocDialog()
    loadDocuments()
  } catch (error: any) {
    console.error('保存失败:', error.message)
  }
}

// 删除文档
async function deleteDocument(doc: DocumentVO) {
  if (!confirm(`确认删除文档 "${doc.title}"?`)) return
  try {
    await deleteDocApi(doc.id)
    loadDocuments()
  } catch (error: any) {
    console.error('删除失败:', error.message)
  }
}

// 显示版本历史
async function showVersions(doc: DocumentVO) {
  versionDialog.value = {
    visible: true,
    docId: doc.id,
    title: doc.title,
    versions: []
  }
  versionLoading.value = true
  try {
    versionDialog.value.versions = await getDocumentVersions(doc.id)
  } catch (error: any) {
    console.error('获取版本失败:', error.message)
  } finally {
    versionLoading.value = false
  }
}

// 关闭版本对话框
function closeVersionDialog() {
  versionDialog.value.visible = false
}

// 预览版本
function previewVersion(ver: DocumentVersion) {
  previewDialog.value = {
    visible: true,
    version: ver.versionNumber,
    content: ver.content
  }
}

// 关闭预览对话框
function closePreviewDialog() {
  previewDialog.value.visible = false
}

// 恢复版本
async function restoreVersion(versionNumber: number) {
  if (!confirm(`确认恢复到版本 v${versionNumber}?`)) return
  try {
    await restoreDocumentVersion(versionDialog.value.docId, versionNumber)
    closeVersionDialog()
    loadDocuments()
  } catch (error: any) {
    console.error('恢复失败:', error.message)
  }
}

// 格式化日期
function formatDate(dateStr: string): string {
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

onMounted(() => {
  loadDocuments()
})
</script>

<style scoped>
.documents-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
}

.search-section {
  display: flex;
  gap: 12px;
}

.search-section input {
  padding: 8px 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--glass-border);
  background: rgba(255, 255, 255, 0.5);
  outline: none;
  width: 300px;
}

.search-section button {
  padding: 8px 16px;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  background: var(--color-primary);
  color: white;
}

.create-btn {
  padding: 8px 16px;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  background: var(--gradient-success);
  color: white;
  display: flex;
  align-items: center;
  gap: 8px;
}

.documents-list {
  padding: 24px;
}

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px;
  gap: 12px;
}

.empty-state span {
  font-size: 48px;
}

.documents-table {
  display: flex;
  flex-direction: column;
}

.table-header, .table-row {
  display: flex;
  align-items: center;
  padding: 12px 16px;
}

.table-header {
  background: rgba(255, 255, 255, 0.3);
  font-weight: 600;
  border-radius: var(--radius-md);
}

.table-row {
  border-bottom: 1px solid var(--glass-border);
}

.table-row:hover {
  background: rgba(255, 255, 255, 0.2);
}

.col-title {
  flex: 2;
}

.col-title a {
  cursor: pointer;
  color: var(--color-primary);
  text-decoration: none;
}

.col-title a:hover {
  text-decoration: underline;
}

.col-category {
  flex: 1;
}

.col-version {
  flex: 0.5;
  text-align: center;
}

.col-time {
  flex: 1;
  color: var(--color-text-muted);
}

.col-actions {
  flex: 1;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.col-actions button {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  border: none;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.5);
  transition: all var(--transition-base);
}

.col-actions button:hover {
  background: var(--color-primary);
  color: white;
}

.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 200;
}

.doc-dialog, .version-dialog, .preview-dialog {
  padding: 24px;
  max-width: 600px;
  width: 90%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.doc-dialog h3, .version-dialog h3, .preview-dialog h3 {
  text-align: center;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.form-group input, .form-group textarea {
  padding: 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--glass-border);
  background: rgba(255, 255, 255, 0.5);
  outline: none;
}

.form-group textarea {
  resize: vertical;
  min-height: 200px;
}

.dialog-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
}

.cancel-btn, .save-btn {
  padding: 12px 32px;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
}

.cancel-btn {
  background: rgba(255, 255, 255, 0.5);
}

.save-btn {
  background: var(--gradient-primary);
  color: white;
}

.save-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.version-loading, .version-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.version-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.version-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: var(--radius-md);
}

.version-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.version-number {
  font-weight: 600;
  color: var(--color-primary);
}

.version-time {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.version-summary {
  font-size: var(--font-size-sm);
}

.version-actions {
  display: flex;
  gap: 8px;
}

.version-actions button {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  border: none;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.5);
}

.version-actions button:hover {
  background: var(--color-primary);
  color: white;
}

.preview-content {
  padding: 16px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: var(--radius-md);
  white-space: pre-wrap;
  max-height: 400px;
  overflow-y: auto;
}

.dialog-footer {
  display: flex;
  justify-content: center;
}

.dialog-footer button {
  padding: 12px 32px;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  background: var(--color-primary);
  color: white;
}
</style>