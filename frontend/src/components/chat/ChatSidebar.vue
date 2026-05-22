<template>
  <div
    class="chat-sidebar glass-card"
    :class="{ collapsed: layoutStore.copilotCollapsed, expanded: !layoutStore.copilotCollapsed }"
    :style="{ width: layoutStore.copilotWidth + 'px' }"
  >
    <!-- 收起模式 - 仅显示图标 -->
    <div v-if="layoutStore.copilotCollapsed" class="collapsed-view">
      <button class="expand-btn" @click="layoutStore.toggleCopilot" title="展开AI助手">
        <Bot class="ai-icon-mini glow-text-cyan" :size="20" />
      </button>
    </div>

    <!-- 展开模式 - 完整聊天界面 -->
    <div v-else class="expanded-view">
      <!-- 对话标题 -->
      <div class="chat-header">
        <div class="header-left">
          <Bot class="ai-icon glow-text-cyan" :size="24" />
          <span class="chat-title">AI Agent</span>
        </div>
        <div class="header-actions">
          <button class="action-btn" @click="clearChat" title="清空对话">
            <Trash2 :size="18" />
          </button>
          <button class="collapse-btn" @click="layoutStore.toggleCopilot" title="收起面板">
            <X :size="18" />
          </button>
        </div>
      </div>

      <!-- 消息列表 -->
      <div class="message-list" ref="messageContainer">
        <div v-if="messages.length === 0" class="empty-state">
          <div class="empty-icon-wrapper">
            <MessageCircle class="empty-icon" :size="48" />
            <div class="empty-glow"></div>
          </div>
          <p class="empty-title">开始与AI对话</p>
          <p class="empty-subtitle">提问文档内容、请求分析摘要</p>
        </div>

        <ChatBubble
          v-for="(msg, index) in messages"
          :key="msg.timestamp"
          :role="msg.role"
          :content="msg.content"
          :style="{ animationDelay: `${index * 50}ms` }"
          class="animate-fade-in"
        />
      </div>

      <!-- 输入区域 -->
      <div class="chat-input-area">
        <div class="input-wrapper">
          <input
            v-model="inputText"
            type="text"
            placeholder="输入消息..."
            class="chat-input"
            :disabled="isSending"
            @keydown.enter="sendMessage"
          />
          <button
            class="send-btn aurora-glow-hover"
            :disabled="!inputText.trim() || isSending"
            @click="sendMessage"
          >
            <span v-if="isSending" class="sending-icon">
              <Loader2 :size="18" class="spin" />
            </span>
            <Send v-else class="send-icon" :size="18" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick } from 'vue'
import ChatBubble from './ChatBubble.vue'
import { useLayoutStore } from '@/stores/layout'
import { startConversation, sendAgentMessage, endConversation } from '@/api/ai'
import type { ChatMessage } from '@/types/api'
import { Bot, Trash2, X, MessageCircle, Loader2, Send } from '@lucide/vue'

const layoutStore = useLayoutStore()

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
  height: 100vh;
  position: sticky;
  right: 0;
  top: 0;
  display: flex;
  flex-direction: column;
  border-left: 1px solid var(--glass-border);
  z-index: 100;
  transition: width var(--transition-base);
  overflow: hidden;
  align-self: start;
}

[data-theme="dark"] .chat-sidebar {
  background: var(--glass-dark-bg);
  backdrop-filter: blur(var(--glass-dark-blur));
  border-left: 1px solid var(--glass-dark-border);
  box-shadow: var(--glass-dark-shadow);
}

.chat-sidebar.collapsed {
  width: 48px;
}

.chat-sidebar.expanded {
  width: 320px;
}

/* 收起模式 */
.collapsed-view {
  width: 48px;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px;
}

.expand-btn {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  background: var(--gradient-aurora);
  border: none;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all var(--transition-base);
  box-shadow: var(--glow-aurora);
}

