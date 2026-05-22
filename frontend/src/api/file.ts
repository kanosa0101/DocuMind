// 文件API

import axios from 'axios'
import api from './index'
import { tokenManager } from '@/utils/token'
import type { FileMetadata } from '@/types/api'

// API基础URL
const baseURL = import.meta.env.VITE_API_BASE_URL || ''

// 单文件上传
export async function uploadFile(file: File, onProgress?: (percent: number) => void): Promise<{ fileId: string; message: string }> {
  const formData = new FormData()
  formData.append('file', file)

  return api.post<{ fileId: string; message: string }>('/api/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (progressEvent) => {
      if (progressEvent.total && onProgress) {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(percent)
      }
    }
  })
}

// 多文件上传
export async function uploadMultipleFiles(files: File[]): Promise<{ fileIds: string[]; count: number; message: string }> {
  const formData = new FormData()
  files.forEach(file => formData.append('files', file))

  return api.post<{ fileIds: string[]; count: number; message: string }>('/api/files/upload-multiple', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 文件下载 (二进制响应，使用独立axios请求)
export async function downloadFile(fileId: string): Promise<{ blob: Blob; fileName: string }> {
  // 使用tokenManager获取token
  const tokenInfo = tokenManager.getTokenInfo()
  const accessToken = tokenInfo?.accessToken || ''

  const response = await axios.get(`${baseURL}/api/files/download/${fileId}`, {
    responseType: 'blob',
    headers: {
      Authorization: accessToken ? `Bearer ${accessToken}` : ''
      // 注意：不再设置X-User-Id头，网关会从JWT解析并设置该头
    }
  })

  // 从响应头获取文件名
  let fileName = 'download'
  const contentDisposition = response.headers['content-disposition']
  if (contentDisposition) {
    // 解析 RFC 5987 格式: filename*=UTF-8''encoded_name
    const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/)
    if (utf8Match && utf8Match[1]) {
      fileName = decodeURIComponent(utf8Match[1])
    } else {
      // 尝试解析普通格式: filename="name" 或 filename=name
      const normalMatch = contentDisposition.match(/filename="?([^";\n]+)"?/)
      if (normalMatch && normalMatch[1]) {
        fileName = normalMatch[1]
      }
    }
  }

  return { blob: response.data, fileName }
}

// 文件预览 (二进制响应，使用独立axios请求)
export async function previewFile(fileId: string): Promise<Blob> {
  // 使用tokenManager获取token
  const tokenInfo = tokenManager.getTokenInfo()
  const accessToken = tokenInfo?.accessToken || ''

  const response = await axios.get(`${baseURL}/api/files/preview/${fileId}`, {
    responseType: 'blob',
    headers: {
      Authorization: accessToken ? `Bearer ${accessToken}` : ''
    }
  })
  return response.data
}

// 获取文件内容（文本格式）
export async function getFileContent(fileId: string): Promise<{ content: string; fileName: string }> {
  const blob = await previewFile(fileId)
  const fileName = 'uploaded-file'

  // 将Blob转换为文本
  const content = await blob.text()

  return { content, fileName }
}

// 删除文件
export async function deleteFile(fileId: string): Promise<{ message: string }> {
  return api.delete<{ message: string }>(`/api/files/${fileId}`)
}

// 重命名文件
export async function renameFile(fileId: string, newName: string): Promise<{ message: string }> {
  return api.put<{ message: string }>(`/api/files/${fileId}/rename`, null, {
    params: { newName }
  })
}

// 获取文件元数据
export async function getFileMetadata(fileId: string): Promise<FileMetadata> {
  return api.get<FileMetadata>(`/api/files/metadata/${fileId}`)
}

// 获取文件列表 - 返回带分页信息的对象
export interface FileListResult {
  files: FileMetadata[]
  total: number
  page: number
  size: number
}

export async function getFileList(page: number = 1, size: number = 10, sortBy: string = 'createTime', direction: string = 'desc'): Promise<FileListResult> {
  return api.get<FileListResult>('/api/files/list', {
    params: { page, size, sortBy, direction }
  })
}

// 搜索文件 - 返回带分页信息的对象
export async function searchFiles(keyword: string, page: number = 1, size: number = 10): Promise<FileListResult> {
  return api.get<FileListResult>('/api/files/search', {
    params: { keyword, page, size }
  })
}

