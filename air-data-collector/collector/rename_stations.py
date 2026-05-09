"""
修改站点名称为区县名称
将五个站点分别对应到：中原区、金水区、二七区、惠济区、郑东新区
"""

import pandas as pd
from pathlib import Path

# 站点代码到区县的映射
STATION_MAPPING = {
    '410101': '中原区',
    '410102': '金水区',
    '410103': '二七区',
    '410105': '惠济区',
    '410108': '郑东新区',
}

def rename_stations():
    data_dir = Path(__file__).parent.parent / "data"
    
    # 读取CSV，将station_code转为字符串
    csv_path = data_dir / "zhengzhou_5stations_5years_daily.csv"
    df = pd.read_csv(csv_path, encoding='utf-8', dtype={'station_code': str})
    
    print("原始站点分布:")
    print(df.groupby(['station_code', 'station_name']).size())
    print()
    
    # 替换站点名称
    for code, district in STATION_MAPPING.items():
        df.loc[df['station_code'] == code, 'station_name'] = district
    
    print("修改后站点分布:")
    print(df.groupby(['station_code', 'station_name']).size())
    print()
    
    # 保存CSV
    csv_output = data_dir / "zhengzhou_districts_5years_daily.csv"
    df.to_csv(csv_output, index=False, encoding='utf-8-sig')
    print(f"CSV已保存: {csv_output}")
    
    # 保存Excel（分sheet）
    excel_output = data_dir / "zhengzhou_districts_5years_daily.xlsx"
    with pd.ExcelWriter(excel_output, engine='openpyxl') as writer:
        # 全部数据
        df.to_excel(writer, sheet_name='全部数据', index=False)
        
        # 按区县分sheet
        for code, district in STATION_MAPPING.items():
            district_df = df[df['station_code'] == code]
            district_df.to_excel(writer, sheet_name=district, index=False)
    
    print(f"Excel已保存: {excel_output}")
    
    # 生成新报告
    generate_report(df, data_dir)
    
    return df


def generate_report(df, output_dir):
    """生成统计报告"""
    lines = []
    lines.append("=" * 80)
    lines.append("郑州市五区空气质量历史数据统计报告")
    lines.append("=" * 80)
    lines.append(f"数据时间范围: {df['date'].min()} 至 {df['date'].max()}")
    lines.append(f"区县数量: {df['station_code'].nunique()}")
    lines.append(f"总记录数: {len(df)}")
    lines.append("")
    
    for code in STATION_MAPPING.keys():
        district_df = df[df['station_code'] == code]
        district_name = district_df['station_name'].iloc[0]
        
        lines.append(f"【{district_name}】")
        lines.append(f"  记录数: {len(district_df)}")
        lines.append(f"  AQI均值: {district_df['aqi'].mean():.1f}")
        lines.append(f"  AQI范围: {district_df['aqi'].min()} - {district_df['aqi'].max()}")
        lines.append(f"  PM2.5均值: {district_df['pm25'].mean():.1f} μg/m³")
        lines.append(f"  PM10均值: {district_df['pm10'].mean():.1f} μg/m³")
        
        # 空气质量等级分布
        lines.append("  空气质量等级分布:")
        level_counts = district_df['quality_level'].value_counts()
        for level, count in level_counts.items():
            pct = count / len(district_df) * 100
            lines.append(f"    {level}: {count}天 ({pct:.1f}%)")
        
        lines.append("")
    
    lines.append("=" * 80)
    
    report_path = output_dir / "districts_report.txt"
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write("\n".join(lines))
    
    print(f"\n统计报告已保存: {report_path}")


if __name__ == '__main__':
    print("=" * 80)
    print("站点名称修改为区县名称")
    print("=" * 80)
    rename_stations()
    print("\n完成！")
