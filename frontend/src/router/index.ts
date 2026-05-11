// 路由配置

import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/pages/auth/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/pages/auth/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/DashboardLayout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/pages/dashboard/index.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'files',
        name: 'Files',
        component: () => import('@/pages/files/index.vue'),
        meta: { title: '文件中心' }
      },
      {
        path: 'ai',
        name: 'AiLab',
        component: () => import('@/pages/ai/index.vue'),
        meta: { title: 'AI实验室' }
      },
      {
        path: 'documents',
        name: 'Documents',
        component: () => import('@/pages/documents/index.vue'),
        meta: { title: '文档版本' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/pages/settings/index.vue'),
        meta: { title: '系统设置' }
      }
    ]
  },
  {
    path: '/404',
    name: '404',
    component: () => import('@/pages/error/404.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router