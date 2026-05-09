@echo off
chcp 65001
cls

echo ======================================
echo  空气质量分析系统 - 前端启动脚本
echo ======================================
echo.

cd air-web-frontend

echo 正在检查依赖...
if not exist node_modules (
    echo 首次运行，正在安装依赖...
    call npm install
    if errorlevel 1 (
        echo 依赖安装失败，请检查网络连接
        pause
        exit /b 1
    )
)

echo.
echo 启动前端开发服务器...
echo 访问地址: http://localhost:3000
echo.

npm run dev

pause
