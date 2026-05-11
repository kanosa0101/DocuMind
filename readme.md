# DocuMind - 企业级文档智能处理系统

## 项目概述

DocuMind 是一个基于 Spring Cloud Alibaba 微服务架构的文档智能处理系统，提供用户管理、文件处理、AI 分析、RAG 知识库、AI Agent 对话等功能模块。系统采用前后端分离架构，前端使用 Vue 3 + TypeScript 实现现代化的玻璃拟态 UI 设计。

## 快速开始

### Windows 用户
```batch
# 双击运行或命令行执行
start-all.bat
# 选择选项 1 启动全部服务
```

### Linux/Mac 用户
```bash
chmod +x start-all.sh
./start-all.sh
# 选择选项 1 启动全部服务
```

### 访问地址
- 前端应用：http://localhost:5173
- API 网关：http://localhost:9080
- Nacos 控制台：http://localhost:8848/nacos (nacos/nacos)
- MinIO 控制台：http://localhost:9001 (admin/minio123)
- RabbitMQ 控制台：http://localhost:15672 (admin/rabbit123)

## 项目结构

```
docAI/
├── frontend/                 # Vue 3 前端应用
│   ├── src/
│   │   ├── api/              # API 接口模块
│   │   ├── components/       # 公共组件
│   │   ├── layouts/          # 页面布局
│   │   ├── pages/            # 页面组件
│   │   ├── router/           # 路由配置
│   │   ├── stores/           # Pinia 状态管理
│   │   ├── types/            # TypeScript 类型
│   │   └── utils/            # 工具函数
│   └── vite.config.ts
├── common/                   # 公共组件模块
├── gateway-service/          # API 网关 (端口 9080)
├── user-service/             # 用户服务 (端口 9081)
├── file-service/             # 文件服务 (端口 9082)
├── ai-service/               # AI 服务 (端口 9083)
├── document-service/         # 文档服务 (端口 9084)
├── docker-compose-standalone.yml  # Docker Compose 部署配置
├── start-all.bat             # Windows 全项目启动脚本
├── start-all.sh              # Linux/Mac 全项目启动脚本
├── start.bat                 # Windows 后端启动脚本
├── start.sh                  # Linux/Mac 后端启动脚本
├── mysql-schema.sql          # 数据库初始化脚本
└── pom.xml                   # Maven 父项目
```

## 技术栈

### 后端
| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.0 | 核心框架 |
| Spring Cloud | 2023.0.3 | 微服务框架 |
| Spring Cloud Alibaba | 2023.0.1.0 | 服务发现/配置 |
| MyBatis-Plus | 3.5.5 | ORM 框架 |
| MySQL | 8.0 | 关系数据库 |
| Redis | 7.0 | 缓存/向量存储 |
| RabbitMQ | 3.8 | 消息队列 |
| MinIO | latest | 对象存储 |
| Nacos | 2.2.0 | 服务注册/配置中心 |
| JWT | 0.11.5 | 身份认证 |
| Spring AI | - | AI 集成 |

### 前端
| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4+ | 前端框架 |
| TypeScript | 5.x | 类型支持 |
| Vite | 5.x | 构建工具 |
| Pinia | 2.x | 状态管理 |
| Element Plus | - | UI 组件库 |
| Tailwind CSS | 4.x | 样式框架 |
| Axios | - | HTTP 客户端 |

### 容器化
- Docker + Docker Compose（单机部署）
- Docker Swarm（集群部署，可选）

## 功能模块

### 1. 用户服务 (user-service)
- 用户注册/登录
- JWT Token 认证（accessToken 30分钟，refreshToken 7天）
- 用户信息管理

### 2. 文件服务 (file-service)
- 文件上传/下载/预览
- 多文件上传
- 大文件分片上传
- 文件元数据管理
- MinIO 对象存储集成

### 3. AI 服务 (ai-service)
- 文档摘要生成
- 关键词提取
- 文本分析（字符统计）
- RAG 知识库问答
- 混合检索（Vector + BM25）
- AI Agent 对话

### 4. 文档服务 (document-service)
- 文档 CRUD
- 文档版本控制
- 版本历史查看
- 版本恢复

### 5. API 网关 (gateway-service)
- 统一路由入口
- JWT 认证过滤
- CORS 跨域配置
- 服务发现（Nacos）

