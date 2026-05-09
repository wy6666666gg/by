package cn.edu.zzu.airweb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 预警规则实体类
 */
@Data
@Builder
@TableName("alert_rule")
public class AlertRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    private String description;
    private String condition;
    private String level;
    private Boolean enabled;
    private String notifyMethods;
    private String notifyTargets;
    private Integer cooldownMinutes;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
