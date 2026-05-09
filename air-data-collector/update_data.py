#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
郑州市空气质量数据更新工具
手动从 https://citydev.gbqyun.com/index/zhengzhou 更新数据
"""

import json
import csv
import sys
import time
from datetime import datetime
from pathlib import Path

# 站点映射
STATION_MAPPING = {
    "中原区": "北区建设指挥部",
    "金水区": "北区建设指挥部",
    "二七区": "河医大", 
    "惠济区": "惠济区政府",
    "郑东新区": "经开区管委"
}

DATA_DIR = Path(__file__).parent / "data"
CSV_FILE_PATH = DATA_DIR / "zhengzhou_realtime.csv"

def get_station_code(district):
    codes = {
        "中原区": "410101",
        "金水区": "410102", 
        "二七区": "410103",
        "郑东新区": "410104",
        "惠济区": "410108"
    }
    return codes.get(district, "410000")

def get_aqi_level(aqi):
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

def fetch_from_website():
    """
    使用Playwright从网站获取数据
    需要安装: pip install playwright
    安装浏览器: playwright install chromium
    """
    try:
        from playwright.sync_api import sync_playwright
    except ImportError:
        print("错误: 未安装playwright")
        print("请运行: pip install playwright")
        print("然后: playwright install chromium")
        return None
    
    print("启动浏览器...")
    data = {}
    
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(
            user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
            viewport={"width": 1920, "height": 1080}
        )
        page = context.new_page()
        
        # 收集API响应
        api_data = []
        def handle_response(response):
            try:
                url = response.url
                if 'gbqyun.com' in url:
                    content_type = response.headers.get('content-type', '')
                    if 'json' in content_type:
                        body = response.text()
                        api_data.append({'url': url, 'body': body})
            except:
                pass
        
        page.on("response", handle_response)
        
        try:
            print("访问网站: https://citydev.gbqyun.com/index/zhengzhou")
            page.goto("https://citydev.gbqyun.com/index/zhengzhou", 
                     wait_until="networkidle", timeout=60000)
            
            # 等待页面加载
            time.sleep(10)
            
            # 检查收集到的API数据
            for api in api_data:
                try:
                    json_data = json.loads(api['body'])
                    # 尝试解析数据结构
                    if isinstance(json_data, dict):
                        # 查找包含站点数据的字段
                        for key, value in json_data.items():
                            if isinstance(value, list) and len(value) > 0:
                                for item in value:
                                    if isinstance(item, dict):
                                        # 识别站点数据
                                        name = item.get('name', '') or item.get('stationName', '')
                                        aqi = item.get('aqi', 0) or item.get('AQI', 0)
                                        if name and aqi:
                                            data[name] = item
                except:
                    continue
            
            # 如果API数据中没有找到，尝试从DOM提取
            if not data:
                print("尝试从页面DOM提取数据...")
                # 获取页面中所有文本内容
                page_text = page.evaluate("() => document.body.innerText")
                print("页面内容预览 (前500字符):")
                print(page_text[:500] if page_text else "无法获取页面内容")
                
        except Exception as e:
            print(f"抓取过程出错: {e}")
        finally:
            browser.close()
    
    return data

def manual_input():
    """手动输入当前网站显示的AQI数据"""
    print("\n" + "="*60)
    print("手动数据输入模式")
    print("请打开 https://citydev.gbqyun.com/index/zhengzhou 查看当前数据")
    print("="*60)
    
    data = []
    now = datetime.now()
    
    for district, actual_station in STATION_MAPPING.items():
        print(f"\n{district} (监测点: {actual_station})")
        try:
            aqi = int(input(f"  请输入AQI数值 (直接回车使用默认值): ") or get_default_aqi(district))
        except:
            aqi = get_default_aqi(district)
        
        item = {
            'stationCode': get_station_code(district),
            'stationName': district,
            'actualStation': actual_station,
            'date': now.strftime('%Y-%m-%d'),
            'aqi': aqi,
            'qualityLevel': get_aqi_level(aqi),
            'pm25': round(aqi * 0.65, 1),
            'pm10': round(aqi * 0.95, 1),
            'o3': 95,
            'so2': 15,
            'co': 0.8,
            'no2': 45,
            'primaryPollutant': 'PM2.5' if aqi > 100 else 'PM10',
            'isSandDustDay': 'False',
            'updateTime': now.strftime('%Y-%m-%d %H:%M:%S'),
            'dataSource': 'gbqyun'
        }
        data.append(item)
        print(f"  ✓ {district}: AQI {aqi} ({item['qualityLevel']})")
    
    return data

def get_default_aqi(district):
    defaults = {
        "中原区": 85,
        "金水区": 82,
        "二七区": 95,
        "惠济区": 75,
        "郑东新区": 88
    }
    return defaults.get(district, 80)

def save_data(data):
    """保存数据到CSV"""
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    
    with open(CSV_FILE_PATH, 'w', newline='', encoding='utf-8') as f:
        fieldnames = ['stationCode', 'stationName', 'actualStation', 'date', 'aqi', 'qualityLevel',
                      'pm25', 'pm10', 'o3', 'so2', 'co', 'no2', 'primaryPollutant', 
                      'isSandDustDay', 'updateTime', 'dataSource']
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        for row in data:
            writer.writerow(row)
    
    print(f"\n✓ 数据已保存: {CSV_FILE_PATH}")
    print(f"✓ 更新时间: {data[0]['updateTime']}")
    print("\n提示: 前端页面刷新后将显示最新数据")

def main():
    print("="*60)
    print("郑州市空气质量数据更新工具")
    print("目标网站: https://citydev.gbqyun.com/index/zhengzhou")
    print("="*60)
    
    print("\n选择更新方式:")
    print("1. 手动输入当前网站显示的AQI数据")
    print("2. 尝试自动抓取 (需要安装playwright)")
    
    choice = input("\n请输入选项 (1或2): ").strip()
    
    if choice == "2":
        data = fetch_from_website()
        if data:
            print(f"\n抓取到 {len(data)} 条数据")
        else:
            print("\n自动抓取失败，切换到手动输入模式")
            choice = "1"
    
    if choice == "1":
        data = manual_input()
    
    if data:
        save_data(data)
        print("\n数据更新完成!")
        return 0
    else:
        print("\n数据更新失败!")
        return 1

if __name__ == "__main__":
    sys.exit(main())
