<template>
  <div class="settings-page">
    <!-- 用户信息 -->
    <div class="user-section glass-card">
      <h3 class="section-header">
        <User class="section-icon" :size="20" />
        用户信息
      </h3>

      <div class="user-profile">
        <div class="avatar-section">
          <div class="avatar glow-text-aurora">{{ userInitial }}</div>
          <button class="change-avatar-btn">更换头像</button>
        </div>

        <div class="info-section">
          <div class="info-item">
            <label>用户名</label>
            <div class="info-value">
              <span>{{ user?.username || '-' }}</span>
            </div>
          </div>

          <div class="info-item">
            <label>邮箱</label>
            <div class="info-value">
              <span>{{ user?.email || '-' }}</span>
            </div>
          </div>

          <div class="info-item">
            <label>手机号</label>
            <div class="info-value">
              <span>{{ user?.phone || '未绑定' }}</span>
            </div>
          </div>

          <div class="info-item">
            <label>角色</label>
            <div class="info-value">
              <span class="role-tag">{{ user?.role || 'USER' }}</span>
            </div>
          </div>

          <div class="info-item">
            <label>注册时间</label>
            <div class="info-value">
              <span>{{ formatDate(user?.createTime) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 账户安全 -->
    <div class="security-section glass-card">
      <h3 class="section-header">
        <Shield class="section-icon" :size="20" />
        账户安全
      </h3>

      <div class="security-actions">
        <div class="action-item">
          <div class="action-info">
            <KeyRound class="action-icon" :size="20" />
            <span class="action-label">修改密码</span>
          </div>
          <button class="action-btn">修改</button>
        </div>

        <div class="action-item">
          <div class="action-info">
            <Mail class="action-icon" :size="20" />
            <span class="action-label">绑定邮箱</span>
          </div>
          <button class="action-btn">{{ user?.email ? '更换' : '绑定' }}</button>
        </div>

        <div class="action-item">
          <div class="action-info">
            <Smartphone class="action-icon" :size="20" />
            <span class="action-label">绑定手机</span>
          </div>
          <button class="action-btn">{{ user?.phone ? '更换' : '绑定' }}</button>
        </div>
      </div>
    </div>

    <!-- 存储空间 -->
    <div class="storage-section glass-card">
      <h3 class="section-header">
        <HardDrive class="section-icon" :size="20" />
        存储空间
      </h3>

      <div class="storage-info">
        <div class="storage-stats">
          <div class="storage-used">
            <span class="used-label">已使用</span>
            <span class="used-value glow-text-cyan">{{ storageInfo.used }} MB</span>
          </div>
          <div class="storage-total">
            <span class="total-label">总容量</span>
            <span class="total-value">{{ storageInfo.total }} MB</span>
          </div>
        </div>

        <div class="storage-bar">
          <div class="storage-fill" :style="{ width: storageInfo.percentage + '%' }"></div>
        </div>

        <div class="storage-percentage">{{ storageInfo.percentage }}%</div>
      </div>
    </div>

    <!-- 系统设置 -->
    <div class="system-section glass-card">
      <h3 class="section-header">
        <Settings class="section-icon" :size="20" />
        系统设置
      </h3>

      <div class="system-options">
        <div class="option-item">
          <span class="option-label">界面主题</span>
          <select v-model="themeMode" @change="handleThemeChange">
            <option value="light">浅色模式</option>
            <option value="dark">暗色模式 (Obsidian)</option>
          </select>
        </div>

        <div class="option-item">
          <span class="option-label">侧边栏</span>
          <button class="toggle-btn" @click="handleSidebarToggle">
            {{ layoutStore.sidebarCollapsed ? '展开' : '收起' }}
          </button>
        </div>

        <div class="option-item">
          <span class="option-label">AI面板</span>
          <button class="toggle-btn" @click="handleCopilotToggle">
            {{ layoutStore.copilotCollapsed ? '展开' : '收起' }}
          </button>
        </div>

        <div class="option-item">
          <span class="option-label">语言</span>
          <select v-model="language">
            <option value="zh-CN">简体中文</option>
            <option value="en-US">English</option>
          </select>
        </div>

        <div class="option-item">
          <label class="checkbox">
            <input type="checkbox" v-model="autoSave" />
            <span>自动保存文档</span>
          </label>
        </div>

        <div class="option-item">
          <label class="checkbox">
            <input type="checkbox" v-model="notifications" />
            <span>启用通知</span>
          </label>
        </div>
      </div>
    </div>

    <!-- 退出登录 -->
    <div class="logout-section glass-card">
      <button class="logout-btn" @click="handleLogout">
        <LogOut :size="20" />
        退出登录
      </button>
    </div>

    <!-- 关于 -->
    <div class="about-section glass-card">
      <h3 class="section-header">
        <Info class="section-icon" :size="20" />
        关于
      </h3>
      <div class="about-content">
        <p><strong class="glow-text-aurora">DocuMind</strong> - 智能文档处理系统</p>
        <p>版本: 1.0.0 (Obsidian Dark Edition)</p>
        <p>基于 Spring Cloud Alibaba + Vue 3 构建</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useThemeStore } from '@/stores/theme'
import { useLayoutStore } from '@/stores/layout'
import { getFileStats } from '@/api/file'
import { User, Shield, KeyRound, Mail, Smartphone, HardDrive, Settings, LogOut, Info } from '@lucide/vue'

const router = useRouter()
const authStore = useAuthStore()
const themeStore = useThemeStore()
const layoutStore = useLayoutStore()

const user = computed(() => authStore.user)
const userInitial = computed(() => user.value?.username?.charAt(0).toUpperCase() || 'U')

// 存储信息（动态获取）
const storageInfo = ref({
  used: 0,
  total: 1024,
  percentage: 0
})

// 系统设置
const themeMode = ref(themeStore.mode)
const language = ref('zh-CN')
const autoSave = ref(true)
const notifications = ref(true)

// 加载存储信息（真实数据）
async function loadStorageInfo() {
  if (!authStore.user) return

  try {
    const stats = await getFileStats()
    const usedMB = stats.totalSizeMB || 0

    storageInfo.value.used = usedMB
    storageInfo.value.percentage = Math.round((usedMB / storageInfo.value.total) * 100)
  } catch (error) {
    console.error('加载存储信息失败:', error)
  }
}

onMounted(async () => {
  await loadStorageInfo()
})

// 监听主题变化并应用到store
function handleThemeChange() {
  themeStore.setTheme(themeMode.value)
}

// 切换侧边栏
function handleSidebarToggle() {
  layoutStore.toggleSidebar()
}

// 切换Copilot面板
function handleCopilotToggle() {
  layoutStore.toggleCopilot()
}

// 退出登录
async function handleLogout() {
  await authStore.handleLogout()
  router.push('/login')
}

// 格式化日期
function formatDate(dateStr?: string): string {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}
</script>

<style scoped>
.settings-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.user-section, .security-section, .storage-section, .system-section, .logout-section, .about-section {
  padding: 24px;
}

[data-theme="dark"] .user-section,
[data-theme="dark"] .security-section,
[data-theme="dark"] .storage-section,
[data-theme="dark"] .system-section,
[data-theme="dark"] .logout-section,
[data-theme="dark"] .about-section {
  background: var(--glass-dark-bg);
  border-color: var(--glass-dark-border);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  color: var(--color-text);
}

.section-icon {
  color: var(--aurora-cyan);
}

[data-theme="dark"] .section-icon {
  color: var(--aurora-cyan-light);
}

.user-profile {
  display: flex;
  gap: 32px;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.avatar {
  width: 100px;
  height: 100px;
  border-radius: var(--radius-full);
  background: var(--gradient-aurora);
  color: white;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 36px;
  font-weight: 600;
}

[data-theme="dark"] .avatar {
  box-shadow: var(--glow-aurora);
}

.change-avatar-btn {
  padding: 8px 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--glass-border);
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  font-size: var(--font-size-sm);
  transition: all var(--transition-base);
}

[data-theme="dark"] .change-avatar-btn {
  background: rgba(26, 26, 26, 0.4);
  border-color: var(--glass-dark-border);
}

.change-avatar-btn:hover {
  background: rgba(8, 145, 178, 0.2);
  border-color: var(--aurora-cyan);
}

.info-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.info-item label {
  width: 100px;
  color: var(--color-text-muted);
  font-size: var(--font-size-sm);
}

.info-value {
  flex: 1;
}

.role-tag {
  padding: 4px 12px;
  background: var(--gradient-aurora);
  color: white;
  border-radius: var(--radius-full);
  font-size: var(--font-size-sm);
}

.security-actions {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.action-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: var(--radius-md);
}

[data-theme="dark"] .action-item {
  background: rgba(26, 26, 26, 0.4);
}

.action-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.action-icon {
  color: var(--aurora-cyan);
}

[data-theme="dark"] .action-icon {
  color: var(--aurora-cyan-light);
}

.action-label {
  font-weight: 500;
}

.action-btn {
  padding: 8px 16px;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  background: var(--gradient-aurora);
  color: white;
  transition: all var(--transition-base);
}

[data-theme="dark"] .action-btn {
  box-shadow: var(--glow-cyan-soft);
}

.action-btn:hover {
  transform: scale(1.05);
}

.storage-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.storage-stats {
  display: flex;
  justify-content: space-between;
}

.storage-used, .storage-total {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.used-label, .total-label {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.used-value {
  font-size: var(--font-size-lg);
  font-weight: 600;
}

.total-value {
  font-size: var(--font-size-lg);
  font-weight: 600;
}

.storage-bar {
  height: 12px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: var(--radius-full);
}

[data-theme="dark"] .storage-bar {
  background: rgba(26, 26, 26, 0.4);
}

.storage-fill {
  height: 100%;
  background: var(--gradient-aurora);
  border-radius: var(--radius-full);
  transition: width var(--transition-base);
}

.storage-percentage {
  text-align: center;
  font-weight: 600;
  color: var(--aurora-emerald);
}

[data-theme="dark"] .storage-percentage {
  text-shadow: var(--glow-text-emerald);
}

.system-options {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.option-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.option-label {
  font-weight: 500;
}

.option-item select {
  padding: 8px 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--glass-border);
  background: rgba(255, 255, 255, 0.5);
  outline: none;
  transition: all var(--transition-base);
}

[data-theme="dark"] .option-item select {
  background: rgba(26, 26, 26, 0.4);
  border-color: var(--glass-dark-border);
  color: var(--color-text);
}

.option-item select:focus {
  border-color: var(--aurora-cyan);
}

.toggle-btn {
  padding: 8px 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--glass-border);
  background: rgba(255, 255, 255, 0.5);
  cursor: pointer;
  transition: all var(--transition-base);
}

[data-theme="dark"] .toggle-btn {
  background: rgba(8, 145, 178, 0.2);
  border-color: var(--glass-dark-border);
}

.toggle-btn:hover {
  background: rgba(8, 145, 178, 0.3);
  border-color: var(--aurora-cyan);
}

.checkbox {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.logout-section {
  display: flex;
  justify-content: center;
}

.logout-btn {
  padding: 16px 48px;
  border-radius: var(--radius-md);
  border: none;
  cursor: pointer;
  background: rgba(239, 68, 68, 0.1);
  color: var(--color-error);
  font-size: var(--font-size-lg);
  display: flex;
  align-items: center;
  gap: 12px;
  transition: all var(--transition-base);
}

[data-theme="dark"] .logout-btn {
  background: rgba(239, 68, 68, 0.15);
}

.logout-btn:hover {
  background: var(--color-error);
  color: white;
}

[data-theme="dark"] .logout-btn:hover {
  box-shadow: 0 0 20px rgba(239, 68, 68, 0.4);
}

.about-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: var(--color-text-muted);
}

.about-content p {
  font-size: var(--font-size-sm);
}

[data-theme="dark"] .about-content p {
  color: var(--color-text-muted);
}

/* 响应式适配 */
@media (max-width: 768px) {
  .user-profile {
    flex-direction: column;
    gap: 24px;
  }

  .avatar-section {
    width: 100%;
  }

  .info-item {
    flex-wrap: wrap;
    gap: 8px;
  }

  .info-item label {
    width: 80px;
  }

  .info-value {
    flex: 1;
    min-width: 0;
  }

  .action-item {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }

  .action-btn {
    width: 100%;
    text-align: center;
  }

  .storage-stats {
    flex-direction: column;
    gap: 16px;
  }

  .option-item {
    flex-wrap: wrap;
    gap: 12px;
  }

  .option-item select,
  .toggle-btn {
    min-width: 120px;
  }

  .logout-btn {
    width: 100%;
    justify-content: center;
  }
}

@media (max-width: 480px) {
  .user-section, .security-section, .storage-section, .system-section, .logout-section, .about-section {
    padding: 16px;
  }

  .avatar {
    width: 80px;
    height: 80px;
    font-size: 28px;
  }

  .info-section {
    gap: 12px;
  }

  .action-item {
    padding: 12px;
  }

  .system-options {
    gap: 12px;
  }

  .option-item {
    flex-direction: column;
    align-items: flex-start;
  }

  .option-item select,
  .toggle-btn,
  .checkbox {
    width: 100%;
  }
}
</style>