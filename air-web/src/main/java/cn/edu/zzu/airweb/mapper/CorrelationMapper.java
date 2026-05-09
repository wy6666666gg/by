package cn.edu.zzu.airweb.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 相关性分析数据访问层
 */
@Mapper
public interface CorrelationMapper {

    @Select("SELECT pm25, pm10, so2, no2, co, o3, " +
            "temperature, humidity, wind_speed, pressure " +
            "FROM dwd_air_quality_dt " +
            "WHERE monitor_date BETWEEN #{startDate} AND #{endDate} " +
            "<if test='stationCode != null'> AND station_code = #{stationCode} </if> " +
            "ORDER BY monitor_date, monitor_hour")
    List<Map<String, Object>> selectCorrelationData(@Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate,
                                                     @Param("stationCode") String stationCode);

    @Select("SELECT ${factorX} as x, ${factorY} as y " +
            "FROM dwd_air_quality_dt " +
            "WHERE monitor_date BETWEEN #{startDate} AND #{endDate} " +
            "<if test='stationCode != null'> AND station_code = #{stationCode} </if> " +
            "AND ${factorX} IS NOT NULL AND ${factorY} IS NOT NULL")
    List<Map<String, Object>> selectScatterData(@Param("factorX") String factorX,
                                                 @Param("factorY") String factorY,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate,
                                                 @Param("stationCode") String stationCode);
}
