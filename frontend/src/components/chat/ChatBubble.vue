<template>
  <div class="chat-bubble" :class="[role, { typing: isTyping }]">
    <!-- 头像 -->
    <div class="bubble-avatar">
      <User v-if="role === 'user'" :size="20" />
      <Bot v-else class="ai-avatar glow-text-aurora" :size="24" />
    </div>

    <!-- 内容 -->
    <div class="bubble-content glass-card">
      <!-- AI回复加载动画 -->
      <div v-if="isTyping" class="typing-indicator">
        <span class="dot dot-1"></span>
        <span class="dot dot-2"></span>
        <span class="dot dot-3"></span>
      </div>

      <!-- 正常内容 -->
      <div v-else class="content-wrapper">
        <!-- 纯文本 -->
        <p v-if="!hasMarkdown" class="text-content">{{ content }}</p>

        <!-- Markdown内容 (安全渲染) -->
        <div v-else class="markdown-content" v-html="sanitizedContent"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { User, Bot } from '@lucide/vue'

interface Props {
  role: 'user' | 'assistant'
  content: string
  isTyping?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  isTyping: false
})

// 检测是否包含Markdown格式
const hasMarkdown = computed(() => {
  return /[#*_`]|```/.test(props.content)
})

// 安全的Markdown渲染 - 过滤危险HTML标签
const sanitizedContent = computed(() => {
  if (!hasMarkdown.value) return props.content

  // 简化处理：代码块和基本格式
  let html = props.content
    .replace(/```(\w+)?\n([\s\S]*?)```/g, '<pre class="code-block"><code>$2</code></pre>')
    .replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
    .replace(/#{1,6}\s(.+)/g, '<strong>$1</strong>')

  // XSS防护：移除危险标签和属性
  html = sanitizeHtml(html)

  return html
})

/**
 * 简化的HTML清理函数
 * 移除script标签、危险属性、危险事件处理器
 * 注意：完整方案应使用DOMPurify库
 */
function sanitizeHtml(html: string): string {
  // 移除script标签
  html = html.replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')

  // 移除危险属性（on开头的事件处理器）
  html = html.replace(/\s+on\w+\s*=\s*["'][^"']*["']/gi, '')
  html = html.replace(/\s+on\w+\s*=\s*[^\s>]+/gi, '')

  // 移除javascript:协议
  html = html.replace(/javascript:/gi, '')

  // 移除data:协议（可能用于注入）
  html = html.replace(/data:/gi, '')

  // 移除iframe标签
  html = html.replace(/<iframe\b[^<]*(?:(?!<\/iframe>)<[^<]*)*<\/iframe>/gi, '')

  // 移除object/embed标签
  html = html.replace(/<object\b[^<]*(?:(?!<\/object>)<[^<]*)*<\/object>/gi, '')
  html = html.replace(/<embed\b[^>]*>/gi, '')

  // 移除style标签（可能包含expression等危险CSS）
  html = html.replace(/<style\b[^<]*(?:(?!<\/style>)<[^<]*)*<\/style>/gi, '')

  return html
}
</script>

<style scoped>
.chat-bubble {
  display: flex;
  gap: 12px;
  animation: fade-in 0.3s ease forwards;
}

.chat-bubble.user {
  flex-direction: row-reverse;
}

/* 头像 */
.bubble-avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  display: flex;
  justify-content: center;
  align-items: center;
  flex-shrink: 0;
}

.chat-bubble.user .bubble-avatar {
  background: var(--gradient-aurora);
  color: white;
}

[data-theme="dark"] .chat-bubble.user .bubble-avatar {
  box-shadow: var(--glow-aurora);
}

.chat-bubble.assistant .bubble-avatar {
  background: transparent;
}

.ai-avatar {
  color: var(--aurora-cyan);
}

[data-theme="dark"] .ai-avatar {
  color: var(--aurora-cyan-light);
}

/* 内容区域 */
.bubble-content {
  max-width: 85%;
  padding: 14px 18px;
  border-radius: var(--radius-lg);
}

[data-theme="dark"] .bubble-content {
  background: var(--glass-dark-bg);
  border: 1px solid var(--glass-dark-border);
}

.chat-bubble.user .bubble-content {
  background: var(--gradient-aurora);
  color: white;
  border: none;
}

[data-theme="dark"] .chat-bubble.user .bubble-content {
  box-shadow: var(--glow-aurora);
}

.chat-bubble.assistant .bubble-content:hover {
  box-shadow: var(--glow-cyan-soft);
}

[data-theme="dark"] .chat-bubble.assistant .bubble-content:hover {
  border-color: rgba(8, 145, 178, 0.3);
}

/* 文本内容 */
.text-content {
  font-size: var(--font-size-sm);
  line-height: 1.7;
  white-space: pre-wrap;
}

[data-theme="dark"] .chat-bubble.assistant .text-content {
  color: var(--color-text);
}

/* Markdown内容 */
.markdown-content {
  font-size: var(--font-size-sm);
  line-height: 1.7;
}

.code-block {
  background: rgba(26, 26, 26, 0.8);
  padding: 12px;
  border-radius: var(--radius-md);
  margin: 8px 0;
  overflow-x: auto;
}

[data-theme="dark"] .code-block {
  background: var(--obsidian-surface);
  border: 1px solid var(--glass-dark-border);
}

.code-block code {
  font-size: var(--font-size-sm);
  color: var(--aurora-cyan);
}

.inline-code {
  background: rgba(8, 145, 178, 0.15);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  color: var(--aurora-cyan);
}

[data-theme="dark"] .inline-code {
  background: rgba(8, 145, 178, 0.25);
}

/* 加载动画 */
.typing-indicator {
  display: flex;
  gap: 6px;
  padding: 8px 0;
}

.dot {
  width: 8px;
  height: 8px;
  background: var(--aurora-cyan);
  border-radius: 50%;
  animation: typing-bounce 1.4s ease-in-out infinite;
}

[data-theme="dark"] .dot {
  box-shadow: var(--glow-cyan-soft);
}

.dot-1 {
  animation-delay: 0s;
}

.dot-2 {
  animation-delay: 0.2s;
}

.dot-3 {
  animation-delay: 0.4s;
}

@keyframes typing-bounce {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  30% {
    transform: translateY(-10px);
    opacity: 1;
  }
}

/* 渐入动画 */
@keyframes fade-in {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>