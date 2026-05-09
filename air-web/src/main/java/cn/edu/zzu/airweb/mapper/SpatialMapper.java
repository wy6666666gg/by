package cn.edu.zzu.airweb.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 空间分析数据访问层
 */
@Mapper
public interface SpatialMapper {

    @Select("SELECT station_code, station_name, latitude, longitude, " +
            "aqi, pm25, pm10, so2, no2, co, o3, monitor_time " +
            "FROM air_quality_data a " +
            "JOIN station s ON a.station_code = s.station_code " +
            "WHERE DATE_FORMAT(monitor_time, '%Y-%m-%d %H:%i') = " +
            "DATE_FORMAT(#{time}, '%Y-%m-%d %H:%i') " +
            "ORDER BY a.station_code")
    List<Map<String, Object>> selectStationRealtimeData(@Param("time") LocalDateTime time);

    @Select("SELECT station_code, station_name, latitude, longitude, district_name " +
            "FROM station WHERE is_active = 1 ORDER BY station_code")
    List<Map<String, Object>> selectAllStations();

    @Select("<script>" +
            "SELECT station_code, station_name, latitude, longitude, " +
            "aqi, pm25, pm10, so2, no2, co, o3 " +
            "FROM air_quality_data a JOIN station s ON a.station_code = s.station_code " +
            "WHERE a.station_code IN " +
            "<foreach collection='codes' item='code' open='(' separator=',' close=')' >#{code}</foreach> " +
            "AND DATE_FORMAT(monitor_time, '%Y-%m-%d %H:%i') = DATE_FORMAT(#{time}, '%Y-%m-%d %H:%i') " +
            "ORDER BY a.station_code" +
            "</script>")
    List<Map<String, Object>> selectStationComparison(@Param("codes") List<String> stationCodes, 
                                                       @Param("time") LocalDateTime time);

    @Select("SELECT DATE_FORMAT(monitor_time, '%Y-%m-%d %H:%i') as time_point, " +
            "AVG(${pollutant}) as avg_value, MAX(${pollutant}) as max_value " +
            "FROM air_quality_data " +
            "WHERE monitor_time BETWEEN #{startTime} AND #{endTime} " +
            "GROUP BY DATE_FORMAT(monitor_time, '%Y-%m-%d %H:%i') " +
            "ORDER BY time_point")
    List<Map<String, Object>> selectTimelineData(@Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime,
                                                  @Param("pollutant") String pollutant);
}
