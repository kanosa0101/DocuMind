import { ref } from 'vue'

interface ToastItem {
  id: number
  message: string
  type: 'success' | 'error' | 'warning' | 'info'
  duration: number
}

const toasts = ref<ToastItem[]>([])
let toastId = 0

export const useToast = () => {
  const addToast = (message: string, type: ToastItem['type'] = 'info', duration = 3000) => {
    const id = ++toastId
    toasts.value.push({ id, message, type, duration })

    if (duration > 0) {
      setTimeout(() => removeToast(id), duration)
    }
    return id
  }

  const removeToast = (id: number) => {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }

  const success = (message: string) => addToast(message, 'success', 3000)
  const error = (message: string) => addToast(message, 'error', 5000)
  const warning = (message: string) => addToast(message, 'warning', 4000)
  const info = (message: string) => addToast(message, 'info', 3000)

  return {
    toasts,
    addToast,
    removeToast,
    success,
    error,
    warning,
    info
  }
}