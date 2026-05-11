// 文件API

import api from './index'
import type { FileMetadata } from '@/types/api'

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

// 文件下载
export async function downloadFile(fileId: string): Promise<Blob> {
  const response = await api.get(`/api/files/download/${fileId}`, {
    responseType: 'blob'
  })
  return response.data
}

// 文件预览
export async function previewFile(fileId: string): Promise<Blob> {
  const response = await api.get(`/api/files/preview/${fileId}`, {
    responseType: 'blob'
  })
  return response.data
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

// 获取文件列表
export async function getFileList(page: number = 1, size: number = 10, sortBy: string = 'createTime', direction: string = 'desc'): Promise<FileMetadata[]> {
  return api.get<FileMetadata[]>('/api/files/list', {
    params: { page, size, sortBy, direction }
  })
}

// 搜索文件
export async function searchFiles(keyword: string, page: number = 1, size: number = 10): Promise<FileMetadata[]> {
  return api.get<FileMetadata[]>('/api/files/search', {
    params: { keyword, page, size }
  })
}

// 获取文件名
export async function getFileName(fileId: string): Promise<string> {
  return api.get<string>(`/api/files/info/${fileId}/name`)
}

// 获取文件类型
export async function getFileType(fileId: string): Promise<string> {
  return api.get<string>(`/api/files/info/${fileId}/type`)
}