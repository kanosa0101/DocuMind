import request from './index'

/**
 * 全局搜索API
 */

export interface GlobalSearchResult {
  query: string
  strategy: 'QA' | 'KEYWORD' | 'CLASSIFY' | 'HYBRID'
  answer?: string
  sources?: SourceDocument[]
  documents?: SearchResultDocument[]
  message?: string
  recommendedQuestions?: string[]  // 推荐问题
}

export interface SourceDocument {
  id: string
  title: string
  similarity: number
}

export interface SearchResultDocument {
  id: string
  title: string
  content: string
  similarity: number
  source?: string
}

/**
 * 全局搜索
 */
export function globalSearch(query: string, userId?: number) {
  return request.get<GlobalSearchResult>('/api/ai/search', {
    params: { query, userId }
  })
}

/**
 * 检测搜索策略
 */
export function detectSearchStrategy(query: string) {
  return request.get<string>('/api/ai/search/strategy', {
    params: { query }
  })
}