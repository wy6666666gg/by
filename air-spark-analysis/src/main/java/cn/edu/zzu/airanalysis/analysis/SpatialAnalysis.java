package cn.edu.zzu.airanalysis.analysis;

import org.apache.spark.sql.*;
import org.apache.spark.sql.types.*;
import org.apache.spark.sql.functions.*;
import org.apache.spark.sql.expressions.Window;

import java.util.*;

/**
 * 时空分析模块
 * 功能：计算不同粒度的统计指标，生成污染分布热力图数据
 */
public class SpatialAnalysis {

    private final SparkSession spark;

    public SpatialAnalysis(SparkSession spark) {
        this.spark = spark;
    }

    /**
     * 1. 区域统计（按区县/站点/时间维度聚合）
     */
    public Dataset<Row> districtStatistics(String startDate, String endDate) {
        String dwsPath = "/user/hive/warehouse/air_quality_db.db/dws_station_hour";

        Dataset<Row> df = spark.read().format("orc").load(dwsPath)
            .filter("day >= '" + startDate + "' and day <= '" + endDate + "'");

        return df.groupBy("station_code", "station_name", "year", "month", "day")
            .agg(
                avg("pm25_avg").as("pm25_avg"),
                avg("pm10_avg").as("pm10_avg"),
                avg("aqi_avg").as("aqi_avg"),
                max("aqi_max").as("aqi_max"),
                min("aqi_min").as("aqi_min"),
                sum(when(col("aqi_avg").leq(100), 1).otherwise(0)).as("clean_hours"),
                count("*").as("total_hours")
            )
            .withColumn("clean_rate", expr("clean_hours * 100.0 / total_hours"))
            .orderBy(desc("aqi_avg"));
    }

    /**
     * 2. 时间趋势分析（月度/季节趋势）
     */
    public Dataset<Row> timeTrendAnalysis(String districtCode) {
        String dwsPath = "/user/hive/warehouse/air_quality_db.db/dws_station_hour";

        Dataset<Row> df = spark.read().format("orc").load(dwsPath);

        Dataset<Row> aggregated;
        if ("all".equals(districtCode)) {
            aggregated = df.groupBy("year", "month");
        } else {
            aggregated = df.filter("station_code like '" + districtCode + "%'")
                .groupBy("year", "month");
        }

        return aggregated.agg(
                avg("pm25_avg").as("pm25_avg"),
                avg("pm10_avg").as("pm10_avg"),
                avg("aqi_avg").as("aqi_avg"),
                avg("o3_avg").as("o3_avg"),
                stddev("aqi_avg").as("aqi_std")
            )
            .withColumn("period", concat(col("year"), lit("-"), lpad(col("month"), 2, "0")))
            .orderBy("year", "month");
    }

    /**
     * 3. 时段分析（24小时分布）
     */
    public Dataset<Row> hourlyDistribution(String stationCode, int dateRange) {
        String dwsPath = "/user/hive/warehouse/air_quality_db.db/dws_station_hour";

        return spark.read().format("orc").load(dwsPath)
            .filter("station_code = '" + stationCode + "'")
            .filter("day >= date_sub(current_date(), " + dateRange + ")")
            .groupBy("hour")
            .agg(
                avg("pm25_avg").as("avg_pm25"),
                avg("pm10_avg").as("avg_pm10"),
                avg("aqi_avg").as("avg_aqi")
            )
            .orderBy("hour");
    }

    /**
     * 4. 空间分布（生成热力图数据）
     */
    public Dataset<Row> spatialDistribution(String date) {
        String dimStationPath = "/user/hive/warehouse/air_quality_db.db/dim_station";
        String dwdPath = "/user/hive/warehouse/air_quality_db.db/dwd_air_quality_dt";

        // 读取站点维度信息
        Dataset<Row> stations = spark.read().format("orc").load(dimStationPath);

        // 读取当日空气质量数据
        Dataset<Row> airData = spark.read().format("orc").load(dwdPath)
            .filter("day = '" + date + "'")
            .groupBy("station_code")
            .agg(
                avg("pm25").as("pm25"),
                avg("pm10").as("pm10"),
                avg("aqi").as("aqi")
            );

        // 关联站点坐标
        Dataset<Row> geoData = stations.join(airData, "station_code")
            .select(
                col("station_code"),
                col("station_name"),
                col("latitude"),
                col("longitude"),
                col("pm25"),
                col("pm10"),
                col("aqi")
            );

        return generateGridHeatmap(geoData);
    }