// 分片上传
export async function uploadChunk(
  chunk: Blob,
  fileId: string,
  chunkIndex: number,
  totalChunks: number,
  onProgress?: (percent: number) => void
): Promise<{ message: string; chunkIndex: number }> {
  const formData = new FormData()
  formData.append('chunk', chunk)
  formData.append('fileId', fileId)
  formData.append('chunkIndex', String(chunkIndex))
  formData.append('totalChunks', String(totalChunks))

  return api.post<{ message: string; chunkIndex: number }>('/api/files/upload-chunk', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (progressEvent) => {
      if (progressEvent.total && onProgress) {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(percent)
      }
    }
  })
}

// 分片合并
export async function mergeChunk(
  fileId: string,
  fileName: string
): Promise<{ fileId: string; message: string }> {
  return api.post<{ fileId: string; message: string }>('/api/files/merge-chunk', null, {
    params: { fileId, fileName }
  })
}

// 文件移动
export async function moveFile(
  fileId: string,
  targetPath: string
): Promise<{ message: string }> {
  return api.put<{ message: string }>(`/api/files/${fileId}/move`, null, {
    params: { targetPath }
  })
}

// 文件复制
export async function copyFile(
  fileId: string,
  targetPath: string
): Promise<{ fileId: string; message: string }> {
  return api.put<{ fileId: string; message: string }>(`/api/files/${fileId}/copy`, null, {
    params: { targetPath }
  })
}

// 获取用户文件统计
export async function getFileStats(): Promise<{ fileCount: number; totalSize: number; totalSizeMB: number }> {
  return api.get<{ fileCount: number; totalSize: number; totalSizeMB: number }>('/api/files/stats')
}

// ============ 回收站 API ============

export interface RecycleFile {
  recycleId: string
  bucketName: string
  originalObjectName: string
  deleteTime: number
  expiryTime: number
  fileSize: number
  deleter: string
}

export interface RecycleBinVO {
  totalCount: number
  files: RecycleFile[]
}

// 获取回收站文件列表
export async function getRecycleBinList(): Promise<RecycleBinVO> {
  return api.get<RecycleBinVO>('/api/files/recycle/list')
}

// 从回收站恢复文件
export async function restoreFile(recycleId: string): Promise<string> {
  return api.post<string>(`/api/files/recycle/restore/${recycleId}`)
}

// 永久删除回收站文件
export async function permanentDeleteFile(recycleId: string): Promise<void> {
  return api.delete(`/api/files/recycle/${recycleId}`)
}

// ============ v2.0 版本识别 API ============

export interface SimilarityResult {
  similarityDetected: boolean
  similarFileUuid?: string  // v3.0: 使用fileUuid而非docId
  similarFileName?: string
  similarityScore: number
  currentVersion?: number
  recommendation: 'NEW' | 'UPDATE_VERSION' | 'USER_DECIDE' | string
}

// 检测文件相似度 - 使用POST请求避免中文编码问题
export async function checkFileSimilarity(
  fileName: string,
  fileType?: string
): Promise<SimilarityResult | null> {
  return api.post<SimilarityResult | null>('/api/files/similarity/check', {
    fileName,
    fileType
  })
}

// 上传文件（支持版本更新参数）
export async function uploadFileWithVersion(
  file: File,
  docId?: string,
  changeSummary?: string,
  onProgress?: (percent: number) => void
): Promise<{ fileId: string; message: string }> {
  const formData = new FormData()
  formData.append('file', file)
  if (docId) {
    formData.append('docId', docId)
  }
  if (changeSummary) {
    formData.append('changeSummary', changeSummary)
  }

  return api.post<{ fileId: string; message: string }>('/api/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (progressEvent) => {
      if (progressEvent.total && onProgress) {
        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        onProgress(percent)
      }
    }
  })
}

// ============ v3.0 统一文件管理 API ============

// FileInfo类型定义
export interface FileInfo {
  id: number
  fileUuid: string
  fileName: string
  originalName: string
  fileType: string
  fileSize: number
  storagePath: string
  summary?: string
  keywords?: string[]
  category?: string
  version: number
  versionHistory?: string
  indexed: boolean
  vectorId?: string
  indexTime?: string
  processStatus: string
  processTime?: string
  retryCount: number
  userId: number
  status: string
  deleteTime?: string
  createTime: string
  updateTime?: string
}

