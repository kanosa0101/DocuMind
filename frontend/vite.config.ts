import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

export default defineConfig({
  plugins: [
    vue(),
    // Element Plus 按需引入
    AutoImport({
      resolvers: [ElementPlusResolver()],
    }),
    Components({
      resolvers: [ElementPlusResolver()],
    }),
  ],
  // 定义浏览器缺失的Node.js全局变量
  define: {
    global: 'globalThis',
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:18080',
        changeOrigin: true,
        rewrite: (path) => path,
        configure: (proxy) => {
          proxy.on('error', (err) => {
            console.error('代理错误:', err.message)
          })
          proxy.on('proxyRes', (proxyRes) => {
            console.log('代理响应:', proxyRes.statusCode)
          })
        }
      },
      // WebSocket代理 - v3.0: 转发到file-service
      '/ws': {
        target: 'http://localhost:18082',
        changeOrigin: true,
        ws: true  // 启用WebSocket代理
      }
    },
    cors: true
  },
  // 禁用环境代理（关键修复）
  // Vite会自动检测http_proxy环境变量，需要显式禁用
  preview: {
    proxy: {}
  },
  // 强制禁用系统代理影响（需要在启动时设置环境变量）
  // 实际上需要设置 NO_PROXY=localhost,127.0.0.1
  build: {
    // 代码分割优化
    chunkSizeWarningLimit: 1000,
    rollupOptions: {
      output: {
        // 使用函数形式的 manualChunks 避免 TypeScript 类型错误
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('vue') || id.includes('vue-router') || id.includes('pinia')) {
              return 'vue-vendor'
            }
            if (id.includes('axios')) {
              return 'utils'
            }
            // Element Plus 按需引入会自动处理
          }
        }
      }
    },
    // CSS分离
    cssCodeSplit: true
  },
  // 优化依赖预构建
  optimizeDeps: {
    include: ['vue', 'vue-router', 'pinia', 'axios', 'sockjs-client', 'stompjs']
  }
})