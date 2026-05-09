#!/bin/bash
# ============================================
# 数据采集模块启动脚本
# ============================================

cd "$(dirname "$0")/air-data-collector"

# 检查虚拟环境
if [ ! -d "venv" ]; then
    echo "创建虚拟环境..."
    python3 -m venv venv
fi

# 激活虚拟环境
source venv/bin/activate

# 安装/更新依赖
echo "检查依赖..."
pip install -q -r requirements.txt

# 运行调度器
echo "启动数据采集调度器..."
python -m collector.scheduler --mode schedule
