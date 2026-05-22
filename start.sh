#!/bin/bash
# DocuMind 快速启动脚本

echo "=========================================="
echo "  DocuMind 服务启动脚本"
echo "=========================================="

# 检查.env文件
if [ ! -f .env ]; then
    echo "⚠️  .env文件不存在，从.env.example复制..."
    cp .env.example .env
    echo "📝 请编辑.env文件配置密码和API密钥"
fi

# 启动Docker服务
echo ""
echo "🚀 启动基础设施和微服务..."
docker-compose -f docker-compose-standalone.yml up -d

echo ""
echo "⏳ 等待服务启动（约60秒）..."
sleep 60

# 检查服务状态
echo ""
echo "📊 服务状态检查:"
docker-compose -f docker-compose-standalone.yml ps

echo ""
echo "=========================================="
echo "  服务已启动，访问地址:"
echo "=========================================="
echo "  前端: http://localhost:3000"
echo "  Gateway: http://localhost:18080"
echo "  Nacos: http://localhost:18848/nacos"
echo "  RabbitMQ: http://localhost:29672"
echo "  MinIO: http://localhost:19001"
echo "=========================================="
echo ""
echo "💡 提示："
echo "  - 执行数据库迁移: mysql -u root -p doc_ai < mysql-schema-update.sql"
echo "  - 安装前端依赖: cd frontend && npm install"
echo "  - 启动前端: cd frontend && npm run dev"
echo ""
