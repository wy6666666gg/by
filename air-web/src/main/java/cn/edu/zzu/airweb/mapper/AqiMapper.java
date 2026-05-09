package cn.edu.zzu.airweb.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AQI数据访问层
 */
@Mapper
public interface AqiMapper {

    @Select("SELECT a.*, s.station_name, s.latitude, s.longitude, s.district_name " +
            "FROM air_quality_data a " +
            "JOIN station s ON a.station_code = s.station_code " +
            "WHERE a.monitor_time = (SELECT MAX(monitor_time) FROM air_quality_data) " +
            "ORDER BY a.aqi DESC")
    List<Map<String, Object>> selectRealtimeAqi();

    @Select("SELECT a.*, s.station_name " +
            "FROM air_quality_data a " +
            "JOIN station s ON a.station_code = s.station_code " +
            "WHERE a.station_code = #{stationCode} " +
            "AND DATE(a.monitor_time) BETWEEN #{startDate} AND #{endDate} " +
            "ORDER BY a.monitor_time")
    List<Map<String, Object>> selectHistoryAqi(@Param("stationCode") String stationCode,
                                                @Param("startDate") LocalDate startDate,
                                                @Param("endDate") LocalDate endDate);

    @Select("SELECT DATE_FORMAT(monitor_time, '%Y-%m-%d') as date, " +
            "AVG(aqi) as avg_aqi, MAX(aqi) as max_aqi, MIN(aqi) as min_aqi " +
            "FROM air_quality_data " +
            "WHERE station_code = #{stationCode} " +
            "AND monitor_time >= DATE_SUB(NOW(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE_FORMAT(monitor_time, '%Y-%m-%d') " +
            "ORDER BY date")
    List<Map<String, Object>> selectTrend(@Param("stationCode") String stationCode,
                                           @Param("type") String type,
                                           @Param("days") Integer days);

    @Select("SELECT a.*, s.station_name, s.latitude, s.longitude " +
            "FROM air_quality_data a " +
            "JOIN station s ON a.station_code = s.station_code " +
            "WHERE a.station_code = #{stationCode} " +
            "ORDER BY a.monitor_time DESC LIMIT 1")
    Map<String, Object> selectStationAqi(@Param("stationCode") String stationCode);

    @Select("SELECT " +
            "COUNT(*) as total_count, " +
            "AVG(aqi) as avg_aqi, " +
            "MAX(aqi) as max_aqi, " +
            "MIN(aqi) as min_aqi, " +
            "SUM(CASE WHEN aqi <= 50 THEN 1 ELSE 0 END) as excellent_count, " +
            "SUM(CASE WHEN aqi > 50 AND aqi <= 100 THEN 1 ELSE 0 END) as good_count, " +
            "SUM(CASE WHEN aqi > 100 THEN 1 ELSE 0 END) as polluted_count " +
            "FROM air_quality_data " +
            "WHERE DATE(monitor_time) = #{date}")
    Map<String, Object> selectStatistics(@Param("date") LocalDate date);

    // 空间分析相关
    @Select("SELECT a.*, s.station_name, s.latitude, s.longitude " +
            "FROM air_quality_data a " +
            "JOIN station s ON a.station_code = s.station_code " +
            "WHERE DATE_FORMAT(a.monitor_time, '%Y-%m-%d %H:%i') = " +
            "DATE_FORMAT(#{time}, '%Y-%m-%d %H:%i')")
    List<Map<String, Object>> selectStationRealtimeData(@Param("time") LocalDateTime time);

    @Select("SELECT station_code, station_name, latitude, longitude, district_name " +
            "FROM station WHERE is_active = 1")
    List<Map<String, Object>> selectAllStations();

    @Select("<script>" +
            "SELECT a.*, s.station_name, s.latitude, s.longitude " +
            "FROM air_quality_data a JOIN station s ON a.station_code = s.station_code " +
            "WHERE a.station_code IN " +
            "<foreach collection='codes' item='code' open='(' separator=',' close=')' >#{code}</foreach> " +
            "AND DATE_FORMAT(a.monitor_time, '%Y-%m-%d %H:%i') = DATE_FORMAT(#{time}, '%Y-%m-%d %H:%i')" +
            "</script>")
    List<Map<String, Object>> selectStationComparison(@Param("codes") List<String> stationCodes,
                                                       @Param("time") LocalDateTime time);

    @Select("SELECT DATE_FORMAT(monitor_time, '%Y-%m-%d %H:%i') as time_point, " +
            "AVG(aqi) as avg_aqi, MAX(aqi) as max_aqi " +
            "FROM air_quality_data " +
            "WHERE monitor_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY DATE_FORMAT(monitor_time, '%Y-%m-%d %H:%i') " +
            "ORDER BY time_point")
    List<Map<String, Object>> selectTimelineData(@Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime,
                                                  @Param("pollutant") String pollutant);

    // 相关性分析相关
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
            "AND ${factorX} IS NOT NULL AND ${factorY} IS NOT NULL " +
            "<if test='stationCode != null'> AND station_code = #{stationCode} </if>")
    List<Map<String, Object>> selectScatterData(@Param("factorX") String factorX,
                                                 @Param("factorY") String factorY,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate,
                                                 @Param("stationCode") String stationCode);
}
