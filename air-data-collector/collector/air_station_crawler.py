"""
郑州市空气质量监测站点数据采集模块（优化版）
支持真实API接入和模拟数据生成
"""

import random
from dataclasses import dataclass, asdict
from datetime import datetime
from typing import List, Dict, Optional, Any, Tuple
from pathlib import Path

import requests
from loguru import logger

from config.settings import (
    settings, ZHENGZHOU_STATIONS, POLLUTANT_LIMITS,
    AQI_BREAKPOINTS, AQILevel
)
from collector.base_crawler import BaseCrawler


@dataclass
class AirQualityData:
    """空气质量数据结构"""
    station_code: str
    station_name: str
    monitor_time: str
    pm25: float
    pm10: float
    so2: float
    no2: float
    co: float
    o3: float
    aqi: int = 0
    primary_pollutant: str = ""
    quality_level: str = ""
    aqi_level_id: int = 0
    latitude: Optional[float] = None
    longitude: Optional[float] = None

    def to_dict(self) -> Dict[str, Any]:
        return asdict(self)


class AirQualityCollector(BaseCrawler):
    """空气质量数据采集器（优化版）"""

    # API端点配置
    API_ENDPOINTS = {
        'cnemc': 'http://www.cnemc.cn/sssj/index.jsp',
        'aqicn': 'https://api.waqi.info/feed',
    }

    def __init__(self, output_dir: Path = None, use_mock: bool = True):
        super().__init__(output_dir=output_dir, name="air_quality")
        self.use_mock = use_mock
        self.stations = ZHENGZHOU_STATIONS

    def calculate_iaqi(self, concentration: float, pollutant: str) -> int:
        """
        计算单项污染物的IAQI（Individual AQI）
        依据《环境空气质量指数(AQI)技术规定》HJ 633-2012
        """
        if pollutant not in POLLUTANT_LIMITS:
            return 0

        limits = POLLUTANT_LIMITS[pollutant]['limits']
        breakpoints = AQI_BREAKPOINTS

        # 找到对应区间
        for i, limit in enumerate(limits):
            if concentration <= limit:
                # 线性插值公式: I = (I_high - I_low) / (C_high - C_low) * (C - C_low) + I_low
                c_low = 0 if i == 0 else limits[i - 1]
                c_high = limit
                i_low = breakpoints[i]
                i_high = breakpoints[i + 1]

                iaqi = (i_high - i_low) / (c_high - c_low) * (concentration - c_low) + i_low
                return max(0, int(iaqi))

        return 500  # 超过最高限值

    def calculate_aqi(self, data: Dict[str, float]) -> Tuple[int, str]:
        """
        计算综合AQI和首要污染物
        返回: (AQI值, 首要污染物)
        """
        iaqis = {}

        # 计算各项污染物的IAQI
        for pollutant in ['pm25', 'pm10', 'so2', 'no2', 'co', 'o3']:
            value = data.get(pollutant, 0)
            iaqis[pollutant] = self.calculate_iaqi(value, pollutant)

        # AQI取最大值
        max_aqi = max(iaqis.values()) if iaqis else 0

        # 确定首要污染物（IAQI最大的，且需大于50）
        primary = ""
        if max_aqi > 50:
            primary_candidates = [
                (k, v) for k, v in iaqis.items() if v == max_aqi
            ]
            if primary_candidates:
                pollutant_code = primary_candidates[0][0]
                primary = POLLUTANT_LIMITS.get(pollutant_code, {}).get('name', pollutant_code.upper())

        return max_aqi, primary

    def generate_mock_data(self, station_code: str) -> Optional[AirQualityData]:
        """生成模拟数据（用于测试和演示）"""
        station = self.stations.get(station_code)
        if not station:
            logger.warning(f"未知站点: {station_code}")
            return None

        timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S')

        # 使用随机种子确保相同时间点相同站点的数据一致
        seed = hash(f"{station_code}{timestamp[:13]}")  # 精确到小时
        random.seed(seed)

        # 生成基础浓度值（模拟真实波动范围）
        base_data = {
            'pm25': random.uniform(15, 120),
            'pm10': random.uniform(30, 180),
            'so2': random.uniform(5, 60),
            'no2': random.uniform(15, 80),
            'co': random.uniform(0.3, 2.5),
            'o3': random.uniform(30, 160),
        }

        # 根据时段调整（模拟早晚高峰）
        hour = datetime.now().hour
        if 7 <= hour <= 9 or 17 <= hour <= 19:  # 交通高峰
            base_data['no2'] *= 1.3
            base_data['pm25'] *= 1.2

        # 计算AQI
        aqi, primary = self.calculate_aqi(base_data)

        # 获取等级信息
        level_name, level_id, color, advice = AQILevel.get_level(aqi)

        return AirQualityData(
            station_code=station_code,
            station_name=station.name,
            monitor_time=timestamp,
            pm25=round(base_data['pm25'], 1),
            pm10=round(base_data['pm10'], 1),
            so2=round(base_data['so2'], 1),
            no2=round(base_data['no2'], 1),
            co=round(base_data['co'], 2),
            o3=round(base_data['o3'], 1),
            aqi=aqi,
            primary_pollutant=primary if primary else "无",
            quality_level=level_name,
            aqi_level_id=level_id,
            latitude=station.lat,
            longitude=station.lon
        )

    def fetch_real_data(self, station_code: str) -> Optional[AirQualityData]:
        """
        从真实API获取数据
        注: 需要接入实际API，此处为框架实现
        """
        # TODO: 实现真实API调用
        # 示例:
        # url = f"{settings.api_base_url}/api/data/{station_code}"
        # data = self.fetch_data(url)
        # if data:
        #     return self._parse_api_response(data, station_code)

        logger.debug(f"真实API未配置，使用模拟数据: {station_code}")
        return self.generate_mock_data(station_code)

    def _parse_api_response(self, data: Dict, station_code: str) -> AirQualityData:
        """解析API响应数据"""
        station = self.stations.get(station_code, {})
        return AirQualityData(
            station_code=station_code,
            station_name=station.get('name', '未知站点'),
            monitor_time=data.get('time', datetime.now().isoformat()),
            pm25=float(data.get('pm25', 0)),
            pm10=float(data.get('pm10', 0)),
            so2=float(data.get('so2', 0)),
            no2=float(data.get('no2', 0)),
            co=float(data.get('co', 0)),
            o3=float(data.get('o3', 0)),
            aqi=int(data.get('aqi', 0)),
            primary_pollutant=data.get('primary', ''),
            quality_level=data.get('quality', ''),
        )

    def collect_station(self, station_code: str) -> Optional[AirQualityData]:
        """采集单个站点数据"""
        try:
            logger.info(f"采集站点: {self.stations.get(station_code, {}).name} ({station_code})")

            if self.use_mock:
                data = self.generate_mock_data(station_code)
            else:
                data = self.fetch_real_data(station_code)

            if data:
                logger.debug(f"站点 {station_code} 数据采集成功: AQI={data.aqi}")
            return data

        except Exception as e:
            logger.error(f"采集站点 {station_code} 失败: {e}")
            self.stats.errors.append(f"{station_code}: {str(e)}")
            return None

    def collect(self) -> List[Dict[str, Any]]:
        """采集所有站点数据"""
        results = []

        logger.info(f"开始采集 {len(self.stations)} 个站点数据...")

        for i, station_code in enumerate(self.stations.keys(), 1):
            logger.info(f"[{i}/{len(self.stations)}] 正在采集...")
            data = self.collect_station(station_code)
            if data:
                results.append(data.to_dict())

        logger.info(f"采集完成: {len(results)}/{len(self.stations)} 个站点成功")
        return results

    def get_statistics(self, data: List[Dict]) -> Dict[str, Any]:
        """生成数据统计信息"""
        if not data:
            return {}

        import pandas as pd
        df = pd.DataFrame(data)

        return {
            'total_stations': len(data),
            'avg_aqi': round(df['aqi'].mean(), 2),
            'max_aqi': int(df['aqi'].max()),
            'min_aqi': int(df['aqi'].min()),
            'quality_distribution': df['quality_level'].value_counts().to_dict(),
            'primary_pollutants': df['primary_pollutant'].value_counts().to_dict(),
            'avg_pm25': round(df['pm25'].mean(), 2),
            'avg_pm10': round(df['pm10'].mean(), 2),
        }


def main():
    """主函数"""
    print("=" * 60)
    print("郑州市空气质量数据采集系统")
    print("=" * 60)

    with AirQualityCollector(use_mock=True) as collector:
        report = collector.run()

        # 输出统计信息
        print("\n" + "=" * 60)
        print("采集统计")
        print("=" * 60)
        stats = report['statistics']
        print(f"总请求数: {stats['total_requests']}")
        print(f"成功请求: {stats['success_requests']}")
        print(f"失败请求: {stats['failed_requests']}")
        print(f"成功率: {stats['success_rate']}")
        print(f"耗时: {stats['duration_seconds']:.2f}秒")
        print(f"数据量: {stats['records_collected']} 条")

    print("\n" + "=" * 60)
    print("数据采集完成！")
    print("=" * 60)


if __name__ == '__main__':
    main()
