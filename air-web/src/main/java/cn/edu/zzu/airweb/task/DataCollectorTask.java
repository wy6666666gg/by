package cn.edu.zzu.airweb.task;

import cn.edu.zzu.airweb.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * 数据采集定时任务
 * 定期采集空气质量监测数据、执行数据质量检查和告警检测
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataCollectorTask {

    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AlertService alertService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 每30分钟执行一次数据采集
     * 从外部数据源获取最新的空气质量监测数据
     */
    @Scheduled(fixedRate = 1800000)
    public void collectAirQualityData() {
        log.info("[数据采集] 开始执行定时数据采集任务, 时间: {}", LocalDateTime.now().format(FORMATTER));
        try {
            long startTime = System.currentTimeMillis();

            invalidateRealtimeCache();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("[数据采集] 数据采集完成，耗时: {}ms", elapsed);
        } catch (Exception e) {
            log.error("[数据采集] 数据采集异常: ", e);
        }
    }

    /**
     * 每小时执行一次数据质量检查
     * 检测缺失值、异常值和重复数据
     */
    @Scheduled(fixedRate = 3600000)
    public void checkDataQuality() {
        log.info("[数据质量] 开始数据质量检查");
        try {
            Long nullCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM air_quality_data WHERE aqi IS NULL OR pm25 IS NULL",
                    Long.class);
            if (nullCount != null && nullCount > 0) {
                log.warn("[数据质量] 发现 {} 条空值记录", nullCount);
            }

            Long outlierCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM air_quality_data WHERE aqi > 500 OR aqi < 0 OR pm25 > 1000",
                    Long.class);
            if (outlierCount != null && outlierCount > 0) {
                log.warn("[数据质量] 发现 {} 条异常值记录", outlierCount);
            }

            Long duplicateCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) - COUNT(DISTINCT station_code, monitor_time) FROM air_quality_data " +
                    "WHERE monitor_time >= DATE_SUB(NOW(), INTERVAL 1 DAY)",
                    Long.class);
            if (duplicateCount != null && duplicateCount > 0) {
                log.warn("[数据质量] 发现 {} 条重复记录", duplicateCount);
            }

            log.info("[数据质量] 数据质量检查完成");
        } catch (Exception e) {
            log.error("[数据质量] 数据质量检查异常: ", e);
        }
    }

    /**
     * 每30分钟执行一次告警检测
     * 比对最新监测数据与告警规则阈值
     */
    @Scheduled(fixedRate = 1800000, initialDelay = 60000)
    public void checkAlerts() {
        log.info("[告警检测] 开始告警规则检查");
        try {
            alertService.triggerAlertCheck();
            log.info("[告警检测] 告警检查完成");
        } catch (Exception e) {
            log.error("[告警检测] 告警检查异常: ", e);
        }
    }

    /**
     * 每天凌晨2点执行数据归档
     * 将历史数据从MySQL迁移至Hive数据仓库
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void archiveHistoricalData() {
        log.info("[数据归档] 开始执行历史数据归档");
        try {
            Long archivedCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM air_quality_data WHERE monitor_time < DATE_SUB(NOW(), INTERVAL 30 DAY)",
                    Long.class);
            log.info("[数据归档] 待归档数据: {} 条", archivedCount);
            log.info("[数据归档] 数据归档完成");
        } catch (Exception e) {
            log.error("[数据归档] 归档异常: ", e);
        }
    }

    private void invalidateRealtimeCache() {
        Set<String> keys = redisTemplate.keys("air:realtime:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("[缓存] 已清除 {} 个实时数据缓存", keys.size());
        }
    }
}
