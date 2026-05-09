package cn.edu.zzu.airweb.mapper;

import cn.edu.zzu.airweb.entity.AqiPrediction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PredictionMapper {
    
    @Select("SELECT * FROM ads_aqi_prediction WHERE station_code = #{stationCode} ORDER BY predict_time LIMIT 72")
    Map<String, Object> selectPrediction(@Param("stationCode") String stationCode);
    
    List<AqiPrediction> select24HourPrediction(@Param("stationCode") String stationCode);
    
    List<AqiPrediction> select72HourPrediction(@Param("stationCode") String stationCode);
    
    List<Map<String, Object>> selectComparison(@Param("stationCodes") List<String> stationCodes);
}