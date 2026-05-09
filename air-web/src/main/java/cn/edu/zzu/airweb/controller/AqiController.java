package cn.edu.zzu.airweb.controller;

import cn.edu.zzu.airweb.common.Result;
import cn.edu.zzu.airweb.entity.AqiPrediction;
import cn.edu.zzu.airweb.service.AqiService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Api(tags = "空气质量数据接口")
@RestController
@RequestMapping("/v1/aqi")
@RequiredArgsConstructor
public class AqiController {
    
    private final AqiService aqiService;
    
    @GetMapping("/realtime")
    @ApiOperation("获取实时AQI数据")
    public Result<List<Map<String, Object>>> getRealtimeAqi() {
        List<Map<String, Object>> data = aqiService.getRealtimeAqi();
        return Result.success(data);
    }
    
    @GetMapping("/history")
    @ApiOperation("获取历史AQI数据")
    public Result<List<Map<String, Object>>> getHistoryAqi(
            @RequestParam String stationCode,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        List<Map<String, Object>> data = aqiService.getHistoryAqi(stationCode, startDate, endDate);
        return Result.success(data);
    }
    
    @GetMapping("/trend")
    @ApiOperation("获取AQI趋势数据")
    public Result<List<Map<String, Object>>> getTrend(
            @RequestParam(required = false) String stationCode,
            @RequestParam(defaultValue = "daily") String type,
            @RequestParam(required = false) Integer days) {
        List<Map<String, Object>> data = aqiService.getTrend(stationCode, type, days);
        return Result.success(data);
    }
    
    @GetMapping("/station/{code}")
    @ApiOperation("获取站点AQI详情")
    public Result<Map<String, Object>> getStationAqi(@PathVariable String code) {
        Map<String, Object> data = aqiService.getStationAqi(code);
        return Result.success(data);
    }
    
    @GetMapping("/statistics")
    @ApiOperation("获取AQI统计信息")
    public Result<Map<String, Object>> getStatistics(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        Map<String, Object> data = aqiService.getStatistics(date);
        return Result.success(data);
    }
}