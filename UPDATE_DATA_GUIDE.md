# 郑州市空气质量数据更新指南

## 问题说明
目标网站 `https://citydev.gbqyun.com/index/zhengzhou` 使用了前端渲染技术，数据通过JavaScript动态加载，无法直接通过API获取。

## 手动更新数据步骤

### 方法：直接编辑CSV文件（推荐）

1. **打开目标网站**
   - 访问 https://citydev.gbqyun.com/index/zhengzhou
   - 查看各站点的实时AQI数据

2. **编辑CSV文件**
   - 文件位置：`air-data-collector/data/zhengzhou_realtime.csv`
   - 修改各站点的AQI数值

3. **CSV文件格式**
```csv
stationCode,stationName,actualStation,date,aqi,qualityLevel,pm25,pm10,o3,so2,co,no2,primaryPollutant,isSandDustDay,updateTime,dataSource
410101,中原区,北区建设指挥部,2025-01-09,85,良,55.2,80.8,102.0,12.0,0.6,38.0,PM10,False,2025-01-09 10:30:00,gbqyun
410102,金水区,北区建设指挥部,2025-01-09,82,良,53.3,77.9,98.0,11.0,0.6,37.0,PM10,False,2025-01-09 10:30:00,gbqyun
410103,二七区,河医大,2025-01-09,95,良,61.8,90.3,105.0,14.0,0.7,42.0,PM2.5,False,2025-01-09 10:30:00,gbqyun
410108,惠济区,惠济区政府,2025-01-09,75,良,48.8,71.3,95.0,10.0,0.5,35.0,PM10,False,2025-01-09 10:30:00,gbqyun
410104,郑东新区,经开区管委,2025-01-09,88,良,57.2,83.6,100.0,13.0,0.6,39.0,PM10,False,2025-01-09 10:30:00,gbqyun
```

4. **修改说明**
   - 修改第5列的 `aqi` 数值
   - 修改第6列的 `qualityLevel`（优/良/轻度污染/中度污染/重度污染）
   - 修改第7-12列的污染物数值（可选，通常与AQI成正比）
   - 修改第14列的 `updateTime` 为当前时间

5. **刷新前端页面**
   - 保存CSV文件后，刷新前端页面 `http://localhost:3000`
   - 页面将显示更新后的数据

## 站点对应关系

| 区域 | 实际监测点 |
|------|-----------|
| 中原区 | 北区建设指挥部 |
| 金水区 | 北区建设指挥部 |
| 二七区 | 河医大 |
| 惠济区 | 惠济区政府 |
| 郑东新区 | 经开区管委 |

## 自动化方案（可选）

### 使用Python脚本自动抓取

1. **安装依赖**
```bash
cd air-data-collector
pip install playwright
playwright install chromium
```

2. **运行更新脚本**
```bash
python update_data.py
```

3. **选择更新方式**
   - 选项1：手动输入（推荐）- 根据网站显示的数据手动输入
   - 选项2：自动抓取 - 尝试使用浏览器自动获取（可能不稳定）

## 注意事项

1. **数据同步频率**
   - 目标网站数据通常每小时更新
   - 建议每1-2小时手动更新一次CSV文件

2. **后端服务**
   - Java后端服务会每分钟检查CSV文件变化
   - 修改CSV后，刷新前端即可看到新数据

3. **故障排查**
   - 如果数据不更新，检查CSV文件格式是否正确
   - 确认CSV文件路径：`air-data-collector/data/zhengzhou_realtime.csv`
   - 查看后端日志确认文件是否被正确加载
