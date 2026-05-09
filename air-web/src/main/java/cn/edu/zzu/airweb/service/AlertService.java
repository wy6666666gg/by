package cn.edu.zzu.airweb.service;

import cn.edu.zzu.airweb.entity.AlertRule;
import cn.edu.zzu.airweb.mapper.AlertMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 预警服务
 * 实现实时预警检测、预警规则管理等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertMapper alertMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String ALERT_CACHE_KEY = "air:alerts:active";

    /**
     * 获取当前活跃预警
     */
    public List<Map<String, Object>> getActiveAlerts() {
        // 先从缓存获取
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cached = (List<Map<String, Object>>) redisTemplate.opsForValue().get(ALERT_CACHE_KEY);
        if (cached != null) {
            return cached;
        }
        
        // 从数据库查询
        List<Map<String, Object>> alerts = alertMapper.selectActiveAlerts();
        redisTemplate.opsForValue().set(ALERT_CACHE_KEY, alerts, 5, TimeUnit.MINUTES);
        return alerts;
    }

    /**
     * 获取历史预警
     */
    public List<Map<String, Object>> getAlertHistory(Integer days) {
        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        return alertMapper.selectAlertHistory(startTime);
    }

    /**
     * 获取预警规则列表
     */
    public List<AlertRule> getAlertRules() {
        return alertMapper.selectAllRules();
    }

    /**
     * 保存预警规则
     */
    public void saveAlertRule(AlertRule rule) {
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        alertMapper.insertRule(rule);
        log.info("新增预警规则: {}", rule.getName());
    }

    /**
     * 更新预警规则
     */
    public void updateAlertRule(Long id, AlertRule rule) {
        rule.setId(id);
        rule.setUpdateTime(LocalDateTime.now());
        alertMapper.updateRule(rule);
        log.info("更新预警规则: id={}", id);
    }

    /**
     * 删除预警规则
     */
    public void deleteAlertRule(Long id) {
        alertMapper.deleteRule(id);
        log.info("删除预警规则: id={}", id);
    }

    /**
     * 标记预警为已处理
     */
    public void resolveAlert(Long id) {
        alertMapper.updateAlertStatus(id, "resolved", LocalDateTime.now());
        redisTemplate.delete(ALERT_CACHE_KEY);
        log.info("预警已处理: id={}", id);
    }

    /**
     * 获取预警统计
     */
    public Map<String, Object> getAlertStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeCount", alertMapper.countActiveAlerts());
        stats.put("todayCount", alertMapper.countTodayAlerts());
        stats.put("severeCount", alertMapper.countAlertsByLevel("severe"));
        stats.put("resolvedCount", alertMapper.countResolvedAlerts());
        stats.put("byType", alertMapper.countAlertsByType());
        return stats;
    }

    /**
     * 触发预警检查
     */
    public void triggerAlertCheck() {
        log.info("手动触发预警检查");
        List<AlertRule> rules = alertMapper.selectAllRules();
        
        for (AlertRule rule : rules) {
            if (Boolean.TRUE.equals(rule.getEnabled())) {
                checkAndCreateAlert(rule);
            }
        }
        
        redisTemplate.delete(ALERT_CACHE_KEY);
    }

    /**
     * 检查规则并创建预警
     * 根据规则中定义的条件表达式，查询最新监测数据并判断是否超过阈值
     */
    private void checkAndCreateAlert(AlertRule rule) {
        log.debug("检查预警规则: {}", rule.getName());
        try {
            String condition = rule.getCondition();
            if (condition == null || condition.isEmpty()) return;

            // 解析条件格式: "pollutant > threshold"
            String[] parts = condition.split("\\s+");
            if (parts.length < 3) return;

            String pollutant = parts[0].toLowerCase();
            String operator = parts[1];
            double threshold = Double.parseDouble(parts[2]);

            // 查询最新数据中该污染物的最大值
            List<Map<String, Object>> realtimeData = alertMapper.selectActiveAlerts();
            if (realtimeData == null || realtimeData.isEmpty()) return;

            for (Map<String, Object> data : realtimeData) {
                Object valueObj = data.get(pollutant);
                if (valueObj == null) continue;

                double actualValue = ((Number) valueObj).doubleValue();
                boolean triggered = false;

                switch (operator) {
                    case ">": triggered = actualValue > threshold; break;
                    case ">=": triggered = actualValue >= threshold; break;
                    case "<": triggered = actualValue < threshold; break;
                    case "<=": triggered = actualValue <= threshold; break;
                    default: break;
                }

                if (triggered) {
                    alertMapper.insertAlertRecord(
                            rule.getId(),
                            rule.getLevel(),
                            pollutant,
                            threshold,
                            actualValue,
                            LocalDateTime.now()
                    );
                    log.warn("触发预警: 规则[{}], {}={} {} {}",
                            rule.getName(), pollutant, actualValue, operator, threshold);
                    break;
                }
            }
        } catch (Exception e) {
            log.error("预警规则检查异常: rule={}, error={}", rule.getName(), e.getMessage());
        }
    }
}
