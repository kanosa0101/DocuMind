<template>
  <div class="skeleton" :class="[type, { animated: animated }]">
    <div v-if="type === 'text'" class="skeleton-text" :style="{ width: width }"></div>
    <div v-else-if="type === 'avatar'" class="skeleton-avatar" :style="{ width: size, height: size }"></div>
    <div v-else-if="type === 'card'" class="skeleton-card">
      <div class="skeleton-card-header"></div>
      <div class="skeleton-card-body">
        <div class="skeleton-line" style="width: 80%"></div>
        <div class="skeleton-line" style="width: 60%"></div>
      </div>
    </div>
    <div v-else-if="type === 'list'" class="skeleton-list">
      <div v-for="i in count" :key="i" class="skeleton-item">
        <div class="skeleton-avatar" style="width: 40px; height: 40px"></div>
        <div class="skeleton-content">
          <div class="skeleton-line" style="width: 70%"></div>
          <div class="skeleton-line" style="width: 50%"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  type?: 'text' | 'avatar' | 'card' | 'list'
  width?: string
  size?: string
  count?: number
  animated?: boolean
}>(), {
  type: 'text',
  width: '100%',
  size: '48px',
  count: 3,
  animated: true
})
</script>

<style scoped>
.skeleton {
  display: flex;
  flex-direction: column;
}

.skeleton.animated .skeleton-text,
.skeleton.animated .skeleton-avatar,
.skeleton.animated .skeleton-line,
.skeleton.animated .skeleton-card-header,
.skeleton.animated .skeleton-card-body {
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}

.skeleton-text,
.skeleton-line {
  height: 16px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-sm);
}

[data-theme="dark"] .skeleton-text,
[data-theme="dark"] .skeleton-line {
  background: rgba(26, 26, 26, 0.4);
}

.skeleton-avatar {
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.2);
}

[data-theme="dark"] .skeleton-avatar {
  background: rgba(26, 26, 26, 0.4);
}

.skeleton-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-md);
}

[data-theme="dark"] .skeleton-card {
  background: rgba(26, 26, 26, 0.3);
}

.skeleton-card-header {
  height: 24px;
  width: 40%;
  background: rgba(255, 255, 255, 0.2);
  border-radius: var(--radius-sm);
}

[data-theme="dark"] .skeleton-card-header {
  background: rgba(26, 26, 26, 0.4);
}

.skeleton-card-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.skeleton-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.skeleton-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

@keyframes skeleton-pulse {
  0%, 100% {
    opacity: 0.6;
  }
  50% {
    opacity: 1;
  }
}
</style>