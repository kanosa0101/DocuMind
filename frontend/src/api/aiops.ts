// AIOps监控运维API

import api from './index'
import type { AIOpsMetrics, HealthCheckResult, FaultInfo, FaultResolveResult } from '@/types/api'

// 获取监控指标
export async function getAIOpsMetrics(): Promise<AIOpsMetrics> {
  return api.get<AIOpsMetrics>('/api/ai/aiops/monitor')
}

// 健康检查
export async function healthCheck(): Promise<HealthCheckResult> {
  return api.get<HealthCheckResult>('/api/ai/aiops/health')
}

// 重置指标
export async function resetMetrics(): Promise<void> {
  return api.post<void>('/api/ai/aiops/metrics/reset')
}

// 检测故障
export async function detectFaults(): Promise<FaultInfo[]> {
  return api.get<FaultInfo[]>('/api/ai/aiops/detect')
}

// 获取故障列表
export async function getAllFaults(): Promise<FaultInfo[]> {
  return api.get<FaultInfo[]>('/api/ai/aiops/faults')
}

// 处理故障
export async function resolveFault(faultId: string): Promise<FaultResolveResult> {
  return api.post<FaultResolveResult>(`/api/ai/aiops/faults/${faultId}/resolve`)
}

// 记录计数器
export async function incrementCounter(name: string, delta: number = 1): Promise<void> {
  return api.post<void>('/api/ai/aiops/metrics/counter', null, {
    params: { name, delta }
  })
}

// 记录耗时
export async function recordTimer(name: string, duration: number): Promise<void> {
  return api.post<void>('/api/ai/aiops/metrics/timer', null, {
    params: { name, duration }
  })
}