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