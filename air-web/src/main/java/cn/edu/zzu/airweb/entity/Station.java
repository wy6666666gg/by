package cn.edu.zzu.airweb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
@TableName("station")
public class Station {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String stationCode;
    private String stationName;
    private String districtCode;
    private String districtName;
    private String cityName;
    private Double latitude;
    private Double longitude;
    private String stationType;
    private String monitorLevel;
    private Integer isActive;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}