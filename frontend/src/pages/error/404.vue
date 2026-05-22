<template>
  <div class="error-page">
    <SearchX class="error-icon" :size="80" />
    <h1>404</h1>
    <p>页面未找到</p>
    <router-link to="/dashboard" class="back-btn">返回首页</router-link>
  </div>
</template>

<script setup lang="ts">
import { SearchX } from '@lucide/vue'
</script>

<style scoped>
.error-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 16px;
  background: var(--gradient-bg);
}

.error-icon {
  font-size: 80px;
}

.error-page h1 {
  font-size: 48px;
  color: var(--color-text);
}

.error-page p {
  font-size: var(--font-size-lg);
  color: var(--color-text-muted);
}

.back-btn {
  padding: 12px 24px;
  border-radius: var(--radius-md);
  background: var(--gradient-primary);
  color: white;
  text-decoration: none;
  transition: all var(--transition-base);
}

.back-btn:hover {
  transform: translateY(-2px);
}
</style>