    /**
     * 生成网格热力图数据
     */
    private Dataset<Row> generateGridHeatmap(Dataset<Row> stationData) {
        double gridSize = 0.01;

        // 获取站点边界范围
        Row bounds = stationData.agg(
            min("latitude").as("min_lat"),
            max("latitude").as("max_lat"),
            min("longitude").as("min_lon"),
            max("longitude").as("max_lon")
        ).head();

        double minLat = bounds.getDouble(0);
        double maxLat = bounds.getDouble(1);
        double minLon = bounds.getDouble(2);
        double maxLon = bounds.getDouble(3);

        // 计算网格点（简化版）
        List<Row> gridPoints = new ArrayList<>();
        for (double lat = minLat; lat <= maxLat; lat += gridSize) {
            for (double lon = minLon; lon <= maxLon; lon += gridSize) {
                gridPoints.add(RowFactory.create(lat, lon, 0.0, 0.0));
            }
        }

        StructType schema = new StructType(new StructField[]{
            new StructField("latitude", DataTypes.DoubleType, false, Metadata.empty()),
            new StructField("longitude", DataTypes.DoubleType, false, Metadata.empty()),
            new StructField("pm25_interpolated", DataTypes.DoubleType, false, Metadata.empty()),
            new StructField("aqi_interpolated", DataTypes.DoubleType, false, Metadata.empty())
        });

        return spark.createDataFrame(gridPoints, schema);
    }

    /**
     * 5. 周期性分析（周/月/年周期）
     */
    public Dataset<Row> periodicAnalysis() {
        String dwsPath = "/user/hive/warehouse/air_quality_db.db/dws_station_hour";

        Dataset<Row> df = spark.read().format("orc").load(dwsPath);

        // 周内规律
        Dataset<Row> weeklyPattern = df.groupBy("day_of_week")
            .agg(
                avg("aqi_avg").as("avg_aqi"),
                avg("pm25_avg").as("avg_pm25")
            )
            .withColumn("pattern_type", lit("weekly"));

        // 小时规律
        Dataset<Row> hourlyPattern = df.groupBy("hour")
            .agg(
                avg("aqi_avg").as("avg_aqi"),
                avg("pm25_avg").as("avg_pm25")
            )
            .withColumn("pattern_type", lit("hourly"));

        return weeklyPattern.union(hourlyPattern);
    }

    /**
     * 生成空间分析报告
     */
    public void generateReport(String startDate, String endDate) {
        System.out.println("=" + "=".repeat(60));
        System.out.println("空气质量时空分析报告");
        System.out.println("=" + "=".repeat(60));

        // 区域统计
        System.out.println("\n【区域统计】");
        districtStatistics(startDate, endDate).show(10);

        // 时间趋势
        System.out.println("\n【月度趋势】");
        timeTrendAnalysis("all").show(12);

        // 时段分析
        System.out.println("\n【24小时分布】");
        hourlyDistribution("410101", 30).show(24);

        // 空间分布
        System.out.println("\n【空间分布】");
        spatialDistribution(startDate).show(20);

        System.out.println("=" + "=".repeat(60));
    }

    public static void main(String[] args) {
        SparkSession spark = SparkSession.builder()
            .appName("SpatialAnalysis")
            .master("yarn")
            .enableHiveSupport()
            .getOrCreate();

        SpatialAnalysis analysis = new SpatialAnalysis(spark);
        analysis.generateReport("2024-01-01", "2024-01-31");

        spark.stop();
    }
}