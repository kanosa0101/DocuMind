#!/bin/bash

# ========================================
# DocuMind 文档智能处理系统 - 一键部署脚本
# ========================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目配置
PROJECT_NAME="documind"
VERSION="1.0.0"
MANAGER_NODE="manager"

# 服务列表
SERVICES=("user-service" "file-service" "ai-service" "document-service" "gateway-service")

# 打印带颜色的消息
print_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 打印分隔线
print_separator() { echo -e "${BLUE}========================================${NC}"; }

# 检查命令是否存在
check_command() {
    if ! command -v $1 &> /dev/null; then
        print_error "$1 未安装，请先安装 $1"
        exit 1
    fi
}

# 等待服务就绪
wait_for_service() {
    local service_name=$1
    local max_wait=60
    local count=0
    print_info "等待 $service_name 启动..."
    while [ $count -lt $max_wait ]; do
        if docker service ps ${service_name} 2>/dev/null | grep -q "Running"; then
            print_success "$service_name 已就绪"
            return 0
        fi
        sleep 5
        count=$((count + 5))
        echo -n "."
    done
    echo ""
    print_warning "$service_name 启动超时，请检查日志"
    return 1
}

# ========================================
# 开始部署
# ========================================

print_separator
print_info "DocuMind 文档智能处理系统 - 一键部署"
print_separator

# 1. 检查环境
print_info "步骤 1/8: 检查环境依赖..."
check_command "mvn"
check_command "docker"

print_success "环境检查完成"
echo ""

# 2. Maven 构建
print_info "步骤 2/8: Maven 构建项目..."
print_info "正在打包所有模块..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    print_error "Maven 构建失败"
    exit 1
fi
print_success "Maven 构建完成"
echo ""

# 3. 构建 Docker 镜像
print_info "步骤 3/8: 构建 Docker 镜像..."
for service in "${SERVICES[@]}"; do
    print_info "构建镜像: ${PROJECT_NAME}/${service}:${VERSION}"
    docker build -t ${PROJECT_NAME}/${service}:${VERSION} -f ./${service}/Dockerfile .
    if [ $? -ne 0 ]; then
        print_error "构建 ${service} 镜像失败"
        exit 1
    fi
done
print_success "所有镜像构建完成"
echo ""

# 4. 初始化 Docker Swarm
print_info "步骤 4/8: 初始化 Docker Swarm..."
if docker node ls 2>/dev/null | grep -q "Leader"; then
    print_info "Swarm 集群已存在，跳过初始化"
else
    docker swarm init
    print_success "Swarm 集群初始化完成"

    # 显示加入命令
    print_warning "其他节点加入集群命令:"
    docker swarm join-token worker | grep "docker swarm join"
fi
echo ""

# 5. 创建网络和数据卷
print_info "步骤 5/8: 创建网络和数据卷..."

# 创建 overlay 网络
if docker network ls | grep -q "documind-network"; then
    print_info "网络 documind-network 已存在"
else
    docker network create --driver overlay documind-network
    print_success "网络创建完成"
fi

# 创建数据卷
VOLUMES=("mysql-data" "redis-data" "minio-data" "nacos-data" "grafana-data" "prometheus-data")
for vol in "${VOLUMES[@]}"; do
    if docker volume ls | grep -q "${vol}"; then
        print_info "数据卷 ${vol} 已存在"
    else
        docker volume create ${vol}
        print_success "数据卷 ${vol} 创建完成"
    fi
done
echo ""

# 6. 部署基础设施
print_info "步骤 6/8: 部署基础设施..."

# 节点检测（简化，避免模板解析问题）
print_info "检测 Swarm 状态..."
if docker node ls 2>/dev/null | grep -q "Leader"; then
    print_info "Swarm 集群正常"
fi

docker stack deploy -c docker-compose-infra.yml documind-infra

print_info "等待基础设施服务启动..."
sleep 30

# 等待 MySQL 就绪
wait_for_service "documind-infra_mysql" || true

# 等待 Nacos 就绪
print_info "等待 Nacos 启动（可能需要较长时间）..."
sleep 60

print_success "基础设施部署完成"
echo ""

# 7. 部署微服务
print_info "步骤 7/8: 部署微服务..."

# 更新 docker-compose-services.yml 中的镜像名称
print_info "更新服务镜像配置..."
for service in "${SERVICES[@]}"; do
    # 获取服务名简短形式
    short_name=$(echo $service | sed 's/-service//')
    sed -i "s|your_${short_name}_service_image:tag|${PROJECT_NAME}/${service}:${VERSION}|g" docker-compose-services.yml
done

# 更新其他占位符（如果用户设置了环境变量则使用，否则保持默认）
if [ -n "$MYSQL_ROOT_PASSWORD" ]; then
    sed -i "s|your_mysql_root_password|$MYSQL_ROOT_PASSWORD|g" docker-compose-infra.yml
    sed -i "s|your_mysql_password|$MYSQL_ROOT_PASSWORD|g" docker-compose-services.yml
fi

if [ -n "$REDIS_PASSWORD" ]; then
    sed -i "s|your_redis_password|$REDIS_PASSWORD|g" docker-compose-infra.yml
    sed -i "s|your_redis_password|$REDIS_PASSWORD|g" docker-compose-services.yml
fi

docker stack deploy -c docker-compose-services.yml documind-services

print_info "等待微服务启动..."
sleep 30

print_success "微服务部署完成"
echo ""

# 8. 验证部署
print_info "步骤 8/8: 验证部署状态..."
echo ""

print_separator
print_info "服务状态:"
print_separator
docker service ls

echo ""
print_separator
print_info "部署完成! 服务访问地址:"
print_separator

# 获取 Manager IP
MANAGER_IP=$(docker node inspect self --format '{{.Status.Addr}}' 2>/dev/null | cut -d'/' -f2)
if [ -z "$MANAGER_IP" ] || [ "$MANAGER_IP" = "" ]; then
    MANAGER_IP="localhost"
fi

echo ""
echo "| 服务名称           | 访问地址                                    |"
echo "|--------------------|---------------------------------------------|"
echo "| Nacos 控制台       | http://${MANAGER_IP}:18848/nacos            |"
echo "| API 网关           | http://${MANAGER_IP}:18080                  |"
echo "| 用户服务 Swagger   | http://${MANAGER_IP}:18081/swagger-ui.html  |"
echo "| 文件服务 Swagger   | http://${MANAGER_IP}:18082/swagger-ui.html  |"
echo "| AI 服务 Swagger    | http://${MANAGER_IP}:18083/swagger-ui.html  |"
echo "| 文档服务 Swagger   | http://${MANAGER_IP}:18084/swagger-ui.html  |"
echo "| MinIO 控制台       | http://${MANAGER_IP}:19001                  |"
echo "| RabbitMQ 控制台    | http://${MANAGER_IP}:29672                  |"
echo "| Grafana 监控       | http://${MANAGER_IP}:3000                   |"

echo ""
print_warning "注意事项:"
echo "  1. 请修改 docker-compose-*.yml 中的默认密码"
echo "  2. 请修改 docker-compose-services.yml 中的节点约束 (node.hostname)"
echo "  3. 请确保防火墙开放以上端口"
echo "  4. 默认账号: admin / test123"
echo ""
print_separator
print_success "DocuMind 部署完成!"
print_separator