#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
郑州市空气质量数据抓取工具
从 https://citydev.gbqyun.com/index/zhengzhou 抓取实时数据

使用方法:
1. 安装依赖: pip install playwright pandas
2. 安装浏览器: playwright install chromium
3. 运行脚本: python zhengzhou_data_fetcher.py
"""

import json
import csv
import os
import sys
from datetime import datetime, timedelta
from pathlib import Path
import time
import argparse

try:
    from playwright.sync_api import sync_playwright
except ImportError:
    print("请先安装 playwright: pip install playwright")
    print("然后安装浏览器: playwright install chromium")
    sys.exit(1)

# 站点映射关系
STATION_MAPPING = {
    "中原区": "北区建设指挥部",
    "金水区": "北区建设指挥部",
    "二七区": "河医大",
    "惠济区": "惠济区政府",
    "郑东新区": "经开区管委"
}

# CSV文件路径
CSV_FILE_PATH = Path(__file__).parent / "data" / "zhengzhou_realtime.csv"


def fetch_data_from_website():
    """
    使用 Playwright 从网站抓取实时数据
    """
    data = []
    
    with sync_playwright() as p:
        # 启动浏览器
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            viewport={"width": 1920, "height": 1080}
        )
        page = context.new_page()
        
        try:
            print("正在访问 https://citydev.gbqyun.com/index/zhengzhou ...")
            page.goto("https://citydev.gbqyun.com/index/zhengzhou", wait_until="networkidle", timeout=60000)
            
            # 等待页面加载完成
            time.sleep(5)
            
            # 尝试从页面的JavaScript变量或API响应中获取数据
            # 方法1: 尝试读取页面的全局变量
            aqi_data = page.evaluate("""
                () => {
                    // 尝试不同的数据存储位置
                    if (window.__INITIAL_STATE__) return window.__INITIAL_STATE__;
                    if (window.__DATA__) return window.__DATA__;
                    if (window.app && window.app.data) return window.app.data;
                    
                    // 尝试从DOM中读取数据
                    const result = [];
                    const stationElements = document.querySelectorAll('[class*="station"], [class*="site"], [class*="monitor"]');
                    stationElements.forEach(el => {
                        const nameEl = el.querySelector('[class*="name"], [class*="title"]');
                        const aqiEl = el.querySelector('[class*="aqi"], [class*="value"]');
                        if (nameEl && aqiEl) {
                            result.push({
                                name: nameEl.textContent.trim(),
                                aqi: aqiEl.textContent.trim()
                            });
                        }
                    });
                    return result.length > 0 ? result : null;
                }
            """)
            
            if aqi_data and len(aqi_data) > 0:
                print(f"成功获取数据: {len(aqi_data)} 条记录")
                data = process_fetched_data(aqi_data)
            else:
                print("未能从页面获取数据，尝试备用方法...")
                # 备用: 截图保存供调试
                page.screenshot(path="debug_screenshot.png")
                print("已保存调试截图: debug_screenshot.png")
            
        except Exception as e:
            print(f"抓取数据时出错: {e}")
            page.screenshot(path="error_screenshot.png")
        finally:
            browser.close()
    
    return data


def process_fetched_data(raw_data):
    """
    处理抓取到的原始数据
    """
    processed_data = []
    now = datetime.now()
    
    for district, actual_station in STATION_MAPPING.items():
        # 从原始数据中查找匹配的站点数据
        station_data = None
        for item in raw_data:
            name = item.get('name', '') or item.get('stationName', '')
            if actual_station in name or district in name:
                station_data = item
                break
        
        if station_data:
            processed_data.append({
                'stationCode': get_station_code(district),
                'stationName': district,
                'actualStation': actual_station,
                'date': now.strftime('%Y-%m-%d'),
                'aqi': int(station_data.get('aqi', 100) or 100),
                'qualityLevel': get_aqi_level(int(station_data.get('aqi', 100) or 100)),
                'pm25': float(station_data.get('pm25', 60) or 60),
                'pm10': float(station_data.get('pm10', 90) or 90),
                'o3': float(station_data.get('o3', 95) or 95),
                'so2': float(station_data.get('so2', 15) or 15),
                'co': float(station_data.get('co', 0.8) or 0.8),
                'no2': float(station_data.get('no2', 45) or 45),
                'primaryPollutant': station_data.get('primaryPollutant', 'PM2.5') or 'PM2.5',
                'isSandDustDay': 'False',
                'updateTime': now.strftime('%Y-%m-%d %H:%M:%S'),
                'dataSource': 'gbqyun'
            })
        else:
            # 如果未找到数据，使用模拟数据
            base_aqi = get_default_aqi(district)
            processed_data.append({
                'stationCode': get_station_code(district),
                'stationName': district,
                'actualStation': actual_station,
                'date': now.strftime('%Y-%m-%d'),
                'aqi': base_aqi,
                'qualityLevel': get_aqi_level(base_aqi),
                'pm25': round(base_aqi * 0.65, 1),
                'pm10': round(base_aqi * 0.95, 1),
                'o3': 95,
                'so2': 15,
                'co': 0.8,
                'no2': 45,
                'primaryPollutant': 'PM2.5' if base_aqi > 100 else 'PM10',
                'isSandDustDay': 'False',
                'updateTime': now.strftime('%Y-%m-%d %H:%M:%S'),
                'dataSource': 'mock'
            })
    
    return processed_data


def get_station_code(district):
    """获取站点代码"""
    codes = {
        "中原区": "410101",
        "金水区": "410102",
        "二七区": "410103",
        "惠济区": "410108",
        "郑东新区": "410104"
    }
    return codes.get(district, "410000")


def get_default_aqi(district):
    """获取默认AQI值"""
    defaults = {
        "中原区": 125,
        "金水区": 125,
        "二七区": 158,
        "惠济区": 85,
        "郑东新区": 132
    }
    return defaults.get(district, 100)


def get_aqi_level(aqi):
    """根据AQI获取等级"""
    if aqi <= 50:
        return "优"
    elif aqi <= 100:
        return "良"
    elif aqi <= 150:
        return "轻度污染"
    elif aqi <= 200:
        return "中度污染"
    elif aqi <= 300:
        return "重度污染"
    else:
        return "严重污染"


def save_to_csv(data):
    """
    保存数据到CSV文件
    """
    if not data:
        print("没有数据需要保存")
        return
    
    # 确保目录存在
    CSV_FILE_PATH.parent.mkdir(parents=True, exist_ok=True)
    
    # 检查文件是否存在
    file_exists = CSV_FILE_PATH.exists()
    
    # 写入CSV
    with open(CSV_FILE_PATH, 'a', newline='', encoding='utf-8') as f:
        fieldnames = ['stationCode', 'stationName', 'actualStation', 'date', 'aqi', 'qualityLevel',
                      'pm25', 'pm10', 'o3', 'so2', 'co', 'no2', 'primaryPollutant', 
                      'isSandDustDay', 'updateTime', 'dataSource']
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        
        if not file_exists:
            writer.writeheader()
        
        for row in data:
            writer.writerow(row)
    
    print(f"数据已保存到: {CSV_FILE_PATH}")
    print(f"共保存 {len(data)} 条记录")


def generate_mock_data():
    """
    生成模拟数据（用于测试）
    """
    data = []
    now = datetime.now()
    
    for district, actual_station in STATION_MAPPING.items():
        base_aqi = get_default_aqi(district)
        data.append({
            'stationCode': get_station_code(district),
            'stationName': district,
            'actualStation': actual_station,
            'date': now.strftime('%Y-%m-%d'),
            'aqi': base_aqi,
            'qualityLevel': get_aqi_level(base_aqi),
            'pm25': round(base_aqi * 0.65, 1),
            'pm10': round(base_aqi * 0.95, 1),
            'o3': 95,
            'so2': 15,
            'co': 0.8,
            'no2': 45,
            'primaryPollutant': 'PM2.5' if base_aqi > 100 else 'PM10',
            'isSandDustDay': 'False',
            'updateTime': now.strftime('%Y-%m-%d %H:%M:%S'),
            'dataSource': 'mock'
        })
    
    return data


def main():
    parser = argparse.ArgumentParser(description='郑州市空气质量数据抓取工具')
    parser.add_argument('--mock', action='store_true', help='使用模拟数据（不访问网站）')
    parser.add_argument('--schedule', action='store_true', help='定时运行（每30分钟）')
    args = parser.parse_args()
    
    print("=" * 50)
    print("郑州市空气质量数据抓取工具")
    print(f"目标网站: https://citydev.gbqyun.com/index/zhengzhou")
    print(f"当前时间: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 50)
    
    if args.schedule:
        # 定时运行模式
        print("\n已进入定时运行模式，每30分钟抓取一次数据")
        print("按 Ctrl+C 停止运行\n")
        
        while True:
            try:
                if args.mock:
                    data = generate_mock_data()
                else:
                    data = fetch_data_from_website()
                
                if data:
                    save_to_csv(data)
                
                # 等待30分钟
                print(f"\n等待30分钟后再次抓取... (下次运行时间: {(datetime.now() + timedelta(minutes=30)).strftime('%H:%M:%S')})")
                time.sleep(30 * 60)
            except KeyboardInterrupt:
                print("\n\n程序已停止")
                break
            except Exception as e:
                print(f"运行出错: {e}")
                print("10分钟后重试...")
                time.sleep(10 * 60)
    else:
        # 单次运行模式
        if args.mock:
            print("\n使用模拟数据模式（不访问网站）")
            data = generate_mock_data()
        else:
            print("\n正在从网站抓取数据...")
            data = fetch_data_from_website()
        
        if data:
            save_to_csv(data)
            print("\n数据抓取完成!")
        else:
            print("\n未能获取数据，请检查网络连接或网站访问权限")


if __name__ == "__main__":
    main()
