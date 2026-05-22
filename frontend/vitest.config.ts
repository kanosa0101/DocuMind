import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'happy-dom',  // 轻量级DOM环境，比jsdom更快
    globals: true,              // 全局API（describe、it、expect等）
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html', 'lcov'],
      exclude: [
        'node_modules/',
        'src/types/',           // 类型定义不需要测试
        '**/*.d.ts',
        '**/__tests__/**',      // 测试文件本身不计入覆盖率
        'src/main.ts',          // 入口文件
        'src/router/**'         // 路由配置
      ]
    },
    include: ['src/**/__tests__/**/*.test.ts', 'src/**/*.test.ts']
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  }
})