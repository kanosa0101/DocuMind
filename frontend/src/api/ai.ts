// AI处理API

import api from './index'
import type { TextSummarizeDTO, TextSummarizeVO, KeywordExtractDTO, KeywordExtractVO, TextAnalyzeVO, RagQueryVO, ChatMessage } from '@/types/api'

// 文档摘要
export async function summarize(data: TextSummarizeDTO): Promise<TextSummarizeVO> {
  return api.post<TextSummarizeVO>('/api/ai/summarize', data)
}

// 关键词提取
export async function extractKeywords(data: KeywordExtractDTO): Promise<KeywordExtractVO> {
  return api.post<KeywordExtractVO>('/api/ai/keywords', data)
}

// 文档分析
export async function analyzeText(content: string): Promise<TextAnalyzeVO> {
  return api.post<TextAnalyzeVO>('/api/ai/analyze', { content })
}

// RAG知识库问答 - 发送JSON对象 {question, strategy}
export async function ragQuery(question: string, strategy?: string): Promise<RagQueryVO> {
  return api.post<RagQueryVO>('/api/ai/rag/query', { question, strategy })
}

// RAG搜索
export async function ragSearch(query: string, topK: number = 5): Promise<any[]> {
  return api.get<any[]>('/api/ai/rag/search', {
    params: { query, topK }
  })
}

// RAG混合搜索
export async function ragHybridSearch(query: string, topK: number = 5): Promise<any[]> {
  return api.get<any[]>('/api/ai/rag/search/hybrid', {
    params: { query, topK }
  })
}

// 开始Agent对话
export async function startConversation(): Promise<string> {
  return api.post<string>('/api/ai/agent/chat/start')
}

// 发送Agent消息 - 发送JSON对象 {userInput}
export async function sendAgentMessage(conversationId: string, userInput: string): Promise<{ answer: string; conversationId: string }> {
  return api.post<{ answer: string; conversationId: string }>('/api/ai/agent/chat', { userInput }, {
    params: { conversationId }
  })
}

// 结束Agent对话
export async function endConversation(conversationId: string): Promise<boolean> {
  return api.post<boolean>(`/api/ai/agent/chat/${conversationId}/end`)
}

// 获取对话历史
export async function getConversationHistory(conversationId: string): Promise<string[]> {
  return api.get<string[]>(`/api/ai/agent/chat/${conversationId}/history`)
}

// 获取RAG文档列表
export async function getRagDocuments(): Promise<string[]> {
  return api.get<string[]>('/api/ai/rag/documents')
}

// 索引文档到RAG
export async function indexRagDocument(documentId: string, content: string): Promise<void> {
  return api.post('/api/ai/rag/index', content, {
    params: { documentId }
  })
}

// 删除RAG文档
export async function deleteRagDocument(documentId: string): Promise<void> {
  return api.delete(`/api/ai/rag/document/${documentId}`)
}