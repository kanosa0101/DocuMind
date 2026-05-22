@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ========================================
:: DocuMind 全项目启动脚本 (Windows)
:: ========================================

color 0A

set PROJECT_ROOT=%~dp0
set BACKEND_DIR=%PROJECT_ROOT%
set FRONTEND_DIR=%PROJECT_ROOT%frontend

:: ========================================
:: 主菜单
:: ========================================

echo.
echo ========================================
echo     DocuMind 全项目管理工具
echo ========================================
echo.
echo 请选择操作:
echo.
echo   1) 启动全部服务 (前端 + 后端)
echo   2) 仅启动前端
echo   3) 仅启动后端
echo   4) 构建全部项目
echo   5) 仅构建前端
echo   6) 仅构建后端
echo   7) 查看服务状态
echo   8) 停止所有服务
echo   9) 清理项目
echo   q) 退出
echo.

set /p choice="请输入选项: "

if "%choice%"=="1" goto start_all
if "%choice%"=="2" goto start_frontend
if "%choice%"=="3" goto start_backend
if "%choice%"=="4" goto build_all
if "%choice%"=="5" goto build_frontend
if "%choice%"=="6" goto build_backend
if "%choice%"=="7" goto status
if "%choice%"=="8" goto stop
if "%choice%"=="9" goto clean
if "%choice%"=="q" goto end
if "%choice%"=="Q" goto end

echo [错误] 无效选项: %choice%
goto end

:: ========================================
:: 1. 启动全部
:: ========================================
:start_all
echo.
echo ========================================
echo [INFO] 启动全部服务
echo ========================================
echo.
call :start_backend_proc
call :start_frontend_proc
echo.
echo [SUCCESS] 所有服务已启动
call :show_urls
goto end

:: ========================================
:: 2. 启动前端
:: ========================================
:start_frontend
echo.
echo ========================================
echo [INFO] 启动前端开发服务器
echo ========================================
echo.
call :start_frontend_proc
call :show_urls
goto end

:: ========================================
:: 3. 启动后端
:: ========================================
:start_backend
echo.
echo ========================================
echo [INFO] 启动后端 Docker 服务
echo ========================================
echo.
call :start_backend_proc
call :show_urls
goto end

:: ========================================
:: 4. 构建全部
:: ========================================
:build_all
echo.
echo ========================================
echo [INFO] 构建前后端项目
echo ========================================
echo.
call :build_backend_proc
call :build_frontend_proc
echo.
echo [SUCCESS] 所有构建完成
goto end

:: ========================================
:: 5. 构建前端
:: ========================================
:build_frontend
echo.
echo ========================================
echo [INFO] 构建前端项目
echo ========================================
echo.
call :build_frontend_proc
goto end

:: ========================================
:: 6. 构建后端
:: ========================================
:build_backend
echo.
echo ========================================
echo [INFO] 构建后端项目
echo ========================================
echo.
call :build_backend_proc
goto end

:: ========================================
:: 7. 查看状态
:: ========================================
:status
echo.
echo ========================================
echo [INFO] 服务状态
echo ========================================
echo.
echo [Docker] 容器列表:
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" 2>nul
if errorlevel 1 echo   无运行容器
echo.
call :show_urls
goto end

:: ========================================
:: 8. 停止服务
:: ========================================
:stop
echo.
echo ========================================
echo [INFO] 停止所有服务
echo ========================================
echo.
echo [前端] 停止前端进程...
taskkill /F /IM node.exe 2>nul
if errorlevel 1 echo   前端未运行
echo.
echo [后端] 停止 Docker 服务...
docker-compose -f "%BACKEND_DIR%docker-compose-standalone.yml" down 2>nul
if errorlevel 1 echo   Docker 服务未运行
echo.
echo [SUCCESS] 所有服务已停止
goto end

:: ========================================
:: 9. 清理项目
:: ========================================
:clean
echo.
echo ========================================
echo [INFO] 清理项目
echo ========================================
echo.
echo [WARNING] 即将删除:
echo   - 后端: 各服务 target/ 目录
echo   - 前端: node_modules/, dist/, .vite/
echo   - Docker: 停止并删除容器
echo.
set /p confirm="确认清理? [y/N]: "
if /i not "%confirm%"=="y" (
    echo [INFO] 取消清理
    goto end
)
echo.
echo [INFO] 停止 Docker 服务...
docker-compose -f "%BACKEND_DIR%docker-compose-standalone.yml" down 2>nul
echo.
echo [INFO] 清理后端 target 目录...
for %%s in (gateway-service user-service file-service ai-service document-service common) do (
    if exist "%BACKEND_DIR%\%%s\target" (
        rmdir /s /q "%BACKEND_DIR%\%%s\target"
        echo   已清理 %%s\target
    )
)
echo.
echo [INFO] 清理前端...
if exist "%FRONTEND_DIR%\node_modules" rmdir /s /q "%FRONTEND_DIR%\node_modules"
if exist "%FRONTEND_DIR%\dist" rmdir /s /q "%FRONTEND_DIR%\dist"
if exist "%FRONTEND_DIR%\.vite" rmdir /s /q "%FRONTEND_DIR%\.vite"
echo   已清理 frontend
echo.
echo [SUCCESS] 清理完成
goto end

