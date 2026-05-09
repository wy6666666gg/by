@echo off
chcp 65001 >nul
REM ============================================
REM 数据采集模块启动脚本 (Windows)
REM ============================================

cd /d "%~dp0air-data-collector"

REM 检查虚拟环境
if not exist "venv" (
    echo 创建虚拟环境...
    python -m venv venv
)

REM 激活虚拟环境
call venv\Scripts\activate

REM 安装/更新依赖
echo 检查依赖...
pip install -q -r requirements.txt

REM 运行调度器
echo 启动数据采集调度器...
python -m collector.scheduler --mode schedule

pause
