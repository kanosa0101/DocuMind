# DocuMind 部署配置说明

## 默认账号密码（仅用于开发测试）

### 数据库服务

| 服务 | 用户名 | 密码 | 端口 |
|-----|-------|------|------|
| MySQL | root | admin123 | 3306 |
| Redis | - | redis123 | 6379 |
| RabbitMQ | admin | rabbit123 | 5672/15672 |
| MinIO | admin | minio123 | 9000/9001 |

### 注册中心

| 服务 | 用户名 | 密码 | 端口 |
|-----|-------|------|------|
| Nacos | nacos | nacos | 8848 |

### 监控服务

| 服务 | 用户名 | 密码 | 端口 |
|-----|-------|------|------|
| Grafana | admin | admin | 3000 |

### 业务系统

| 角色 | 用户名 | 密码 |
|-----|-------|------|
| 管理员 | admin | admin123 |
| 普通用户 | user | user123 |

## 服务访问地址

| 服务 | 地址 |
|-----|------|
| Nacos 控制台 | http://localhost:8848/nacos |
| API 网关 | http://localhost:8080 |
| 用户服务 Swagger | http://localhost:8081/swagger-ui.html |
| 文件服务 Swagger | http://localhost:8082/swagger-ui.html |
| AI 服务 Swagger | http://localhost:8083/swagger-ui.html |
| 文档服务 Swagger | http://localhost:8084/swagger-ui.html |
| MinIO 控制台 | http://localhost:9001 |
| RabbitMQ 控制台 | http://localhost:15672 |
| Grafana 监控 | http://localhost:3000 |

## 部署命令

```bash
# 一键部署
./deploy.sh

# 或手动部署
# 1. 构建项目
mvn clean package -DskipTests

# 2. 构建镜像
docker build -t docai/user-service:1.0 ./user-service
docker build -t docai/file-service:1.0 ./file-service
docker build -t docai/ai-service:1.0 ./ai-service
docker build -t docai/document-service:1.0 ./document-service
docker build -t docai/gateway-service:1.0 ./gateway-service

# 3. 初始化 Swarm
docker swarm init

# 4. 创建网络
docker network create --driver overlay docai-network

# 5. 部署基础设施
docker stack deploy -c docker-compose-infra.yml docai-infra

# 6. 等待基础设施启动后部署微服务
docker stack deploy -c docker-compose-services.yml docai-services
```

## 生产环境注意事项

⚠️ **生产环境请务必修改默认密码！**

1. 修改 `docker-compose-infra.yml` 和 `docker-compose-services.yml` 中所有密码
2. 启用 Nacos 认证：`NACOS_AUTH_ENABLE: "true"`
3. 使用 Docker Secrets 管理敏感信息
4. 启用 HTTPS 和防火墙规则
5. 定期备份数据库和 MinIO 数据