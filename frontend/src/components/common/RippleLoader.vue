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
  border: 2px solid var(--aurora-cyan);
  animation: ripple-expand 2s ease-out infinite;
  opacity: 0;
}

[data-theme="dark"] .ripple-wave {
  border-color: var(--aurora-cyan);
  box-shadow: 0 0 10px rgba(8, 145, 178, 0.3);
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
  background: var(--gradient-aurora);
  display: flex;
  justify-content: center;
  align-items: center;
  animation: pulse-glow 1.5s ease-in-out infinite;
  z-index: 10;
}

[data-theme="dark"] .ripple-center {
  box-shadow: var(--glow-cyan);
}

@keyframes pulse-glow {
  0%, 100% {
    box-shadow: 0 0 20px rgba(8, 145, 178, 0.4), 0 0 30px rgba(16, 185, 129, 0.3);
  }
  50% {
    box-shadow: 0 0 40px rgba(8, 145, 178, 0.7), 0 0 50px rgba(59, 130, 246, 0.5);
  }
}

.ai-icon {
  font-size: 28px;
}

[data-theme="dark"] .ai-icon {
  filter: drop-shadow(0 0 10px rgba(8, 145, 178, 0.5));
}

.processing-text {
  margin-top: 24px;
  font-size: var(--font-size-sm);
  color: var(--aurora-cyan);
  animation: fade-in 0.5s ease;
}

[data-theme="dark"] .processing-text {
  text-shadow: var(--glow-text-cyan);
}
</style>