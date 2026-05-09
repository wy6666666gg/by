"""
空气质量数据采集系统 - 配置文件
支持YAML配置文件和环境变量覆盖
"""

import os
from pathlib import Path
from typing import Dict, List, Optional
from dataclasses import dataclass, field
from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


# ============ 项目路径配置 ============
BASE_DIR = Path(__file__).parent.parent
DATA_DIR = BASE_DIR / "data"
LOG_DIR = BASE_DIR / "logs"
CONFIG_DIR = BASE_DIR / "config"

# 自动创建必要目录
for dir_path in [DATA_DIR, LOG_DIR]:
    dir_path.mkdir(parents=True, exist_ok=True)


@dataclass
class StationInfo:
    """监测站点信息"""
    code: str
    name: str
    district: Optional[str] = None
    lat: Optional[float] = None
    lon: Optional[float] = None


@dataclass
class WeatherStationInfo:
    """气象站点信息"""
    code: str
    name: str
    lat: float
    lon: float
    elevation: float


# ============ 郑州站点配置 ============
ZHENGZHOU_STATIONS: Dict[str, StationInfo] = {
    '410101': StationInfo('410101', '郑州郑纺机', '金水区', 34.78, 113.68),
    '410102': StationInfo('410102', '郑州银行学校', '金水区', 34.77, 113.69),
    '410103': StationInfo('410103', '郑州供水公司', '二七区', 34.75, 113.65),
    '410104': StationInfo('410104', '郑州烟厂', '二七区', 34.74, 113.67),
    '410105': StationInfo('410105', '郑州北站', '惠济区', 34.82, 113.62),
    '410106': StationInfo('410106', '郑州经开区管委', '经开区', 34.72, 113.75),
    '410107': StationInfo('410107', '郑州南区交通学校', '管城回族区', 34.73, 113.70),
    '410108': StationInfo('410108', '郑州东区CBD', '郑东新区', 34.76, 113.73),
    '410109': StationInfo('410109', '郑州白鹭湾', '中原区', 34.76, 113.60),
    '410110': StationInfo('410110', '郑州登封', '登封市', 34.46, 113.03),
    '410111': StationInfo('410111', '郑州新密', '新密市', 34.54, 113.39),
    '410112': StationInfo('410112', '郑州荥阳', '荥阳市', 34.79, 113.40),
    '410113': StationInfo('410113', '郑州中牟', '中牟县', 34.72, 114.02),
    '410114': StationInfo('410114', '郑州巩义', '巩义市', 34.76, 112.98),
    '410115': StationInfo('410115', '郑州新郑', '新郑市', 34.40, 113.73),
}

ZHENGZHOU_WEATHER_STATIONS: Dict[str, WeatherStationInfo] = {
    '54511': WeatherStationInfo('54511', '郑州', 34.72, 113.65, 110.4),
    '57073': WeatherStationInfo('57073', '巩义', 34.76, 112.98, 105.8),
    '57075': WeatherStationInfo('57075', '新密', 34.54, 113.39, 142.3),
    '57076': WeatherStationInfo('57076', '登封', 34.45, 113.02, 287.5),
    '57077': WeatherStationInfo('57077', '荥阳', 34.66, 113.35, 105.2),
    '57078': WeatherStationInfo('57078', '新郑', 34.43, 113.74, 98.7),
    '57079': WeatherStationInfo('57079', '中牟', 34.72, 114.02, 78.5),
}


