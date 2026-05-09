package cn.edu.zzu.airweb.controller;

import cn.edu.zzu.airweb.common.Result;
import cn.edu.zzu.airweb.service.ZhengzhouDataService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 郑州市空气质量数据接口
 * 对接 https://datadev.gbqyun.com/city/zhengzhou 数据源
 */
@Api(tags = "郑州市空气质量数据")
@RestController
@RequestMapping("/v1/zhengzhou")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ZhengzhouController {

    private final ZhengzhouDataService zhengzhouDataService;

    @GetMapping("/realtime")
    @ApiOperation("获取郑州市各区实时AQI数据")
    public Result<List<Map<String, Object>>> getRealtimeData() {
        List<Map<String, Object>> data = zhengzhouDataService.getRealtimeData();
        return Result.success(data);
    }

    @GetMapping("/city-aqi")
    @ApiOperation("获取郑州市整体AQI")
    public Result<Map<String, Object>> getCityAQI() {
        Map<String, Object> data = zhengzhouDataService.getCityAQI();
        return Result.success(data);
    }

    @GetMapping("/hourly/{stationName}")
    @ApiOperation("获取站点24小时历史数据")
    public Result<List<Map<String, Object>>> getHourlyData(@PathVariable String stationName) {
        List<Map<String, Object>> data = zhengzhouDataService.getHourlyData(stationName);
        return Result.success(data);
    }

    @GetMapping("/prediction/{stationName}")
    @ApiOperation("基于历史数据预测未来AQI")
    public Result<Map<String, Object>> getPrediction(
            @PathVariable String stationName,
            @RequestParam(defaultValue = "24") int hours) {
        Map<String, Object> data = zhengzhouDataService.predictAQI(stationName, hours);
        return Result.success(data);
    }

    @GetMapping("/prediction/multi")
    @ApiOperation("获取多站点预测对比")
    public Result<List<Map<String, Object>>> getMultiStationPrediction(
            @RequestParam(defaultValue = "24") int hours) {
        List<Map<String, Object>> data = zhengzhouDataService.getMultiStationPrediction(hours);
        return Result.success(data);
    }

    @GetMapping("/prediction/historical/{stationName}")
    @ApiOperation("基于历史同期数据预测指定日期")
    public Result<Map<String, Object>> predictByHistoricalData(
            @PathVariable String stationName,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam int day,
            @RequestParam(defaultValue = "24") int hours) {
        Map<String, Object> data = zhengzhouDataService.predictAQIForDate(
                stationName, year, month, day, hours);
        return Result.success(data);
    }

    @GetMapping("/prediction/historical/range/{stationName}")
    @ApiOperation("基于历史同期数据预测日期区间")
    public Result<Map<String, Object>> predictByHistoricalDateRange(
            @PathVariable String stationName,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        Map<String, Object> data = zhengzhouDataService.predictAQIForDateRange(
                stationName, startDate, endDate);
        return Result.success(data);
    }

    @GetMapping("/historical/{stationName}")
    @ApiOperation("获取指定日期的历史同期数据")
    public Result<Map<String, Object>> getHistoricalData(
            @PathVariable String stationName,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam int day) {
        Map<String, Object> data = zhengzhouDataService.getHistoricalDataForDate(
                stationName, year, month, day);
        return Result.success(data);
    }

    @GetMapping("/query/{stationName}")
    @ApiOperation("查询指定日期的空气质量数据")
    public Result<Map<String, Object>> queryAirQualityByDate(
            @PathVariable String stationName,
            @RequestParam String date) {
        Map<String, Object> data = zhengzhouDataService.queryAirQualityByDate(stationName, date);
        return Result.success(data);
    }

    @GetMapping("/query/range/{stationName}")
    @ApiOperation("查询日期区间的空气质量数据")
    public Result<List<Map<String, Object>>> queryAirQualityByDateRange(
            @PathVariable String stationName,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        List<Map<String, Object>> data = zhengzhouDataService.queryAirQualityByDateRange(
                stationName, startDate, endDate);
        return Result.success(data);
    }
}
