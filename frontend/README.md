# DocuMind 前端项目

企业级文档智能处理系统前端看板，基于 Vue 3 + TypeScript + Vite 构建。

## 快速开始

### Windows 用户

```bash
# 双击运行或在命令行执行
start.bat

# 或选择选项 5（全部执行）
```

### Linux/Mac 用户

```bash
# 添加执行权限
chmod +x start.sh

# 运行脚本
./start.sh
```

### 直接使用 npm

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build

# 预览生产版本
npm run preview
```

## 项目结构

```
src/
├── api/           # API 接口层
│   ├── auth.ts    # 认证接口
│   ├── file.ts    # 文件接口
│   ├── ai.ts      # AI处理接口
│   └── document.ts # 文档接口
│
├── assets/styles/ # 全局样式
│   ├── variables.css # CSS变量
│   └ global.css   # 全局样式
│
├── components/    # 公共组件
│   ├── common/    # 通用组件（玻璃卡片、波纹动画）
│   ├── layout/    # 布局组件（侧边栏、顶部栏）
│   ├── upload/    # 上传组件
│   ├── document/  # 文档组件
│   ├── ai/        # AI组件
│   └── chat/      # 聊天组件
│
├── layouts/       # 页面布局
├── pages/         # 页面组件
├── router/        # 路由配置
├── stores/        # Pinia 状态管理
├── types/         # TypeScript 类型定义
└── utils/         # 工具函数
```

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue 3 | 3.5+ | 响应式框架 |
| TypeScript | 6.0 | 类型安全 |
| Vite | 8.0 | 构建工具 |
| Element Plus | 2.14 | UI组件库 |
| Pinia | 3.0 | 状态管理 |
| Vue Router | 4.6 | 路由管理 |
| Axios | 1.16 | HTTP客户端 |

## 环境要求

- Node.js 18+
- npm 9+

## 配置

### 开发环境

编辑 `.env.development`:

```env
VITE_API_BASE_URL=http://localhost:8080
```

### 生产环境

编辑 `.env`:

```env
VITE_API_BASE_URL=https://your-api-server.com
```

## 页面路由

| 路径 | 页面 | 说明 |
|------|------|------|
| `/login` | Login | 登录页 |
| `/register` | Register | 注册页 |
| `/dashboard` | Dashboard | 工作台首页 |
| `/files` | Files | 文件中心 |
| `/ai` | AiLab | AI实验室 |
| `/documents` | Documents | 文档版本 |
| `/settings` | Settings | 系统设置 |

## 后端依赖

前端需要连接后端 API 服务：

- 默认地址：`http://localhost:8080`
- 确保后端服务已启动
- 后端启动命令：`./deploy.sh`（在后端项目目录）

## 功能模块

### 1. 认证模块
- JWT Token 自动管理
- Token 过期自动刷新
- 登录状态持久化

### 2. 文件模块
- 拖拽上传
- 波纹扫描动画
- 大文件分片上传

### 3. AI模块
- 双栏对比视图（原文 vs AI结果）
- 文档摘要提取
- 关键词标签
- 文档分析统计

### 4. AI Agent对话
- 聊天气泡流
- RAG知识库问答
- 知识来源显示

## 开发指南

### 添加新页面

1. 在 `src/pages/` 创建页面组件
2. 在 `src/router/index.ts` 添加路由
3. 在 `src/components/layout/Sidebar.vue` 添加导航项

### 添加新API

1. 在 `src/types/api.ts` 定义类型
2. 在 `src/api/` 创建API模块
3. 在组件中使用

## 常见问题

### 端口冲突
如果 5173 端口被占用，Vite 会自动切换到下一个可用端口。

### 后端连接失败
检查：
1. 后端服务是否启动
2. `.env.development` 配置是否正确
3. 网络防火墙是否阻止

### Token刷新失败
- 确保 refreshToken 有效（7天有效期）
- 清除浏览器 localStorage 重新登录

## 构建部署

```bash
# 构建
npm run build

# 输出目录: dist/
# 可部署到任何静态服务器
```

## License

MIT