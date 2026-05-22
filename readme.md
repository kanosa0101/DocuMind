# DocuMind - 企业级文档智能处理系统

## 项目概述

DocuMind 是一个基于 Spring Cloud Alibaba 微服务架构的文档智能处理系统，提供用户管理、文件处理、AI 分析、RAG 知识库、AI Agent 对话等功能模块。系统采用前后端分离架构，前端使用 Vue 3 + TypeScript 实现现代化的玻璃拟态 UI 设计，支持深色模式（Obsidian Dark Edition）。

## 功能亮点

### 核心业务流程
- **一键智能处理**：上传文件 → 自动创建文档 → AI摘要分析 → 知识库索引
- **文件回收站**：软删除机制，支持文件恢复和永久删除
- **文档版本对比**：可视化 diff 算法展示版本差异
- **知识库管理**：从已有文档批量索引，支持 RAG 问答

### 用户安全
- **用户数据隔离**：所有文件/文档操作自动绑定用户ID，防止跨用户访问
- **JWT 认证**：accessToken 30分钟 + refreshToken 24小时
- **Gateway 签名验证**：HMAC-SHA256 签名防止请求伪造

### 用户体验
- **实时表单验证**：登录/注册页面实时验证 + 密码强度指示器
- **Toast 消息提示**：统一错误/成功消息提示，替代原生 alert
- **骨架屏加载**：列表加载时显示骨架屏，提升感知速度
- **响应式设计**：支持桌面端和移动端自适应布局
- **深色模式**：完整的 Obsidian Dark 主题，玻璃拟态设计

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
| Vue | 3.5+ | 前端框架（Composition API） |
| TypeScript | 6.x | 类型支持 |
| Vite | 8.x | 构建工具（按需引入优化） |
| Pinia | 3.x | 状态管理 |
| Element Plus | 2.x | UI 组件库（按需引入） |
| Axios | 1.x | HTTP 客户端（请求缓存） |
| markdown-it | - | Markdown 渲染 |
| DOMPurify | - | XSS 防护 |

### 容器化
- Docker + Docker Compose（单机部署）
- Docker Swarm（集群部署，可选）

## 功能模块

### 1. 用户服务 (user-service)
- 用户注册/登录（实时表单验证）
- JWT Token 认证（accessToken 30分钟，refreshToken 24小时）
- 用户信息管理（真实统计数据展示）
- Token刷新机制（自动刷新 + 过期提示）

### 2. 文件服务 (file-service)
- 文件上传/下载/预览（用户隔离）
- 多文件上传（批量进度显示）
- 大文件分片上传（断点续传支持）
- 文件元数据管理
- MinIO 对象存储集成
- **回收站功能**：软删除、恢复、永久删除
- **文件统计**：真实存储空间计算

### 3. AI 服务 (ai-service)
- 文档摘要生成（进度指示）
- 关键词提取（可视化标签）
- 文本分析（字符统计图表）
- RAG 知识库问答（多来源引用）
- 混合检索（Vector + BM25 + 重排序）
- AI Agent 对话（Markdown 渲染）
- **批量文档索引**：从文档列表一键索引
- **知识库管理**：索引状态、删除索引

> 注：文件操作接口（upload/download/delete）已统一至 file-service，AI服务仅提供纯AI处理功能

### 4. 文档服务 (document-service)
- 文档 CRUD（用户所有权验证）
- 文档版本控制（自动版本号）
- 版本历史查看（时间线展示）
- 版本恢复（一键恢复）
- **版本对比**：可视化 diff 展示差异
- **从文件创建文档**：上传后自动创建
- **真实文档统计**：动态获取数据

### 5. API 网关 (gateway-service)
- 统一路由入口
- JWT 认证过滤
- CORS 跨域配置
- 服务发现（Nacos）
- **请求签名机制**：对转发请求添加HMAC-SHA256签名，内部服务验证防止请求伪造

## 前端页面