## 前端页面

| 页面 | 路径 | 功能 |
|------|------|------|
| 登录 | /login | 用户登录 |
| 注册 | /register | 用户注册 |
| 工作台 | /dashboard | 拖拽上传、AI处理、最近文档 |
| 文件中心 | /files | 文件列表、搜索、上传下载 |
| AI实验室 | /ai | RAG问答、智能搜索、知识库管理 |
| 文档版本 | /documents | 文档管理、版本历史 |
| 系统设置 | /settings | 用户信息、退出登录 |

## 部署说明

### 方式一：单机 Docker Compose（推荐开发环境）

```bash
# 1. 构建后端
mvn clean package -DskipTests

# 2. 构建镜像
docker build -t docai/gateway-service:1.0.0 -f gateway-service/Dockerfile .
docker build -t docai/user-service:1.0.0 -f user-service/Dockerfile .
docker build -t docai/file-service:1.0.0 -f file-service/Dockerfile .
docker build -t docai/ai-service:1.0.0 -f ai-service/Dockerfile .
docker build -t docai/document-service:1.0.0 -f document-service/Dockerfile .

# 3. 启动服务
docker-compose -f docker-compose-standalone.yml up -d

# 4. 启动前端
cd frontend && npm install && npm run dev
```

### 方式二：使用启动脚本

Windows:
```batch
start-all.bat
# 选择 4) 全部执行
```

Linux/Mac:
```bash
./start-all.sh
# 选择 4) 构建全部项目
# 选择 1) 启动全部服务
```

### 端口说明

由于 Windows Hyper-V 占用 8080-8084 端口，网关和微服务使用 9080-9084：

| 服务 | 内部端口 | 外部端口 |
|------|---------|---------|
| Gateway | 8080 | 9080 |
| User | 8081 | 9081 |
| File | 8082 | 9082 |
| AI | 8083 | 9083 |
| Document | 8084 | 9084 |
| MySQL | 3306 | 13306 |
| Redis | 6379 | 6379 |
| Nacos | 8848 | 8848 |

## API 接口

### 认证 API
- `POST /api/users/login` - 登录
- `POST /api/users/register` - 注册
- `POST /api/users/refresh` - 刷新 Token
- `POST /api/users/logout` - 登出

### 文件 API
- `POST /api/files/upload` - 文件上传
- `GET /api/files/download/{fileId}` - 文件下载
- `GET /api/files/list` - 文件列表
- `DELETE /api/files/{fileId}` - 删除文件

### AI API
- `POST /api/ai/summarize` - 文档摘要
- `POST /api/ai/keywords` - 关键词提取
- `POST /api/ai/analyze` - 文本分析
- `POST /api/ai/rag/query` - RAG 问答
- `GET /api/ai/rag/search/hybrid` - 混合搜索
- `POST /api/ai/agent/chat` - Agent 对话

### 文档 API
- `POST /api/documents` - 创建文档
- `PUT /api/documents/{id}` - 更新文档
- `GET /api/documents/{id}` - 获取文档
- `GET /api/documents/{id}/versions` - 版本历史
- `POST /api/documents/{id}/restore/{version}` - 恢复版本

## 默认账号

| 服务 | 用户名 | 密码 |
|------|-------|------|
| MySQL | root | admin123 |
| Redis | - | redis123 |
| RabbitMQ | admin | rabbit123 |
| MinIO | admin | minio123 |
| Nacos | nacos | nacos |
| 系统管理员 | admin | admin123 |

⚠️ **生产环境请务必修改默认密码！**

## 开发指南

### 后端开发
```bash
# 启动单个服务（需要基础设施先启动）
cd user-service
mvn spring-boot:run
```

### 前端开发
```bash
cd frontend
npm install
npm run dev     # 开发模式
npm run build   # 生产构建
```

### 常见问题

1. **端口冲突**
   - Windows Hyper-V 可能占用 8080-8084
   - 使用 9080-9084 或禁用 Hyper-V 端口保留

2. **Nacos 连接失败**
   - 确保 Nacos 先启动并健康
   - 检查 healthcheck 状态

3. **前端 Network Error**
   - 检查 `.env` 文件中 API URL 配置
   - 确保使用相对路径走 Vite 代理

## 许可证

MIT License