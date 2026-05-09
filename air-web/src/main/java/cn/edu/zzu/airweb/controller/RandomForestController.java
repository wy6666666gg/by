package cn.edu.zzu.airweb.controller;

import cn.edu.zzu.airweb.common.Result;
import cn.edu.zzu.airweb.service.RandomForestService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 随机森林预测控制器
 * 基于历史数据使用随机森林算法预测空气质量
 */
@Slf4j
@Api(tags = "随机森林预测")
@RestController
@RequestMapping("/v1/prediction")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RandomForestController {

    private final RandomForestService randomForestService;

    @PostMapping("/random-forest")
    @ApiOperation("使用随机森林算法预测次日空气质量")
    public Result<Map<String, Object>> predictWithRandomForest(@RequestBody Map<String, Object> request) {
        try {
            String stationName = (String) request.get("stationName");
            List<Map<String, Object>> historicalData = (List<Map<String, Object>>) request.get("historicalData");
            
            if (stationName == null || historicalData == null || historicalData.size() < 5) {
                return Result.error("需要至少5天历史数据");
            }
            
            Map<String, Object> result = randomForestService.predict(stationName, historicalData);
            return Result.success(result);
        } catch (Exception e) {
            log.error("随机森林预测失败", e);
            return Result.error("预测失败: " + e.getMessage());
        }
    }
}
