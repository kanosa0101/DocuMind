<template>
  <div class="register-page">
    <div class="register-card glass-card">
      <!-- Logo -->
      <div class="register-header">
        <FileText class="logo-icon" :size="48" />
        <h1 class="logo-text">DocuMind</h1>
        <p class="register-subtitle">创建您的账号</p>
      </div>

      <!-- 注册表单 -->
      <form class="register-form" @submit.prevent="handleRegister">
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

        <div class="form-item" :class="{ 'has-error': emailError }">
          <label>邮箱</label>
          <input
            v-model="email"
            type="email"
            placeholder="请输入邮箱"
            :class="{ 'input-error': emailError, 'input-valid': emailValid }"
            @blur="validateEmail"
            @input="clearEmailError"
          />
          <span v-if="emailError" class="field-error">{{ emailError }}</span>
        </div>

        <div class="form-item">
          <label>手机号（可选）</label>
          <input
            v-model="phone"
            type="tel"
            placeholder="请输入手机号"
            :class="{ 'input-valid': phoneValid }"
            @blur="validatePhone"
          />
          <span v-if="phoneError" class="field-error">{{ phoneError }}</span>
        </div>

        <div class="form-item" :class="{ 'has-error': passwordError }">
          <label>密码</label>
          <input
            v-model="password"
            type="password"
            placeholder="请输入密码（至少6位）"
            :class="{ 'input-error': passwordError, 'input-valid': passwordValid }"
            @blur="validatePassword"
            @input="onPasswordInput"
          />
          <span v-if="passwordError" class="field-error">{{ passwordError }}</span>
          <!-- 密码强度指示器 -->
          <div v-if="password.length > 0 && !passwordError" class="password-strength">
            <div class="strength-bar">
              <div class="strength-fill" :style="{ width: strengthPercent + '%' }" :class="strengthClass"></div>
            </div>
            <span class="strength-text" :class="strengthClass">{{ strengthText }}</span>
          </div>
        </div>

        <div class="form-item" :class="{ 'has-error': confirmPasswordError }">
          <label>确认密码</label>
          <input
            v-model="confirmPassword"
            type="password"
            placeholder="请再次输入密码"
            :class="{ 'input-error': confirmPasswordError, 'input-valid': confirmPasswordValid }"
            @blur="validateConfirmPassword"
            @input="clearConfirmPasswordError"
          />
          <span v-if="confirmPasswordError" class="field-error">{{ confirmPasswordError }}</span>
        </div>

        <button type="submit" class="register-btn" :disabled="loading || !isFormValid">
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
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/auth'
import { FileText } from '@lucide/vue'

const router = useRouter()

const username = ref('')
const email = ref('')
const phone = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)
const error = ref('')

// 实时验证状态
const usernameError = ref('')
const emailError = ref('')
const phoneError = ref('')
const passwordError = ref('')
const confirmPasswordError = ref('')

// 密码强度计算
const strengthPercent = computed(() => {
  if (!password.value) return 0
  let score = 0
  if (password.value.length >= 6) score += 20
  if (password.value.length >= 10) score += 20
  if (/[a-z]/.test(password.value) && /[A-Z]/.test(password.value)) score += 20
  if (/\d/.test(password.value)) score += 20
  if (/[^a-zA-Z0-9]/.test(password.value)) score += 20
  return score
})

const strengthClass = computed(() => {
  if (strengthPercent.value < 40) return 'weak'
  if (strengthPercent.value < 80) return 'medium'
  return 'strong'
})

const strengthText = computed(() => {
  if (strengthPercent.value < 40) return '弱'
  if (strengthPercent.value < 80) return '中等'
  return '强'
})

// 字段验证状态
const usernameValid = computed(() => username.value.length >= 3 && username.value.length <= 20 && !usernameError.value)
const emailValid = computed(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value) && !emailError.value)
const phoneValid = computed(() => !phone.value || /^1[3-9]\d{9}$/.test(phone.value))
const passwordValid = computed(() => password.value.length >= 6 && !passwordError.value)
const confirmPasswordValid = computed(() => confirmPassword.value === password.value && confirmPassword.value.length > 0 && !confirmPasswordError.value)