// 文件统计类型
export interface FileStats {
  totalFiles: number
  activeFiles: number
  deletedFiles: number
  totalSize: number
  indexedFiles: number
  categoryCounts: Record<string, number>
  typeCounts: Record<string, number>
  multiVersionFiles: number
}

// 版本历史项
export interface VersionHistoryItem {
  version: number
  fileUuid: string
  originalName: string
  storagePath: string
  fileSize: number
  summary?: string
  keywords?: string[]
  category?: string
  changeSummary?: string
  createTime: string
}

// 预览结果
export interface PreviewResult {
  fileUuid: string
  fileName: string
  fileType: string
  contentType: string // pdf, html, text, unsupported, error
  totalPages?: number
  pages?: Array<{ pageNumber: number; content: string }>
  htmlContent?: string
  textContent?: string
  message?: string
}

// ===== v3.0 文件操作 =====

// v3上传新文件
export async function uploadFileV3(
  file: File,
  onProgress?: (percent: number) => void
): Promise<{ fileUuid: string; fileName: string; fileSize: number; message: string }> {
  const formData = new FormData()
  formData.append('file', file)

  return api.post<{ fileUuid: string; fileName: string; fileSize: number; message: string }>(
    '/api/v3/files/upload',
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total && onProgress) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(percent)
        }
      }
    }
  )
}

// v3上传新版本
export async function uploadNewVersionV3(
  file: File,
  fileUuid: string,
  changeSummary?: string,
  onProgress?: (percent: number) => void
): Promise<{ fileUuid: string; version: number; message: string }> {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('fileUuid', fileUuid)
  if (changeSummary) {
    formData.append('changeSummary', changeSummary)
  }

  return api.post<{ fileUuid: string; version: number; message: string }>(
    '/api/v3/files/upload-version',
    formData,
    {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total && onProgress) {
          const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          onProgress(percent)
        }
      }
    }
  )
}

// v3批量上传
export async function uploadMultipleV3(
  files: File[]
): Promise<{ count: number; files: Array<{ fileUuid: string; fileName: string }>; message: string }> {
  const formData = new FormData()
  files.forEach(file => formData.append('files', file))

  return api.post<{ count: number; files: Array<{ fileUuid: string; fileName: string }>; message: string }>(
    '/api/v3/files/upload-multiple',
    formData,
    { headers: { 'Content-Type': 'multipart/form-data' } }
  )
}

// v3获取文件详情
export async function getFileInfoV3(fileUuid: string): Promise<FileInfo> {
  return api.get<FileInfo>(`/api/v3/files/${fileUuid}`)
}

// v3获取文件列表
export async function getFileListV3(): Promise<FileInfo[]> {
  return api.get<FileInfo[]>('/api/v3/files/list')
}

// v3获取回收站文件
export async function getDeletedFilesV3(): Promise<FileInfo[]> {
  return api.get<FileInfo[]>('/api/v3/files/deleted')
}

// v3按分类获取文件
export async function getFilesByCategoryV3(category: string): Promise<FileInfo[]> {
  return api.get<FileInfo[]>(`/api/v3/files/category/${category}`)
}

// v3搜索文件
export async function searchFilesV3(keyword: string): Promise<FileInfo[]> {
  return api.get<FileInfo[]>('/api/v3/files/search', { params: { keyword } })
}

// v3获取文件统计
export async function getFileStatsV3(): Promise<FileStats> {
  return api.get<FileStats>('/api/v3/files/stats')
}

// v3获取版本历史
export async function getVersionHistoryV3(fileUuid: string): Promise<VersionHistoryItem[]> {
  return api.get<VersionHistoryItem[]>(`/api/v3/files/${fileUuid}/versions`)
}

// v3切换版本
export async function switchVersionV3(fileUuid: string, targetVersion: number): Promise<FileInfo> {
  return api.put<FileInfo>(`/api/v3/files/${fileUuid}/version/${targetVersion}`)
}

// v3下载指定版本
export async function downloadVersionV3(fileUuid: string, version: number): Promise<{ blob: Blob; fileName: string }> {
  const tokenInfo = tokenManager.getTokenInfo()
  const accessToken = tokenInfo?.accessToken || ''

  const response = await axios.get(`${baseURL}/api/v3/files/${fileUuid}/download/${version}`, {
    responseType: 'blob',
    headers: { Authorization: accessToken ? `Bearer ${accessToken}` : '' }
  })

  let fileName = 'download'
  const contentDisposition = response.headers['content-disposition']
  if (contentDisposition) {
    const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/)
    if (utf8Match && utf8Match[1]) {
      fileName = decodeURIComponent(utf8Match[1])
    }
  }

  return { blob: response.data, fileName }
}

