<template>
  <div class="chat-sidebar glass-card">
    <!-- 对话标题 -->
    <div class="chat-header">
      <span class="ai-icon">🤖</span>
      <span class="chat-title">AI Agent</span>
      <button class="clear-btn" @click="clearChat">🗑️</button>
    </div>

    <!-- 消息列表 -->
    <div class="message-list" ref="messageContainer">
      <div v-if="messages.length === 0" class="empty-state">
        <span class="empty-icon">💬</span>
        <p>开始与AI对话</p>
      </div>

      <ChatBubble
        v-for="msg in messages"
        :key="msg.timestamp"
        :role="msg.role"
        :content="msg.content"
      />
    </div>

    <!-- 输入区域 -->
    <div class="chat-input-area">
      <input
        v-model="inputText"
        type="text"
        placeholder="输入消息..."
        class="chat-input"
        :disabled="isSending"
        @keydown.enter="sendMessage"
      />
      <button class="send-btn" :disabled="!inputText.trim() || isSending" @click="sendMessage">
        {{ isSending ? '⏳' : '📤' }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import ChatBubble from './ChatBubble.vue'
import { startConversation, sendAgentMessage, endConversation } from '@/api/ai'
import type { ChatMessage } from '@/types/api'

const messages = ref<ChatMessage[]>([])
const inputText = ref('')
const isSending = ref(false)
const conversationId = ref<string | null>(null)
const messageContainer = ref<HTMLElement | null>(null)

const sendMessage = async () => {
  if (!inputText.value.trim() || isSending.value) return

  const text = inputText.value.trim()
  inputText.value = ''

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: text,
    timestamp: Date.now()
  })

  isSending.value = true

  try {
    // 开始对话
    if (!conversationId.value) {
      conversationId.value = await startConversation()
    }

    // 发送消息
    const response = await sendAgentMessage(conversationId.value, text)

    // 添加AI回复
    messages.value.push({
      role: 'assistant',
      content: response.answer,
      timestamp: Date.now()
    })

    // 滚动到底部
    nextTick(() => {
      messageContainer.value?.scrollTo({
        top: messageContainer.value!.scrollHeight,
        behavior: 'smooth'
      })
    })
  } catch (error: any) {
    messages.value.push({
      role: 'assistant',
      content: `错误: ${error.message || '发送失败'}`,
      timestamp: Date.now()
    })
  } finally {
    isSending.value = false
  }
}

const clearChat = async () => {
  try {
    if (conversationId.value) {
      await endConversation(conversationId.value)
    }
  } catch (error: any) {
    console.warn('结束对话失败:', error.message || error)
  } finally {
    messages.value = []
    conversationId.value = null
  }
}
</script>

<style scoped>
.chat-sidebar {
  width: var(--chat-sidebar-width);
  height: 100vh;
  position: fixed;
  right: 0;
  top: 0;
  display: flex;
  flex-direction: column;
  background: var(--glass-bg);
  border-left: 1px solid var(--glass-border);
  z-index: 100;
}

.chat-header {
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid var(--glass-border);
}

.ai-icon {
  font-size: 24px;
}

.chat-title {
  font-weight: 600;
  color: var(--color-text);
}

.clear-btn {
  margin-left: auto;
  background: transparent;
  border: none;
  cursor: pointer;
  opacity: 0.6;
  transition: opacity var(--transition-base);
}

.clear-btn:hover {
  opacity: 1;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: var(--color-text-muted);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.chat-input-area {
  padding: 16px;
  display: flex;
  gap: 12px;
  border-top: 1px solid var(--glass-border);
}

.chat-input {
  flex: 1;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--glass-border);
  background: rgba(255, 255, 255, 0.5);
  font-size: var(--font-size-sm);
  outline: none;
}

.chat-input:focus {
  border-color: var(--color-primary);
}

.send-btn {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  background: var(--gradient-primary);
  border: none;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all var(--transition-base);
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-2px);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>