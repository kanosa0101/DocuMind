@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

:: ========================================
:: DocuMind 前端项目启动脚本 (Windows)
:: ========================================

color 0A

echo.
echo ========================================
echo     DocuMind 前端项目管理工具
echo ========================================
echo.
echo 请选择操作:
echo   1) 安装依赖
echo   2) 启动开发服务器
echo   3) 构建生产版本
echo   4) 预览生产版本
echo   5) 全部执行 (安装 + 启动)
echo   6) 清理项目
echo   q) 退出
echo.

set /p choice="请输入选项 [1-6 或 q]: "

if "%choice%"=="1" goto install
if "%choice%"=="2" goto dev
if "%choice%"=="3" goto build
if "%choice%"=="4" goto preview
if "%choice%"=="5" goto all
if "%choice%"=="6" goto clean
if "%choice%"=="q" goto end
if "%choice%"=="Q" goto end

echo [错误] 无效选项
goto end

:: ========================================
:: 安装依赖
:: ========================================
:install
echo.
echo [INFO] 步骤 1: 安装项目依赖...
echo.

:: 检查 Node.js
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] Node.js 未安装，请先安装 Node.js 18+
    echo 下载地址: https://nodejs.org/
    goto end
)

:: 检查 npm
where npm >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] npm 未安装
    goto end
)

:: 显示版本
for /f "tokens=*" %%i in ('node -v') do set NODE_VER=%%i
for /f "tokens=*" %%i in ('npm -v') do set NPM_VER=%%i
echo [INFO] Node.js 版本: %NODE_VER%
echo [INFO] npm 版本: %NPM_VER%
echo.

echo [INFO] 正在安装 npm 包...
call npm install

echo.
echo [SUCCESS] 依赖安装完成
goto end

:: ========================================
:: 启动开发服务器
:: ========================================
:dev
echo.
echo [INFO] 步骤 2: 启动开发服务器...
echo.

:: 检查 node_modules
if not exist "node_modules" (
    echo [WARNING] node_modules 不存在，先安装依赖
    call npm install
)

:: 检查后端服务
echo [INFO] 检查后端服务状态...
curl -s -o nul -w "" http://localhost:18080/api/users/login -X POST -H "Content-Type: application/json" -d "{\"username\":\"test\"}" --max-time 5 >nul 2>&1
if %errorlevel% equ 0 (
    echo [SUCCESS] 后端服务正常运行 (localhost:18080)
) else (
    echo [WARNING] 后端服务未启动，请确保后端在 localhost:18080 运行
    echo [INFO] 后端启动命令: cd .. ^&^& deploy.sh
)

echo.
echo ========================================
echo [SUCCESS] 前端开发服务器启动成功!
echo ========================================
echo.
echo 访问地址: http://localhost:5175
echo API代理: http://localhost:18080
echo.
echo 按 Ctrl+C 停止服务器
echo.

call npm run dev
goto end

:: ========================================
:: 构建生产版本
:: ========================================
:build
echo.
echo [INFO] 步骤 3: 构建生产版本...
echo.

if not exist "node_modules" (
    echo [WARNING] node_modules 不存在，先安装依赖
    call npm install
)

echo [INFO] 正在构建...
call npm run build

echo.
echo [SUCCESS] 构建完成!
echo [INFO] 输出目录: %CD%\dist
goto end

:: ========================================
:: 预览生产版本
:: ========================================
:preview
echo.
echo [INFO] 步骤 4: 预览生产版本...
echo.

if not exist "dist" (
    echo [WARNING] dist 目录不存在，先执行构建
    goto build
)

echo [INFO] 启动预览服务器...
call npm run preview
goto end

:: ========================================
:: 全部执行
:: ========================================
:all
call :install
call :dev
goto end

:: ========================================
:: 清理项目
:: ========================================
:clean
echo.
echo [INFO] 步骤 5: 清理项目...
echo.

echo [WARNING] 即将删除以下内容:
echo   - node_modules
echo   - dist
echo   - .vite
echo.

set /p confirm="确认清理? [y/N]: "
if /i "%confirm%"=="y" (
    if exist "node_modules" rmdir /s /q node_modules
    if exist "dist" rmdir /s /q dist
    if exist ".vite" rmdir /s /q .vite
    echo [SUCCESS] 清理完成
) else (
    echo [INFO] 取消清理
)
goto end

:: ========================================
:: 结束
:: ========================================
:end
echo.
pause