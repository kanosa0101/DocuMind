@echo off
chcp 65001 >nul

:: ========================================
:: DocuMind 后端启动脚本 (Windows)
:: ========================================

color 0A

echo.
echo ========================================
echo     DocuMind 后端管理工具
echo ========================================
echo.
echo 请选择操作:
echo   1) Maven 构建
echo   2) 构建 Docker 镜像
echo   3) 启动 Docker 服务
echo   4) 全部执行 (构建 + 镜像 + 启动)
echo   5) 查看服务状态
echo   6) 停止服务
echo   7) 清理项目
echo   q) 退出
echo.

set /p choice="请输入选项: "

if "%choice%"=="1" goto build
if "%choice%"=="2" goto docker_build
if "%choice%"=="3" goto docker_start
if "%choice%"=="4" goto all
if "%choice%"=="5" goto status
if "%choice%"=="6" goto stop
if "%choice%"=="7" goto clean
if "%choice%"=="q" goto end
if "%choice%"=="Q" goto end

echo [错误] 无效选项
goto end

:: ========================================
:build
echo.
echo [INFO] Maven 构建...
where mvn >nul 2>&1
if errorlevel 1 (
    echo [错误] Maven 未安装
    goto end
)
call mvn clean package -DskipTests
echo [SUCCESS] Maven 构建完成
goto end

:: ========================================
:docker_build
echo.
echo [INFO] 构建 Docker 镜像...
for %%s in (gateway-service user-service file-service ai-service document-service) do (
    echo [INFO] 构建 docai/%%s:1.0.0...
    docker build -t docai/%%s:1.0.0 -f %%s\Dockerfile .
)
echo [SUCCESS] 镜像构建完成
goto end

:: ========================================
:docker_start
echo.
echo [INFO] 启动 Docker 服务...
if not exist "docker-compose-standalone.yml" (
    echo [错误] docker-compose-standalone.yml 不存在
    goto end
)
echo [INFO] 启动基础设施 (等待健康检查)...
docker-compose -f docker-compose-standalone.yml up -d --wait mysql redis rabbitmq minio nacos
echo [INFO] 启动微服务...
docker-compose -f docker-compose-standalone.yml up -d --wait gateway user file ai document
echo [SUCCESS] 服务已启动
call :show_urls
goto end

:: ========================================
:all
call :build
call :docker_build
call :docker_start
goto end

:: ========================================
:status
echo.
echo ========================================
echo [INFO] 服务状态
echo ========================================
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
call :show_urls
goto end

:: ========================================
:stop
echo.
echo [INFO] 停止 Docker 服务...
docker-compose -f docker-compose-standalone.yml down
echo [SUCCESS] 服务已停止
goto end

:: ========================================
:clean
echo.
echo [WARNING] 即将删除各服务 target/ 目录
set /p confirm="确认清理? [y/N]: "
if /i "%confirm%"=="y" (
    for %%s in (gateway-service user-service file-service ai-service document-service common) do (
        if exist "%%s\target" rmdir /s /q "%%s\target"
    )
    echo [SUCCESS] 清理完成
) else (
    echo [INFO] 取消清理
)
goto end

:: ========================================
:show_urls
echo.
echo ========================================
echo [SUCCESS] 访问地址
echo ========================================
echo.
echo   API 网关         http://localhost:18080
echo   用户服务         http://localhost:18081/swagger-ui.html
echo   文件服务         http://localhost:18082/swagger-ui.html
echo   AI 服务          http://localhost:18083/swagger-ui.html
echo   文档服务         http://localhost:18084/swagger-ui.html
echo   Nacos 控制台     http://localhost:18848/nacos
echo   MinIO 控制台     http://localhost:19001
echo   RabbitMQ 控制台  http://localhost:29672
echo.
echo   默认账号: admin / test123
echo.
exit /b 0

:: ========================================
:end
echo.
pause