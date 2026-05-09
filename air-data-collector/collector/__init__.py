"""数据采集模块"""
from collector.base_crawler import BaseCrawler, CrawlerStats
from collector.air_station_crawler import AirQualityCollector, AirQualityData
from collector.weather_crawler import WeatherCollector, WeatherData

__all__ = [
    'BaseCrawler',
    'CrawlerStats',
    'AirQualityCollector',
    'AirQualityData',
    'WeatherCollector',
    'WeatherData'
]
