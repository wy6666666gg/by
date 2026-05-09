package cn.edu.zzu.airweb.mapper;

import cn.edu.zzu.airweb.entity.AlertRule;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 预警数据访问层
 */
@Mapper
public interface AlertMapper {

    @Select("SELECT a.*, s.station_name " +
            "FROM alert_record a " +
            "LEFT JOIN station s ON a.station_code = s.station_code " +
            "WHERE a.status = 'active' " +
            "ORDER BY a.create_time DESC")
    List<Map<String, Object>> selectActiveAlerts();

    @Select("SELECT a.*, s.station_name " +
            "FROM alert_record a " +
            "LEFT JOIN station s ON a.station_code = s.station_code " +
            "WHERE a.create_time >= #{startTime} " +
            "ORDER BY a.create_time DESC")
    List<Map<String, Object>> selectAlertHistory(@Param("startTime") LocalDateTime startTime);

    @Select("SELECT * FROM alert_rule WHERE is_deleted = 0 ORDER BY id")
    List<AlertRule> selectAllRules();

    @Insert("INSERT INTO alert_rule (name, description, condition, level, enabled, " +
            "notify_methods, notify_targets, cooldown_minutes, create_time, update_time) " +
            "VALUES (#{name}, #{description}, #{condition}, #{level}, #{enabled}, " +
            "#{notifyMethods}, #{notifyTargets}, #{cooldownMinutes}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertRule(AlertRule rule);

    @Update("UPDATE alert_rule SET name = #{name}, description = #{description}, " +
            "condition = #{condition}, level = #{level}, enabled = #{enabled}, " +
            "notify_methods = #{notifyMethods}, notify_targets = #{notifyTargets}, " +
            "cooldown_minutes = #{cooldownMinutes}, update_time = #{updateTime} " +
            "WHERE id = #{id}")
    void updateRule(AlertRule rule);

    @Update("UPDATE alert_rule SET is_deleted = 1 WHERE id = #{id}")
    void deleteRule(@Param("id") Long id);

    @Update("UPDATE alert_record SET status = #{status}, resolve_time = #{resolveTime} " +
            "WHERE id = #{id}")
    void updateAlertStatus(@Param("id") Long id, @Param("status") String status, 
                           @Param("resolveTime") LocalDateTime resolveTime);

    @Select("SELECT COUNT(*) FROM alert_record WHERE status = 'active'")
    Integer countActiveAlerts();

    @Select("SELECT COUNT(*) FROM alert_record WHERE DATE(create_time) = CURDATE()")
    Integer countTodayAlerts();

    @Select("SELECT COUNT(*) FROM alert_record WHERE level = #{level} AND status = 'active'")
    Integer countAlertsByLevel(@Param("level") String level);

    @Select("SELECT COUNT(*) FROM alert_record WHERE status = 'resolved'")
    Integer countResolvedAlerts();

    @Select("SELECT alert_type, COUNT(*) as count FROM alert_record " +
            "WHERE create_time >= DATE_SUB(NOW(), INTERVAL 7 DAY) " +
            "GROUP BY alert_type")
    List<Map<String, Object>> countAlertsByType();

    @Insert("INSERT INTO alert_record (rule_id, level, alert_type, threshold_value, " +
            "actual_value, status, create_time) " +
            "VALUES (#{ruleId}, #{level}, #{alertType}, #{threshold}, #{actualValue}, 'active', #{createTime})")
    void insertAlertRecord(@Param("ruleId") Long ruleId,
                           @Param("level") String level,
                           @Param("alertType") String alertType,
                           @Param("threshold") double threshold,
                           @Param("actualValue") double actualValue,
                           @Param("createTime") LocalDateTime createTime);
}
