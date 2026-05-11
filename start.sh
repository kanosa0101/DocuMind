#!/bin/bash

# ========================================
# DocuMind 后端启动脚本
# ========================================

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }
print_separator() { echo -e "${BLUE}========================================${NC}"; }

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
SERVICES=("gateway-service" "user-service" "file-service" "ai-service" "document-service")

# ========================================
# 主菜单
# ========================================

print_separator
echo -e "${BLUE}DocuMind 后端管理工具${NC}"
print_separator
echo ""
echo "请选择操作:"
echo "  1) Maven 构建"
echo "  2) 构建 Docker 镜像"
echo "  3) 启动 Docker 服务"
echo "  4) 全部执行 (构建 + 镜像 + 启动)"
echo "  5) 查看服务状态"
echo "  6) 停止服务"
echo "  7) 清理项目"
echo "  q) 退出"
echo ""

read -p "请输入选项: " choice

case $choice in
  1) maven_build ;;
  2) docker_build ;;
  3) docker_start ;;
  4) maven_build && docker_build && docker_start ;;
  5) check_status ;;
  6) docker_stop ;;
  7) clean_project ;;
  q|Q) echo "退出..."; exit 0 ;;
  *) print_error "无效选项"; exit 1 ;;
esac

# ========================================
# 函数
# ========================================

maven_build() {
  print_info "Maven 构建..."
  cd "$PROJECT_DIR"

  if ! command -v mvn &> /dev/null; then
    print_error "Maven 未安装"
    exit 1
  fi

  mvn clean package -DskipTests
  print_success "Maven 构建完成"
}

docker_build() {
  print_info "构建 Docker 镜像..."
  cd "$PROJECT_DIR"

  VERSION="1.0.0"
  for service in "${SERVICES[@]}"; do
    print_info "构建 docai/${service}:${VERSION}..."
    docker build -t docai/${service}:${VERSION} -f ./${service}/Dockerfile .
  done
  print_success "镜像构建完成"
}

docker_start() {
  print_info "启动 Docker 服务..."
  cd "$PROJECT_DIR"

  if [ ! -f "docker-compose-standalone.yml" ]; then
    print_error "docker-compose-standalone.yml 不存在"
    exit 1
  fi

  # 先启动基础设施
  print_info "启动基础设施 (等待健康检查)..."
  docker-compose -f docker-compose-standalone.yml up -d --wait mysql redis rabbitmq minio nacos

  print_info "等待基础设施就绪..."
  sleep 5

  # 再启动微服务
  print_info "启动微服务..."
  docker-compose -f docker-compose-standalone.yml up -d --wait gateway user file ai document

  print_success "服务已启动"
  show_urls
}

docker_stop() {
  print_info "停止 Docker 服务..."
  cd "$PROJECT_DIR"
  docker-compose -f docker-compose-standalone.yml down
  print_success "服务已停止"
}

check_status() {
  print_separator
  print_info "服务状态"
  print_separator
  docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
  show_urls
}

clean_project() {
  print_warning "即将删除各服务 target/ 目录"
  read -p "确认清理? [y/N]: " confirm

  if [ "$confirm" = "y" ]; then
    for service in "${SERVICES[@]}"; do
      rm -rf "${service}/target"
    done
    rm -rf common/target
    print_success "清理完成"
  else
    print_info "取消清理"
  fi
}

show_urls() {
  echo ""
  print_separator
  print_success "访问地址:"
  print_separator
  echo ""
  echo "  API 网关         http://localhost:9080"
  echo "  用户服务         http://localhost:9081/swagger-ui.html"
  echo "  文件服务         http://localhost:9082/swagger-ui.html"
  echo "  AI 服务          http://localhost:9083/swagger-ui.html"
  echo "  文档服务         http://localhost:9084/swagger-ui.html"
  echo "  Nacos 控制台     http://localhost:8848/nacos"
  echo ""
}