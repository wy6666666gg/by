package cn.edu.zzu.airweb.controller;

import cn.edu.zzu.airweb.common.Result;
import cn.edu.zzu.airweb.service.SystemService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统管理控制器
 * 提供系统监控、数据管理、日志查询等接口
 */
@Api(tags = "系统管理接口")
@RestController
@RequestMapping("/v1/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;

    @GetMapping("/status")
    @ApiOperation("获取系统运行状态")
    public Result<Map<String, Object>> getSystemStatus() {
        Map<String, Object> data = systemService.getSystemStatus();
        return Result.success(data);
    }

    @GetMapping("/monitor")
    @ApiOperation("获取系统监控指标")
    public Result<Map<String, Object>> getMonitorMetrics() {
        Map<String, Object> data = systemService.getMonitorMetrics();
        return Result.success(data);
    }

    @GetMapping("/logs")
    @ApiOperation("获取系统日志")
    public Result<Map<String, Object>> getSystemLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String level) {
        Map<String, Object> data = systemService.getSystemLogs(page, size, level);
        return Result.success(data);
    }

    @GetMapping("/data-quality")
    @ApiOperation("获取数据质量报告")
    public Result<Map<String, Object>> getDataQualityReport() {
        Map<String, Object> data = systemService.getDataQualityReport();
        return Result.success(data);
    }

    @PostMapping("/cache/clear")
    @ApiOperation("清除缓存")
    public Result<Void> clearCache() {
        systemService.clearCache();
        return Result.success();
    }

    @GetMapping("/version")
    @ApiOperation("获取系统版本信息")
    public Result<Map<String, Object>> getVersionInfo() {
        Map<String, Object> data = systemService.getVersionInfo();
        return Result.success(data);
    }
}
