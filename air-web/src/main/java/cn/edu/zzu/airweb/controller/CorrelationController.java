package cn.edu.zzu.airweb.controller;

import cn.edu.zzu.airweb.common.Result;
import cn.edu.zzu.airweb.service.CorrelationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 相关性分析控制器
 * 提供污染物与气象因子相关性分析接口
 */
@Api(tags = "相关性分析接口")
@RestController
@RequestMapping("/v1/correlation")
@RequiredArgsConstructor
public class CorrelationController {

    private final CorrelationService correlationService;

    @GetMapping("/matrix")
    @ApiOperation("获取相关性矩阵")
    public Result<Map<String, Object>> getCorrelationMatrix(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(defaultValue = "pearson") String method,
            @RequestParam(required = false) String stationCode) {
        Map<String, Object> data = correlationService.getCorrelationMatrix(startDate, endDate, method, stationCode);
        return Result.success(data);
    }

    @GetMapping("/feature-importance")
    @ApiOperation("获取随机森林特征重要性")
    public Result<List<Map<String, Object>>> getFeatureImportance(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String stationCode) {
        List<Map<String, Object>> data = correlationService.getFeatureImportance(startDate, endDate, stationCode);
        return Result.success(data);
    }

    @GetMapping("/lag")
    @ApiOperation("获取时滞相关性")
    public Result<List<Map<String, Object>>> getLagCorrelation(
            @RequestParam String factor1,
            @RequestParam String factor2,
            @RequestParam(defaultValue = "24") Integer maxLag,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        List<Map<String, Object>> data = correlationService.getLagCorrelation(factor1, factor2, maxLag, startDate, endDate);
        return Result.success(data);
    }

    @GetMapping("/scatter")
    @ApiOperation("获取散点图数据")
    public Result<List<Map<String, Object>>> getScatterData(
            @RequestParam String factorX,
            @RequestParam String factorY,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @RequestParam(required = false) String stationCode) {
        List<Map<String, Object>> data = correlationService.getScatterData(factorX, factorY, startDate, endDate, stationCode);
        return Result.success(data);
    }
}
