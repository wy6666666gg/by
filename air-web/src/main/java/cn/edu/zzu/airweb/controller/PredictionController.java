package cn.edu.zzu.airweb.controller;

import cn.edu.zzu.airweb.common.Result;
import cn.edu.zzu.airweb.entity.AqiPrediction;
import cn.edu.zzu.airweb.service.PredictionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Api(tags = "预测接口")
@RestController
@RequestMapping("/v1/predict")
@RequiredArgsConstructor
public class PredictionController {
    
    private final PredictionService predictionService;
    
    @GetMapping("/{stationCode}")
    @ApiOperation("获取站点预测数据")
    public Result<Map<String, Object>> getPrediction(@PathVariable String stationCode) {
        Map<String, Object> data = predictionService.getPrediction(stationCode);
        return Result.success(data);
    }
    
    @GetMapping("/24h")
    @ApiOperation("获取24小时预测")
    public Result<List<AqiPrediction>> get24HourPrediction(
            @RequestParam(required = false) String stationCode) {
        List<AqiPrediction> data = predictionService.get24HourPrediction(stationCode);
        return Result.success(data);
    }
    
    @GetMapping("/72h")
    @ApiOperation("获取72小时预测")
    public Result<List<AqiPrediction>> get72HourPrediction(
            @RequestParam(required = false) String stationCode) {
        List<AqiPrediction> data = predictionService.get72HourPrediction(stationCode);
        return Result.success(data);
    }
    
    @GetMapping("/comparison")
    @ApiOperation("获取多站点预测对比")
    public Result<List<Map<String, Object>>> getComparison(
            @RequestParam List<String> stationCodes) {
        List<Map<String, Object>> data = predictionService.getComparison(stationCodes);
        return Result.success(data);
    }
    
    @PostMapping("/trigger")
    @ApiOperation("触发预测任务")
    public Result<String> triggerPrediction(
            @RequestParam(required = false) String stationCode) {
        predictionService.triggerPrediction(stationCode);
        return Result.success("预测任务已触发");
    }
}