// 文档API

import api from './index'
import type { DocumentVO, DocumentVersion } from '@/types/api'

// 创建文档
export async function createDocument(data: { title: string; content: string; fileId?: string; category?: string; tags?: string[] }): Promise<DocumentVO> {
  return api.post<DocumentVO>('/api/documents', data)
}

// 更新文档
export async function updateDocument(id: string, data: {
  title?: string
  content?: string
  summary?: string
  category?: string
  keywords?: string[]
  tags?: string[]
  changeLog?: string
}): Promise<DocumentVO> {
  return api.put<DocumentVO>(`/api/documents/${id}`, data)
}

// 删除文档
export async function deleteDocument(id: string): Promise<void> {
  return api.delete(`/api/documents/${id}`)
}

// 获取文档详情
export async function getDocument(id: string): Promise<DocumentVO> {
  return api.get<DocumentVO>(`/api/documents/${id}`)
}

// 获取用户文档列表
export async function getUserDocuments(userId: number): Promise<DocumentVO[]> {
  return api.get<DocumentVO[]>(`/api/documents/user/${userId}`)
}

// 搜索文档
export async function searchDocuments(keyword: string, category?: string): Promise<DocumentVO[]> {
  return api.get<DocumentVO[]>('/api/documents/search', {
    params: { keyword, category }
  })
}

// 获取文档版本历史
export async function getDocumentVersions(id: string): Promise<DocumentVersion[]> {
  return api.get<DocumentVersion[]>(`/api/documents/${id}/versions`)
}

// 恢复文档版本
export async function restoreDocumentVersion(id: string, versionNumber: number): Promise<DocumentVO> {
  return api.post<DocumentVO>(`/api/documents/${id}/restore/${versionNumber}`)
}