# ============ AQI等级标准 ============
class AQILevel:
    """AQI等级定义"""
    EXCELLENT = (0, 50, '优', 1, '#00E400', '空气质量令人满意，基本无空气污染')
    GOOD = (51, 100, '良', 2, '#FFFF00', '空气质量可接受，某些污染物对极少数敏感人群健康有较弱影响')
    LIGHT = (101, 150, '轻度污染', 3, '#FF7E00', '易感人群症状有轻度加剧，健康人群出现刺激症状')
    MODERATE = (151, 200, '中度污染', 4, '#FF0000', '进一步加剧易感人群症状，可能对健康人群心脏、呼吸系统有影响')
    HEAVY = (201, 300, '重度污染', 5, '#99004C', '心脏病和肺病患者症状显著加剧，运动耐受力降低，健康人群普遍出现症状')
    SEVERE = (301, 999, '严重污染', 6, '#7E0023', '健康人群运动耐受力降低，有明显强烈症状，提前出现某些疾病')

    LEVELS = [EXCELLENT, GOOD, LIGHT, MODERATE, HEAVY, SEVERE]

    @classmethod
    def get_level(cls, aqi: int) -> tuple:
        """根据AQI值获取等级信息"""
        for low, high, name, level, color, advice in cls.LEVELS:
            if low <= aqi <= high:
                return name, level, color, advice
        return '严重污染', 6, '#7E0023', '健康人群运动耐受力降低，有明显强烈症状'


# ============ 污染物标准限值 ============
POLLUTANT_LIMITS = {
    # 24小时平均浓度限值 (μg/m³, CO为mg/m³)
    'pm25': {'name': 'PM2.5', 'unit': 'μg/m³', 'limits': [35, 75, 115, 150, 250, 500]},
    'pm10': {'name': 'PM10', 'unit': 'μg/m³', 'limits': [50, 150, 250, 350, 420, 600]},
    'so2': {'name': 'SO₂', 'unit': 'μg/m³', 'limits': [50, 150, 475, 800, 1600, 2620]},
    'no2': {'name': 'NO₂', 'unit': 'μg/m³', 'limits': [40, 80, 180, 280, 565, 940]},
    'co': {'name': 'CO', 'unit': 'mg/m³', 'limits': [2, 4, 14, 24, 36, 60]},
    'o3': {'name': 'O₃', 'unit': 'μg/m³', 'limits': [100, 160, 215, 265, 800, 1200]},
}

AQI_BREAKPOINTS = [0, 50, 100, 150, 200, 300, 500]


# ============ 应用配置 ============
class AppConfig(BaseSettings):
    """应用配置类，支持环境变量覆盖"""

    model_config = SettingsConfigDict(
        env_file='.env',
        env_file_encoding='utf-8',
        case_sensitive=False,
        extra='ignore'
    )

    # 应用信息
    app_name: str = Field(default="AirQualityCollector", description="应用名称")
    app_version: str = Field(default="1.0.0", description="应用版本")
    debug: bool = Field(default=False, description="调试模式")

    # 数据采集配置
    api_base_url: str = Field(default="http://www.cnemc.cn", description="空气质量API基础URL")
    api_timeout: int = Field(default=30, description="API请求超时(秒)")
    request_retry_times: int = Field(default=3, description="请求重试次数")
    request_retry_delay: float = Field(default=1.0, description="重试间隔(秒)")
    request_rate_limit: float = Field(default=0.5, description="请求频率限制(秒/请求)")

    # 数据存储配置
    output_format: str = Field(default="both", description="输出格式: json/csv/both")
    output_dir: Path = Field(default=DATA_DIR, description="输出目录")
    log_dir: Path = Field(default=LOG_DIR, description="日志目录")

    # 数据库配置
    db_host: str = Field(default="localhost", description="数据库主机")
    db_port: int = Field(default=3306, description="数据库端口")
    db_name: str = Field(default="air_quality_db", description="数据库名称")
    db_user: str = Field(default="root", description="数据库用户")
    db_password: str = Field(default="", description="数据库密码")

    # Redis配置
    redis_host: str = Field(default="localhost", description="Redis主机")
    redis_port: int = Field(default=6379, description="Redis端口")
    redis_password: Optional[str] = Field(default=None, description="Redis密码")
    redis_db: int = Field(default=0, description="Redis数据库")

    @field_validator('output_dir', 'log_dir', mode='before')
    @classmethod
    def validate_path(cls, v):
        if isinstance(v, str):
            path = Path(v)
            path.mkdir(parents=True, exist_ok=True)
            return path
        return v


# 全局配置实例
settings = AppConfig()
