import { ref, onMounted, onUnmounted } from 'vue'
import SockJS from 'sockjs-client'
import Stomp from 'stompjs'
import { useToast } from './useToast'

/**
 * WebSocket文档进度监听Hook
 * 监听文档处理的实时进度
 */

interface ProgressMessage {
  type: 'PROGRESS' | 'COMPLETE' | 'ERROR'
  fileUuid: string  // v3.0: 使用fileUuid替代documentId
  documentId?: string  // 兼容旧字段名
  step?: string
  progress?: number
  status?: string
  success?: boolean
  message?: string
}

export function useDocumentProgress(userId: number) {
  const toast = useToast()
  const connected = ref(false)
  const currentProgress = ref<ProgressMessage | null>(null)
  const progresses = ref<Map<string, ProgressMessage>>(new Map())

  let stompClient: Stomp.Client | null = null

  const connect = () => {
    if (connected.value) return

    // 使用环境变量配置WebSocket地址，空字符串使用Vite代理
    const wsBaseUrl = import.meta.env.VITE_WS_BASE_URL || ''
    const socket = new SockJS(`${wsBaseUrl}/ws/progress`)
    stompClient = Stomp.over(socket)

    stompClient.connect({}, () => {
      connected.value = true

      // 订阅进度消息
      stompClient!.subscribe(`/topic/progress/${userId}`, (message: Stomp.Message) => {
        const data: ProgressMessage = JSON.parse(message.body)
        handleProgressMessage(data)
      })

      // 订阅完成消息
      stompClient!.subscribe(`/topic/complete/${userId}`, (message: Stomp.Message) => {
        const data: ProgressMessage = JSON.parse(message.body)
        handleCompleteMessage(data)
      })
    }, (error: any) => {
      console.error('WebSocket连接失败:', error)
      connected.value = false
    })
  }

  const disconnect = () => {
    if (stompClient && stompClient.connected) {
      stompClient.disconnect(() => {
        stompClient = null
        connected.value = false
      })
    } else {
      stompClient = null
      connected.value = false
    }
  }

  const handleProgressMessage = (data: ProgressMessage) => {
    currentProgress.value = data
    // v3.0: 使用fileUuid，兼容documentId
    const id = data.fileUuid || data.documentId || ''
    progresses.value.set(id, data)

    if (data.step && data.progress) {
      toast.info(`${data.step}: ${data.progress}%`)
    }
  }

  const handleCompleteMessage = (data: ProgressMessage) => {
    // v3.0: 使用fileUuid，兼容documentId
    const id = data.fileUuid || data.documentId || ''
    progresses.value.set(id, data)

    if (data.type === 'COMPLETE' && data.success) {
      toast.success('智能整理完成！')
    } else if (data.type === 'ERROR') {
      toast.error(`${data.message || '处理失败'}`)
    }
  }

  onMounted(() => {
    connect()
  })

  onUnmounted(() => {
    disconnect()
  })

  return {
    connected,
    currentProgress,
    progresses,
    connect,
    disconnect
  }
}