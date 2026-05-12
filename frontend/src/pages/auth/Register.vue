<template>
  <div class="register-page">
    <div class="register-card glass-card">
      <!-- Logo -->
      <div class="register-header">
        <span class="logo-icon">📄</span>
        <h1 class="logo-text">DocuMind</h1>
        <p class="register-subtitle">创建您的账号</p>
      </div>

      <!-- 注册表单 -->
      <form class="register-form" @submit.prevent="handleRegister">
        <div class="form-item">
          <label>用户名</label>
          <input
            v-model="username"
            type="text"
            placeholder="请输入用户名"
            required
          />
        </div>

        <div class="form-item">
          <label>邮箱</label>
          <input
            v-model="email"
            type="email"
            placeholder="请输入邮箱"
            required
          />
        </div>

        <div class="form-item">
          <label>手机号（可选）</label>
          <input
            v-model="phone"
            type="tel"
            placeholder="请输入手机号"
          />
        </div>

        <div class="form-item">
          <label>密码</label>
          <input
            v-model="password"
            type="password"
            placeholder="请输入密码"
            required
          />
        </div>

        <button type="submit" class="register-btn" :disabled="loading">
          {{ loading ? '注册中...' : '注册' }}
        </button>

        <p v-if="error" class="error-msg">{{ error }}</p>
      </form>

      <!-- 登录链接 -->
      <div class="register-footer">
        <span>已有账号？</span>
        <router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/auth'

const router = useRouter()

const username = ref('')
const email = ref('')
const phone = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

const handleRegister = async () => {
  if (!username.value || !email.value || !password.value) return

  loading.value = true
  error.value = ''

  try {
    await register({
      username: username.value,
      email: email.value,
      password: password.value,
      phone: phone.value || undefined
    })
    // 注册成功，跳转登录页并显示成功消息
    router.push({ path: '/login', query: { registered: 'success', username: username.value } })
  } catch (err: any) {
    error.value = err.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: var(--gradient-bg);
}

.register-card {
  width: 400px;
  padding: 40px;
}

.register-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-bottom: 32px;
}

.logo-icon {
  font-size: 48px;
}

.logo-text {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-primary);
}

.register-subtitle {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.register-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item label {
  font-size: var(--font-size-sm);
  color: var(--color-text);
  font-weight: 500;
}

.form-item input {
  padding: 12px 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--glass-border);
  background: rgba(255, 255, 255, 0.5);
  font-size: var(--font-size-base);
  outline: none;
  transition: all var(--transition-base);
}

.form-item input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(26, 115, 232, 0.1);
}

.register-btn {
  padding: 14px;
  border-radius: var(--radius-md);
  background: var(--gradient-primary);
  color: white;
  border: none;
  font-size: var(--font-size-base);
  font-weight: 600;
  cursor: pointer;
  transition: all var(--transition-base);
}

.register-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.register-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.error-msg {
  color: var(--color-error);
  font-size: var(--font-size-sm);
  text-align: center;
}

.register-footer {
  margin-top: 24px;
  text-align: center;
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.register-footer a {
  color: var(--color-primary);
  margin-left: 8px;
}
</style>