<template>
  <div class="ripple-loader">
    <div class="ripple-wave" v-for="i in 4" :key="i" :style="{ animationDelay: `${i * 0.2}s` }"></div>
    <div class="ripple-center">
      <span class="ai-icon">🧠</span>
    </div>
    <p class="processing-text">AI 正在解析...</p>
  </div>
</template>

<style scoped>
.ripple-loader {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: relative;
  min-height: 150px;
}

.ripple-wave {
  position: absolute;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid var(--color-primary);
  animation: ripple-expand 2s ease-out infinite;
  opacity: 0;
}

@keyframes ripple-expand {
  0% {
    width: 40px;
    height: 40px;
    opacity: 1;
  }
  100% {
    width: 120px;
    height: 120px;
    opacity: 0;
  }
}

.ripple-center {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: var(--gradient-primary);
  display: flex;
  justify-content: center;
  align-items: center;
  animation: pulse-glow 1.5s ease-in-out infinite;
  z-index: 10;
}

@keyframes pulse-glow {
  0%, 100% {
    box-shadow: 0 0 20px rgba(26, 115, 232, 0.4);
  }
  50% {
    box-shadow: 0 0 40px rgba(26, 115, 232, 0.8);
  }
}

.ai-icon {
  font-size: 28px;
}

.processing-text {
  margin-top: 24px;
  font-size: var(--font-size-sm);
  color: var(--color-primary);
  animation: fade-in 0.5s ease;
}
</style>