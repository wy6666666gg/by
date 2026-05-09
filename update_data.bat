@echo off
chcp 65001
cls

echo ======================================
echo  郑州市空气质量数据更新工具
echo ======================================
echo.
echo 请先访问 https://citydev.gbqyun.com/index/zhengzhou
echo 查看当前各站点的AQI数值
echo.

cd air-data-collector

python update_data_simple.py

pause
