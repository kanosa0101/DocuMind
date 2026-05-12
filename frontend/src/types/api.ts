// 统一响应格式
export interface Result<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 用户信息
export interface UserVO {
  id: number
  username: string
  email: string
  phone?: string
  avatar?: string
  nickname?: string
  role: string
  status: number
  createTime: string
}

// 登录响应
export interface LoginVO {
  accessToken: string
  refreshToken: string
  user: UserVO
}

// 登录请求
export interface LoginDTO {
  username: string
  password: string
}

// 注册请求
export interface RegisterDTO {
  username: string
  password: string
  email: string
  phone?: string
}

// 文件元数据
export interface FileMetadata {
  id: string
  fileId: string
  fileName: string
  originalFileName: string
  filePath: string
  fileType: string
  fileSize: number
  md5?: string
  storageType: string
  bucketName?: string
  objectKey?: string
  status: string
  createBy?: string
  createTime: string
  updateTime: string
}

// 文档信息
export interface DocumentVO {
  id: string
  title: string
  content?: string
  summary?: string
  keywords?: string[]
  fileId?: string
  category?: string
  tags?: string[]
  version: number
  status: string
  userId?: string
  createdBy?: string
  createTime: string
  updateTime: string
}

// 文档版本
export interface DocumentVersion {
  id: string
  documentId: string
  versionNumber: number
  content: string
  changeLog?: string
  createdBy?: string
  createTime: string
}

// AI摘要响应
export interface TextSummarizeVO {
  summary: string
  originalLength: number
  summaryLength: number
  compressionRatio: number
}

// AI摘要请求
export interface TextSummarizeDTO {
  content: string
  maxLength?: number
}

// 关键词提取响应
export interface KeywordExtractVO {
  keywords: KeywordItem[]
  totalCount: number
}

export interface KeywordItem {
  word: string
  score: number
  type?: string
}

// 关键词提取请求
export interface KeywordExtractDTO {
  content: string
  count?: number
}

// 文档分析响应
export interface TextAnalyzeVO {
  totalCharacters: number
  chineseCharacters: number
  englishCharacters: number
  digits: number
  spaces: number
  punctuations: number
  lines: number
  words: number
  sentences: number
}

// RAG查询响应
export interface RagQueryVO {
  question: string
  context: string
  answer: string
  sources: string[]
  retrievalStrategy?: string
}

// 聊天消息
export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  timestamp: number
}

// 存储信息
export interface StorageInfo {
  used: number
  total: number
  percentage: number
}