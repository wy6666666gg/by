package cn.edu.zzu.airweb.service;

import cn.edu.zzu.airweb.mapper.AqiMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AqiService {
    
    private final AqiMapper aqiMapper;
    
    /**
     * 获取实时AQI数据
     */
    public List<Map<String, Object>> getRealtimeAqi() {
        log.info("查询实时AQI数据");
        return aqiMapper.selectRealtimeAqi();
    }
    
    /**
     * 获取历史AQI数据
     */
    public List<Map<String, Object>> getHistoryAqi(String stationCode, LocalDate startDate, LocalDate endDate) {
        log.info("查询历史AQI数据: station={}, start={}, end={}", stationCode, startDate, endDate);
        return aqiMapper.selectHistoryAqi(stationCode, startDate, endDate);
    }
    
    /**
     * 获取趋势数据
     */
    public List<Map<String, Object>> getTrend(String stationCode, String type, Integer days) {
        log.info("查询趋势数据: station={}, type={}, days={}", stationCode, type, days);
        return aqiMapper.selectTrend(stationCode, type, days);
    }
    
    /**
     * 获取站点AQI详情
     */
    public Map<String, Object> getStationAqi(String stationCode) {
        log.info("查询站点AQI详情: {}", stationCode);
        return aqiMapper.selectStationAqi(stationCode);
    }
    
    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics(LocalDate date) {
        log.info("查询统计信息: {}", date);
        return aqiMapper.selectStatistics(date);
    }
}