| 页面 | 路径 | 功能 |
|------|------|------|
| 登录 | /login | 用户登录（实时验证） |
| 注册 | /register | 用户注册（密码强度指示） |
| 工作台 | /dashboard | 智能上传 → 自动创建文档 → AI分析 |
| 文件中心 | /files | 文件列表/回收站、搜索、创建文档 |
| AI实验室 | /ai | RAG问答、智能搜索、知识库索引管理 |
| 文档版本 | /documents | 文档管理、版本历史、版本对比 |
| 系统设置 | /settings | 用户信息、存储统计、主题切换 |

### 前端优化
- **Element Plus 按需引入**：减少打包体积，仅加载使用的组件
- **API 请求缓存**：统计数据 3 分钟缓存，避免重复请求
- **代码分割**：vue-vendor 独立打包，优化加载性能
- **虚拟滚动**：大列表性能优化（可扩展）
- **XSS 防护**：Markdown 内容使用 DOMPurify 过滤
- **响应式布局**：完整移动端适配（768px/480px 断点）

## 部署说明

### 安全配置（生产环境必读）

**⚠️ 生产环境部署前，必须完成以下安全配置！**

#### 1. 环境变量配置

复制 `.env.docker` 为 `.env` 并配置所有敏感信息：

```bash
cp .env.docker .env
```

编辑 `.env` 文件，生成并设置强密码：

```bash
# MySQL密码（建议16字符以上混合字符）
MYSQL_ROOT_PASSWORD=<生成强密码>
MYSQL_DATABASE=doc_ai

# Redis密码
REDIS_PASSWORD=<生成强密码>

# RabbitMQ密码
RABBITMQ_PASSWORD=<生成强密码>

# MinIO密钥
MINIO_SECRET_KEY=<生成强密码>

# Grafana密码
GRAFANA_PASSWORD=<生成强密码>

# JWT密钥（必须64字符以上，推荐使用openssl生成）
JWT_SECRET=$(openssl rand -base64 64)

# Gateway签名密钥（用于内部服务间请求验证）
GATEWAY_SIGNATURE_SECRET=<32字符以上随机密钥>

# API密钥
OPENAI_API_KEY=<实际API密钥>
DASHSCOPE_API_KEY=<实际API密钥>
```

密码生成示例：
```bash
# Linux/Mac
openssl rand -base64 32  # 生成32字节随机密码

# Windows PowerShell
[Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

#### 2. 安全加固措施

本系统已实现以下安全机制：

| 安全措施 | 说明 |
|---------|------|
| JWT强密钥验证 | 启动时检查密钥长度≥32字节，拒绝默认模式密钥 |
| X-User-Id签名验证 | Gateway使用HMAC-SHA256签名，内部服务验证防伪造 |
| 时间戳防重放 | 签名包含时间戳，5分钟窗口内有效 |
| 刷新令牌限制 | refreshToken有效期24小时（原7天已缩短） |
| 环境变量强制 | Docker Compose使用`${VAR:?message}`语法，未配置则启动失败 |

#### 3. 启动验证

```bash
# 使用环境变量启动
docker-compose --env-file .env up -d

# 检查服务健康状态
docker-compose ps
```

---

### 方式一：单机 Docker Compose（推荐开发环境）

```bash
# 1. 配置环境变量
cp .env.docker .env
vim .env  # 编辑配置

# 2. 构建后端
mvn clean package -DskipTests

# 3. 构建镜像
docker build -t docai/gateway-service:1.0.0 -f gateway-service/Dockerfile .
docker build -t docai/user-service:1.0.0 -f user-service/Dockerfile .
docker build -t docai/file-service:1.0.0 -f file-service/Dockerfile .
docker build -t docai/ai-service:1.0.0 -f ai-service/Dockerfile .
docker build -t docai/document-service:1.0.0 -f document-service/Dockerfile .

# 4. 启动服务
docker-compose --env-file .env -f docker-compose-standalone.yml up -d

