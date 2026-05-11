import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { setupRouterGuards } from './router/guards'
import pinia from './stores'

const app = createApp(App)

// 状态管理
app.use(pinia)

// 路由 + 守卫
setupRouterGuards(app)

// 启动应用
app.mount('#app')