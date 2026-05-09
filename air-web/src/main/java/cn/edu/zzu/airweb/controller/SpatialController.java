package cn.edu.zzu.airweb.controller;

import cn.edu.zzu.airweb.common.Result;
import cn.edu.zzu.airweb.service.SpatialService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 空间分析控制器
 * 提供污染分布热力图、站点空间分布等接口
 */
@Api(tags = "空间分析接口")
@RestController
@RequestMapping("/v1/spatial")
@RequiredArgsConstructor
public class SpatialController {

    private final SpatialService spatialService;

    @GetMapping("/heatmap")
    @ApiOperation("获取污染分布热力图数据")
    public Result<List<Map<String, Object>>> getHeatmapData(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime time,
            @RequestParam String pollutant,
            @RequestParam(defaultValue = "idw") String method) {
        List<Map<String, Object>> data = spatialService.getHeatmapData(time, pollutant, method);
        return Result.success(data);
    }

    @GetMapping("/stations")
    @ApiOperation("获取站点空间分布")
    public Result<List<Map<String, Object>>> getStationDistribution() {
        List<Map<String, Object>> data = spatialService.getStationDistribution();
        return Result.success(data);
    }

    @GetMapping("/interpolation")
    @ApiOperation("获取插值分析结果")
    public Result<Map<String, Object>> getInterpolationResult(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime time,
            @RequestParam String pollutant,
            @RequestParam(defaultValue = "idw") String method) {
        Map<String, Object> data = spatialService.getInterpolationResult(time, pollutant, method);
        return Result.success(data);
    }

    @GetMapping("/comparison")
    @ApiOperation("获取多站点空间对比")
    public Result<List<Map<String, Object>>> getSpatialComparison(
            @RequestParam List<String> stationCodes,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime time) {
        List<Map<String, Object>> data = spatialService.getSpatialComparison(stationCodes, time);
        return Result.success(data);
    }

    @GetMapping("/timeline")
    @ApiOperation("获取时空演变数据")
    public Result<List<Map<String, Object>>> getTimelineData(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam String pollutant) {
        List<Map<String, Object>> data = spatialService.getTimelineData(startTime, endTime, pollutant);
        return Result.success(data);
    }
}
