package cn.edu.zzu.airweb.service;

import cn.edu.zzu.airweb.entity.AqiPrediction;
import cn.edu.zzu.airweb.mapper.PredictionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionService {
    
    private final PredictionMapper predictionMapper;
    
    /**
     * 获取站点预测数据
     */
    public Map<String, Object> getPrediction(String stationCode) {
        log.info("查询预测数据: {}", stationCode);
        return predictionMapper.selectPrediction(stationCode);
    }
    
    /**
     * 获取24小时预测
     */
    public List<AqiPrediction> get24HourPrediction(String stationCode) {
        log.info("查询24小时预测: {}", stationCode);
        return predictionMapper.select24HourPrediction(stationCode);
    }
    
    /**
     * 获取72小时预测
     */
    public List<AqiPrediction> get72HourPrediction(String stationCode) {
        log.info("查询72小时预测: {}", stationCode);
        return predictionMapper.select72HourPrediction(stationCode);
    }
    
    /**
     * 获取多站点预测对比
     */
    public List<Map<String, Object>> getComparison(List<String> stationCodes) {
        log.info("查询预测对比: {}", stationCodes);
        return predictionMapper.selectComparison(stationCodes);
    }
    
    /**
     * 触发预测任务
     */
    public void triggerPrediction(String stationCode) {
        log.info("触发预测任务: {}", stationCode);
        // 调用Spark预测任务
        // 可通过REST API或直接调用Spark-submit
    }
}