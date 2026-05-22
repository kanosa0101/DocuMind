<template>
  <div class="login-page">
    <div class="login-card glass-card">
      <!-- Logo -->
      <div class="login-header">
        <FileText class="logo-icon" :size="48" />
        <h1 class="logo-text">DocuMind</h1>
        <p class="login-subtitle">企业级文档智能处理系统</p>
      </div>

      <!-- 登录表单 -->
      <form class="login-form" @submit.prevent="handleLogin">
        <div class="form-item" :class="{ 'has-error': usernameError }">
          <label>用户名</label>
          <input
            v-model="username"
            type="text"
            placeholder="请输入用户名"
            :class="{ 'input-error': usernameError, 'input-valid': usernameValid }"
            @blur="validateUsername"
            @input="clearUsernameError"
          />
          <span v-if="usernameError" class="field-error">{{ usernameError }}</span>
        </div>

        <div class="form-item" :class="{ 'has-error': passwordError }">
          <label>密码</label>
          <input
            v-model="password"
            type="password"
            placeholder="请输入密码"
            :class="{ 'input-error': passwordError, 'input-valid': passwordValid }"
            @blur="validatePassword"
            @input="clearPasswordError"
          />
          <span v-if="passwordError" class="field-error">{{ passwordError }}</span>
        </div>

        <button type="submit" class="login-btn" :disabled="loading || !isFormValid">
          {{ loading ? '登录中...' : '登录' }}
        </button>

        <p v-if="successMsg" class="success-msg">{{ successMsg }}</p>
        <p v-if="error" class="error-msg">{{ error }}</p>
      </form>

      <!-- 注册链接 -->
      <div class="login-footer">
        <span>还没有账号？</span>
        <router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { FileText } from '@lucide/vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')
const successMsg = ref('')

// 实时验证状态
const usernameError = ref('')
const passwordError = ref('')

const usernameValid = computed(() => username.value.length >= 3 && !usernameError.value)
const passwordValid = computed(() => password.value.length >= 6 && !passwordError.value)
const isFormValid = computed(() => usernameValid.value && passwordValid.value)

onMounted(() => {
  // 处理注册成功后的提示消息
  if (route.query.registered === 'success') {
    successMsg.value = `账号 ${route.query.username || ''} 注册成功，请登录`
  }
})

const validateUsername = () => {
  if (!username.value) {
    usernameError.value = '请输入用户名'
  } else if (username.value.length < 3) {
    usernameError.value = '用户名至少3个字符'
  } else if (username.value.length > 20) {
    usernameError.value = '用户名最多20个字符'
  } else {
    usernameError.value = ''
  }
}

const validatePassword = () => {
  if (!password.value) {
    passwordError.value = '请输入密码'
  } else if (password.value.length < 6) {
    passwordError.value = '密码至少6个字符'
  } else {
    passwordError.value = ''
  }
}

const clearUsernameError = () => {
  if (usernameError.value && username.value.length >= 3) {
    usernameError.value = ''
  }
}

const clearPasswordError = () => {
  if (passwordError.value && password.value.length >= 6) {
    passwordError.value = ''
  }
}

const handleLogin = async () => {
  validateUsername()
  validatePassword()

  if (!isFormValid.value) return

  loading.value = true
  error.value = ''

  const success = await authStore.handleLogin({
    username: username.value,
    password: password.value
  })

  loading.value = false

  if (success) {
    // 登录成功后跳转到redirect指定的页面或默认首页
    const redirect = route.query.redirect as string
    router.push(redirect || '/dashboard')
  } else {
    error.value = '用户名或密码错误'
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: var(--gradient-bg);
}

.login-card {
  width: 400px;
  padding: 40px;
}

.login-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  margin-bottom: 32px;
}

.logo-icon {
  color: var(--color-primary);
}

.logo-text {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-primary);
}

.login-subtitle {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.login-form {
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

.login-btn {
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

.login-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.login-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.error-msg {
  color: var(--color-error);
  font-size: var(--font-size-sm);
  text-align: center;
}

.success-msg {
  color: var(--color-success);
  font-size: var(--font-size-sm);
  text-align: center;
}

.field-error {
  color: var(--color-error);
  font-size: 12px;
  margin-top: 4px;
}

.form-item.has-error .form-item label {
  color: var(--color-error);
}

.input-error {
  border-color: var(--color-error) !important;
  box-shadow: 0 0 0 3px rgba(220, 53, 69, 0.1) !important;
}

.input-valid {
  border-color: var(--color-success) !important;
  box-shadow: 0 0 0 3px rgba(40, 167, 69, 0.1) !important;
}

.login-footer {
  margin-top: 24px;
  text-align: center;
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.login-footer a {
  color: var(--color-primary);
  margin-left: 8px;
}

/* 响应式适配 */
@media (max-width: 600px) {
  .login-card {
    width: 90%;
    max-width: 400px;
    padding: 24px;
  }

  .logo-icon {
    width: 36px;
    height: 36px;
  }

  .logo-text {
    font-size: 24px;
  }

  .login-form {
    gap: 16px;
  }

  .form-item input {
    padding: 10px 14px;
  }

  .login-btn {
    padding: 12px;
  }
}

@media (max-width: 400px) {
  .login-card {
    padding: 20px;
  }

  .login-header {
    gap: 8px;
    margin-bottom: 24px;
  }
}
</style>