package cn.edu.zzu.airweb.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
@TableName("aqi_prediction")
public class AqiPrediction {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String stationCode;
    private String stationName;
    private LocalDateTime predictTime;
    private Integer predictHour;
    private Double pm25Pred;
    private Double pm10Pred;
    private Integer aqiPred;
    private Integer aqiLevelPred;
    private Double confidence;
    private String modelType;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}