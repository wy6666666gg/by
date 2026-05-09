"""
气象数据采集模块（优化版）
"""

import random
from dataclasses import dataclass, asdict
from datetime import datetime
from typing import List, Dict, Optional, Any
from pathlib import Path

from loguru import logger

from config.settings import ZHENGZHOU_WEATHER_STATIONS
from collector.base_crawler import BaseCrawler


@dataclass
class WeatherData:
    """气象数据结构"""
    station_code: str
    station_name: str
    latitude: float
    longitude: float
    elevation: float
    monitor_time: str
    temperature: float  # 温度(℃)
    humidity: float     # 湿度(%)
    wind_speed: float   # 风速(m/s)
    wind_direction: int # 风向角度(0-360)
    wind_direction_name: str = ""
    wind_speed_level: int = 0
    pressure: float     # 气压(hPa)
    visibility: float   # 能见度(km)
    weather_type: str = ""

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


class WeatherCollector(BaseCrawler):
    """气象数据采集器（优化版）"""

    # 风向映射
    WIND_DIRECTIONS = ['北', '东北', '东', '东南', '南', '西南', '西', '西北']

    # 天气类型
    WEATHER_TYPES = ['晴', '多云', '阴', '小雨', '中雨', '大雨', '雾', '霾']

    def __init__(self, output_dir: Path = None, use_mock: bool = True):
        super().__init__(output_dir=output_dir, name="weather")
        self.use_mock = use_mock
        self.stations = ZHENGZHOU_WEATHER_STATIONS

    def get_wind_direction_name(self, degree: int) -> str:
        """将角度转换为风向名称"""
        index = int((degree + 22.5) / 45) % 8
        return self.WIND_DIRECTIONS[index]

    def get_wind_speed_level(self, speed: float) -> int:
        """根据风速判断风力等级（蒲福风级）"""
        levels = [
            (0.0, 0.2, 0),   # 无风
            (0.3, 1.5, 1),   # 软风
            (1.6, 3.3, 2),   # 轻风
            (3.4, 5.4, 3),   # 微风
            (5.5, 7.9, 4),   # 和风
            (8.0, 10.7, 5),  # 清风
            (10.8, 13.8, 6), # 强风
            (13.9, 17.1, 7), # 疾风
            (17.2, 20.7, 8), # 大风
            (20.8, 24.4, 9), # 烈风
        ]
        for low, high, level in levels:
            if low <= speed <= high:
                return level
        return 10  # 狂风及以上

    def generate_mock_data(self, station_code: str) -> Optional[WeatherData]:
        """生成模拟气象数据"""
        station = self.stations.get(station_code)
        if not station:
            logger.warning(f"未知气象站点: {station_code}")
            return None

        timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')

        # 基于季节和时段生成合理的气象数据
        month = datetime.now().month
        hour = datetime.now().hour

        # 季节温度基准
        if month in [12, 1, 2]:      # 冬季
            base_temp = random.uniform(-5, 8)
        elif month in [3, 4, 5]:     # 春季
            base_temp = random.uniform(10, 25)
        elif month in [6, 7, 8]:     # 夏季
            base_temp = random.uniform(25, 38)
        else:                         # 秋季
            base_temp = random.uniform(12, 22)

        # 昼夜温差调整
        if 6 <= hour < 18:  # 白天
            base_temp += random.uniform(0, 5)
        else:  # 夜间
            base_temp -= random.uniform(3, 8)

        wind_dir = random.randint(0, 360)
        wind_speed = random.uniform(0, 12)

        return WeatherData(
            station_code=station_code,
            station_name=station.name,
            latitude=station.lat,
            longitude=station.lon,
            elevation=station.elevation,
            monitor_time=timestamp,
            temperature=round(base_temp, 1),
            humidity=round(random.uniform(30, 90), 1),
            wind_speed=round(wind_speed, 1),
            wind_direction=wind_dir,
            wind_direction_name=self.get_wind_direction_name(wind_dir),
            wind_speed_level=self.get_wind_speed_level(wind_speed),
            pressure=round(random.uniform(990, 1030), 1),
            visibility=round(random.uniform(2, 20), 1),
            weather_type=random.choice(self.WEATHER_TYPES)
        )

    def fetch_real_data(self, station_code: str) -> Optional[WeatherData]:
        """从真实API获取气象数据"""
        # TODO: 接入真实气象API
        logger.debug(f"真实API未配置，使用模拟数据: {station_code}")
        return self.generate_mock_data(station_code)

    def collect_station(self, station_code: str) -> Optional[WeatherData]:
        """采集单个站点数据"""
        try:
            station = self.stations.get(station_code)
            logger.info(f"采集气象数据: {station.name} ({station_code})")

            if self.use_mock:
                data = self.generate_mock_data(station_code)
            else:
                data = self.fetch_real_data(station_code)

            return data

        except Exception as e:
            logger.error(f"采集气象站点 {station_code} 失败: {e}")
            self.stats.errors.append(f"{station_code}: {str(e)}")
            return None

    def collect(self) -> List[Dict[str, Any]]:
        """采集所有气象站点数据"""
        results = []

        logger.info(f"开始采集 {len(self.stations)} 个气象站点数据...")

        for i, station_code in enumerate(self.stations.keys(), 1):
            logger.info(f"[{i}/{len(self.stations)}] 正在采集...")
            data = self.collect_station(station_code)
            if data:
                results.append(data.to_dict())

        logger.info(f"气象数据采集完成: {len(results)}/{len(self.stations)} 个站点成功")
        return results


def main():
    """主函数"""
    print("=" * 60)
    print("气象数据采集系统")
    print("=" * 60)

    with WeatherCollector(use_mock=True) as collector:
        report = collector.run()

        print("\n" + "=" * 60)
        print("采集完成")
        print("=" * 60)


if __name__ == '__main__':
    main()
