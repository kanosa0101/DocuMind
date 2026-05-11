#!/bin/bash

# ========================================
# DocuMind 全项目启动脚本
# ========================================

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# 打印函数
print_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }
print_separator() { echo -e "${BLUE}========================================${NC}"; }

# 项目路径
PROJECT_ROOT="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$PROJECT_ROOT"
FRONTEND_DIR="$PROJECT_ROOT/frontend"

# ========================================
# 函数定义
# ========================================

show_urls() {
  echo ""
  print_separator
  print_success "服务访问地址:"
  print_separator
  echo ""
  echo "  前端应用         http://localhost:5173"
  echo "  API 网关         http://localhost:9080"
  echo "  用户服务         http://localhost:9081/swagger-ui.html"
  echo "  文件服务         http://localhost:9082/swagger-ui.html"
  echo "  AI 服务          http://localhost:9083/swagger-ui.html"
  echo "  文档服务         http://localhost:9084/swagger-ui.html"
  echo "  Nacos 控制台     http://localhost:8848/nacos"
  echo "  MinIO 控制台     http://localhost:9001"
  echo "  RabbitMQ 控制台  http://localhost:15672"
  echo ""
  echo "  默认账号: admin / admin123"
  echo ""
}

check_frontend_deps() {
  if ! command -v npm &> /dev/null; then
    print_error "npm 未安装"
    return 1
  fi
  if [ ! -d "$FRONTEND_DIR" ]; then
    print_error "前端目录不存在: $FRONTEND_DIR"
    return 1
  fi
  cd "$FRONTEND_DIR"
  if [ ! -d "node_modules" ]; then
    print_info "安装前端依赖..."
    npm install
  fi
  cd "$PROJECT_ROOT"
}

# 1. 启动全部
start_all() {
  print_separator
  print_info "启动 DocuMind 全栈服务"
  print_separator

  start_backend
  start_frontend

  print_success "所有服务已启动!"
  show_urls
}

# 2. 启动前端
start_frontend() {
  print_separator
  print_info "启动前端开发服务器"
  print_separator

  check_frontend_deps

  cd "$FRONTEND_DIR"
  print_info "启动 Vite..."

  npm run dev &

  print_success "前端已启动: http://localhost:5173"
  cd "$PROJECT_ROOT"
  show_urls
}

# 3. 启动后端
start_backend() {
  print_separator
  print_info "启动后端 Docker 服务"
  print_separator

  if ! command -v docker &> /dev/null; then
    print_error "Docker 未安装"
    return 1
  fi

  cd "$BACKEND_DIR"

  # 检查镜像是否存在
  IMAGES=("docai/gateway-service:1.0.0" "docai/user-service:1.0.0"
          "docai/file-service:1.0.0" "docai/ai-service:1.0.0" "docai/document-service:1.0.0")

  MISSING=0
  for img in "${IMAGES[@]}"; do
    if ! docker image inspect "$img" &>/dev/null; then
      print_warning "镜像 $img 不存在，需要构建"
      MISSING=1
    fi
  done

  if [ "$MISSING" = "1" ]; then
    print_info "构建后端镜像..."
    build_backend_images
  fi

  # 先启动基础设施并等待健康检查
  print_info "启动基础设施 (MySQL, Redis, RabbitMQ, MinIO, Nacos)..."
  docker-compose -f docker-compose-standalone.yml up -d --wait mysql redis rabbitmq minio nacos

  print_info "等待基础设施就绪..."
  sleep 10

  # 再启动微服务
  print_info "启动微服务..."
  docker-compose -f docker-compose-standalone.yml up -d --wait gateway user file ai document

  print_success "后端服务已启动"
  cd "$PROJECT_ROOT"
  show_urls
}

# 4. 构建全部
build_all() {
  print_separator
  print_info "构建前后端项目"
  print_separator
  build_backend
  build_frontend
  print_success "所有构建完成"
}

# 5. 构建前端
build_frontend() {
  print_info "构建前端..."
  check_frontend_deps

  cd "$FRONTEND_DIR"
  npm run build
  print_success "前端构建完成: $FRONTEND_DIR/dist"
  cd "$PROJECT_ROOT"
}

# 6. 构建后端
build_backend() {
  print_info "构建后端 Maven 项目..."

  if ! command -v mvn &> /dev/null; then
    print_error "Maven 未安装"
    return 1
  fi

  cd "$BACKEND_DIR"
  mvn clean package -DskipTests
  print_success "Maven 构建完成"

  build_backend_images
  print_success "后端构建完成"
  cd "$PROJECT_ROOT"
}

build_backend_images() {
  SERVICES=("gateway-service" "user-service" "file-service" "ai-service" "document-service")
  VERSION="1.0.0"

  for service in "${SERVICES[@]}"; do
    print_info "构建镜像 docai/${service}:${VERSION}..."
    docker build -t docai/${service}:${VERSION} -f ./${service}/Dockerfile .
  done
}

# 7. 查看状态
check_status() {
  print_separator
  print_info "服务状态"
  print_separator

  echo ""
  print_info "Docker 容器:"
  docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | grep -E "gateway|user|file|ai|document|nacos|mysql|redis|rabbitmq|minio" || echo "  无运行容器"

  echo ""
  show_urls
}

# 8. 停止服务
stop_all() {
  print_separator
  print_info "停止所有服务"
  print_separator

  print_info "停止前端..."
  pkill -f "vite" 2>/dev/null || print_info "前端未运行"

  print_info "停止 Docker 服务..."
  cd "$BACKEND_DIR"
  docker-compose -f docker-compose-standalone.yml down 2>/dev/null || print_info "Docker 服务未运行"
  cd "$PROJECT_ROOT"

  print_success "所有服务已停止"
}

# 9. 清理
clean_all() {
  print_separator
  print_info "清理项目"
  print_separator

  print_warning "即将删除:"
  echo "  - 后端: 各服务 target/ 目录"
  echo "  - 前端: node_modules/, dist/, .vite/"
  echo "  - Docker: 停止并删除容器"
  echo ""

  read -p "确认清理? [y/N]: " confirm
  if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
    # 停止服务
    docker-compose -f docker-compose-standalone.yml down 2>/dev/null

    # 清理后端
    for service in gateway-service user-service file-service ai-service document-service common; do
      rm -rf "${service}/target"
    done

    # 清理前端
    cd "$FRONTEND_DIR"
    rm -rf node_modules dist .vite
    cd "$PROJECT_ROOT"

    print_success "清理完成"
  else
    print_info "取消清理"
  fi
}

# ========================================
# 主菜单
# ========================================

print_separator
echo -e "${BLUE}DocuMind 全项目管理工具${NC}"
print_separator
echo ""
echo "请选择操作:"
echo ""
echo "  1) 启动全部服务 (前端 + 后端)"
echo "  2) 仅启动前端"
echo "  3) 仅启动后端"
echo "  4) 构建全部项目"
echo "  5) 仅构建前端"
echo "  6) 仅构建后端"
echo "  7) 查看服务状态"
echo "  8) 停止所有服务"
echo "  9) 清理项目"
echo "  q) 退出"
echo ""

read -p "请输入选项: " choice

case $choice in
  1) start_all ;;
  2) start_frontend ;;
  3) start_backend ;;
  4) build_all ;;
  5) build_frontend ;;
  6) build_backend ;;
  7) check_status ;;
  8) stop_all ;;
  9) clean_all ;;
  q|Q) echo "退出..."; exit 0 ;;
  *) print_error "无效选项"; exit 1 ;;
esac