// v3软删除
export async function softDeleteV3(fileUuid: string): Promise<{ message: string }> {
  return api.delete<{ message: string }>(`/api/v3/files/${fileUuid}`)
}

// v3恢复文件
export async function restoreFileV3(fileUuid: string): Promise<{ message: string }> {
  return api.put<{ message: string }>(`/api/v3/files/${fileUuid}/restore`)
}

// v3永久删除
export async function permanentDeleteV3(fileUuid: string): Promise<{ message: string }> {
  return api.delete<{ message: string }>(`/api/v3/files/${fileUuid}/permanent`)
}

// v3批量删除
export async function batchDeleteV3(fileUuids: string[]): Promise<{ message: string; count: string }> {
  return api.post<{ message: string; count: string }>('/api/v3/files/batch/delete', fileUuids)
}

// v3批量分类
export async function batchClassifyV3(fileUuids: string[], category: string): Promise<{ message: string }> {
  return api.post<{ message: string }>('/api/v3/files/batch/classify', { fileUuids, category })
}

// v3更新分类
export async function updateCategoryV3(fileUuid: string, category: string): Promise<{ message: string }> {
  return api.put<{ message: string }>(`/api/v3/files/${fileUuid}/category`, null, { params: { category } })
}

// v3下载文件
export async function downloadFileV3(fileUuid: string): Promise<{ blob: Blob; fileName: string }> {
  const tokenInfo = tokenManager.getTokenInfo()
  const accessToken = tokenInfo?.accessToken || ''

  const response = await axios.get(`${baseURL}/api/v3/files/${fileUuid}/download`, {
    responseType: 'blob',
    headers: { Authorization: accessToken ? `Bearer ${accessToken}` : '' }
  })

  let fileName = 'download'
  const contentDisposition = response.headers['content-disposition']
  if (contentDisposition) {
    const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/)
    if (utf8Match && utf8Match[1]) {
      fileName = decodeURIComponent(utf8Match[1])
    }
  }

  return { blob: response.data, fileName }
}

// v3获取预览URL
export async function getPreviewUrlV3(fileUuid: string): Promise<{ previewUrl: string }> {
  return api.get<{ previewUrl: string }>(`/api/v3/files/${fileUuid}/preview`)
}

// v3获取预览内容
export async function getPreviewContentV3(fileUuid: string): Promise<PreviewResult> {
  return api.get<PreviewResult>(`/api/v3/files/${fileUuid}/preview-content`)
}

// v3获取PDF指定页面
export async function getPdfPageV3(fileUuid: string, pageNumber: number): Promise<{ pageNumber: number; content: string }> {
  return api.get<{ pageNumber: number; content: string }>(
    `/api/v3/files/${fileUuid}/preview-page/${pageNumber}`
  )
}

// v3分享给用户
export async function shareToUsersV3(
  fileUuid: string,
  shareToIds: number[],
  permission: string = 'VIEW',
  expireDays?: number
): Promise<{ message: string }> {
  return api.post<{ message: string }>(`/api/v3/files/${fileUuid}/share`, {
    shareToIds,
    permission,
    expireDays
  })
}

// v3创建公开分享
export async function createPublicShareV3(
  fileUuid: string,
  expireDays?: number,
  password?: string,
  downloadLimit?: number
): Promise<{ shareCode: string; message: string }> {
  return api.post<{ shareCode: string; message: string }>(
    `/api/v3/files/${fileUuid}/share-public`,
    { expireDays, password, downloadLimit }
  )
}

// v3获取公开分享文件
export async function getPublicSharedFileV3(shareCode: string, password?: string): Promise<FileInfo> {
  return api.get<FileInfo>(`/api/v3/files/public/${shareCode}`, { params: { password } })
}

// v3获取分享给我的文件
export async function getSharedToMeV3(): Promise<FileInfo[]> {
  return api.get<FileInfo[]>('/api/v3/files/shared-to-me')
}

// v3相似度检测
export async function checkSimilarityV3(fileName: string): Promise<{
  similarityDetected: boolean
  similarFileUuid?: string
  similarFileName?: string
  similarityScore?: number
  currentVersion?: number
  recommendation?: string
}> {
  return api.post('/api/v3/files/similarity/check', { fileName })
}