# 5. 启动前端
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
- `POST /api/files/upload-multiple` - 多文件上传
- `GET /api/files/download/{fileId}` - 文件下载
- `GET /api/files/preview/{fileId}` - 文件预览
- `GET /api/files/list` - 文件列表（分页）
- `GET /api/files/search` - 文件搜索
- `DELETE /api/files/{fileId}` - 删除文件（移入回收站）
- `GET /api/files/stats` - 用户文件统计
- `GET /api/files/recycle/list` - 回收站列表
- `POST /api/files/recycle/restore/{recycleId}` - 恢复文件
- `DELETE /api/files/recycle/{recycleId}` - 永久删除

### AI API
- `POST /api/ai/summarize` - 文档摘要
- `POST /api/ai/keywords` - 关键词提取
- `POST /api/ai/analyze` - 文本分析
- `POST /api/ai/rag/index` - 知识库索引
- `POST /api/ai/rag/query` - RAG 问答
- `GET /api/ai/rag/search/hybrid` - 混合搜索
- `GET /api/ai/rag/documents` - 已索引文档列表
- `DELETE /api/ai/rag/documents/{documentId}` - 删除索引
- `POST /api/ai/agent/chat` - Agent 对话

### 文档 API
- `POST /api/documents` - 创建文档
- `PUT /api/documents/{id}` - 更新文档（创建新版本）
- `GET /api/documents/{id}` - 获取文档详情
- `GET /api/documents/user/{userId}` - 用户文档列表
- `GET /api/documents/stats` - 用户文档统计
- `DELETE /api/documents/{id}` - 删除文档
- `GET /api/documents/{id}/versions` - 版本历史
- `POST /api/documents/{id}/restore/{version}` - 恢复版本

## 默认账号

**开发环境默认账号（仅限本地开发使用）：**

| 服务 | 用户名 | 默认密码 | 环境变量 |
|------|-------|---------|---------|
| MySQL | root | - | MYSQL_ROOT_PASSWORD |
| Redis | - | - | REDIS_PASSWORD |
| RabbitMQ | admin | - | RABBITMQ_PASSWORD |
| MinIO | admin | - | MINIO_SECRET_KEY |
| Nacos | nacos | nacos | (保持默认) |
| Grafana | admin | - | GRAFANA_PASSWORD |

⚠️ **生产环境部署：**
- 所有密码通过 `.env` 文件配置，无硬编码默认值
- 请使用强密码（16字符以上，包含大小写、数字、特殊字符）
- JWT密钥必须64字符以上
- 系统启动时会验证密钥强度，不符合要求将启动失败

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

4. **服务启动失败 - JWT密钥错误**
   - 错误信息：`JWT密钥长度不足32字节` 或 `请勿使用默认JWT密钥模式`
   - 解决：在 `.env` 中配置足够长度的JWT_SECRET

5. **服务启动失败 - 环境变量未配置**
   - 错误信息：`MySQL密码未设置` 或类似提示
   - 解决：确保 `.env` 文件存在且包含所有必需变量

6. **X-User-Id验证失败**
   - 错误信息：返回401 Unauthorized
   - 原因：Gateway签名密钥(GATEWAY_SIGNATURE_SECRET)配置不一致
   - 解决：确保所有服务使用相同的签名密钥

## 许可证

MIT License

---

## 更新日志

### v1.0.0 (2026-05-13)

**功能完善**
- 实现文件回收站功能（软删除、恢复、永久删除）
- 实现文档版本对比功能（可视化 diff）
- 实现一键智能处理流程（上传→创建文档→AI分析→索引）
- 实现从文档列表批量索引知识库
- 实现真实统计数据展示（文件统计、文档统计）

**用户体验优化**
- 登录/注册页面实时表单验证
- 密码强度可视化指示器
- Toast 消息统一提示系统
- 骨架屏加载状态
- 完整移动端响应式适配
- 深色模式（Obsidian Dark）完善

**性能优化**
- Element Plus 按需引入（减少打包体积）
- API 请求缓存策略（3分钟缓存）
- Vite 代码分割优化
- TypeScript 类型完善

**安全加固**
- 用户数据隔离（所有操作绑定 X-User-Id）
- XSS 防护（Markdown 渲染使用 DOMPurify）
- Gateway 签名验证防请求伪造