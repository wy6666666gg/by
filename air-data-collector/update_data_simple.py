#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
郑州市空气质量数据更新工具 - 简化版
直接手动输入从 https://citydev.gbqyun.com/index/zhengzhou 看到的AQI数据
"""

import csv
import os
from datetime import datetime
from pathlib import Path

DATA_DIR = Path(__file__).parent / "data"
CSV_FILE_PATH = DATA_DIR / "zhengzhou_realtime.csv"

STATIONS = [
    ("410101", "中原区", "北区建设指挥部"),
    ("410102", "金水区", "北区建设指挥部"),
    ("410103", "二七区", "河医大"),
    ("410108", "惠济区", "惠济区政府"),
    ("410104", "郑东新区", "经开区管委"),
]

def get_aqi_level(aqi):
    if aqi <= 50: return "优"
    if aqi <= 100: return "良"
    if aqi <= 150: return "轻度污染"
    if aqi <= 200: return "中度污染"
    if aqi <= 300: return "重度污染"
    return "严重污染"

def get_pollutant_values(aqi):
    """根据AQI估算其他污染物数值"""
    return {
        'pm25': round(aqi * 0.65, 1),
        'pm10': round(aqi * 0.95, 1),
        'o3': round(80 + aqi * 0.2, 1),
        'so2': round(10 + aqi * 0.05, 1),
        'co': round(0.5 + aqi * 0.005, 2),
        'no2': round(30 + aqi * 0.15, 1),
        'primary': 'PM2.5' if aqi > 100 else 'PM10'
    }

def main():
    print("=" * 60)
    print("郑州市空气质量数据更新")
    print("=" * 60)
    print("\n请访问 https://citydev.gbqyun.com/index/zhengzhou")
    print("查看各站点当前AQI数值，然后在此输入")
    print("(直接回车保持默认值)")
    print("=" * 60)
    
    data = []
    now = datetime.now()
    
    # 默认值 - 您应该根据网站实际数据修改这些值
    default_aqis = {
        "中原区": 85,
        "金水区": 82,
        "二七区": 95,
        "惠济区": 75,
        "郑东新区": 88
    }
    
    for code, district, actual in STATIONS:
        print(f"\n{district} (监测点: {actual})")
        default = default_aqis.get(district, 80)
        
        while True:
            user_input = input(f"  请输入AQI [{default}]: ").strip()
            if not user_input:
                aqi = default
                break
            try:
                aqi = int(user_input)
                if 0 <= aqi <= 500:
                    break
                print("  AQI范围应为0-500")
            except:
                print("  请输入有效数字")
        
        level = get_aqi_level(aqi)
        pol = get_pollutant_values(aqi)
        
        row = {
            'stationCode': code,
            'stationName': district,
            'actualStation': actual,
            'date': now.strftime('%Y-%m-%d'),
            'aqi': aqi,
            'qualityLevel': level,
            'pm25': pol['pm25'],
            'pm10': pol['pm10'],
            'o3': pol['o3'],
            'so2': pol['so2'],
            'co': pol['co'],
            'no2': pol['no2'],
            'primaryPollutant': pol['primary'],
            'isSandDustDay': 'False',
            'updateTime': now.strftime('%Y-%m-%d %H:%M:%S'),
            'dataSource': 'gbqyun'
        }
        data.append(row)
        print(f"  ✓ {district}: AQI {aqi} ({level})")
    
    # 保存到CSV
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    
    with open(CSV_FILE_PATH, 'w', newline='', encoding='utf-8') as f:
        fieldnames = ['stationCode', 'stationName', 'actualStation', 'date', 'aqi', 'qualityLevel',
                      'pm25', 'pm10', 'o3', 'so2', 'co', 'no2', 'primaryPollutant', 
                      'isSandDustDay', 'updateTime', 'dataSource']
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(data)
    
    print("\n" + "=" * 60)
    print(f"✓ 数据已保存: {CSV_FILE_PATH}")
    print(f"✓ 更新时间: {now.strftime('%Y-%m-%d %H:%M:%S')}")
    print("\n提示: 请刷新前端页面 http://localhost:3000 查看最新数据")
    print("=" * 60)

if __name__ == "__main__":
    main()
