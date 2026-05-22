// AI处理API

import api from './index'
import { tokenManager } from '@/utils/token'
import type { TextSummarizeDTO, TextSummarizeVO, KeywordExtractDTO, KeywordExtractVO, TextAnalyzeVO, RagQueryVO, SearchResult, KnowledgeIndexResult, KnowledgeSearchResult, PlanExecuteResult } from '@/types/api'

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

// RAG知识库问答（流式）- 使用SSE
export async function streamRagQuery(
  question: string,
  strategy: string = 'HYBRID',
  onChunk: (chunk: string) => void,
  onComplete?: () => void,
  onError?: (error: string) => void
): Promise<void> {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
  const url = `${baseUrl}/api/ai/rag/query/stream?question=${encodeURIComponent(question)}&strategy=${encodeURIComponent(strategy)}`

  // 获取认证令牌（使用tokenManager正确获取accessToken）
  const tokenInfo = tokenManager.getTokenInfo()
  const token = tokenInfo?.accessToken

  try {
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Accept': 'text/event-stream',
        ...(token ? { 'Authorization': `Bearer ${token}` } : {})
      }
    })

    if (!response.ok) {
      throw new Error(`HTTP error: ${response.status}`)
    }

    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('No response body')
    }

    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })

      // 处理缓冲区中的完整行
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        // SSE格式: 'data:' 或 'data: '（兼容有无空格）
        if (line.startsWith('data:')) {
          // 提取数据部分（跳过'data:'和可能的空格）
          const data = line.startsWith('data: ') ? line.substring(6) : line.substring(5)
          if (data === '[DONE]') {
            onComplete?.()
            return
          }
          if (data.startsWith('[ERROR]')) {
            onError?.(data.substring(8))
            return
          }
          onChunk(data)
        } else if (line.trim() && !line.startsWith(':')) {
          // 直接文本输出（非SSE格式）
          if (line.startsWith('[ERROR]')) {
            onError?.(line.substring(8))
            return
          }
          onChunk(line)
        }
      }
    }

    onComplete?.()
  } catch (error: any) {
    onError?.(error.message || '流式请求失败')
  }
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

// 发送Agent消息（流式）- 使用SSE
export async function streamAgentMessage(
  conversationId: string,
  userInput: string,
  onChunk: (chunk: string) => void,
  onComplete?: () => void,
  onError?: (error: string) => void
): Promise<void> {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || ''
  const url = `${baseUrl}/api/ai/agent/chat/stream?conversationId=${encodeURIComponent(conversationId)}&userInput=${encodeURIComponent(userInput)}`

  // 获取认证令牌（使用tokenManager正确获取accessToken）
  const tokenInfo = tokenManager.getTokenInfo()
  const token = tokenInfo?.accessToken
  const userId = tokenManager.getUser()?.id?.toString() || ''

  try {
    const response = await fetch(url, {
      method: 'GET',
      headers: {
        'Accept': 'text/event-stream',
        'X-User-Id': userId,
        ...(token ? { 'Authorization': `Bearer ${token}` } : {})
      }
    })

    if (!response.ok) {
      throw new Error(`HTTP error: ${response.status}`)
    }

    const reader = response.body?.getReader()
    if (!reader) {
      throw new Error('No response body')
    }

    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })

      // 处理缓冲区中的完整行
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        // SSE格式: 'data:' 或 'data: '（兼容有无空格）
        if (line.startsWith('data:')) {
          // 提取数据部分（跳过'data:'和可能的空格）
          const data = line.startsWith('data: ') ? line.substring(6) : line.substring(5)
          if (data === '[DONE]') {
            onComplete?.()
            return
          }
          if (data.startsWith('[ERROR]')) {
            onError?.(data.substring(8))
            return
          }
          onChunk(data)
        } else if (line.trim() && !line.startsWith(':')) {
          // 直接文本输出（非SSE格式）
          if (line.startsWith('[ERROR]')) {
            onError?.(line.substring(8))
            return
          }
          onChunk(line)
        }
      }
    }

    onComplete?.()
  } catch (error: any) {
    onError?.(error.message || '流式请求失败')
  }
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

// RAG混合检索加重排序
export async function ragHybridSearchWithRerank(
  query: string,
  topK: number = 5,
  strategy: string = 'HYBRID'
): Promise<SearchResult[]> {
  return api.get<SearchResult[]>('/api/ai/rag/search/hybrid/rerank', {
    params: { query, topK, strategy }
  })
}

// 获取重排序策略列表
export async function getRerankStrategies(): Promise<string[]> {
  return api.get<string[]>('/api/ai/rag/rerank/strategies')
}

// 获取RAG文档内容
export async function getRagDocumentContent(documentId: string): Promise<string> {
  return api.get<string>(`/api/ai/rag/document/${documentId}`)
}

// 获取RAG文档元数据
export async function getRagDocumentMetadata(documentId: string): Promise<Record<string, any>> {
  return api.get<Record<string, any>>(`/api/ai/rag/document/${documentId}/metadata`)
}

// Agent知识索引
export async function indexKnowledgeDocument(
  documentId: string,
  content: string
): Promise<KnowledgeIndexResult> {
  return api.post<KnowledgeIndexResult>('/api/ai/agent/knowledge/index', content, {
    params: { documentId }
  })
}

// Agent知识搜索
export async function searchAgentKnowledge(
  query: string,
  topK: number = 5
): Promise<KnowledgeSearchResult[]> {
  return api.get<KnowledgeSearchResult[]>('/api/ai/agent/knowledge/search', {
    params: { query, topK }
  })
}

// Agent删除知识索引
export async function deleteKnowledgeIndex(documentId: string): Promise<KnowledgeIndexResult> {
  return api.delete<KnowledgeIndexResult>(`/api/ai/agent/knowledge/index/${documentId}`)
}

// Agent执行规划任务
export async function executeAgentPlan(task: string): Promise<PlanExecuteResult> {
  return api.post<PlanExecuteResult>('/api/ai/agent/plan/execute', task, {
    headers: { 'Content-Type': 'text/plain' }
  })
}