const isFormValid = computed(() => usernameValid.value && emailValid.value && passwordValid.value && confirmPasswordValid.value)

const validateUsername = () => {
  if (!username.value) {
    usernameError.value = '请输入用户名'
  } else if (username.value.length < 3) {
    usernameError.value = '用户名至少3个字符'
  } else if (username.value.length > 20) {
    usernameError.value = '用户名最多20个字符'
  } else if (!/^[a-zA-Z0-9_一-龥]+$/.test(username.value)) {
    usernameError.value = '用户名只能包含字母、数字、下划线或中文'
  } else {
    usernameError.value = ''
  }
}

const validateEmail = () => {
  if (!email.value) {
    emailError.value = '请输入邮箱'
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) {
    emailError.value = '邮箱格式不正确'
  } else {
    emailError.value = ''
  }
}

const validatePhone = () => {
  if (phone.value && !/^1[3-9]\d{9}$/.test(phone.value)) {
    phoneError.value = '手机号格式不正确'
  } else {
    phoneError.value = ''
  }
}

const validatePassword = () => {
  if (!password.value) {
    passwordError.value = '请输入密码'
  } else if (password.value.length < 6) {
    passwordError.value = '密码至少6个字符'
  } else if (password.value.length > 50) {
    passwordError.value = '密码最多50个字符'
  } else {
    passwordError.value = ''
  }
  // 同时验证确认密码
  if (confirmPassword.value) {
    validateConfirmPassword()
  }
}

const validateConfirmPassword = () => {
  if (!confirmPassword.value) {
    confirmPasswordError.value = '请确认密码'
  } else if (confirmPassword.value !== password.value) {
    confirmPasswordError.value = '两次密码不一致'
  } else {
    confirmPasswordError.value = ''
  }
}

const clearUsernameError = () => {
  if (usernameError.value && username.value.length >= 3 && username.value.length <= 20) {
    validateUsername()
  }
}

const clearEmailError = () => {
  if (emailError.value && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.value)) {
    emailError.value = ''
  }
}

const clearPasswordError = () => {
  if (passwordError.value && password.value.length >= 6) {
    passwordError.value = ''
  }
}

const clearConfirmPasswordError = () => {
  if (confirmPasswordError.value && confirmPassword.value === password.value) {
    confirmPasswordError.value = ''
  }
}

const onPasswordInput = () => {
  clearPasswordError()
  if (confirmPassword.value) {
    clearConfirmPasswordError()
  }
}

const handleRegister = async () => {
  validateUsername()
  validateEmail()
  validatePhone()
  validatePassword()
  validateConfirmPassword()

  if (!isFormValid.value) return

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
  color: var(--color-primary);
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

/* 密码强度指示器 */
.password-strength {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}

.strength-bar {
  width: 100px;
  height: 4px;
  background: var(--glass-border);
  border-radius: 2px;
  overflow: hidden;
}

.strength-fill {
  height: 100%;
  transition: width 0.3s ease, background-color 0.3s ease;
}

.strength-fill.weak {
  background: var(--color-error);
}

.strength-fill.medium {
  background: var(--color-warning);
}

.strength-fill.strong {
  background: var(--color-success);
}

.strength-text {
  font-size: 12px;
  font-weight: 500;
}

.strength-text.weak {
  color: var(--color-error);
}

.strength-text.medium {
  color: var(--color-warning);
}

.strength-text.strong {
  color: var(--color-success);
}

/* 响应式适配 */
@media (max-width: 600px) {
  .register-card {
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

  .register-form {
    gap: 16px;
  }

  .form-item input {
    padding: 10px 14px;
  }

  .register-btn {
    padding: 12px;
  }
}

@media (max-width: 400px) {
  .register-card {
    padding: 20px;
  }

  .register-header {
    gap: 8px;
    margin-bottom: 24px;
  }

  .password-strength {
    flex-wrap: wrap;
  }
}
</style>