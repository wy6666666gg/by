package cn.edu.zzu.airweb.controller;

import cn.edu.zzu.airweb.common.Result;
import cn.edu.zzu.airweb.entity.AlertRule;
import cn.edu.zzu.airweb.service.AlertService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 预警中心控制器
 * 提供实时预警、预警规则配置等接口
 */
@Api(tags = "预警中心接口")
@RestController
@RequestMapping("/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping("/active")
    @ApiOperation("获取当前活跃预警")
    public Result<List<Map<String, Object>>> getActiveAlerts() {
        List<Map<String, Object>> data = alertService.getActiveAlerts();
        return Result.success(data);
    }

    @GetMapping("/history")
    @ApiOperation("获取历史预警")
    public Result<List<Map<String, Object>>> getAlertHistory(
            @RequestParam(defaultValue = "7") Integer days) {
        List<Map<String, Object>> data = alertService.getAlertHistory(days);
        return Result.success(data);
    }

    @GetMapping("/rules")
    @ApiOperation("获取预警规则列表")
    public Result<List<AlertRule>> getAlertRules() {
        List<AlertRule> data = alertService.getAlertRules();
        return Result.success(data);
    }

    @PostMapping("/rules")
    @ApiOperation("保存预警规则")
    public Result<Void> saveAlertRule(@RequestBody AlertRule rule) {
        alertService.saveAlertRule(rule);
        return Result.success();
    }

    @PutMapping("/rules/{id}")
    @ApiOperation("更新预警规则")
    public Result<Void> updateAlertRule(@PathVariable Long id, @RequestBody AlertRule rule) {
        alertService.updateAlertRule(id, rule);
        return Result.success();
    }

    @DeleteMapping("/rules/{id}")
    @ApiOperation("删除预警规则")
    public Result<Void> deleteAlertRule(@PathVariable Long id) {
        alertService.deleteAlertRule(id);
        return Result.success();
    }

    @PostMapping("/{id}/resolve")
    @ApiOperation("标记预警为已处理")
    public Result<Void> resolveAlert(@PathVariable Long id) {
        alertService.resolveAlert(id);
        return Result.success();
    }

    @GetMapping("/stats")
    @ApiOperation("获取预警统计")
    public Result<Map<String, Object>> getAlertStats() {
        Map<String, Object> data = alertService.getAlertStats();
        return Result.success(data);
    }

    @PostMapping("/trigger")
    @ApiOperation("手动触发预警检查")
    public Result<Void> triggerAlertCheck() {
        alertService.triggerAlertCheck();
        return Result.success();
    }
}
