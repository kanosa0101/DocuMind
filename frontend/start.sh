#!/bin/bash

# ========================================
# DocuMind 前端项目启动脚本
# ========================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 打印带颜色的消息
print_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_warning() { echo -e "${YELLOW}[WARNING]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 打印分隔线
print_separator() { echo -e "${BLUE}========================================${NC}"; }

# 项目根目录
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

# ========================================
# 主菜单
# ========================================

print_separator
echo -e "${BLUE}DocuMind 前端项目管理工具${NC}"
print_separator
echo ""
echo "请选择操作:"
echo "  1) 安装依赖"
echo "  2) 启动开发服务器"
echo "  3) 构建生产版本"
echo "  4) 预览生产版本"
echo "  5) 全部执行（安装 + 启动）"
echo "  6) 清理项目"
echo "  q) 退出"
echo ""

read -p "请输入选项 [1-6 或 q]: " choice

case $choice in
  1)
    install_dependencies
    ;;
  2)
    start_dev
    ;;
  3)
    build_production
    ;;
  4)
    preview_production
    ;;
  5)
    install_dependencies
    start_dev
    ;;
  6)
    clean_project
    ;;
  q|Q)
    echo "退出..."
    exit 0
    ;;
  *)
    print_error "无效选项，请重新运行脚本"
    exit 1
    ;;
esac

# ========================================
# 功能函数
# ========================================

# 安装依赖
install_dependencies() {
  print_info "步骤 1: 安装项目依赖..."

  cd "$PROJECT_DIR"

  # 检查 node
  if ! command -v node &> /dev/null; then
    print_error "Node.js 未安装，请先安装 Node.js 18+"
    exit 1
  fi

  # 检查 npm
  if ! command -v npm &> /dev/null; then
    print_error "npm 未安装"
    exit 1
  fi

  # 检查 Node 版本
  NODE_VERSION=$(node -v | cut -d 'v' -f 2 | cut -d '.' -f 1)
  if [ "$NODE_VERSION" -lt 18 ]; then
    print_warning "Node.js 版本过低，建议使用 18+"
  fi

  print_info "Node.js 版本: $(node -v)"
  print_info "npm 版本: $(npm -v)"

  # 安装依赖
  print_info "正在安装 npm 包..."
  npm install

  print_success "依赖安装完成"
}

# 启动开发服务器
start_dev() {
  print_info "步骤 2: 启动开发服务器..."

  cd "$PROJECT_DIR"

  # 检查依赖是否安装
  if [ ! -d "node_modules" ]; then
    print_warning "node_modules 不存在，先安装依赖"
    npm install
  fi

  # 检查后端服务是否运行
  print_info "检查后端服务状态..."
  if curl -s http://localhost:8080/api/users/login -X POST -H "Content-Type: application/json" -d '{"username":"test"}' --max-time 5 > /dev/null 2>&1; then
    print_success "后端服务正常运行 (localhost:8080)"
  else
    print_warning "后端服务未启动，请确保后端在 localhost:8080 运行"
    print_info "后端启动命令: cd ../ && ./deploy.sh"
  fi

  print_info "启动 Vite 开发服务器..."
  echo ""
  print_separator
  print_success "前端开发服务器启动成功!"
  print_separator
  echo ""
  echo "访问地址: http://localhost:5173"
  echo "API代理: http://localhost:8080"
  echo ""
  echo "按 Ctrl+C 停止服务器"
  echo ""

  npm run dev
}

# 构建生产版本
build_production() {
  print_info "步骤 3: 构建生产版本..."

  cd "$PROJECT_DIR"

  # 检查依赖
  if [ ! -d "node_modules" ]; then
    print_warning "node_modules 不存在，先安装依赖"
    npm install
  fi

  print_info "正在构建..."
  npm run build

  print_success "构建完成!"
  print_info "输出目录: $PROJECT_DIR/dist"
}

# 预览生产版本
preview_production() {
  print_info "步骤 4: 预览生产版本..."

  cd "$PROJECT_DIR"

  # 检查 dist 目录
  if [ ! -d "dist" ]; then
    print_warning "dist 目录不存在，先执行构建"
    build_production
  fi

  print_info "启动预览服务器..."
  npm run preview
}

# 清理项目
clean_project() {
  print_info "步骤 5: 清理项目..."

  cd "$PROJECT_DIR"

  print_warning "即将删除以下内容:"
  echo "  - node_modules"
  echo "  - dist"
  echo "  - .vite"

  read -p "确认清理? [y/N]: " confirm

  if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
    rm -rf node_modules dist .vite
    print_success "清理完成"
  else
    print_info "取消清理"
  fi
}