.expand-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 0 30px rgba(8, 145, 178, 0.6);
}

.ai-icon-mini {
  color: white;
}

/* 展开模式 */
.expanded-view {
  width: 320px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--glass-border);
}

[data-theme="dark"] .chat-header {
  border-bottom: 1px solid var(--glass-dark-border);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ai-icon {
  color: var(--aurora-cyan);
}

[data-theme="dark"] .ai-icon {
  color: var(--aurora-cyan-light);
}

.chat-title {
  font-weight: 600;
  color: var(--color-text);
}

[data-theme="dark"] .chat-title {
  color: var(--color-text);
}

.header-actions {
  display: flex;
  gap: 8px;
}

.action-btn,
.collapse-btn {
  width: 32px;
  height: 32px;
  border-radius: var(--radius-sm);
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.1);
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  color: var(--color-text-muted);
  transition: all var(--transition-base);
}

[data-theme="dark"] .action-btn,
[data-theme="dark"] .collapse-btn {
  background: rgba(255, 255, 255, 0.05);
  border-color: var(--glass-dark-border);
}

.action-btn:hover,
.collapse-btn:hover {
  background: rgba(8, 145, 178, 0.15);
  color: var(--aurora-cyan);
  border-color: var(--aurora-cyan);
}

[data-theme="dark"] .action-btn:hover,
[data-theme="dark"] .collapse-btn:hover {
  box-shadow: var(--glow-cyan-soft);
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
  text-align: center;
}

.empty-icon-wrapper {
  position: relative;
  margin-bottom: 16px;
}

.empty-icon {
  color: var(--aurora-cyan);
}

[data-theme="dark"] .empty-icon {
  color: var(--aurora-cyan-light);
}

.empty-glow {
  position: absolute;
  inset: -10px;
  background: radial-gradient(circle, rgba(8, 145, 178, 0.2), transparent);
  filter: blur(20px);
  z-index: -1;
}

.empty-title {
  font-size: var(--font-size-lg);
  color: var(--color-text);
  margin-bottom: 8px;
}

[data-theme="dark"] .empty-title {
  color: var(--color-text);
}

.empty-subtitle {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.chat-input-area {
  padding: 16px;
  border-top: 1px solid var(--glass-border);
}

[data-theme="dark"] .chat-input-area {
  border-top: 1px solid var(--glass-dark-border);
}

.input-wrapper {
  display: flex;
  gap: 12px;
  align-items: center;
}

.chat-input {
  flex: 1;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--glass-border);
  background: rgba(255, 255, 255, 0.5);
  font-size: var(--font-size-sm);
  outline: none;
  transition: all var(--transition-base);
}

[data-theme="dark"] .chat-input {
  background: rgba(26, 26, 26, 0.6);
  border-color: var(--glass-dark-border);
  color: var(--color-text);
}

.chat-input:focus {
  border-color: var(--aurora-cyan);
  box-shadow: 0 0 0 3px rgba(8, 145, 178, 0.2);
}

[data-theme="dark"] .chat-input:focus {
  box-shadow: 0 0 0 3px rgba(8, 145, 178, 0.3), var(--glow-cyan-soft);
}

.send-btn {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  background: var(--gradient-aurora);
  border: none;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all var(--transition-base);
  box-shadow: var(--glow-aurora);
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 0 30px rgba(8, 145, 178, 0.6);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.send-icon {
  color: white;
}

.sending-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.animate-fade-in {
  animation: fade-in 0.3s ease forwards;
}

/* 响应式：小屏幕时隐藏 ChatSidebar */
@media (max-width: 1200px) {
  .chat-sidebar.expanded {
    width: 280px;
  }
}

@media (max-width: 768px) {
  .chat-sidebar {
    position: fixed;
    transform: translateX(100%);
    z-index: 200;
  }

  .chat-sidebar.expanded {
    transform: translateX(0);
    width: 100%;
    max-width: 320px;
  }
}
</style>