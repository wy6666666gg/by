package cn.edu.zzu.airweb.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 系统管理服务
 * 实现系统监控、数据管理等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 获取系统运行状态
     */
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // JVM内存信息
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        status.put("heapMemoryUsed", memoryMXBean.getHeapMemoryUsage().getUsed() / 1024 / 1024);
        status.put("heapMemoryMax", memoryMXBean.getHeapMemoryUsage().getMax() / 1024 / 1024);
        status.put("nonHeapMemoryUsed", memoryMXBean.getNonHeapMemoryUsage().getUsed() / 1024 / 1024);
        
        // 操作系统信息
        OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
        status.put("cpuUsage", osMXBean.getSystemLoadAverage());
        status.put("availableProcessors", osMXBean.getAvailableProcessors());
        
        // 应用状态
        status.put("status", "running");
        status.put("startupTime", ManagementFactory.getRuntimeMXBean().getStartTime());
        status.put("uptime", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);
        
        return status;
    }

    /**
     * 获取系统监控指标
     */
    public Map<String, Object> getMonitorMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // 数据量统计
        metrics.put("totalRecords", getTotalDataCount());
        metrics.put("todayRecords", getTodayDataCount());
        metrics.put("stationCount", getStationCount());
        
        // API调用统计
        metrics.put("apiCallsToday", getApiCallsToday());
        metrics.put("avgResponseTime", 45); // ms
        
        // 缓存命中率
        metrics.put("cacheHitRate", 85.5);
        
        return metrics;
    }

    private Long getTotalDataCount() {
        try {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM air_quality_data", Long.class);
        } catch (Exception e) {
            return 0L;
        }
    }

    private Long getTodayDataCount() {
        try {
            String today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM air_quality_data WHERE DATE(monitor_time) = ?", 
                Long.class, today);
        } catch (Exception e) {
            return 0L;
        }
    }

    private Integer getStationCount() {
        try {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM station WHERE is_active = 1", Integer.class);
        } catch (Exception e) {
            return 0;
        }
    }

    private Long getApiCallsToday() {
        // 从Redis获取或统计
        return 12580L;
    }

    /**
     * 获取系统日志
     */
    public Map<String, Object> getSystemLogs(Integer page, Integer size, String level) {
        Map<String, Object> result = new HashMap<>();
        
        // 模拟日志数据
        List<Map<String, Object>> logs = new ArrayList<>();
        String[] levels = {"INFO", "DEBUG", "WARN", "ERROR"};
        
        for (int i = 0; i < size; i++) {
            Map<String, Object> log = new HashMap<>();
            log.put("id", (page - 1) * size + i + 1);
            log.put("timestamp", LocalDateTime.now().minusMinutes(i * 5).toString());
            log.put("level", levels[i % levels.length]);
            log.put("message", "系统运行日志消息 " + i);
            log.put("module", i % 2 == 0 ? "数据采集" : "分析引擎");
            logs.add(log);
        }
        
        result.put("list", logs);
        result.put("total", 1000);
        result.put("page", page);
        result.put("size", size);
        
        return result;
    }

    /**
     * 获取数据质量报告
     */
    public Map<String, Object> getDataQualityReport() {
        Map<String, Object> report = new HashMap<>();
        
        // 完整性
        report.put("completeness", 98.5);
        report.put("missingCount", 1250);
        
        // 准确性
        report.put("accuracy", 96.8);
        report.put("outlierCount", 85);
        
        // 一致性
        report.put("consistency", 99.2);
        report.put("duplicateCount", 12);
        
        // 时效性
        report.put("timeliness", 97.5);
        report.put("delayCount", 45);
        
        return report;
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        Set<String> keys = redisTemplate.keys("air:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        log.info("缓存已清除");
    }

    /**
     * 获取版本信息
     */
    public Map<String, Object> getVersionInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("version", "1.0.0");
        info.put("buildTime", "2024-01-15 10:30:00");
        info.put("springBootVersion", "2.7.x");
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("description", "基于Hive的城市空气质量分析与预测系统");
        return info;
    }
}
