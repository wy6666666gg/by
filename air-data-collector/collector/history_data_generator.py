"""
郑州市空气质量历史数据生成器
生成五个站点近五年(2020-2024)的每日监测数据
包含：AQI、空气质量等级、PM2.5、PM10、O3、SO2、CO、NO2、首要污染物、沙尘扣除标识
"""

import random
import pandas as pd
from datetime import datetime, timedelta
from pathlib import Path
from dataclasses import dataclass, asdict
from typing import List, Dict, Optional, Tuple
from loguru import logger

import sys
sys.path.insert(0, str(Path(__file__).parent.parent))

from config.settings import POLLUTANT_LIMITS, AQI_BREAKPOINTS, AQILevel


@dataclass
class DailyAirQualityData:
    """每日空气质量数据结构"""
    station_code: str
    station_name: str
    date: str
    aqi: int
    quality_level: str
    pm25: float
    pm10: float
    o3: float
    so2: float
    co: float
    no2: float
    primary_pollutant: str
    is_sand_dust_day: bool  # 是否为沙尘扣除天
    
    def to_dict(self) -> Dict:
        return asdict(self)


# 郑州市五个主要站点
ZHENGZHOU_5_STATIONS = {
    '410101': {'name': '郑州郑纺机', 'district': '金水区', 'lat': 34.78, 'lon': 113.68},
    '410102': {'name': '郑州银行学校', 'district': '金水区', 'lat': 34.77, 'lon': 113.69},
    '410103': {'name': '郑州供水公司', 'district': '二七区', 'lat': 34.75, 'lon': 113.65},
    '410105': {'name': '郑州北站', 'district': '惠济区', 'lat': 34.82, 'lon': 113.62},
    '410108': {'name': '郑州东区CBD', 'district': '郑东新区', 'lat': 34.76, 'lon': 113.73},
}


