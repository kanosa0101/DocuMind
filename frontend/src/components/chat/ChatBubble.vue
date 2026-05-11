<template>
  <div class="chat-bubble" :class="role">
    <div class="bubble-avatar">
      {{ role === 'user' ? '👤' : '🤖' }}
    </div>
    <div class="bubble-content">
      <p>{{ content }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  role: 'user' | 'assistant'
  content: string
}>()
</script>

<style scoped>
.chat-bubble {
  display: flex;
  gap: 12px;
  animation: fade-in 0.3s ease;
}

.chat-bubble.user {
  flex-direction: row-reverse;
}

.bubble-avatar {
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 20px;
}

.chat-bubble.user .bubble-avatar {
  background: var(--gradient-primary);
}

.chat-bubble.assistant .bubble-avatar {
  background: var(--gradient-success);
}

.bubble-content {
  max-width: 80%;
  padding: 12px 16px;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.8);
}

.chat-bubble.user .bubble-content {
  background: var(--gradient-primary);
  color: white;
}

.bubble-content p {
  font-size: var(--font-size-sm);
  line-height: 1.6;
}
</style>