:: ========================================
:: 辅助函数
:: ========================================

:start_frontend_proc
:: 检查 Node.js
where node >nul 2>&1
if errorlevel 1 (
    echo [错误] Node.js 未安装
    echo   下载地址: https://nodejs.org/
    exit /b 1
)
:: 检查前端目录
if not exist "%FRONTEND_DIR%\package.json" (
    echo [错误] 前端目录不存在: %FRONTEND_DIR%
    exit /b 1
)
:: 安装依赖
if not exist "%FRONTEND_DIR%\node_modules" (
    echo [INFO] 安装前端依赖...
    cd /d "%FRONTEND_DIR%"
    call npm install
    cd /d "%PROJECT_ROOT%"
)
echo [INFO] 启动前端 Vite 开发服务器...
start "DocuMind Frontend" cmd /k "cd /d %FRONTEND_DIR% && npm run dev"
echo [SUCCESS] 前端已启动
exit /b 0

:start_backend_proc
:: 检查 Docker
where docker >nul 2>&1
if errorlevel 1 (
    echo [错误] Docker 未安装
    echo   下载地址: https://www.docker.com/products/docker-desktop
    exit /b 1
)
:: 检查 compose 文件
if not exist "%BACKEND_DIR%docker-compose-standalone.yml" (
    echo [错误] docker-compose-standalone.yml 不存在
    exit /b 1
)
:: 检查镜像
set IMAGES_MISSING=0
for %%s in (gateway-service user-service file-service ai-service document-service) do (
    docker image inspect docai/%%s:1.0.0 >nul 2>&1
    if errorlevel 1 (
        echo [WARNING] 镜像 docai/%%s:1.0.0 不存在
        set IMAGES_MISSING=1
    )
)
if "%IMAGES_MISSING%"=="1" (
    echo.
    echo [INFO] 需要先构建后端镜像，请选择选项 6 构建后端
    echo   或运行: mvn clean package -DskipTests
    echo   然后构建镜像
    exit /b 1
)
echo [INFO] 启动基础设施 (等待健康检查)...
cd /d "%BACKEND_DIR%"
docker-compose -f docker-compose-standalone.yml up -d --wait mysql redis rabbitmq minio nacos
echo [INFO] 启动微服务...
docker-compose -f docker-compose-standalone.yml up -d --wait gateway user file ai document
cd /d "%PROJECT_ROOT%"
echo [SUCCESS] 后端服务已启动
exit /b 0

:build_frontend_proc
where npm >nul 2>&1
if errorlevel 1 (
    echo [错误] npm 未安装
    exit /b 1
)
if not exist "%FRONTEND_DIR%\package.json" (
    echo [错误] 前端目录不存在
    exit /b 1
)
cd /d "%FRONTEND_DIR%"
if not exist "node_modules" (
    echo [INFO] 安装前端依赖...
    call npm install
)
echo [INFO] 构建前端...
call npm run build
cd /d "%PROJECT_ROOT%"
echo [SUCCESS] 前端构建完成: %FRONTEND_DIR%\dist
exit /b 0

:build_backend_proc
where mvn >nul 2>&1
if errorlevel 1 (
    echo [错误] Maven 未安装
    echo   下载地址: https://maven.apache.org/download.cgi
    exit /b 1
)
where docker >nul 2>&1
if errorlevel 1 (
    echo [错误] Docker 未安装
    exit /b 1
)
echo [INFO] Maven 构建后端...
cd /d "%BACKEND_DIR%"
call mvn clean package -DskipTests
cd /d "%PROJECT_ROOT%"
echo [SUCCESS] Maven 构建完成
echo.
echo [INFO] 构建 Docker 镜像...
for %%s in (gateway-service user-service file-service ai-service document-service) do (
    echo [INFO] 构建镜像 docai/%%s:1.0.0...
    docker build -t docai/%%s:1.0.0 -f "%BACKEND_DIR%\%%s\Dockerfile" "%BACKEND_DIR%"
)
echo [SUCCESS] Docker 镜像构建完成
exit /b 0

:show_urls
echo.
echo ========================================
echo [SUCCESS] 服务访问地址
echo ========================================
echo.
echo   前端应用         http://localhost:5175
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
:: 结束
:: ========================================
:end
echo.
pause