class HistoryDataGenerator:
    """历史数据生成器"""
    
    def __init__(self):
        self.stations = ZHENGZHOU_5_STATIONS
        
    def calculate_iaqi(self, concentration: float, pollutant: str) -> int:
        """计算单项污染物的IAQI"""
        if pollutant not in POLLUTANT_LIMITS:
            return 0
        
        limits = POLLUTANT_LIMITS[pollutant]['limits']
        breakpoints = AQI_BREAKPOINTS
        
        for i, limit in enumerate(limits):
            if concentration <= limit:
                c_low = 0 if i == 0 else limits[i - 1]
                c_high = limit
                i_low = breakpoints[i]
                i_high = breakpoints[i + 1]
                
                iaqi = (i_high - i_low) / (c_high - c_low) * (concentration - c_low) + i_low
                return max(0, int(iaqi))
        return 500
    
    def calculate_aqi(self, data: Dict[str, float]) -> Tuple[int, str]:
        """计算综合AQI和首要污染物"""
        iaqis = {}
        for pollutant in ['pm25', 'pm10', 'so2', 'no2', 'co', 'o3']:
            value = data.get(pollutant, 0)
            iaqis[pollutant] = self.calculate_iaqi(value, pollutant)
        
        max_aqi = max(iaqis.values()) if iaqis else 0
        
        primary = ""
        if max_aqi > 50:
            primary_candidates = [(k, v) for k, v in iaqis.items() if v == max_aqi]
            if primary_candidates:
                pollutant_code = primary_candidates[0][0]
                primary = POLLUTANT_LIMITS.get(pollutant_code, {}).get('name', pollutant_code.upper())
        
        return max_aqi, primary
    
    def get_seasonal_factor(self, month: int) -> Dict[str, float]:
        """获取季节性因子"""
        # 郑州的季节性特征
        if month in [12, 1, 2]:  # 冬季 - 采暖期，污染较重
            return {'pm25': 1.8, 'pm10': 1.4, 'so2': 1.3, 'no2': 1.2, 'co': 1.3, 'o3': 0.6}
        elif month in [3, 4, 5]:  # 春季 - 沙尘天气多
            return {'pm25': 1.1, 'pm10': 1.8, 'so2': 0.9, 'no2': 0.95, 'co': 0.9, 'o3': 1.2}
        elif month in [6, 7, 8]:  # 夏季 - O3高，PM低
            return {'pm25': 0.6, 'pm10': 0.7, 'so2': 0.6, 'no2': 0.7, 'co': 0.6, 'o3': 1.8}
        else:  # 秋季 - 相对较好
            return {'pm25': 1.0, 'pm10': 1.0, 'so2': 0.8, 'no2': 0.9, 'co': 0.8, 'o3': 0.9}
    
    def is_sand_dust_day(self, date: datetime) -> bool:
        """判断是否为沙尘天（春季3-5月概率较高）"""
        if date.month in [3, 4, 5]:
            # 春季沙尘天概率约5%
            return random.random() < 0.05
        return False
    
    def generate_daily_data(self, station_code: str, date: datetime) -> DailyAirQualityData:
        """生成单日的空气质量数据"""
        station = self.stations[station_code]
        
        # 使用固定种子保证数据可重现性
        seed = int(date.strftime('%Y%m%d')) + int(station_code)
        random.seed(seed)
        
        # 获取季节性因子
        factors = self.get_seasonal_factor(date.month)
        
        # 判断是否沙尘天
        sand_dust = self.is_sand_dust_day(date)
        
        # 基础浓度值（基于郑州历史数据均值）
        base_values = {
            'pm25': random.uniform(25, 75),
            'pm10': random.uniform(50, 120),
            'so2': random.uniform(8, 35),
            'no2': random.uniform(20, 55),
            'co': random.uniform(0.6, 1.8),
            'o3': random.uniform(40, 120),
        }
        
        # 应用季节因子
        for pollutant in base_values:
            base_values[pollutant] *= factors.get(pollutant, 1.0)
        
        # 沙尘天PM10显著升高
        if sand_dust:
            base_values['pm10'] *= random.uniform(2.5, 4.0)
            base_values['pm25'] *= random.uniform(1.5, 2.0)
        
        # 添加年际改善趋势（2020-2024年污染逐渐降低）
        year_factor = 1.0 - (date.year - 2020) * 0.05  # 每年改善约5%
        for pollutant in ['pm25', 'pm10', 'so2', 'no2']:
            base_values[pollutant] *= year_factor
        
        # 添加随机波动
        for pollutant in base_values:
            noise = random.uniform(0.8, 1.2)
            base_values[pollutant] *= noise
        
        # 计算AQI
        aqi, primary = self.calculate_aqi(base_values)
        
        # 获取空气质量等级
        level_name, _, _, _ = AQILevel.get_level(aqi)
        
        return DailyAirQualityData(
            station_code=station_code,
            station_name=station['name'],
            date=date.strftime('%Y-%m-%d'),
            aqi=aqi,
            quality_level=level_name,
            pm25=round(base_values['pm25'], 1),
            pm10=round(base_values['pm10'], 1),
            o3=round(base_values['o3'], 1),
            so2=round(base_values['so2'], 1),
            co=round(base_values['co'], 2),
            no2=round(base_values['no2'], 1),
            primary_pollutant=primary if primary else "无",
            is_sand_dust_day=sand_dust
        )
    
    def generate_history_data(self, start_year: int = 2020, end_year: int = 2024) -> pd.DataFrame:
        """生成历史数据"""
        logger.info(f"开始生成 {start_year}-{end_year} 年历史数据...")
        
        all_data = []
        
        for station_code in self.stations.keys():
            logger.info(f"生成站点 {self.stations[station_code]['name']} 的数据...")
            
            current_date = datetime(start_year, 1, 1)
            end_date = datetime(end_year, 12, 31)
            
            while current_date <= end_date:
                daily_data = self.generate_daily_data(station_code, current_date)
                all_data.append(daily_data.to_dict())
                current_date += timedelta(days=1)
        
        df = pd.DataFrame(all_data)
        logger.info(f"数据生成完成: 共 {len(df)} 条记录")
        return df
    
    def save_data(self, df: pd.DataFrame, output_dir: Path = None):
        """保存数据到文件"""
        if output_dir is None:
            output_dir = Path(__file__).parent.parent / "data"
        output_dir.mkdir(parents=True, exist_ok=True)
        
        # 保存为CSV
        csv_path = output_dir / "zhengzhou_5stations_5years_daily.csv"
        df.to_csv(csv_path, index=False, encoding='utf-8-sig')
        logger.info(f"CSV数据已保存: {csv_path}")
        
        # 保存为Excel（分sheet）
        excel_path = output_dir / "zhengzhou_5stations_5years_daily.xlsx"
        with pd.ExcelWriter(excel_path, engine='openpyxl') as writer:
            # 全部数据
            df.to_excel(writer, sheet_name='全部数据', index=False)
            
            # 按站点分sheet
            for station_code in df['station_code'].unique():
                station_df = df[df['station_code'] == station_code]
                sheet_name = self.stations[station_code]['name'][:10]  # Excel sheet名长度限制
                station_df.to_excel(writer, sheet_name=sheet_name, index=False)
        
        logger.info(f"Excel数据已保存: {excel_path}")
        
        # 生成统计报告
        self.generate_report(df, output_dir)
    
    def generate_report(self, df: pd.DataFrame, output_dir: Path):
        """生成数据统计报告"""
        report_lines = []
        report_lines.append("=" * 80)
        report_lines.append("郑州市空气质量历史数据统计报告")
        report_lines.append("=" * 80)
        report_lines.append(f"数据时间范围: {df['date'].min()} 至 {df['date'].max()}")
        report_lines.append(f"站点数量: {df['station_code'].nunique()}")
        report_lines.append(f"总记录数: {len(df)}")
        report_lines.append("")
        
        # 各站点统计
        report_lines.append("-" * 80)
        report_lines.append("各站点统计")
        report_lines.append("-" * 80)
        
        for station_code in df['station_code'].unique():
            station_df = df[df['station_code'] == station_code]
            station_name = station_df['station_name'].iloc[0]
            
            report_lines.append(f"\n【{station_name}】")
            report_lines.append(f"  记录数: {len(station_df)}")
            report_lines.append(f"  AQI均值: {station_df['aqi'].mean():.1f}")
            report_lines.append(f"  AQI范围: {station_df['aqi'].min()} - {station_df['aqi'].max()}")
            report_lines.append(f"  PM2.5均值: {station_df['pm25'].mean():.1f} μg/m³")
            report_lines.append(f"  PM10均值: {station_df['pm10'].mean():.1f} μg/m³")
            report_lines.append(f"  O3均值: {station_df['o3'].mean():.1f} μg/m³")
            
            # 空气质量等级分布
            report_lines.append("  空气质量等级分布:")
            level_counts = station_df['quality_level'].value_counts()
            for level, count in level_counts.items():
                pct = count / len(station_df) * 100
                report_lines.append(f"    {level}: {count}天 ({pct:.1f}%)")
            
            # 首要污染物统计
            report_lines.append("  首要污染物统计:")
            primary_counts = station_df[station_df['primary_pollutant'] != '无']['primary_pollutant'].value_counts()
            for pollutant, count in primary_counts.head(3).items():
                report_lines.append(f"    {pollutant}: {count}天")
            
            # 沙尘天统计
            sand_days = station_df['is_sand_dust_day'].sum()
            report_lines.append(f"  沙尘天数量: {int(sand_days)}天")
        
        # 整体统计
        report_lines.append("\n" + "=" * 80)
        report_lines.append("整体统计")
        report_lines.append("=" * 80)
        report_lines.append(f"全市AQI年均值变化:")
        df['year'] = pd.to_datetime(df['date']).dt.year
        yearly_avg = df.groupby('year')['aqi'].mean()
        for year, avg_aqi in yearly_avg.items():
            report_lines.append(f"  {year}年: {avg_aqi:.1f}")
        
        report_lines.append("\n" + "=" * 80)
        
        report_text = "\n".join(report_lines)
        report_path = output_dir / "data_report.txt"
        with open(report_path, 'w', encoding='utf-8') as f:
            f.write(report_text)
        
        logger.info(f"统计报告已保存: {report_path}")
        print("\n" + report_text)


def main():
    """主函数"""
    print("=" * 80)
    print("郑州市五个站点近五年空气质量历史数据生成器")
    print("=" * 80)
    print("\n站点列表:")
    for code, info in ZHENGZHOU_5_STATIONS.items():
        print(f"  {code}: {info['name']} ({info['district']})")
    print("\n")
    
    generator = HistoryDataGenerator()
    df = generator.generate_history_data(2020, 2024)
    generator.save_data(df)
    
    print("\n" + "=" * 80)
    print("数据生成完成！")
    print("=" * 80)


if __name__ == '__main__':
    main()
