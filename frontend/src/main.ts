import { createApp } from 'vue'
import App from './App.vue'
import { setupRouterGuards } from './router/guards'
import pinia from './stores'

const app = createApp(App)

// Element Plus 已配置按需引入，无需手动注册

// 状态管理
app.use(pinia)

// 路由 + 守卫
setupRouterGuards(app)

// 启动应用
app.mount('#app')